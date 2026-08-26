package cn.ituknown.generator.model;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段, 持有一段可发号区间, 区间耗尽后需申请下一档
 */
public class IdSegment {

    private final AtomicLong begin;
    private final Long end;

    public IdSegment(Pair<Long, Long> range) {
        this.begin = new AtomicLong(range.getLeft());
        this.end = range.getRight();
    }

    public Long take() {
        if (isExhausted()) {
            return null;
        }

        long seq = begin.getAndIncrement();
        if (seq <= end) {
            return seq;
        } else {
            return null;
        }
    }

    /**
     * 批量获取指定数量的序列
     */
    public List<Long> take(int size) {
        if (isExhausted()) {
            return Collections.emptyList();
        }

        List<Long> seq = new ArrayList<>();

        long begin = this.begin.getAndAdd(size);
        long end = (begin + size);

        for (; begin < end; ++begin) {
            if (begin <= this.end) {
                seq.add(begin);
            } else {
                break;
            }
        }

        return seq;
    }

    /**
     * 是否已耗尽
     */
    public boolean isExhausted() {
        return begin.get() > end;
    }
}