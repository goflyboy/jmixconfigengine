package com.jmix.executor.bmodel.logic;

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
     * 优先级策略
     */
    private PriorityStrategy priorityStrategy;

    /**
     * 引用编程对象,没有去重的
     */
    private List<RefProgObjSchema> leftRefProgObjs = new ArrayList<>();

    /**
     * 引用编程对象,没有去重的
     */
    private List<RefProgObjSchema> rightRefProgObjs = new ArrayList<>();

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return leftRefProgObjs != null ? leftRefProgObjs : new ArrayList<>();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return rightRefProgObjs != null ? rightRefProgObjs : new ArrayList<>();
    }
}
