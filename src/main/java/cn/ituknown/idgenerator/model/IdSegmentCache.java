package cn.ituknown.idgenerator.model;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * 号段缓存, 按业务组与业务名维度的发号缓冲: 优先消费已缓存号段, 余量不足时异步向存储层申请补充
 */
public class IdSegmentCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdSegmentCache.class);

    /**
     * 安全号段数量下限, 低于该余量触发异步补充
     */
    private static final int SEGMENT_MIN_LIMIT = 3;

    /**
     * 号段数量上限, 防止预取过多造成浪费
     */
    private static final int SEGMENT_MAX_LIMIT = 16;

    /**
     * 业务组
     */
    private final String bizGroup;

    /**
     * 业务名
     */
    private final String bizTag;

    /**
     * 号段区间申请函数, 入参为业务组与业务名
     */
    private final BiFunction<String, String, Pair<Long, Long>> fetcher;

    private final TaskExecutor executor;

    private final List<IdSegment> segments = new ArrayList<>();

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    private final AtomicBoolean supplementing = new AtomicBoolean(false);

    public IdSegmentCache(String bizGroup, String bizTag, BiFunction<String, String, Pair<Long, Long>> fetcher, TaskExecutor executor) {
        this.bizGroup = bizGroup;
        this.bizTag = bizTag;
        this.fetcher = fetcher;
        this.executor = executor;
        try {
            syncSupplementSegment(); // 同步填充号段
        } catch (Exception e) {
            LOGGER.error("syncSupplementSegment failure: {}", e.getMessage(), e);
        }
    }

    public Long take() {
        rw.readLock().lock();
        boolean exhausted = segments.size() < SEGMENT_MIN_LIMIT;
        try {
            Long result = null;
            for (IdSegment segment : segments) {
                result = segment.take();
                if (result == null || segment.isExhausted()) {
                    exhausted = true;
                }
                if (result != null) {
                    break;
                }
            }
            return result;
        } finally {
            rw.readLock().unlock();
            if (exhausted) { // 余量不足, 异步补充号段
                LOGGER.info("take: [{}:{}] is exhausted!, asyncSupplementSegment...", bizGroup, bizTag);
                asyncSupplementSegment();
            }
        }
    }

    public List<Long> take(int size) {
        rw.readLock().lock();
        List<Long> result = new ArrayList<>();
        boolean exhausted = segments.size() < SEGMENT_MIN_LIMIT;
        try {
            int s = size;
            for (IdSegment segment : segments) {
                result.addAll(segment.take(s));
                s = s - result.size();
                if (segment.isExhausted()) {
                    exhausted = true;
                }
                if (result.size() == size) {
                    break;
                }
            }
            return result;
        } finally {
            rw.readLock().unlock();
            if (exhausted) {
                LOGGER.info("take(int): [{}:{}] is exhausted!, asyncSupplementSegment...", bizGroup, bizTag);
                asyncSupplementSegment();
            }
        }
    }

    /**
     * 异步补充号段: 耗尽段清理也放在补充任务内执行, 取号路径只提交任务不竞争写锁, 避免高并发取号时写锁风暴拖慢补充
     */
    private void asyncSupplementSegment() {
        if (supplementing.compareAndSet(false, true)) {
            executor.execute(() -> {
                try {
                    removeExhaustedSegment();
                    syncSupplementSegment();
                } catch (Exception e) {
                    LOGGER.error("asyncSupplementSegment failure: {}", e.getMessage(), e);
                } finally {
                    supplementing.set(false);
                }
            });
        }
    }

    /**
     * 同步补充号段直至达到数量上限
     */
    private void syncSupplementSegment() {
        do {
            // 向存储层申请新号段区间
            Pair<Long, Long> range = fetcher.apply(bizGroup, bizTag);
            if (Objects.nonNull(range)) {
                supplementSegment(range);
            }
        } while (segments.size() < SEGMENT_MAX_LIMIT);
    }

    /**
     * 补充号段
     */
    private void supplementSegment(Pair<Long, Long> range) {
        rw.writeLock().lock();
        try {
            segments.add(new IdSegment(range));
        } finally {
            rw.writeLock().unlock();
        }
    }

    /**
     * 移除已耗尽的号段
     */
    private void removeExhaustedSegment() {
        rw.writeLock().lock();
        try {
            segments.removeIf(IdSegment::isExhausted);
        } finally {
            rw.writeLock().unlock();
        }
    }

    final public ReentrantReadWriteLock lock() {
        return rw;
    }
}