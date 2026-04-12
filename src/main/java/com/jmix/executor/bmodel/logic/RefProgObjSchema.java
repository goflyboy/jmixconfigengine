package com.jmix.executor.bmodel.logic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 编程对象引用Schema
 * 定义规则中对编程对象的引用
 * 
 * @since 2025-09-22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RefProgObjSchema.class, name = "RefProgObjSchema")
})
public class RefProgObjSchema {

    /**
     * 参数类型常量
     */
    public static final String PROG_OBJ_TYPE_PARA = "Para";

    /**
     * 部件类型常量
     */
    public static final String PROG_OBJ_TYPE_PART = "Part";

    /**
     * 部件分类类型常量
     */
    public static final String PROG_OBJ_TYPE_PARTCATEGORY = "PartCategorys";

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