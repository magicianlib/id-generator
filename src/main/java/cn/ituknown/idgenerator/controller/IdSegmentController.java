package cn.ituknown.idgenerator.controller;

import cn.ituknown.idgenerator.repository.IdSegmentRepository;
import cn.ituknown.idgenerator.request.ApplyIdSegmentRequest;
import cn.ituknown.idgenerator.request.TakeIdSegmentRequest;
import cn.ituknown.idgenerator.result.Result;
import cn.ituknown.idgenerator.service.CommonIdSegmentService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(
        value = "/api/idSegment",
        consumes = {MediaType.APPLICATION_JSON_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE}
)
public class IdSegmentController {

    @Resource
    private CommonIdSegmentService commonSeqService;

    @Resource
    private IdSegmentRepository idSegmentRepository;

    /**
     * 申请新号段, 同名号段已存在时幂等返回
     */
    @PostMapping("/apply")
    public Result<Void> apply(@Validated @RequestBody ApplyIdSegmentRequest request) {
        idSegmentRepository.apply(request);
        return Result.success();
    }

    @PostMapping("/take")
    public Result<Long> take(@Validated @RequestBody TakeIdSegmentRequest request) {
        if (!commonSeqService.exist(request.getBizGroup(), request.getBizTag())) {
            return Result.failure(illegalSegmentMessage(request));
        }

        return Result.success(commonSeqService.take(request.getBizGroup(), request.getBizTag()));
    }

    @PostMapping("/take/{n}")
    public Result<List<Long>> takeN(@PathVariable Integer n, @Validated @RequestBody TakeIdSegmentRequest request) {
        if (n <= 0) {
            return Result.failure(String.format("take N must be greater than zero, bizGroup: %s, bizTag: %s", request.getBizGroup(), request.getBizTag()));
        }
        if (!commonSeqService.exist(request.getBizGroup(), request.getBizTag())) {
            return Result.failure(illegalSegmentMessage(request));
        }

        return Result.success(commonSeqService.take(request.getBizGroup(), request.getBizTag(), n));
    }

    /**
     * 号段尚未申请或申请后还未同步进缓存的提示
     */
    private String illegalSegmentMessage(TakeIdSegmentRequest request) {
        return String.format("bizGroup: %s, bizTag: %s illegal, please apply id segment first.", request.getBizGroup(), request.getBizTag());
    }
}