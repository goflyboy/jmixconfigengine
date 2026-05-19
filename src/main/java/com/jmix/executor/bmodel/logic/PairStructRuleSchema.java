package com.jmix.executor.bmodel.logic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 二元结构化规则 Schema。
 *
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PairStructRuleSchema extends RuleSchema {
    private StructExprSchema expr1;

    private BusinessRelationType relationType;

    private StructExprSchema expr2;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return expr1 == null ? new ArrayList<>() : expr1.getRefProgObjs();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return expr2 == null ? new ArrayList<>() : expr2.getRefProgObjs();
    }
}
