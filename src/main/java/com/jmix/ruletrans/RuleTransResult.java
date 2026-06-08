package com.jmix.ruletrans;

import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;

/**
 * Final RuleTrans pipeline result.
 */
public record RuleTransResult(
        boolean success,
        String methodBody,
        int attempts,
        CompilationResult compilationResult,
        TestExecutionResult testExecutionResult) {

    public String snippet() {
        return methodBody;
    }
}
