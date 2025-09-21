package com.jmix.executor.model.schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 兼容规则Schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("checkstyle:DesignForExtensionCheck")
public class CompatiableRuleSchema extends RuleSchema {

    /**
     * 操作符常量定义
     */
    public static final class Operator {
        /**
         * 不兼容约束：表示两个选项不能同时选择
         */
        public static final String INCOMPATIBLE = "Incompatible";

        /**
         * 共存性约束：表示两个选项必须同时选择或同时不选择
         */
        public static final String CO_REFENT = "CoDependent";

        /**
         * 依赖约束：表示选择某个选项时必须选择另一个选项
         */
        public static final String REQUIRES = "Requires";

        private Operator() {
            // 防止实例化
        }
    }

    /**
     * 左表达式
     */
    private ExprSchema leftExpr;

    /**
     * 操作符
     * 可选值：
     * - {@link Operator#INCOMPATIBLE}: 不兼容约束
     * - {@link Operator#CO_REFENT}: 共存性约束
     * - {@link Operator#REQUIRES}: 依赖约束
     */
    private String operator;

    /**
     * 右表达式
     */
    private ExprSchema rightExpr;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (leftExpr != null && leftExpr.getRefProgObjs() != null) {
            return leftExpr.getRefProgObjs();
        }
        return new ArrayList<>();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        if (rightExpr != null && rightExpr.getRefProgObjs() != null) {
            return rightExpr.getRefProgObjs();
        }
        return new ArrayList<>();
    }
}