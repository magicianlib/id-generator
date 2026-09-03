package cn.ituknown.generator.model;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 跨段批量取号验证: 批量需求跨多个号段满足时, 不应回拨后续号段的消费进度, 避免重复发号甚至越界发号
 */
class IdSegmentCacheBatchTakeTest {

    @Test
    void batchTakeAcrossSegmentsKeepsSequenceMonotonic() {
        AtomicInteger fetched = new AtomicInteger();
        BiFunction<String, String, SegmentSupply> fetcher = (bizGroup, bizTag) -> {
            // 每次领段供给 100 个号, 该标签缓存水位为下限 1、上限 4 段
            long base = fetched.incrementAndGet() * 100L;
            return new SegmentSupply(Pair.of(base, base + 99), 1, 4);
        };

        IdSegmentCache cache = new IdSegmentCache("benchmark", "batch", fetcher, Runnable::run);

        // 一次批量取 250 个, 需跨前三个号段凑齐, 应得自 100 起连续的 250 个号
        List<Long> batch = cache.take(250);
        assertEquals(250, batch.size(), "批量取号应足额");
        assertEquals(Long.valueOf(100L), batch.get(0), "批量首个号应为缓存内最早号段的起始号");
        assertEquals(Long.valueOf(349L), batch.get(249), "批量末个号应与前序连续无重号");

        // 批量取号后单个取号应继续顺延, 不得回头重发已发过的号
        assertEquals(Long.valueOf(350L), cache.take());
    }
}
