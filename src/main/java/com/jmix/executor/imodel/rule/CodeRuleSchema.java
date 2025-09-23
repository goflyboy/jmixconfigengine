package com.jmix.executor.imodel.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码规则Schema
 * 用于表示没有结构化的规则，代码是Code（需要手工转化为约束规则）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodeRuleSchema extends RuleSchema {
    /**
     * 原始代码
     */
    private String rawCode;

    /**
     * 引用编程对象,没有去重的
     */
    private List<RefProgObjSchema> rightRefProgObjs;

    /**
     * 引用编程对象,没有去重的
     */
    private List<RefProgObjSchema> leftRefProgObjs;

    /**
     * 获取左侧编程对象列表
     * 
     * @return 左侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return leftRefProgObjs != null ? leftRefProgObjs : new ArrayList<>();
    }

    /**
     * 获取右侧编程对象列表
     * 
     * @return 右侧编程对象列表
     */
    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return rightRefProgObjs != null ? rightRefProgObjs : new ArrayList<>();
    }
}
