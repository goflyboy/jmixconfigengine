package com.jmix.ruletrans;

import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;

/**
 * Diagnostic snapshot for one pipeline attempt.
 */
public record RuleTransAttemptState(
        int attempt,
        String methodBody,
        CompilationResult compilationResult,
        BusinessRuleTestCaseSet businessCaseSet,
        RuleUnitTestCaseSetReport ruleUnitReport,
        RuleTransFailureKind failureKind) {
}
