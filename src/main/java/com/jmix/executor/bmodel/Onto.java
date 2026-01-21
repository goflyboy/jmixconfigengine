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
import java.util.Optional;

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
     * 初始化方法
     * 对partCategoryMap、partMap、paraMap进行初始化
     */
    @JsonIgnore
    public void init() {
        // 初始化 paraMap
        if (paras != null) {
            for (Para para : paras) {
                paraMap.put(para.getCode(), para);
            }
        }
        // 其他初始化逻辑将在Module的init方法中实现
    }

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

    /**
     * 查询实例类型的动态属性定义
     *
     * @return 实例类型(instType=1)的动态属性列表
     */
    @JsonIgnore
    public List<DynamicAttribute> queryDynAttrSchemas4Inst() {
        return getDynAttrSchemas().stream()
                .filter(attr -> attr.getInstType() == 1)
                .toList();
    }

    /**
     * 查询非实例类型的动态属性定义
     *
     * @return 非实例类型(instType=0)的动态属性列表
     */
    @JsonIgnore
    public List<DynamicAttribute> queryDynAttrSchemas4NotInst() {
        return getDynAttrSchemas().stream()
                .filter(attr -> attr.getInstType() == 0)
                .toList();
    }

    /**
     * 根据属性代码查询动态属性定义
     *
     * @param code 属性代码
     * @return 找到的动态属性定义，如果未找到则返回null
     */
    @JsonIgnore
    public DynamicAttribute queryDynAttrSchemas(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return getDynAttrSchemas().stream()
                .filter(attr -> code.equals(attr.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据编码获取参数对象
     * 
     * @param code 参数编码
     * @return 参数对象，如果不存在则返回Optional.empty()
     */
    @JsonIgnore
    public Optional<Para> queryPara(String code) {
        if (code == null || paraMap.isEmpty()) {
            return Optional.empty();
        }
        Para para = paraMap.get(code);
        return para != null ? Optional.of(para) : Optional.empty();
    }
}
