package com.jmix.executor.impl;

import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.logic.PriorityType;
import com.jmix.executor.omodel.PriorityAttrValue;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 优先级属性值实现类
 * 扩展了 PriorityAttrValue，增加了优先级约束相关信息
 * 
 * @since 2025-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PriorityAttrValueImpl extends PriorityAttrValue {
    /**
     * 优先级约束
     */
    private PriorityConstraint pConstraint;

    /**
     * 获取属性代码
     * 
     * @return 属性代码
     */
    public String getAttrCode() {
        if (pConstraint != null && pConstraint.getAttrCode() != null) {
            return pConstraint.getAttrCode();
        }
        return super.getAttrCode();
    }

    /**
     * 获取优先级类型
     * 
     * @return 优先级类型
     */
    public PriorityType getPriorityType() {
        if (pConstraint != null && pConstraint.getRule() != null) {
            Object rawCode = pConstraint.getRule().getRawCode();
            if (rawCode instanceof PriorityRuleSchema) {
                return ((PriorityRuleSchema) rawCode).getPriorityType();
            }
        }
        return null;
    }

    /**
     * 获取优先级策略
     * 
     * @return 优先级策略
     */
    public PriorityStrategy getPriorityStrategy() {
        if (pConstraint != null && pConstraint.getRule() != null) {
            Object rawCode = pConstraint.getRule().getRawCode();
            if (rawCode instanceof PriorityRuleSchema) {
                return ((PriorityRuleSchema) rawCode).getPriorityStrategy();
            }
        }
        return null;
    }
}
