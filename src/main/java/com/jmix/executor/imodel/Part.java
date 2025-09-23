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
    private PartType type = PartType.ATOMIC;

    /**
     * 原子Part的目录价
     */
    private Long price = 0L;

    /**
     * 原子Part的最大数量
     */
    private Integer maxQuantity = MAX_QUANTITY;

    /**
     * 原子Part的规格属性
     */
    private Map<String, String> attrs = new HashMap<>();

    /**
     * Part分类的规格描述
     */
    private List<AttrSchema> attrSchemas = new ArrayList<>();

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
        return attrs.get(key);
    }

    /**
     * 设置规格属性值
     * 
     * @param key   属性键
     * @param value 属性值
     */
    @JsonIgnore
    public void setAttr(String key, String value) {
        attrs.put(key, value);
    }
}