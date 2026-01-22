package com.jmix.executor.bmodel.base;

import java.util.Map;

/**
 * 可扩展接口
 * 定义了可扩展对象的基本操作
 * 
 * @since 2025-01-XX
 */
public interface IExtensible {
    /**
     * 设置扩展属性
     * 
     * @param extAttrs
     */
    void setExtAttrs(Map<String, String> extAttrs);

    /**
     * 获取扩展属性
     * 
     * @return
     */
    Map<String, String> getExtAttrs();

    /**
     * 获取扩展属性值
     * 
     * @param key
     * @return
     */
    String getExtAttr(String key);

    /**
     * 设置扩展属性值
     * 
     * @param key
     * @param value
     */
    void setExtAttr(String key, String value);

    /**
     * 获取扩展属性schema
     * 
     * @return
     */
    String getExtSchema();

    /**
     * 设置扩展属性schema
     * 
     * @param extSchema
     */
    void setExtSchema(String extSchema);
}
