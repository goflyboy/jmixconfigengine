package com.jmix.executor.bmodel.attr;

import java.util.List;
import java.util.Map;

/**
 * 动态属性接口
 * 定义了动态属性对象的基本操作
 * 
 * @since 2025-01-XX
 */
public interface IDynamicAttributable {

    /**
     * 动态属性值，
     */
    Map<String, String> getDynAttr();

    /**
     * 设置动态属性值
     * 
     * @param dynAttr
     */
    void setDynAttr(Map<String, String> dynAttr);

    /**
     * 获取动态属性值
     * 
     * @param key
     * @param value
     */
    void setAttr(String key, String value);

    /**
     * 获取动态属性值
     * 
     * @param key
     * @return
     */
    String getAttr(String key);

    /**
     * 动态属性定义
     */
    List<DynamicAttribute> getDynAttrSchemas();

    /**
     * 设置动态属性定义
     * 
     * @param dynAttrSchemas
     */
    void setDynAttrSchemas(List<DynamicAttribute> dynAttrSchemas);

    /**
     * 获取动态属性定义
     * 
     * @param code
     * @return
     */
    DynamicAttribute getDynAttrSchema(String code);

    /**
     * 设置动态属性定义
     * 
     * @param code
     * @param dynAttrSchema
     */
    void setDynAttrSchema(String code, DynamicAttribute dynAttrSchema);
}
