package com.jmix.executor.bmodel.logic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合结构化规则父 Schema。
 *
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CombinationStructRuleSchema extends RuleSchema {
    private int arity;

    private List<String> dimensionCategoryCodes = new ArrayList<>();

    private List<String> subRuleCodes = new ArrayList<>();

    public PartCombinationType getCombinationType() {
        return getType() == null || getType().isEmpty() ? null : PartCombinationType.valueOf(getType());
    }

    public void setCombinationType(PartCombinationType combinationType) {
        setType(combinationType == null ? null : combinationType.name());
    }

    public void setType(PartCombinationType combinationType) {
        setCombinationType(combinationType);
    }

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return dimensionRefs();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return dimensionRefs();
    }

    private List<RefProgObjSchema> dimensionRefs() {
        List<RefProgObjSchema> refs = new ArrayList<>();
        for (String categoryCode : dimensionCategoryCodes) {
            refs.add(new RefProgObjSchema(RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY, categoryCode, null));
        }
        return refs;
    }
}
