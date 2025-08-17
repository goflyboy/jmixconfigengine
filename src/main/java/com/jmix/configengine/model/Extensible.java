package com.jmix.configengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore; 
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 可扩展对象基类
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
     */
    @JsonIgnore
    public String getExtAttr(String key) {
        return extAttrs.get(key);
    }
    
    /**
     * 设置扩展属性值
     */
    @JsonIgnore
    public void setExtAttr(String key, String value) {
        extAttrs.put(key, value);
    }
} 