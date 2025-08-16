package com.jmix.configengine.schema;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
    }
} 