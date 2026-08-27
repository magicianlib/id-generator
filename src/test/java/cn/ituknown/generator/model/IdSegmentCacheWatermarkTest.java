package cn.ituknown.generator.model;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 缓存水位验证: 领段结果携带的每标签配置应约束构造期预取数量
 */
class IdSegmentCacheWatermarkTest {

    @Test
    void bootstrapFillRespectsPerTagMaxLimit() {
        AtomicInteger fetched = new AtomicInteger();
        BiFunction<String, String, SegmentSupply> fetcher = (bizGroup, bizTag) -> {
            // 每次领段给出 10 个号的区间, 并声明该标签的缓存水位为下限 1、上限 2
            long base = fetched.incrementAndGet() * 10L;
            return new SegmentSupply(Pair.of(base, base + 9), 1, 2);
        };

        new IdSegmentCache("benchmark", "watermark", fetcher, Runnable::run);

        assertEquals(2, fetched.get(), "构造期预取应止于标签配置的缓存上限 2 段");
    }
}
