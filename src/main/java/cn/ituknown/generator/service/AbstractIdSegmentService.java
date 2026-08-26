package cn.ituknown.generator.service;

import cn.ituknown.generator.enums.IdSegmentTypeEnum;
import cn.ituknown.generator.model.IdSegmentCache;
import cn.ituknown.generator.model.IdSegmentKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractIdSegmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIdSegmentService.class);

    private static final String CACHE_KEY_SEPARATOR = ":";

    private final ReentrantLock LOCK = new ReentrantLock();

    /**
     * 号段缓存, 以业务组与业务名的组合标识为键
     */
    private final Map<String, IdSegmentCache> segmentCache = new ConcurrentHashMap<>();

    /**
     * 号段业务类型
     */
    protected abstract IdSegmentTypeEnum type();

    /**
     * 业务类型下全部已申请的号段维度
     */
    protected abstract List<IdSegmentKey> segmentList();

    /**
     * 为指定业务组与业务名申请下一档号段区间
     */
    protected abstract Pair<Long, Long> nextSegmentRange(String bizGroup, String bizTag);

    /**
     * 线程池
     */
    protected abstract TaskExecutor executor();

    /**
     * 定时同步号段周期(ms)
     */
    protected abstract long timerPeriod();

    /**
     * 定时同步号段
     */
    @PostConstruct
    public void initTimer() {
        initCacheList(segmentList());
        Timer timer = new Timer(type().name(), true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    initCacheList(segmentList());
                } catch (Exception e) {
                    LOGGER.error("timer schedule initCacheList failure: {}", e.getMessage(), e);
                }
            }
        }, timerPeriod(), timerPeriod());
    }

    private void initCacheList(List<IdSegmentKey> segmentList) {
        removeCacheIfAbandoned(segmentList);
        CountDownLatch countDownLatch = new CountDownLatch(segmentList.size());
        try {
            for (IdSegmentKey segment : segmentList) {
                executor().execute(() -> {
                    try {
                        LOGGER.info("initCacheIfNeed(bizGroup='{}', bizTag='{}')...", segment.getBizGroup(), segment.getBizTag());
                        initCacheIfNeed(segment.getBizGroup(), segment.getBizTag());
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            }
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("initCacheList failure: {}", e.getMessage(), e);
        }
    }

    private void initCacheIfNeed(String bizGroup, String bizTag) {
        String cacheKey = cacheKey(bizGroup, bizTag);
        if (!segmentCache.containsKey(cacheKey)) { // 双重检查, 避免重复初始化
            LOCK.lock();
            try {
                if (!segmentCache.containsKey(cacheKey)) {
                    LOGGER.info("initCache(bizGroup='{}', bizTag='{}')", bizGroup, bizTag);
                    segmentCache.put(cacheKey, new IdSegmentCache(bizGroup, bizTag, this::nextSegmentRange, executor()));
                }
            } finally {
                LOCK.unlock();
            }
        }
    }

    /**
     * 移除已废弃号段的缓存
     */
    private void removeCacheIfAbandoned(List<IdSegmentKey> segmentList) {
        Set<String> availableKeys = new HashSet<>();
        for (IdSegmentKey segment : segmentList) {
            availableKeys.add(cacheKey(segment.getBizGroup(), segment.getBizTag()));
        }

        Collection<String> subtract = CollectionUtils.subtract(segmentCache.keySet(), availableKeys);
        LOGGER.info("removeCacheIfAbandoned, subtract={}", subtract);
        if (CollectionUtils.isNotEmpty(subtract)) {
            LOCK.lock();
            try {
                subtract = CollectionUtils.subtract(segmentCache.keySet(), availableKeys);
                if (CollectionUtils.isNotEmpty(subtract)) {
                    for (String cacheKey : subtract) {
                        if (segmentCache.containsKey(cacheKey)) {
                            final ReentrantReadWriteLock rw = segmentCache.get(cacheKey).lock();
                            if (rw.getReadLockCount() == 0 && rw.getWriteHoldCount() == 0) {
                                LOGGER.info("segment='{}' has been abandoned, remove!", cacheKey);
                                segmentCache.remove(cacheKey);
                            }
                        }
                    }
                }
            } finally {
                LOCK.unlock();
            }
        }
    }

    public boolean exist(String bizGroup, String bizTag) {
        return segmentCache.containsKey(cacheKey(bizGroup, bizTag));
    }

    public Long take(String bizGroup, String bizTag) {
        initCacheIfNeed(bizGroup, bizTag);
        IdSegmentCache cache = segmentCache.get(cacheKey(bizGroup, bizTag));
        return cache.take();
    }

    public List<Long> take(String bizGroup, String bizTag, int size) {
        initCacheIfNeed(bizGroup, bizTag);
        IdSegmentCache cache = segmentCache.get(cacheKey(bizGroup, bizTag));
        return cache.take(size);
    }

    private String cacheKey(String bizGroup, String bizTag) {
        return bizGroup + CACHE_KEY_SEPARATOR + bizTag;
    }
}