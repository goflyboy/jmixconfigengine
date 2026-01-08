package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态属性
 * 表示可配置的动态属性，支持多种类型和可选值
 *
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DynamicAttribute extends ProgrammableObject<String> {
    private String name = ""; // 名称
    // 动态属性可选值列表
    private List<DynamicAttributerOption> options = new ArrayList<>();
    // 动态属性类型
    private DynamicAttributeType dynAttrType = DynamicAttributeType.Integer;
    // 动态属性的值，拥有计算实例化的值，存在算法
    private String value;
    // 扩展模式
    private String optionExtSchema;
    // 实例类型：0-非实例属性（默认），1-实例属性
    private int instType = 0;
    // 动态属性的Logic
    private List<Rule> valueLogics;

    /**
     * 选项映射：codeValue -> DynamicAttributerOption
     */
    @JsonIgnore
    private Map<String, DynamicAttributerOption> codeValueOptionMap = new HashMap<>();

    /**
     * 选项映射：codeId -> DynamicAttributerOption
     */
    @JsonIgnore
    private Map<String, DynamicAttributerOption> codeIdOptionMap = new HashMap<>();

    /**
     * 选项映射：code -> DynamicAttributerOption
     */
    @JsonIgnore
    private Map<String, DynamicAttributerOption> codeOptionMap = new HashMap<>();

    /**
     * 根据codeValue查询选项
     *
     * @param codeValue 选项的codeValue
     * @return 对应的DynamicAttributerOption，如果不存在返回null
     */
    @JsonIgnore
    public DynamicAttributerOption queryOptionByCodeValue(String codeValue) {
        if (codeValueOptionMap.isEmpty() && !options.isEmpty()) {
            // 构建codeValueOptionMap
            for (DynamicAttributerOption option : options) {
                codeValueOptionMap.put(option.getCodeValue(), option);
            }
        }
        return codeValueOptionMap.get(codeValue);
    }

    /**
     * 根据codeId查询选项
     *
     * @param codeId 选项的codeId
     * @return 对应的DynamicAttributerOption，如果不存在返回null
     */
    @JsonIgnore
    public DynamicAttributerOption queryOptionByCodeId(String codeId) {
        if (codeIdOptionMap.isEmpty() && !options.isEmpty()) {
            // 构建codeIdOptionMap
            for (DynamicAttributerOption option : options) {
                codeIdOptionMap.put(option.getCodeId(), option);
            }
        }
        return codeIdOptionMap.get(codeId);
    }

    /**
     * 根据code查询选项
     *
     * @param code 选项的code
     * @return 对应的DynamicAttributerOption，如果不存在返回null
     */
    @JsonIgnore
    public DynamicAttributerOption queryOptionByCode(String code) {
        if (codeOptionMap.isEmpty() && !options.isEmpty()) {
            // 构建codeOptionMap
            for (DynamicAttributerOption option : options) {
                codeOptionMap.put(option.getCode(), option);
            }
        }
        return codeOptionMap.get(code);
    }

    /**
     * 创建Integer类型的动态属性
     *
     * @param code 属性编码
     * @param name 属性名称
     * @return 创建的DynamicAttribute实例
     */
    public static DynamicAttribute createIntegerAttr(String code, String name) {
        DynamicAttribute attr = new DynamicAttribute();
        attr.setName(name);
        attr.setCode(code);
        attr.setDynAttrType(DynamicAttributeType.Integer);
        return attr;
    }
}
