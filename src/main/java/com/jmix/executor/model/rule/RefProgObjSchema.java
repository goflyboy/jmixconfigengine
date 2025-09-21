package com.jmix.executor.model.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

/**
 * 编程对象引用Schema
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RefProgObjSchema.class, name = "RefProgObjSchema")
})
public class RefProgObjSchema {
    /**
     * 可编程对象类型
     */
    private String progObjType;

    /**
     * 可编程对象编码
     */
    private String progObjCode;

    /**
     * 可编程对象属性
     */
    private String progObjField;
}