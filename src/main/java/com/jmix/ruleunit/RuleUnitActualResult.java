package com.jmix.ruleunit;

import com.jmix.executor.model.Result;
import com.jmix.ruletrans.testgen.business.BusinessCaseExpect;
import com.jmix.ruletrans.testgen.business.RuleUnitDiagnostic;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;

import java.util.List;

/**
 * Business-facing actual result from one single-rule unit execution.
 */
public record RuleUnitActualResult(
        int engineResultCode,
        String message,
        Boolean compatible,
        List<RuleUnitParameter> parameters,
        List<RuleUnitPart> parts,
        List<RuleUnitSolution> solutions,
        List<RuleUnitDiagnostic> diagnostics) {

    public RuleUnitActualResult {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        parts = parts == null ? List.of() : List.copyOf(parts);
        solutions = solutions == null ? List.of() : List.copyOf(solutions);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public static RuleUnitActualResult failed(String message) {
        return new RuleUnitActualResult(Result.FAILED, message, false, List.of(), List.of(), List.of(), List.of());
    }

    public BusinessCaseExpect toExpectShape() {
        return new BusinessCaseExpect(compatible, parameters, parts, solutions, diagnostics);
    }
}
