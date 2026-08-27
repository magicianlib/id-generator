package cn.ituknown.generator.repository;

import cn.ituknown.generator.mapper.IdSegmentMapper;
import cn.ituknown.generator.model.IdSegmentKey;
import cn.ituknown.generator.model.SegmentSupply;
import cn.ituknown.generator.po.IdSegmentPo;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.PageSegmentRequest;
import cn.ituknown.generator.result.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
public class IdSegmentRepository {
    private static final String FIND_SEGMENT_FAILURE_MESSAGE = "cannot find `bizGroup`: '%s', `bizTag`: '%s', please apply first.";
    private static final String FIND_TAG_FAILURE_MESSAGE = "cannot find `bizTag`: '%s' under `bizGroup`: '%s', please apply tag first.";
    private static final String ILLEGAL_CACHE_LIMIT_MESSAGE = "illegal cache limits: min %s > max %s, please adjust.";

    @Value("${id.segment.init_id}")
    private Long idSegmentInitId;

    @Value("${id.segment.step}")
    private Long idSegmentStep;

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
     * 申请新号段; 所属业务组与业务名必须已登记, 已存在同名号段时幂等返回, 入参缺省时以默认初始值和默认步阶补齐, 缓存水位下限不得大于上限, 创建与更新时间由数据库生成
     */
    public void apply(ApplyIdSegmentRequest request) {
        if (Objects.nonNull(request.getCacheMinLimit()) && Objects.nonNull(request.getCacheMaxLimit())
                && request.getCacheMinLimit() > request.getCacheMaxLimit()) {
            throw new RuntimeException(String.format(ILLEGAL_CACHE_LIMIT_MESSAGE, request.getCacheMinLimit(), request.getCacheMaxLimit()));
        }

        if (Objects.isNull(request.getCurrentMaxId())) {
            request.setCurrentMaxId(idSegmentInitId);
        }
        if (Objects.isNull(request.getStep())) {
            request.setStep(idSegmentStep);
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
        record.setCacheMinLimit(request.getCacheMinLimit());
        record.setCacheMaxLimit(request.getCacheMaxLimit());
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
     * 分页查询已申请号段, 业务组与业务名条件存在时精确匹配, 结果按登记顺序排列
     */
    public Page<IdSegmentPo> page(PageSegmentRequest request) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IdSegmentPo> recordPage =
                new LambdaQueryChainWrapper<>(idSegmentMapper)
                        .eq(StringUtils.isNotBlank(request.getBizGroup()), IdSegmentPo::getBizGroup, request.getBizGroup())
                        .eq(StringUtils.isNotBlank(request.getBizTag()), IdSegmentPo::getBizTag, request.getBizTag())
                        .orderByDesc(IdSegmentPo::getId)
                        .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getCurrent(), request.getPageSize()));

        return Page.of(recordPage.getRecords(), request.getCurrent(), request.getPageSize(), recordPage.getTotal());
    }

    /**
     * 领取下一档号段的完整供给: 按已分配最大值加步阶推进, 带原值校验的更新失败时重试直至成功, 推进时显式刷新更新时间(UTC); 该标签行内登记的缓存水位随供给回传
     */
    public SegmentSupply nextSegmentRange(String bizGroup, String bizTag) {

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
                    .set(IdSegmentPo::getUpdatedAt, OffsetDateTime.now(ZoneOffset.UTC))
                    .eq(IdSegmentPo::getBizGroup, bizGroup)
                    .eq(IdSegmentPo::getBizTag, bizTag)
                    .eq(IdSegmentPo::getCurrentMaxId, current.getCurrentMaxId());

            update = idSegmentMapper.update(null, updateWrapper) > 0;
        } while (!update); // 更新失败说明被并发抢占, 重新读取后重试

        return new SegmentSupply(Pair.of(current.getCurrentMaxId() + 1, newMaxId), current.getCacheMinLimit(), current.getCacheMaxLimit());
    }
}