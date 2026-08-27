package cn.ituknown.generator.controller;

import cn.ituknown.generator.po.IdGroupPo;
import cn.ituknown.generator.po.IdSegmentPo;
import cn.ituknown.generator.po.IdTagPo;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.PageGroupRequest;
import cn.ituknown.generator.request.PageSegmentRequest;
import cn.ituknown.generator.request.PageTagRequest;
import cn.ituknown.generator.request.TakeIdSegmentRequest;
import cn.ituknown.generator.result.Page;
import cn.ituknown.generator.result.Result;
import cn.ituknown.generator.service.CommonIdSegmentService;
import cn.ituknown.generator.service.IdSegmentManageService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(
        value = "/api/common",
        consumes = {MediaType.APPLICATION_JSON_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class CommonIdSegmentController {

    @Resource
    private CommonIdSegmentService commonIdSegmentService;

    @Resource
    private IdSegmentManageService idSegmentManageService;

    /**
     * 申请新业务组, 同名业务组已存在时幂等返回
     */
    @PostMapping("/applyGroup")
    public Result<Void> applyGroup(@Validated @RequestBody ApplyGroupRequest request) {
        idSegmentManageService.applyGroup(request);
        return Result.success();
    }

    /**
     * 申请新业务标签, 所属业务组必须已登记, 同名标签已存在时幂等返回
     */
    @PostMapping("/applyTag")
    public Result<Void> applyTag(@Validated @RequestBody ApplyTagRequest request) {
        idSegmentManageService.applyTag(request);
        return Result.success();
    }

    /**
     * 申请新号段, 所属业务组与业务名必须已登记, 同名号段已存在时幂等返回
     */
    @PostMapping("/applySegment")
    public Result<Void> applySegment(@Validated @RequestBody ApplyIdSegmentRequest request) {
        idSegmentManageService.applySegment(request);
        return Result.success();
    }

    /**
     * 分页查询业务组, 业务组名条件存在时模糊匹配
     */
    @PostMapping("/pageGroup")
    public Result<Page<IdGroupPo>> pageGroup(@Validated @RequestBody PageGroupRequest request) {
        return Result.success(idSegmentManageService.pageGroup(request));
    }

    /**
     * 分页查询业务标签, 所属业务组条件存在时精确匹配, 业务名条件存在时模糊匹配
     */
    @PostMapping("/pageTag")
    public Result<Page<IdTagPo>> pageTag(@Validated @RequestBody PageTagRequest request) {
        return Result.success(idSegmentManageService.pageTag(request));
    }

    /**
     * 分页查询已申请号段, 业务组与业务名条件存在时精确匹配
     */
    @PostMapping("/pageSegment")
    public Result<Page<IdSegmentPo>> pageSegment(@Validated @RequestBody PageSegmentRequest request) {
        return Result.success(idSegmentManageService.pageSegment(request));
    }

    @PostMapping("/takeSegment")
    public Result<Long> takeSegment(@Validated @RequestBody TakeIdSegmentRequest request) {
        if (!commonIdSegmentService.exist(request.getBizGroup(), request.getBizTag())) {
            return Result.failure(illegalSegmentMessage(request));
        }

        return Result.success(commonIdSegmentService.take(request.getBizGroup(), request.getBizTag()));
    }

    @PostMapping("/takeSegment/{n}")
    public Result<List<Long>> takeSegmentN(@PathVariable Integer n, @Validated @RequestBody TakeIdSegmentRequest request) {
        if (n <= 0) {
            return Result.failure(String.format("take N must be greater than zero, bizGroup: %s, bizTag: %s", request.getBizGroup(), request.getBizTag()));
        }
        if (!commonIdSegmentService.exist(request.getBizGroup(), request.getBizTag())) {
            return Result.failure(illegalSegmentMessage(request));
        }

        return Result.success(commonIdSegmentService.take(request.getBizGroup(), request.getBizTag(), n));
    }

    /**
     * 号段尚未申请或申请后还未同步进缓存的提示
     */
    private String illegalSegmentMessage(TakeIdSegmentRequest request) {
        return String.format("bizGroup: %s, bizTag: %s illegal, please apply id segment first.", request.getBizGroup(), request.getBizTag());
    }
}
