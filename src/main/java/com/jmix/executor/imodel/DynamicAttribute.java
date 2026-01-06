package com.jmix.executor.imodel;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

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
