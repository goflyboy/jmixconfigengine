package com.jmix.executor.bmodel;

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
}