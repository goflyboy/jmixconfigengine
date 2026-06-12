package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Business-facing ranked solution expectation or actual solution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleUnitSolution(
        Integer rank,
        List<RuleUnitPart> parts,
        List<RuleUnitParameter> parameters) {

    public RuleUnitSolution {
        parts = parts == null ? List.of() : List.copyOf(parts);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
