package cn.ituknown.generator.repository;

import cn.ituknown.generator.mapper.IdGroupMapper;
import cn.ituknown.generator.po.IdGroupPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
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
}