package com.jmix.configengine.schema;

import lombok.Data;

/**
 * 规则Schema基类
 */
@Data
public abstract class RuleSchema {
    /**
     * Schema类型
     * 支持的类型包括：
     * - CompatiableRule: 兼容性规则，用于定义参数或部件之间的兼容关系
     *   * Incompatible: 不兼容约束，表示两个选项不能同时选择
     *   * CoRefent: 共存性约束，表示两个选项必须同时选择或同时不选择
     *   * Requires: 依赖约束，表示选择某个选项时必须选择另一个选项
     * - SelectRule: 选择规则，用于定义参数或部件的选择约束
     *   * single: 单选约束，必须且只能选择一个选项
     *   * multiple: 多选约束，可以选择多个选项
     * - CalculateRule: 计算规则，用于定义数量、价格等计算关系 
     * 这些类型对应不同的业务场景，每种类型都有其特定的Schema结构和处理逻辑
     */
    private String type;
    
    /**
     * Schema版本
     */
    private String version;
} 