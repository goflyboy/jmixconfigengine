package com.jmix.ruleunit;

import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;

import java.util.List;

/**
 * Internal normalized input for single-rule unit execution.
 */
public record RuleUnitInput(
        List<RuleUnitParameter> parameters,
        List<RuleUnitPart> parts,
        List<RuleUnitPartCategory> partCategories) {

    public RuleUnitInput {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        parts = parts == null ? List.of() : List.copyOf(parts);
        partCategories = partCategories == null ? List.of() : List.copyOf(partCategories);
    }
}
