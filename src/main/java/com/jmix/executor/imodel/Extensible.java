package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 可扩展对象基类
 * 提供扩展属性的基础功能
 * 
 * @since 2025-09-22
 */
@Data
public class Extensible {
    /**
     * 扩展属性schema
     */
    private String extSchema;

    /**
     * 扩展属性，方便扩展
     */
    private Map<String, String> extAttrs = new HashMap<>();

    /**
     * 获取扩展属性值
     * 
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    @JsonIgnore
    public String getExtAttr(String key) {
        return extAttrs.get(key);
    }

    /**
     * 设置扩展属性值
     * 
     * @param key   属性键
     * @param value 属性值
     */
    @JsonIgnore
    public void setExtAttr(String key, String value) {
        extAttrs.put(key, value);
    }
}