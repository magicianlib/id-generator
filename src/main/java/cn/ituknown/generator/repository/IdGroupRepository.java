package cn.ituknown.generator.repository;

import cn.ituknown.generator.mapper.IdGroupMapper;
import cn.ituknown.generator.po.IdGroupPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.PageGroupRequest;
import cn.ituknown.generator.result.Page;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Objects;

@Repository
public class IdGroupRepository {

    @Resource
    private IdGroupMapper idGroupMapper;

    public IdGroupPo get(String bizGroup) {
        return new LambdaQueryChainWrapper<>(idGroupMapper)
                .eq(IdGroupPo::getBizGroup, bizGroup)
                .last("limit 1")
                .one();
    }

    /**
     * 申请新业务组; 已存在同名业务组时幂等返回, 创建与更新时间由数据库生成
     */
    public void apply(ApplyGroupRequest request) {
        if (Objects.nonNull(get(request.getBizGroup()))) {
            return;
        }

        IdGroupPo record = new IdGroupPo();
        record.setBizGroup(request.getBizGroup());
        record.setDescription(request.getDescription());

        try {
            idGroupMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发申请同名业务组发生插入冲突, 说明他人已抢先登记, 视为幂等成功
        }
    }

    /**
     * 分页查询业务组, 业务组名条件存在时模糊匹配, 结果按登记顺序排列
     */
    public Page<IdGroupPo> page(PageGroupRequest request) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IdGroupPo> recordPage =
                new LambdaQueryChainWrapper<>(idGroupMapper)
                        .like(StringUtils.isNotBlank(request.getBizGroup()), IdGroupPo::getBizGroup, request.getBizGroup())
                        .orderByDesc(IdGroupPo::getId)
                        .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(request.getCurrent(), request.getPageSize()));

        return Page.of(recordPage.getRecords(), request.getCurrent(), request.getPageSize(), recordPage.getTotal());
    }
}