package com.jmix.configengine.schema;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * 规则Schema基类
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompatiableRuleSchema.class, name = "CompatiableRule"),
    @JsonSubTypes.Type(value = CalculateRuleSchema.class, name = "CalculateRule"),
    @JsonSubTypes.Type(value = SelectRuleSchema.class, name = "SelectRule"),
    @JsonSubTypes.Type(value = CodeRuleSchema.class, name = "CodeRule")
})
public abstract class RuleSchema {
    /**
     * Schema类型
     * 支持的类型包括：
     * - CompatiableRule: 兼容性规则，用于定义参数或部件之间的兼容关系
     *   * Incompatible: 不兼容约束，表示两个选项不能同时选择
     *   * CoDependent: 共存性约束，表示两个选项必须同时选择或同时不选择
     *   * Requires: 依赖约束，表示选择某个选项时必须选择另一个选项
     * - SelectRule: 选择规则，用于定义参数或部件的选择约束
     *   * single: 单选约束，必须且只能选择一个选项
     *   * multiple: 多选约束，可以选择多个选项
     * - CalculateRule: 计算规则，用于定义数量、价格等计算关系
     * - CodeRule: 代码规则，用于表示没有结构化的规则，代码是Code（需要手工转化为约束规则）
     * 这些类型对应不同的业务场景，每种类型都有其特定的Schema结构和处理逻辑
     */
    private String type;
    
    /**
     * Schema版本
     */
    private String version;
}
