package com.jmix.ruletrans;

/**
 * Test-side wrapper for a single RuleTrans pipeline run.
 */
public record RuleTransPipelineRunResult(
        String naturalLanguage,
        String contextSummary,
        RuleTransPipelineResult pipelineResult,
        RuleTransPipelineDiagnostics diagnostics) {

    public String methodBody() {
        return pipelineResult.methodBody();
    }

    public boolean success() {
        return pipelineResult.success();
    }
}
