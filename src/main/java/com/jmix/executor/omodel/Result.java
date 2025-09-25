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
    /**
     * 操作成功状态码
     */
    public static final int SUCCESS = 0;

    /**
     * 操作失败状态码
     */
    public static final int FAILED = 1;

    /**
     * 无解状态码
     */
    public static final int NO_SOLUTION = 2;

    /**
     * 状态码
     */
    private int code = SUCCESS;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 消息
     */
    private String message;

    /**
     * 扩展属性
     */
    private Map<String, Object> extAttrs;

    /**
     * 创建成功结果
     * 
     * @param data 成功时返回的数据
     * @param <X>  数据类型
     * @return 成功结果对象
     */
    public static <X> Result<X> success(X data) {
        Result<X> r = new Result<>();
        r.setCode(SUCCESS);
        r.setData(data);
        return r;
    }

    /**
     * 创建失败结果
     * 
     * @param msg 失败消息
     * @param <X> 数据类型
     * @return 失败结果对象
     */
    public static <X> Result<X> failed(String msg) {
        Result<X> r = new Result<>();
        r.setCode(FAILED);
        r.setMessage(msg);
        return r;
    }

    /**
     * 创建无解结果
     * 
     * @param <X> 数据类型
     * @return 无解结果对象
     */
    public static <X> Result<X> noSolution() {
        Result<X> r = new Result<>();
        r.setCode(NO_SOLUTION);
        return r;
    }
}
