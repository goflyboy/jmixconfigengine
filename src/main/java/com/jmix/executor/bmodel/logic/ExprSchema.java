package com.jmix.executor.bmodel.logic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式Schema
 * 定义规则中的表达式结构
 * 
 * @since 2025-09-22
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
    private List<RefProgObjSchema> refProgObjs = new ArrayList<>();
}