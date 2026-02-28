package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValue;
import com.jmix.executor.bmodel.base.ProgrammableObject;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
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
public class Onto extends ProgrammableObject<Integer> implements IOnto {

    /**
     * 实例属性键名
     */
    public static final String INSTANCE_ATTRS = "instanceAttrs";

    /**
     * 参数列表，为了兼容老的，保留参数
     */
    private List<Para> paras = new ArrayList<>();

    /**
     * 规则列表
     */
    private List<Rule> rules = new ArrayList<>();

    /**
     * 参数映射表
     */
    @JsonIgnore
    private Map<String, Para> paraMap = new HashMap<>();

    /**
     * 动态属性值，
     */
    private Map<String, String> dynAttr = new HashMap<>();

    /**
     * 动态属性定义
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
     * 初始化短代码
     */
    @JsonIgnore
    public int initShortCode(int startIndex) {
        int index = startIndex;
        for (Para para : getParas()) {
            if (para.getCode().length() <= 3) { // 如果编码长度小于等于3，则直接使用编码
                para.setShortCode(para.getCode());
            } else {
                para.setShortCode(Para.SHORT_CODE_PREFIX + index);
                index++;
            }
        }
        return index;
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
        return getPara(code);
    }

    @Override
    public Optional<Para> getPara(String code) {
        if (code == null || paraMap.isEmpty()) {
            for (Para para : getParas()) {
                paraMap.put(para.getCode(), para);
            }
        }
        Para para = paraMap.get(code);
        return para != null ? Optional.of(para) : Optional.empty();
    }

    @Override
    public Optional<Rule> getRule(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return getRules().stream()
                .filter(rule -> code.equals(rule.getCode()))
                .findFirst();
    }

    @Override
    @JsonIgnore
    public boolean hasPriorityRule() {
        return !getPriorityRules().isEmpty();
    }

    @Override
    @JsonIgnore
    public List<Rule> getPriorityRules() {
        return getRules().stream()
                .filter(rule -> RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName()))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public DynamicAttribute getDynAttrSchema(String code) {
        return getDynAttrSchemas().stream()
                .filter(attr -> code.equals(attr.getCode()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void setDynAttrSchema(String code, DynamicAttribute dynAttrSchema) {
        getDynAttrSchemas().add(dynAttrSchema);
    }

    /**
     * 克隆Onto对象
     *
     * @param to 目标对象
     * @return 克隆的Onto对象
     */
    public void clone(Onto to) {
        super.clone(to);
        to.setDynAttr(new HashMap<>(this.getDynAttr()));
        to.setDynAttrSchemas(this.getDynAttrSchemas());
        to.setDynAttrSchema(this.getDynAttrSchema());
        to.setRules(new ArrayList<>(this.getRules()));
        to.setParas(new ArrayList<>(this.getParas()));
        // paraMap将在init方法中重新初始化
        to.init();
    }
}
