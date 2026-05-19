package com.jmix.executor.bmodel.logic;

import java.util.Optional;

/**
 * 规则类型常量定义
 * 用于统一管理规则类型，避免硬编码字符串
 *
 * @since 2025-09-22
 */
public final class RuleTypeConstants {

    private RuleTypeConstants() {
        // 私有构造函数，防止实例化
    }

    // ==================== 规则Schema类型 ====================

    /**
     * 兼容性规则类型
     */
    public static final String COMPATIABLE_RULE = "CompatiableRule";

    /**
     * 计算规则类型
     */
    public static final String CALCULATE_RULE = "CalculateRule";

    /**
     * 选择规则类型
     */
    public static final String SELECT_RULE = "SelectRule";

    /**
     * 优先级规则类型
     */
    public static final String PRIORITY_RULE = "PriorityRule";

    /**
     * 二元结构化规则类型
     */
    public static final String PAIR_STRUCT_RULE = "PairStructRule";

    /**
     * 三元结构化规则类型
     */
    public static final String TRIPLE_STRUCT_RULE = "TripleStructRule";

    /**
     * 组合结构化规则类型
     */
    public static final String COMBINATION_STRUCT_RULE = "CombinationStructRule";

    /**
     * 结构化组合规则运行态类型
     */
    public static final String CODEPENDANT_RULE = "CodependantRule";

    // ==================== 规则Schema类型全名 ====================

    /**
     * 兼容性规则类型全名
     */
    public static final String COMPATIABLE_RULE_FULL_NAME = "CDSL.V5.Struct.CompatiableRule";

    /**
     * 计算规则类型全名
     */
    public static final String CALCULATE_RULE_FULL_NAME = "CDSL.V5.Struct.CalculateRule";

    /**
     * 选择规则类型全名
     */
    public static final String SELECT_RULE_FULL_NAME = "CDSL.V5.Struct.SelectRule";

    /**
     * 优先级规则类型全名
     */
    public static final String PRIORITY_RULE_FULL_NAME = "CDSL.V5.Struct.PriorityRule";

    /**
     * 二元结构化规则类型全名
     */
    public static final String PAIR_STRUCT_RULE_FULL_NAME = "CDSL.V5.Struct.PairStructRule";

    /**
     * 三元结构化规则类型全名
     */
    public static final String TRIPLE_STRUCT_RULE_FULL_NAME = "CDSL.V5.Struct.TripleStructRule";

    /**
     * 组合结构化规则类型全名
     */
    public static final String COMBINATION_STRUCT_RULE_FULL_NAME = "CDSL.V5.Struct.CombinationStructRule";

    /**
     * 结构化组合规则运行态类型全名
     */
    public static final String CODEPENDANT_RULE_FULL_NAME = "CDSL.V5.Struct.CodependantRule";

    // ==================== 操作符类型 ====================

    /**
     * 不兼容操作符
     */
    public static final String OPERATOR_INCOMPATIBLE = "Incompatible";

    /**
     * 共存性操作符（Codependent）
     */
    public static final String OPERATOR_CODEPENDENT = "CoDependent";

    /**
     * 依赖操作符
     */
    public static final String OPERATOR_REQUIRES = "Requires";

    // ==================== 选择规则类型 ====================

    /**
     * 单选类型
     */
    public static final String SELECT_TYPE_SINGLE = "single";

    /**
     * 多选类型
     */
    public static final String SELECT_TYPE_MULTIPLE = "multiple";

    // ==================== 工具方法 ====================

    /**
     * 判断是否为兼容性规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为兼容性规则
     */
    public static boolean isCompatiableRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(COMPATIABLE_RULE);
    }

    /**
     * 判断是否为计算规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为计算规则
     */
    public static boolean isCalculateRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(CALCULATE_RULE);
    }

    /**
     * 判断是否为选择规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为选择规则
     */
    public static boolean isSelectRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(SELECT_RULE);
    }

    /**
     * 判断是否为优先级规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为优先级规则
     */
    public static boolean isPriorityRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(PRIORITY_RULE);
    }

    /**
     * 判断是否为结构化规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为结构化规则
     */
    public static boolean isStructRule(String ruleSchemaTypeFullName) {
        return isPairStructRule(ruleSchemaTypeFullName)
                || isTripleStructRule(ruleSchemaTypeFullName)
                || isCombinationStructRule(ruleSchemaTypeFullName)
                || isCodependantRule(ruleSchemaTypeFullName);
    }

    /**
     * 判断是否为二元结构化规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为二元结构化规则
     */
    public static boolean isPairStructRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(PAIR_STRUCT_RULE);
    }

    /**
     * 判断是否为三元结构化规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为三元结构化规则
     */
    public static boolean isTripleStructRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(TRIPLE_STRUCT_RULE);
    }

    /**
     * 判断是否为组合结构化规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为组合结构化规则
     */
    public static boolean isCombinationStructRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(COMBINATION_STRUCT_RULE);
    }

    /**
     * 判断是否为结构化组合运行态规则
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为结构化组合运行态规则
     */
    public static boolean isCodependantRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null
                && ruleSchemaTypeFullName.contains(CODEPENDANT_RULE);
    }

    /**
     * 获取规则类型名称（从全名中提取）
     *
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 规则类型名称，如果无法识别则返回Optional.empty()
     */
    public static Optional<String> getRuleTypeName(String ruleSchemaTypeFullName) {
        if (ruleSchemaTypeFullName == null) {
            return Optional.empty();
        }

        if (isCompatiableRule(ruleSchemaTypeFullName)) {
            return Optional.of(COMPATIABLE_RULE);
        } else if (isCalculateRule(ruleSchemaTypeFullName)) {
            return Optional.of(CALCULATE_RULE);
        } else if (isSelectRule(ruleSchemaTypeFullName)) {
            return Optional.of(SELECT_RULE);
        } else if (isPriorityRule(ruleSchemaTypeFullName)) {
            return Optional.of(PRIORITY_RULE);
        } else if (isPairStructRule(ruleSchemaTypeFullName)) {
            return Optional.of(PAIR_STRUCT_RULE);
        } else if (isTripleStructRule(ruleSchemaTypeFullName)) {
            return Optional.of(TRIPLE_STRUCT_RULE);
        } else if (isCombinationStructRule(ruleSchemaTypeFullName)) {
            return Optional.of(COMBINATION_STRUCT_RULE);
        } else if (isCodependantRule(ruleSchemaTypeFullName)) {
            return Optional.of(CODEPENDANT_RULE);
        } else {
            throw new IllegalArgumentException("Invalid rule schema type full name: " + ruleSchemaTypeFullName);
        }
    }
}
