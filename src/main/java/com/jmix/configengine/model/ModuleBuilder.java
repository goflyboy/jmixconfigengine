package com.jmix.configengine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一模块构建器，支持流式创建各种模型对象
 * 减少类的数量，提供统一的构建接口
 * 
 * @author JMix Config Engine
 * @since 1.0.0
 */
public class ModuleBuilder {
    
    // ========== ParaOption构建器 ==========
    
    /**
     * ParaOption构建器
     */
    public static class ParaOptionBuilder {
        
        private final ParaOption paraOption;
        
        private ParaOptionBuilder() {
            this.paraOption = new ParaOption();
        }
        
        /**
         * 创建新的构建器实例
         */
        public static ParaOptionBuilder create() {
            return new ParaOptionBuilder();
        }
        
        /**
         * 设置选项编码ID
         */
        public ParaOptionBuilder codeId(int codeId) {
            paraOption.setCodeId(codeId);
            return this;
        }
        
        /**
         * 设置对象编码
         */
        public ParaOptionBuilder code(String code) {
            paraOption.setCode(code);
            return this;
        }
        
        /**
         * 设置父对象编码
         */
        public ParaOptionBuilder fatherCode(String fatherCode) {
            paraOption.setFatherCode(fatherCode);
            return this;
        }
        
        /**
         * 设置默认值
         */
        public ParaOptionBuilder defaultValue(String defaultValue) {
            paraOption.setDefaultValue(defaultValue);
            return this;
        }
        
        /**
         * 设置描述信息
         */
        public ParaOptionBuilder description(String description) {
            paraOption.setDescription(description);
            return this;
        }
        
        /**
         * 设置排序号
         */
        public ParaOptionBuilder sortNo(int sortNo) {
            paraOption.setSortNo(sortNo);
            return this;
        }
        
        /**
         * 设置扩展属性Schema
         */
        public ParaOptionBuilder extSchema(String extSchema) {
            paraOption.setExtSchema(extSchema);
            return this;
        }
        
        /**
         * 动态添加扩展属性
         */
        public ParaOptionBuilder extAttr(String key, String value) {
            paraOption.setExtAttr(key, value);
            return this;
        }
        
        /**
         * 批量添加扩展属性
         */
        public ParaOptionBuilder extAttrs(Map<String, String> extAttrs) {
            if (extAttrs != null) {
                extAttrs.forEach(paraOption::setExtAttr);
            }
            return this;
        }
        
        /**
         * 创建选项的便捷方法
         */
        public ParaOptionBuilder asOption(int codeId, String code, String description) {
            return this.codeId(codeId)
                       .code(code)
                       .description(description);
        }
        
        /**
         * 构建并返回ParaOption对象
         */
        public ParaOption build() {
            return paraOption;
        }
        
        /**
         * 设置默认值并构建
         */
        public ParaOption buildWithDefaults() {
            if (paraOption.getDefaultValue() == null && paraOption.getCode() != null) {
                paraOption.setDefaultValue(paraOption.getCode());
            }
            if (paraOption.getSortNo() == null) {
                paraOption.setSortNo(1);
            }
            return paraOption;
        }
    }
    
    // ========== Part构建器 ==========
    
    /**
     * Part构建器
     */
    public static class PartBuilder {
        
        private final Part part;
        
        private PartBuilder() {
            this.part = new Part();
        }
        
        /**
         * 创建新的构建器实例
         */
        public static PartBuilder create() {
            return new PartBuilder();
        }
        
        /**
         * 设置对象编码
         */
        public PartBuilder code(String code) {
            part.setCode(code);
            return this;
        }
        
        /**
         * 设置父对象编码
         */
        public PartBuilder fatherCode(String fatherCode) {
            part.setFatherCode(fatherCode);
            return this;
        }
        
        /**
         * 设置默认值
         */
        public PartBuilder defaultValue(Integer defaultValue) {
            part.setDefaultValue(defaultValue);
            return this;
        }
        
        /**
         * 设置描述信息
         */
        public PartBuilder description(String description) {
            part.setDescription(description);
            return this;
        }
        
        /**
         * 设置排序号
         */
        public PartBuilder sortNo(int sortNo) {
            part.setSortNo(sortNo);
            return this;
        }
        
        /**
         * 设置扩展属性Schema
         */
        public PartBuilder extSchema(String extSchema) {
            part.setExtSchema(extSchema);
            return this;
        }
        
        /**
         * 动态添加扩展属性
         */
        public PartBuilder extAttr(String key, String value) {
            part.setExtAttr(key, value);
            return this;
        }
        
