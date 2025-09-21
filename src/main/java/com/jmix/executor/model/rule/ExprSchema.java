package com.jmix.executor.model.rule;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

import java.util.List;

/**
 * 表达式Schema
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExprSchema.class, name = "ExprSchema")
})
public class ExprSchema {
    /**
     * 原始代码
     */
    private String rawCode;

    /**
     * 引用的编程对象
     */
    private List<RefProgObjSchema> refProgObjs;
}