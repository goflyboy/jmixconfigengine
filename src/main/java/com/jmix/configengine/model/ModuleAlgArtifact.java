package com.jmix.configengine.model;

import lombok.Data;

/**
 * 算法制品元数据
 */
@Data
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