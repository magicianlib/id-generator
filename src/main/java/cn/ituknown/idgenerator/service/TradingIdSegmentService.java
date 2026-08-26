package cn.ituknown.idgenerator.service;

import cn.ituknown.idgenerator.enums.IdSegmentTypeEnum;
import cn.ituknown.idgenerator.model.IdSegmentKey;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.task.TaskExecutor;

import java.util.Collections;
import java.util.List;

// @Service
public class TradingIdSegmentService extends AbstractIdSegmentService {

    @Override
    protected IdSegmentTypeEnum type() {
        return IdSegmentTypeEnum.TRADING;
    }

    @Override
    protected List<IdSegmentKey> segmentList() {
        return Collections.emptyList();
    }

    @Override
    protected Pair<Long, Long> nextSegmentRange(String bizGroup, String bizTag) {
        // TODO: Depending on the business
        return null;
    }

    @Override
    protected TaskExecutor executor() {
        // TODO: Depending on the business
        return null;
    }

    @Override
    protected long timerPeriod() {
        // TODO: Depending on the business
        return 0L;
    }
}