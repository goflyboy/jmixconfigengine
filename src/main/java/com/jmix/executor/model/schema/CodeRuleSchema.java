package com.jmix.executor.model.schema;

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

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return leftRefProgObjs != null ? leftRefProgObjs : new ArrayList<>();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return rightRefProgObjs != null ? rightRefProgObjs : new ArrayList<>();
    }
}
