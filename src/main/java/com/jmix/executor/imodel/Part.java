package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件定义
 * 表示模块中的部件，支持原子部件和复合部件
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Part extends ProgrammableObject<Integer> {
    /**
     * 部件最大数量限制
     */
    public static final int MAX_QUANTITY = 20;

    /**
     * 部件最小数量限制
     */
    public static final int MIN_QUANTITY = 0;

    /**
     * 部件短编码前缀
     */
    public static final String SHORT_CODE_PREFIX = "PT";

    /**
     * 部件类型
     */
    private PartType partType = PartType.ATOMIC;

    /**
     * 原子Part的目录价
     */
    private Long price = 0L;

    /**
     * 原子Part的最大数量
     */
    private Integer maxQuantity = MAX_QUANTITY;

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
     * 获取默认数量
     * 
     * @return 默认数量
     */
    @JsonIgnore
    public Integer getDefaultQuantity() {
        return super.getDefaultValue();
    }

    /**
     * 获取规格属性值
     *
     * @param key 属性键
     * @return 属性值
     */
    @JsonIgnore
    public String getAttr(String key) {
        return dynAttr.get(key);
    }

    /**
     * 设置规格属性值
     *
     * @param key   属性键
     * @param value 属性值
     */
    @JsonIgnore
    public void setAttr(String key, String value) {
        dynAttr.put(key, value);
    }

    /**
     * 获取实例属性值
     *
     * @return 实例属性值对象
     */
    @JsonIgnore
    public InstanceDynAttrValue getInstanceAttrs() {
        String instanceAttrsStr = this.getAttr("instanceAttrs");
        if (instanceAttrsStr == null || instanceAttrsStr.trim().isEmpty()) {
            return null;
        }
        return InstanceDynAttrValue.fromJsonString(instanceAttrsStr);
    }

    /**
     * 克隆Part对象
     *
     * @return 克隆的Part对象
     */
    @JsonIgnore
    public Part clone() {
        Part pc = new Part();
        // 复制ProgrammableObject属性
        pc.setCode(this.getCode());
        pc.setFatherCode(this.getFatherCode());
        pc.setDefaultValue(this.getDefaultValue());
        pc.setDescription(this.getDescription());
        pc.setSortNo(this.getSortNo());
        pc.setShortCode(this.getShortCode());

        // 复制Part特有属性
        pc.setPartType(this.getPartType());
        pc.setPrice(this.getPrice());
        pc.setMaxQuantity(this.getMaxQuantity());
        pc.setDynAttrSchema(this.getDynAttrSchema());

        // 复制动态属性
        pc.setDynAttr(new HashMap<>(this.getDynAttr()));

        // 复制动态属性schema
        pc.setDynAttrSchemas(new ArrayList<>(this.getDynAttrSchemas()));

        return pc;
    }
}