        /**
         * 批量添加扩展属性
         */
        public PartBuilder extAttrs(Map<String, String> extAttrs) {
            if (extAttrs != null) {
                extAttrs.forEach(part::setExtAttr);
            }
            return this;
        }
        
        /**
         * 设置部件类型
         */
        public PartBuilder type(PartType type) {
            part.setType(type);
            return this;
        }
        
        /**
         * 设置价格
         */
        public PartBuilder price(Long price) {
            part.setPrice(price);
            return this;
        }
        
        /**
         * 设置规格属性
         */
        public PartBuilder attrs(Map<String, String> attrs) {
            part.setAttrs(attrs);
            return this;
        }
        
        /**
         * 添加单个规格属性
         */
        public PartBuilder attr(String key, String value) {
            if (part.getAttrs() == null) {
                part.setAttrs(new HashMap<>());
            }
            part.getAttrs().put(key, value);
            return this;
        }
        
        /**
         * 创建部件的便捷方法
         */
        public PartBuilder asPart(String code, String description, int sortNo, Long price, String material, String weight) {
            return this.code(code)
                       .description(description)
                       .sortNo(sortNo)
                       .type(PartType.ATOMIC)
                       .price(price)
                       .extAttr("material", material)
                       .extAttr("weight", weight);
        }
        
        /**
         * 构建并返回Part对象
         */
        public Part build() {
            return part;
        }
        
        /**
         * 设置默认值并构建
         */
        public Part buildWithDefaults() {
            if (part.getDefaultValue() == null) {
                part.setDefaultValue(0);
            }
            if (part.getSortNo() == null) {
                part.setSortNo(1);
            }
            if (part.getType() == null) {
                part.setType(PartType.ATOMIC);
            }
            return part;
        }
    }
    
    // ========== Para构建器 ==========
    
    /**
     * Para构建器
     */
    public static class ParaBuilder {
        
        private final Para para;
        
        private ParaBuilder() {
            this.para = new Para();
        }
        
        /**
         * 创建新的构建器实例
         */
        public static ParaBuilder create() {
            return new ParaBuilder();
        }
        
        /**
         * 设置对象编码
         */
        public ParaBuilder code(String code) {
            para.setCode(code);
            return this;
        }
        
        /**
         * 设置父对象编码
         */
        public ParaBuilder fatherCode(String fatherCode) {
            para.setFatherCode(fatherCode);
            return this;
        }
        
        /**
         * 设置默认值
         */
        public ParaBuilder defaultValue(String defaultValue) {
            para.setDefaultValue(defaultValue);
            return this;
        }
        
        /**
         * 设置描述信息
         */
        public ParaBuilder description(String description) {
            para.setDescription(description);
            return this;
        }
        
        /**
         * 设置排序号
         */
        public ParaBuilder sortNo(int sortNo) {
            para.setSortNo(sortNo);
            return this;
        }
        
        /**
         * 设置扩展属性Schema
         */
        public ParaBuilder extSchema(String extSchema) {
            para.setExtSchema(extSchema);
            return this;
        }
        
        /**
         * 动态添加扩展属性
         */
        public ParaBuilder extAttr(String key, String value) {
            para.setExtAttr(key, value);
            return this;
        }
        
        /**
         * 批量添加扩展属性
         */
        public ParaBuilder extAttrs(Map<String, String> extAttrs) {
            if (extAttrs != null) {
                extAttrs.forEach(para::setExtAttr);
            }
            return this;
        }
        
        /**
         * 设置参数类型
         */
        public ParaBuilder type(ParaType type) {
            para.setType(type);
            return this;
        }
        
        /**
         * 设置选项列表
         */
        public ParaBuilder options(java.util.List<ParaOption> options) {
            para.setOptions(options);
            return this;
        }
        
        /**
         * 创建颜色参数的便捷方法
         */
        public ParaBuilder asColorPara(String code, String description, int sortNo) {
            return this.code(code)
                       .description(description)
                       .sortNo(sortNo)
                       .type(ParaType.ENUM);
        }
        
        /**
         * 创建尺寸参数的便捷方法
         */
        public ParaBuilder asSizePara(String code, String description, int sortNo) {
            return this.code(code)
                       .description(description)
                       .sortNo(sortNo)
                       .type(ParaType.ENUM);
        }
        
        /**
         * 构建并返回Para对象
         */
        public Para build() {
            return para;
        }
        
        /**
         * 设置默认值并构建
         */
        public Para buildWithDefaults() {
            if (para.getDefaultValue() == null) {
                para.setDefaultValue("");
            }
            if (para.getSortNo() == null) {
                para.setSortNo(1);
            }
            return para;
        }
    }
} 