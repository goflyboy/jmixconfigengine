package com.jmix.executor.bmodel.logic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化组合规则展开后的运行态 Schema。
 *
 * @since 2026-05-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CodependantRuleSchema extends RuleSchema {
    private int arity;

    private List<String> dimensionCategoryCodes = new ArrayList<>();

    private List<PartCombination> combinations = new ArrayList<>();

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return dimensionRefs();
    }

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
