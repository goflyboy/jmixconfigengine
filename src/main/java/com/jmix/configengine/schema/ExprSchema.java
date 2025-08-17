package com.jmix.configengine.schema;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

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