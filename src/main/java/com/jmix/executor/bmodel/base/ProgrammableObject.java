package com.jmix.executor.bmodel.base;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.para.Para;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可编程对象基类（兼容命名：ProgramableObject → ProgrammableObject）
 * 提供可编程对象的基础功能
 * 
 * @param <T> 值类型
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Module.class, name = "Module"),
        @JsonSubTypes.Type(value = Para.class, name = "Para"),
        @JsonSubTypes.Type(value = DynamicAttributerOption.class, name = "DynamicAttributerOption"),
        @JsonSubTypes.Type(value = Part.class, name = "Part"),
        @JsonSubTypes.Type(value = DynamicAttribute.class, name = "DynamicAttribute")
})
public class ProgrammableObject<T> extends Extensible implements Programmable {
    /**
     * 对象编码
     */
    private String code;

    /**
     * 父对象编码
     */
    private String fatherCode;

    /**
     * 默认值
     */
    private T defaultValue;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 短编码,仅用于调试
     */
    @JsonIgnore
    private String shortCode;

    /**
     * 克隆ProgrammableObject对象
     *
     * @param to 目标对象
     * @return 克隆的ProgrammableObject对象
     */
    public void clone(ProgrammableObject<T> to) {
        super.clone(to);
        to.setCode(this.getCode());
        to.setFatherCode(this.getFatherCode());
        to.setDefaultValue(this.getDefaultValue());
        to.setDescription(this.getDescription());
        to.setSortNo(this.getSortNo());
        to.setShortCode(this.getShortCode());
    }
}