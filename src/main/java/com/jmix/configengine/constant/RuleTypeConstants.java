package com.jmix.configengine.constant;

/**
 * 规则类型常量定义
 * 用于统一管理规则类型，避免硬编码字符串
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
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为兼容性规则
     */
    public static boolean isCompatiableRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null && 
               ruleSchemaTypeFullName.contains(COMPATIABLE_RULE);
    }
    
    /**
     * 判断是否为计算规则
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为计算规则
     */
    public static boolean isCalculateRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null && 
               ruleSchemaTypeFullName.contains(CALCULATE_RULE);
    }
    
    /**
     * 判断是否为选择规则
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 是否为选择规则
     */
    public static boolean isSelectRule(String ruleSchemaTypeFullName) {
        return ruleSchemaTypeFullName != null && 
               ruleSchemaTypeFullName.contains(SELECT_RULE);
    }
    
    /**
     * 获取规则类型名称（从全名中提取）
     * @param ruleSchemaTypeFullName 规则Schema类型全名
     * @return 规则类型名称
     */
    public static String getRuleTypeName(String ruleSchemaTypeFullName) {
        if (ruleSchemaTypeFullName == null) {
            return null;
        }
        
        if (isCompatiableRule(ruleSchemaTypeFullName)) {
            return COMPATIABLE_RULE;
        } else if (isCalculateRule(ruleSchemaTypeFullName)) {
            return CALCULATE_RULE;
        } else if (isSelectRule(ruleSchemaTypeFullName)) {
            return SELECT_RULE;
        }
        
        return null;
    }
} 