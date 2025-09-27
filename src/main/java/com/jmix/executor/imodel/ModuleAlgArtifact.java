package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

import java.io.File;

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

    // ==================== 文件命名模式常量 ====================

    /**
     * 模块目录命名模式：cp-{moduleCode}-{moduleId}
     */
    public static final String MODULE_DIR_PATTERN = "cp-%s-%s";

    /**
     * 运行jar包命名模式：cp-{moduleCode}-{moduleId}.jar
     */
    public static final String RUNTIME_JAR_PATTERN = "cp-%s-%s.jar";

    /**
     * 源码jar包命名模式：cp-{moduleCode}-{moduleId}-sources.jar
     */
    public static final String SOURCE_JAR_PATTERN = "cp-%s-%s-sources.jar";

    /**
     * 基础数据文件命名模式：cp-{moduleCode}-{moduleId}.base.json
     */
    public static final String BASE_JSON_PATTERN = "cp-%s-%s.base.json";

    // ==================== 文件命名方法 ====================

    /**
     * 获取模块目录名称
     * 
     * @return 模块目录名称，格式：cp-{moduleCode}-{moduleId}
     */
    @JsonIgnore
    public String getModuleDirName() {
        return String.format(MODULE_DIR_PATTERN, moduleCode, id);
    }

    /**
     * 获取运行jar包文件名
     * 
     * @return 运行jar包文件名，格式：cp-{moduleCode}-{moduleId}.jar
     */
    @JsonIgnore
    public String getRuntimeJarFileName() {
        return String.format(RUNTIME_JAR_PATTERN, moduleCode, id);
    }

    /**
     * 获取源码jar包文件名
     * 
     * @return 源码jar包文件名，格式：cp-{moduleCode}-{moduleId}-sources.jar
     */
    @JsonIgnore
    public String getSourceJarFileName() {
        return String.format(SOURCE_JAR_PATTERN, moduleCode, id);
    }

    /**
     * 获取基础数据文件名
     * 
     * @return 基础数据文件名，格式：cp-{moduleCode}-{moduleId}.base.json
     */
    @JsonIgnore
    public String getBaseJsonFileName() {
        return String.format(BASE_JSON_PATTERN, moduleCode, id);
    }

    /**
     * 获取模块目录完整路径
     * 
     * @param rootDir 根目录路径
     * @return 模块目录完整路径
     */
    @JsonIgnore
    public String getModuleDirPath(String rootDir) {
        return rootDir + File.separator + getModuleDirName();
    }

    /**
     * 获取运行jar包完整路径
     * 
     * @param rootDir 根目录路径
     * @return 运行jar包完整路径
     */
    @JsonIgnore
    public String getRuntimeJarPath(String rootDir) {
        return getModuleDirPath(rootDir) + File.separator + getRuntimeJarFileName();
    }

    /**
     * 获取源码jar包完整路径
     * 
     * @param rootDir 根目录路径
     * @return 源码jar包完整路径
     */
    @JsonIgnore
    public String getSourceJarPath(String rootDir) {
        return getModuleDirPath(rootDir) + File.separator + getSourceJarFileName();
    }

    /**
     * 获取基础数据文件完整路径
     * 
     * @param rootDir 根目录路径
     * @return 基础数据文件完整路径
     */
    @JsonIgnore
    public String getBaseJsonPath(String rootDir) {
        return getModuleDirPath(rootDir) + File.separator + getBaseJsonFileName();
    }
}