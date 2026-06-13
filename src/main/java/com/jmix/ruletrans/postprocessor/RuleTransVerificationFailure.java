package com.jmix.ruletrans.postprocessor;

/**
 * Business-case verification failure used by RuleTrans correction prompts.
 */
public record RuleTransVerificationFailure(
        String id,
        String displayName,
        String input,
        String expected,
        String actual,
        String reason,
        boolean likelyRuleLogicError) {
}
