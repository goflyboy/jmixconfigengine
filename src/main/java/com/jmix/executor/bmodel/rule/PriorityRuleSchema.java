package com.jmix.executor.bmodel.rule;

import com.jmix.executor.bmodel.PriorityStrategy;
import com.jmix.executor.bmodel.PriorityType;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 优先级规则Schema
 * 定义优化目标的优先级规则
 * 
 * @since 2025-01-XX
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PriorityRuleSchema extends RuleSchema {

    /**
     * 优先级类型
     */
    private PriorityType priorityType;

    /**
     * 优先级策略
     */
    private PriorityStrategy priorityStrategy;

    /**
     * 属性代码
     */
    private String attrCode;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return new ArrayList<>();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return new ArrayList<>();
    }
}
