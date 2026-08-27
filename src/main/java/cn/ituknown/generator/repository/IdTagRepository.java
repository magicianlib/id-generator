package cn.ituknown.generator.repository;

import cn.ituknown.generator.mapper.IdTagMapper;
import cn.ituknown.generator.po.IdTagPo;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.PageTagRequest;
import cn.ituknown.generator.result.Page;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Objects;

@Repository
public class IdTagRepository {

    private static final String FIND_GROUP_FAILURE_MESSAGE = "cannot find `bizGroup`: '%s', please apply group first.";

    @Resource
    private IdGroupRepository idGroupRepository;

    @Resource
    private IdTagMapper idTagMapper;

    public IdTagPo get(String bizGroup, String bizTag) {
        return new LambdaQueryChainWrapper<>(idTagMapper)
                .eq(IdTagPo::getBizGroup, bizGroup)
                .eq(IdTagPo::getBizTag, bizTag)
                .last("limit 1")
                .one();
    }

    /**
     * 申请新业务标签; 所属业务组必须已登记, 已存在同名标签时幂等返回, 创建与更新时间由数据库生成
     */
    public void apply(ApplyTagRequest request) {
        if (Objects.isNull(idGroupRepository.get(request.getBizGroup()))) {
            throw new RuntimeException(String.format(FIND_GROUP_FAILURE_MESSAGE, request.getBizGroup()));
        }

        if (Objects.nonNull(get(request.getBizGroup(), request.getBizTag()))) {
            return;
        }

        IdTagPo record = new IdTagPo();
        record.setBizGroup(request.getBizGroup());
        record.setBizTag(request.getBizTag());
        record.setDescription(request.getDescription());

        try {
            idTagMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发申请同名业务标签发生插入冲突, 说明他人已抢先登记, 视为幂等成功
        }
    }

    /**
     * 分页查询业务标签, 所属业务组条件存在时精确匹配, 业务名条件存在时模糊匹配, 结果按登记顺序排列
     */
    public Page<IdTagPo> page(PageTagRequest request) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IdTagPo> recordPage =
                new LambdaQueryChainWrapper<>(idTagMapper)
                        .eq(StringUtils.isNotBlank(request.getBizGroup()), IdTagPo::getBizGroup, request.getBizGroup())
                        .like(StringUtils.isNotBlank(request.getBizTag()), IdTagPo::getBizTag, request.getBizTag())
                        .orderByDesc(IdTagPo::getId)
                        .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getCurrent(), request.getPageSize()));

        return Page.of(recordPage.getRecords(), request.getCurrent(), request.getPageSize(), recordPage.getTotal());
    }
}