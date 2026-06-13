package com.jmix.ruletrans;

import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;

import java.util.List;

/**
 * Final RuleTrans pipeline result.
 */
public record RuleTransPipelineResult(
        boolean success,
        String methodBody,
        int attempts,
        RuleScenario scenario,
        RuleMetadata metadata,
        CompilationResult compilationResult,
        BusinessRuleTestCaseSet businessCaseSet,
        RuleUnitTestCaseSetReport ruleUnitReport,
        RuleTransFailureKind failureKind,
        List<String> messages,
        List<RuleTransAttemptState> attemptStates) {

    public RuleTransPipelineResult {
        businessCaseSet = businessCaseSet == null ? BusinessRuleTestCaseSet.empty() : businessCaseSet;
        failureKind = failureKind == null ? RuleTransFailureKind.NONE : failureKind;
        messages = messages == null ? List.of() : List.copyOf(messages);
        attemptStates = attemptStates == null ? List.of() : List.copyOf(attemptStates);
    }

    public String snippet() {
        return methodBody;
    }
}
