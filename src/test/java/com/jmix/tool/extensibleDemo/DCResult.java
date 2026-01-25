package com.jmix.tool.extensibleDemo;

import com.jmix.executor.model.Result;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DC公司的结果包装类
 * 扩展了原有的Result，添加了DC特有的字段和功能
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DCResult<T> extends Result<T> {

    /**
     * DC公司特有的结果编码
     */
    private String dcResultCode;

    /**
     * 处理时间戳
     */
    private Long processTimestamp;

    /**
     * DC扩展属性
     */
    private java.util.Map<String, String> dcExtAttrs = new java.util.HashMap<>();

    /**
     * 默认构造函数
     */
    public DCResult() {
        super();
        this.processTimestamp = System.currentTimeMillis();
    }

    /**
     * 从标准Result创建DCResult
     * 
     * @param result 标准结果
     */
    public DCResult(Result<T> result) {
        super();
        if (result != null) {
            this.setCode(result.getCode());
            this.setData(result.getData());
            this.setMessage(result.getMessage());
            this.processTimestamp = System.currentTimeMillis();
        }
    }

    /**
     * 创建成功的DC结果
     * 
     * @param data         数据
     * @param dcResultCode DC结果编码
     * @return DC结果
     */
    public static <T> DCResult<T> success(T data, String dcResultCode) {
        DCResult<T> result = new DCResult<>();
        result.setCode(SUCCESS);
        result.setData(data);
        result.dcResultCode = dcResultCode;
        result.processTimestamp = System.currentTimeMillis();
        return result;
    }

    /**
     * 创建成功的DC结果
     * 
     * @param data 数据
     * @return DC结果
     */
    public static <T> DCResult<T> success(T data) {
        return success(data, "DC_SUCCESS");
    }

    /**
     * 创建失败的DC结果
     * 
     * @param message      错误消息
     * @param dcResultCode DC结果编码
     * @return DC结果
     */
    public static <T> DCResult<T> failed(String message, String dcResultCode) {
        DCResult<T> result = new DCResult<>();
        result.setCode(FAILED);
        result.setMessage(message);
        result.dcResultCode = dcResultCode;
        result.processTimestamp = System.currentTimeMillis();
        return result;
    }

    /**
     * 创建失败的DC结果
     * 
     * @param message 错误消息
     * @return DC结果
     */
    public static <T> DCResult<T> failed(String message) {
        return failed(message, "DC_FAILED");
    }

    /**
     * 创建无解的DC结果
     * 
     * @param dcResultCode DC结果编码
     * @return DC结果
     */
    public static <T> DCResult<T> noSolution(String dcResultCode) {
        DCResult<T> result = new DCResult<>();
        result.setCode(NO_SOLUTION);
        result.dcResultCode = dcResultCode;
        result.processTimestamp = System.currentTimeMillis();
        return result;
    }

    /**
     * 创建无解的DC结果
     * 
     * @return DC结果
     */
    public static <T> DCResult<T> noSolution() {
        return noSolution("DC_NO_SOLUTION");
    }

    /**
     * 设置扩展属性
     * 
     * @param key   属性键
     * @param value 属性值
     */
    public void setExtAttr(String key, String value) {
        if (dcExtAttrs == null) {
            dcExtAttrs = new java.util.HashMap<>();
        }
        dcExtAttrs.put(key, value);
    }

    /**
     * 获取扩展属性
     * 
     * @param key 属性键
     * @return 属性值
     */
    public String getExtAttr(String key) {
        return dcExtAttrs != null ? dcExtAttrs.get(key) : null;
    }

    /**
     * 检查是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return getCode() == SUCCESS;
    }

    /**
     * 检查是否失败
     * 
     * @return 是否失败
     */
    public boolean isFailed() {
        return getCode() == FAILED;
    }

    /**
     * 检查是否无解
     * 
     * @return 是否无解
     */
    public boolean isNoSolution() {
        return getCode() == NO_SOLUTION;
    }

    /**
     * 获取处理耗时（毫秒）
     * 
     * @return 处理耗时
     */
    public Long getProcessDuration() {
        return System.currentTimeMillis() - processTimestamp;
    }
}
