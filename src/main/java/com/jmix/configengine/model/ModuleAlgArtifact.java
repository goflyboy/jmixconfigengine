package com.jmix.configengine.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * 算法制品元数据
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = ModuleAlgArtifact.class, name = "ModuleAlgArtifact")
})
public class ModuleAlgArtifact {
	private Long id;
	/**
	 * 算法制品包名，例如：TShirtConstraint.jar
	 */
	private String fileName;
	/**
	 * 算法制品基础包名
	 */
	private String packageName;
} 