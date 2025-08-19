package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * 可编程对象基类（兼容命名：ProgramableObject → ProgrammableObject）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Module.class, name = "Module"),
    @JsonSubTypes.Type(value = Para.class, name = "Para"),
    @JsonSubTypes.Type(value = Part.class, name = "Part"),
    @JsonSubTypes.Type(value = ParaOption.class, name = "ParaOption")
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
} 