package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

/**
 * 算法制品元数据
 * 包含算法制品的元数据信息
 * 
 * @since 2025-09-22
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ModuleAlgArtifact.class, name = "ModuleAlgArtifact")
})
public class ModuleAlgArtifact {
    /**
     * 算法制品ID
     */
    private Long id;

    /**
     * 模块代码
     */
    private String moduleCode;

    /**
     * 算法制品包名，例如：TShirtConstraint.jar
     */
    private String fileName;

    /**
     * 算法制品基础包名
     */
    private String packageName;

    /**
     * 父类名称，用于内部类场景，默认为空字符串
     * 如果有父类，加载时需要拼接父类名称
     */
    private String parentClassName = "";
}