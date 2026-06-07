package com.jmix.ruletrans.postprocessor;

/**
 * Structured failed JUnit test case.
 */
public record FailedTestCase(
        String id,
        String displayName,
        String input,
        String expected,
        String actual,
        String reason,
        boolean likelyRuleLogicError) {
}
