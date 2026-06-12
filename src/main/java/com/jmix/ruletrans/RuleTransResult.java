package com.jmix.ruletrans;

import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;

/**
 * Final RuleTrans pipeline result.
 */
public record RuleTransResult(
        boolean success,
        String methodBody,
        int attempts,
        CompilationResult compilationResult,
        TestExecutionResult testExecutionResult,
        BusinessRuleTestCaseSet businessTestCaseSet) {

    public RuleTransResult(
            boolean success,
            String methodBody,
            int attempts,
            CompilationResult compilationResult,
            TestExecutionResult testExecutionResult) {
        this(success, methodBody, attempts, compilationResult, testExecutionResult, BusinessRuleTestCaseSet.empty());
    }

    public String snippet() {
        return methodBody;
    }
}
