package cn.ituknown.generator.controller;

import cn.ituknown.generator.repository.IdGroupRepository;
import cn.ituknown.generator.repository.IdSegmentRepository;
import cn.ituknown.generator.repository.IdTagRepository;
import cn.ituknown.generator.request.ApplyGroupRequest;
import cn.ituknown.generator.request.ApplyIdSegmentRequest;
import cn.ituknown.generator.request.ApplyTagRequest;
import cn.ituknown.generator.request.TakeIdSegmentRequest;
import cn.ituknown.generator.result.Result;
import cn.ituknown.generator.service.CommonIdSegmentService;
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
    private IdGroupRepository idGroupRepository;

    @Resource
    private IdTagRepository idTagRepository;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    /**
     * 申请新业务组, 同名业务组已存在时幂等返回
     */
    @PostMapping("/applyGroup")
    public Result<Void> applyGroup(@Validated @RequestBody ApplyGroupRequest request) {
        idGroupRepository.apply(request);
        return Result.success();
    }

    /**
     * 申请新业务标签, 所属业务组必须已登记, 同名标签已存在时幂等返回
     */
    @PostMapping("/applyTag")
    public Result<Void> applyTag(@Validated @RequestBody ApplyTagRequest request) {
        idTagRepository.apply(request);
        return Result.success();
    }

    /**
     * 申请新号段, 所属业务组与业务名必须已登记, 同名号段已存在时幂等返回
     */
    @PostMapping("/applySegment")
    public Result<Void> applySegment(@Validated @RequestBody ApplyIdSegmentRequest request) {
        idSegmentRepository.apply(request);
        return Result.success();
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