package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValue;
import com.jmix.executor.bmodel.base.ProgrammableObject;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体类
 * Part和Module的公共基类，包含公共属性和方法
 * 
 * @since 2025-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Onto extends ProgrammableObject<Integer> {

    /**
     * 实例属性键名
     */
    public static final String INSTANCE_ATTRS = "instanceAttrs";

    /**
     * 参数列表
     */
    private List<Para> paras = new ArrayList<>();

    /**
     * 规则列表
     */
    private List<Rule> rules = new ArrayList<>();

    @JsonIgnore
    private Map<String, Para> paraMap = new HashMap<>();

    /**
     * 原子Part的规格属性(动态属性值）
     */
    private Map<String, String> dynAttr = new HashMap<>();

    /**
     * Part分类的规格描述(动态属性定义,partCategoryAttrValues)
     */
    private List<DynamicAttribute> dynAttrSchemas = new ArrayList<>();

    /**
     * Part分类的schema (partCategoryAttrSchema?)
     */
    private String dynAttrSchema;

    /**
     * 获取规格属性值
     *
     * @param key 属性键
     * @return 属性值
     */
    @JsonIgnore
    public String getAttr(String key) {
        return getDynAttr().get(key);
    }

    /**
     * 设置规格属性值
     *
     * @param key   属性键
     * @param value 属性值
     */
    @JsonIgnore
    public void setAttr(String key, String value) {
        getDynAttr().put(key, value);
    }

    /**
     * 获取实例属性值
     *
     * @return 实例属性值对象
     */
    @JsonIgnore
    public InstanceDynAttrValue getInstanceAttrs() {
        String instanceAttrsStr = this.getAttr(INSTANCE_ATTRS);
        if (instanceAttrsStr == null || instanceAttrsStr.trim().isEmpty()) {
            return null;
        }
        return InstanceDynAttrValue.fromJsonString(instanceAttrsStr);
    }

}
