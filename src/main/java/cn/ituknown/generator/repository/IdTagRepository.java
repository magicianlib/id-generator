package cn.ituknown.generator.repository;

import cn.ituknown.generator.mapper.IdTagMapper;
import cn.ituknown.generator.po.IdTagPo;
import cn.ituknown.generator.request.ApplyTagRequest;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
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
}