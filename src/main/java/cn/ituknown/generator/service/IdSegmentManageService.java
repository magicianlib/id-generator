package cn.ituknown.generator.service;

import cn.ituknown.generator.po.IdGroupPo;
import cn.ituknown.generator.po.IdSegmentPo;
import cn.ituknown.generator.po.IdTagPo;
import cn.ituknown.generator.repository.IdGroupRepository;
import cn.ituknown.generator.repository.IdSegmentRepository;
import cn.ituknown.generator.repository.IdTagRepository;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.PageGroupRequest;
import cn.ituknown.generator.request.PageSegmentRequest;
import cn.ituknown.generator.request.PageTagRequest;
import cn.ituknown.generator.result.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 号段管理服务: 承接业务组、业务标签与号段的登记及分页查询, 供控制层统一调用
 */
@Service
public class IdSegmentManageService {

    @Resource
    private IdGroupRepository idGroupRepository;

    @Resource
    private IdTagRepository idTagRepository;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    /**
     * 申请新业务组, 同名业务组已存在时幂等返回
     */
    public void applyGroup(ApplyGroupRequest request) {
        idGroupRepository.apply(request);
    }

    /**
     * 申请新业务标签, 所属业务组必须已登记, 同名标签已存在时幂等返回
     */
    public void applyTag(ApplyTagRequest request) {
        idTagRepository.apply(request);
    }

    /**
     * 申请新号段, 所属业务组与业务名必须已登记, 同名号段已存在时幂等返回
     */
    public void applySegment(ApplyIdSegmentRequest request) {
        idSegmentRepository.apply(request);
    }

    /**
     * 分页查询业务组, 业务组名条件存在时模糊匹配
     */
    public Page<IdGroupPo> pageGroup(PageGroupRequest request) {
        return idGroupRepository.page(request);
    }

    /**
     * 分页查询业务标签, 所属业务组条件存在时精确匹配, 业务名条件存在时模糊匹配
     */
    public Page<IdTagPo> pageTag(PageTagRequest request) {
        return idTagRepository.page(request);
    }

    /**
     * 分页查询已申请号段, 业务组与业务名条件存在时精确匹配
     */
    public Page<IdSegmentPo> pageSegment(PageSegmentRequest request) {
        return idSegmentRepository.page(request);
    }
}
