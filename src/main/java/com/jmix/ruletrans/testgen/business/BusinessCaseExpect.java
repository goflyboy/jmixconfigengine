package com.jmix.ruletrans.testgen.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Business-domain expected outputs of one generated rule unit case.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessCaseExpect(
        Boolean compatible,
        List<RuleUnitParameter> parameters,
        List<RuleUnitPart> parts,
        List<RuleUnitSolution> solutions,
        List<RuleUnitDiagnostic> diagnostics) {

    public BusinessCaseExpect {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        parts = parts == null ? List.of() : List.copyOf(parts);
        solutions = solutions == null ? List.of() : List.copyOf(solutions);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static BusinessCaseExpect empty() {
        return new BusinessCaseExpect(null, List.of(), List.of(), List.of(), List.of());
    }
}
