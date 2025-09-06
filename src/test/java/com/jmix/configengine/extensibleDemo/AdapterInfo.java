package com.jmix.configengine.extensibleDemo;

import lombok.Data;

import java.util.Map;
import java.util.HashMap;

/**
 * 适配器信息基类
 * 定义字段映射关系和扩展属性
 */
@Data
public abstract class AdapterInfo {
    
    /**
     * 字段映射关系
     * key: 源字段名, value: 目标字段名
     */
    protected Map<String, String> fieldMappings = new HashMap<>();
    
    /**
     * 扩展属性定义
     * key: 属性名, value: 属性类型
     */
    protected Map<String, String> extAttrs = new HashMap<>();
    
    /**
     * 默认值映射
     * key: 目标字段名, value: 默认值
     */
    protected Map<String, Object> defaultValues = new HashMap<>();
    
    /**
     * 构造函数
     */
    public AdapterInfo() {
        initMappings();
    }
    
    /**
     * 初始化映射关系
     * 子类需要实现此方法来定义具体的映射规则
     */
    protected abstract void initMappings();
    
    /**
     * 获取目标字段名
     * @param sourceField 源字段名
     * @return 目标字段名，如果未找到则返回源字段名
     */
    public String getTargetField(String sourceField) {
        return fieldMappings.getOrDefault(sourceField, sourceField);
    }
    
    /**
     * 检查是否有字段映射
     * @param sourceField 源字段名
     * @return 是否有映射
     */
    public boolean hasFieldMapping(String sourceField) {
        return fieldMappings.containsKey(sourceField);
    }
    
    /**
     * 获取扩展属性类型
     * @param attrName 属性名
     * @return 属性类型
     */
    public String getExtAttrType(String attrName) {
        return extAttrs.get(attrName);
    }
    
    /**
     * 检查是否有扩展属性
     * @param attrName 属性名
     * @return 是否有扩展属性
     */
    public boolean hasExtAttr(String attrName) {
        return extAttrs.containsKey(attrName);
    }
    
    /**
     * 获取默认值
     * @param fieldName 字段名
     * @return 默认值
     */
    public Object getDefaultValue(String fieldName) {
        return defaultValues.get(fieldName);
    }
}
