package com.jmix.ruletrans;

/**
 * Runtime switches for the RuleTrans end-to-end pipeline.
 */
public record RuleTransPipelineOptions(
        boolean generateBusinessCases,
        boolean executeBusinessCases,
        boolean allowEmptyBusinessCases) {

    public static RuleTransPipelineOptions defaults() {
        return new RuleTransPipelineOptions(true, true, false);
    }

    public static RuleTransPipelineOptions compileOnly() {
        return new RuleTransPipelineOptions(false, false, true);
    }
}
