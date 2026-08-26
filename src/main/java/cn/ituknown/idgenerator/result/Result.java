package cn.ituknown.idgenerator.result;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Result<T> implements Serializable {
    private int code = 0;
    private String message;
    private T data;

    private static <T> Result<T> buildResult() {
        return new Result<>();
    }

    public static <T> Result<T> success() {
        Result<T> result = buildResult();
        result.setMessage("success");
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> failure() {
        Result<T> result = buildResult();
        result.setCode(1);
        result.setMessage("failure");
        return result;
    }

    public static <T> Result<T> failure(String message) {
        Result<T> result = failure();
        result.setMessage(message);
        return result;
    }
}