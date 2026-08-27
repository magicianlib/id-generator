package cn.ituknown.generator.model;

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
     * 缓存段数下限的引导值, 与登记时的库内默认下限保持一致, 首次领段后以该标签的行内配置覆盖
     */
    private static final int DEFAULT_MIN_LIMIT = 3;

    /**
     * 缓存段数上限的引导值, 与登记时的库内默认上限保持一致, 首次领段后以该标签的行内配置覆盖
     */
    private static final int DEFAULT_MAX_LIMIT = 16;

    /**
     * 同步兜底等待在途补充的时间上限(ms)
     */
    private static final long SUPPLEMENT_WAIT_LIMIT_MILLIS = 100;

    /**
     * 业务组
     */
    private final String bizGroup;

    /**
     * 业务名
     */
    private final String bizTag;

    /**
     * 号段供给申请函数, 入参为业务组与业务名, 返回可发号区间与该标签生效的缓存水位
     */
    private final BiFunction<String, String, SegmentSupply> fetcher;

    private final TaskExecutor executor;

    /**
     * 生效中的缓存段数下限, 每次领段后以行内配置刷新
     */
    private volatile int minLimit = DEFAULT_MIN_LIMIT;

    /**
     * 生效中的缓存段数上限, 每次领段后以行内配置刷新
     */
    private volatile int maxLimit = DEFAULT_MAX_LIMIT;

    private final List<IdSegment> segments = new ArrayList<>();

    private final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    private final AtomicBoolean supplementing = new AtomicBoolean(false);

    public IdSegmentCache(String bizGroup, String bizTag, BiFunction<String, String, SegmentSupply> fetcher, TaskExecutor executor) {
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
        Long result = tryTake();
        if (Objects.nonNull(result)) {
            return result;
        }
        // 未取到号, 同步兜底补充一次后重试
        supplementImmediately();
        return tryTake();
    }

    public List<Long> take(int size) {
        List<Long> result = new ArrayList<>(tryTake(size));
        while (result.size() < size) {
            // 不足额, 同步兜底补充后补齐差额, 已取部分不丢弃
            int before = result.size();
            supplementImmediately();
            result.addAll(tryTake(size - result.size()));
            if (result.size() == before) {
                break; // 补充后仍无进展, 存储层暂时取不到号, 返回已凑部分
            }
        }
        return result;
    }

    /**
     * 读锁内尝试单个取号, 余量不足时顺带触发异步预取
     */
    private Long tryTake() {
        rw.readLock().lock();
        boolean exhausted = segments.size() < minLimit;
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
            if (exhausted) { // 余量不足, 异步预取号段
                asyncSupplementSegment();
            }
        }
    }

    /**
     * 读锁内尝试批量取号, 余量不足时顺带触发异步预取
     */
    private List<Long> tryTake(int size) {
        rw.readLock().lock();
        List<Long> result = new ArrayList<>();
        boolean exhausted = segments.size() < minLimit;
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
            if (exhausted) { // 余量不足, 异步预取号段
                asyncSupplementSegment();
            }
        }
    }

    /**
     * 同步兜底补充: 取号未满足结果时由当前线程立即补满缓冲。抢到补充权的线程执行清理与领段;
     * 未抢到的说明补充已在途, 短暂等待其完成, 等待后仍未取得号则再抢一轮补充权,
     * 避免等待结束时号被其他等待线程抢先取空而始终拿不到结果。等待有上限避免调用方无限阻塞
     */
    private void supplementImmediately() {
        for (int round = 0; round < 2; round++) {
            if (supplementing.compareAndSet(false, true)) {
                try {
                    supplementNow();
                } catch (Exception e) {
                    LOGGER.error("supplementImmediately failure: {}", e.getMessage(), e);
                } finally {
                    supplementing.set(false);
                }
                return;
            }

            long deadline = System.currentTimeMillis() + SUPPLEMENT_WAIT_LIMIT_MILLIS;
            while (System.currentTimeMillis() < deadline && supplementing.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * 异步补充号段: 耗尽段清理也放在补充任务内执行, 取号路径只提交任务不竞争写锁, 避免高并发取号时写锁风暴拖慢补充
     */
    private void asyncSupplementSegment() {
        if (supplementing.compareAndSet(false, true)) {
            LOGGER.info("asyncSupplementSegment: [{}:{}]...", bizGroup, bizTag);
            executor.execute(() -> {
                try {
                    supplementNow();
                } catch (Exception e) {
                    LOGGER.error("asyncSupplementSegment failure: {}", e.getMessage(), e);
                } finally {
                    supplementing.set(false);
                }
            });
        }
    }

    /**
     * 清理耗尽段并补充至数量上限
     */
    private void supplementNow() {
        removeExhaustedSegment();
        syncSupplementSegment();
    }

    /**
     * 同步补充号段直至达到数量上限, 先查水位再领段, 避免并发补充时领段过量
     */
    private void syncSupplementSegment() {
        while (segments.size() < maxLimit) {
            // 向存储层申请新号段区间, 供给同时携带该标签生效的缓存水位
            SegmentSupply supply = fetcher.apply(bizGroup, bizTag);
            if (Objects.isNull(supply)) {
                break; // 未取得区间, 结束本轮补充
            }
            refreshWatermark(supply);
            supplementSegment(supply.getRange());
        }
    }

    /**
     * 以领段供给携带的行内配置刷新生效水位, 未携带时维持原值, 支持调优后下次补段即生效
     */
    private void refreshWatermark(SegmentSupply supply) {
        if (Objects.nonNull(supply.getMinLimit())) {
            this.minLimit = supply.getMinLimit();
        }
        if (Objects.nonNull(supply.getMaxLimit())) {
            this.maxLimit = supply.getMaxLimit();
        }
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