package com.jmix.executor.omodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 结果类
 * 用于封装操作结果，包含状态码、数据和消息
 * 
 * @since 2025-09-22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    public static final int SUCCESS = 0;

    public static final int FAILED = 1;

    public static final int NO_SOLUTION = 2;

    private int code = SUCCESS;

    private T data;

    private String message;

    private Map<String, Object> extAttrs;

    public static <X> Result<X> success(X data) {
        Result<X> r = new Result<>();
        r.setCode(SUCCESS);
        r.setData(data);
        return r;
    }

    public static <X> Result<X> failed(String msg) {
        Result<X> r = new Result<>();
        r.setCode(FAILED);
        r.setMessage(msg);
        return r;
    }

    public static <X> Result<X> noSolution() {
        Result<X> r = new Result<>();
        r.setCode(NO_SOLUTION);
        return r;
    }
}
