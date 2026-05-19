package com.jmix.executor.bmodel.logic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 三元结构化规则 Schema。
 *
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TripleStructRuleSchema extends RuleSchema {
    private StructExprSchema expr1;

    private BusinessRelationType relationType;

    private StructExprSchema expr2;

    private StructExprSchema expr3;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return expr1 == null ? new ArrayList<>() : expr1.getRefProgObjs();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        List<RefProgObjSchema> refs = new ArrayList<>();
        if (expr2 != null) {
            refs.addAll(expr2.getRefProgObjs());
        }
        if (expr3 != null) {
            refs.addAll(expr3.getRefProgObjs());
        }
        return refs;
    }
}
