package cn.ituknown.idgenerator.repository;

import cn.ituknown.idgenerator.mapper.IdSegmentMapper;
import cn.ituknown.idgenerator.model.IdSegmentKey;
import cn.ituknown.idgenerator.po.IdSegmentPo;
import cn.ituknown.idgenerator.request.ApplyIdSegmentRequest;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class IdSegmentRepository {
    private static final String FIND_SEGMENT_FAILURE_MESSAGE = "cannot find `bizGroup`: '%s', `bizTag`: '%s', please apply first.";
    private static final String FIND_TAG_FAILURE_MESSAGE = "cannot find `bizTag`: '%s' under `bizGroup`: '%s', please apply tag first.";

    @Value("${id.segment.default.init_id}")
    private Long idSegmentDefaultInitId;

    @Value("${id.segment.default.step}")
    private Long idSegmentDefaultStep;

    @Resource
    private IdSegmentMapper idSegmentMapper;

    @Resource
    private IdTagRepository idTagRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    public IdSegmentPo get(String bizGroup, String bizTag) {
        return new LambdaQueryChainWrapper<>(idSegmentMapper)
                .eq(IdSegmentPo::getBizGroup, bizGroup)
                .eq(IdSegmentPo::getBizTag, bizTag)
                .last("limit 1")
                .one();
    }

    /**
     * 申请新号段; 所属业务组与业务名必须已登记, 已存在同名号段时幂等返回, 入参缺省时以默认初始值和默认步阶补齐, 创建与更新时间由数据库生成
     */
    public void apply(ApplyIdSegmentRequest request) {
        if (Objects.isNull(request.getCurrentMaxId())) {
            request.setCurrentMaxId(idSegmentDefaultInitId);
        }
        if (Objects.isNull(request.getStep())) {
            request.setStep(idSegmentDefaultStep);
        }

        if (Objects.nonNull(get(request.getBizGroup(), request.getBizTag()))) {
            return;
        }

        if (Objects.isNull(idTagRepository.get(request.getBizGroup(), request.getBizTag()))) {
            throw new RuntimeException(String.format(FIND_TAG_FAILURE_MESSAGE, request.getBizTag(), request.getBizGroup()));
        }

        IdSegmentPo record = new IdSegmentPo();
        record.setBizGroup(request.getBizGroup());
        record.setBizTag(request.getBizTag());
        record.setCurrentMaxId(request.getCurrentMaxId());
        record.setStep(request.getStep());
        record.setDescription(request.getDescription());

        try {
            idSegmentMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发申请同名业务段发生插入冲突, 说明他人已抢先登记, 视为幂等成功
        }
    }

    /**
     * 查询全部已申请的号段维度
     */
    public List<IdSegmentKey> findAllSegments() {
        return idSegmentMapper.selectList(null).stream()
                .map(po -> new IdSegmentKey(po.getBizGroup(), po.getBizTag()))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 领取下一档号段的可发号区间: 按已分配最大值加步阶推进, 带原值校验的更新失败时重试直至成功, 更新时间由数据库自动刷新
     */
    public Pair<Long, Long> nextSegmentRange(String bizGroup, String bizTag) {

        boolean update;
        long newMaxId;
        IdSegmentPo current;
        do {
            if (Objects.isNull(current = get(bizGroup, bizTag))) {
                throw new RuntimeException(String.format(FIND_SEGMENT_FAILURE_MESSAGE, bizGroup, bizTag));
            }

            // 新一档已分配最大值 = 当前值 + 步阶
            newMaxId = (current.getCurrentMaxId() + current.getStep());

            LambdaUpdateWrapper<IdSegmentPo> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(IdSegmentPo::getCurrentMaxId, newMaxId)
                    .eq(IdSegmentPo::getBizGroup, bizGroup)
                    .eq(IdSegmentPo::getBizTag, bizTag)
                    .eq(IdSegmentPo::getCurrentMaxId, current.getCurrentMaxId());

            update = idSegmentMapper.update(null, updateWrapper) > 0;
        } while (!update); // 更新失败说明被并发抢占, 重新读取后重试

        return Pair.of(current.getCurrentMaxId() + 1, newMaxId);
    }
}