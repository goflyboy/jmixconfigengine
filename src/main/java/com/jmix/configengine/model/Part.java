package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 部件定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Part extends ProgramableObject<Integer> {
    /**
     * 部件类型
     */
    private PartType type;
    
    /**
     * 原子Part的目录价
     */
    private Long price;
    
    /**
     * 原子Part的规格属性
     */
    private Map<String, String> attrs;
    
    /**
     * Part分类的规格描述
     */
    private List<AttrSchema> attrSchemas;
    
    /**
     * 获取默认数量
     */
    public Integer getDefaultQuantity() {
        return super.getDefaultValue();
    }

   /**
     * 获取规格属性值
     */
    @JsonIgnore
    public String getAttr(String key) {
        return attrs.get(key);
    }
    
    /**
     * 设置规格属性值
     */
    @JsonIgnore
    public void setAttr(String key, String value) {
        attrs.put(key, value);
    }   
} 