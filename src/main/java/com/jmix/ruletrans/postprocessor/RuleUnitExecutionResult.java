package com.jmix.ruletrans.postprocessor;

import com.jmix.ruletrans.RuleTransFailureKind;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;

import java.util.List;

/**
 * RuleUnit execution result adapted for RuleTrans.
 */
public record RuleUnitExecutionResult(
        boolean success,
        RuleUnitTestCaseSetReport report,
        RuleTransFailureKind failureKind,
        List<RuleTransVerificationFailure> failures,
        List<String> messages) {

    public RuleUnitExecutionResult {
        failureKind = failureKind == null ? RuleTransFailureKind.NONE : failureKind;
        failures = failures == null ? List.of() : List.copyOf(failures);
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
