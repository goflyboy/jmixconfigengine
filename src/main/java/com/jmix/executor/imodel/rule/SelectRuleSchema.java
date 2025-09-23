package com.jmix.executor.imodel.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择规则Schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SelectRuleSchema extends RuleSchema {
    /**
     * 筛选类型：single/multiple
     */
    private String type;

    /**
     * 左表达式
     */
    private ExprSchema leftExpr;

    /**
     * 赋值映射Schema
     */
    public static class AssignMapSchema {
        /**
         * 左表达式
         */
        private ExprSchema leftExpr;

        /**
         * 左属性编码
         */
        private String leftAttrCode;

        /**
         * 左引用编程对象
         */
        private List<RefProgObjSchema> leftRefProgObjs;

        @Override
        public String toString() {
            return "AssignMapSchema{"
                    + "leftExpr="
                    + leftExpr
                    + ", leftAttrCode='"
                    + leftAttrCode
                    + '\''
                    + ", leftRefProgObjs="
                    + leftRefProgObjs
                    + '}';
        }
    }

    /**
     * 获取左侧编程对象列表
     * 
     * @return 左侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        if (leftExpr != null && leftExpr.getRefProgObjs() != null) {
            return leftExpr.getRefProgObjs();
        }
        return new ArrayList<>();
    }

    /**
     * 获取右侧编程对象列表
     * 
     * @return 右侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        // SelectRule只有左侧表达式，没有右侧表达式
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "SelectRuleSchema{"
                + "type='"
                + type
                + '\''
                + ", leftExpr="
                + leftExpr
                + '}';
    }
}