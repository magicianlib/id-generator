package cn.ituknown.generator.exception;

import cn.ituknown.generator.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常兜底: 参数校验未通过与业务处理失败统一转为失败响应体, 调用方只需按响应结构解析业务码
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 请求参数校验未通过, 逐项拼接待校验字段与原因
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failure(message);
    }

    /**
     * 请求体缺失或无法解析
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.failure("request body is required and must be valid json.");
    }

    /**
     * 其余异常兜底, 含层级未登记、号段未找到等业务拒绝, 记录日志后返回失败响应体
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        LOGGER.warn("request failure: {}", e.getMessage(), e);
        return Result.failure(Objects.isNull(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage());
    }
}