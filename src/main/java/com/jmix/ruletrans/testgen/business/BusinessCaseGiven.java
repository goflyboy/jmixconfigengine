package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Business-domain inputs of one generated rule unit case.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessCaseGiven(
        List<RuleUnitParameter> parameters,
        List<RuleUnitPart> parts,
        List<RuleUnitPartCategory> partCategories) {

    public BusinessCaseGiven {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        parts = parts == null ? List.of() : List.copyOf(parts);
        partCategories = partCategories == null ? List.of() : List.copyOf(partCategories);
    }

    public static BusinessCaseGiven empty() {
        return new BusinessCaseGiven(List.of(), List.of(), List.of());
    }
}
