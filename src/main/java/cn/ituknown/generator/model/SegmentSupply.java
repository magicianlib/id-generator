package cn.ituknown.generator.model;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;

/**
 * 一次领段的完整供给: 可发号区间与该标签生效的缓存水位, 水位随每次领段回传, 支持行内配置即时生效
 */
public class SegmentSupply implements Serializable {

    private static final long serialVersionUID = -4736389298738455015L;

    /**
     * 可发号区间, 闭区间两端
     */
    private final Pair<Long, Long> range;

    /**
     * 该标签生效的缓存段数下限, 为空表示未携带
     */
    private final Integer minLimit;

    /**
     * 该标签生效的缓存段数上限, 为空表示未携带
     */
    private final Integer maxLimit;

    public SegmentSupply(Pair<Long, Long> range, Integer minLimit, Integer maxLimit) {
        this.range = range;
        this.minLimit = minLimit;
        this.maxLimit = maxLimit;
    }

    public Pair<Long, Long> getRange() {
        return range;
    }

    public Integer getMinLimit() {
        return minLimit;
    }

    public Integer getMaxLimit() {
        return maxLimit;
    }
}
