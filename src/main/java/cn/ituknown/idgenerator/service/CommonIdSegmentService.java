package cn.ituknown.idgenerator.service;

import cn.ituknown.idgenerator.enums.IdSegmentTypeEnum;
import cn.ituknown.idgenerator.model.IdSegmentKey;
import cn.ituknown.idgenerator.repository.IdSegmentRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CommonIdSegmentService extends AbstractIdSegmentService {

    @Resource
    private TaskExecutor taskExecutor;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    @Override
    protected IdSegmentTypeEnum type() {
        return IdSegmentTypeEnum.COMMON;
    }

    @Override
    protected List<IdSegmentKey> segmentList() {
        return idSegmentRepository.findAllSegments();
    }

    @Override
    protected Pair<Long, Long> nextSegmentRange(String bizGroup, String bizTag) {
        return idSegmentRepository.nextSegmentRange(bizGroup, bizTag);
    }

    @Override
    protected TaskExecutor executor() {
        return taskExecutor;
    }

    @Override
    protected long timerPeriod() {
        //return 1000 * 60 * 60; // 1 hour.
        return 1000 * 10; // 10 Second.
    }
}