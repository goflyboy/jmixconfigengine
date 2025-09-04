package com.jmix.configengine.schema;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
     * 引用编程对象
     */
    private List<RefProgObjSchema> refProgObjs;
} 
