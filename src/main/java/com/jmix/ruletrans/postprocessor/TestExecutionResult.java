package com.jmix.ruletrans.postprocessor;

import java.util.List;

/**
 * Structured JUnit execution result.
 */
public record TestExecutionResult(
        boolean success,
        long testsFound,
        long testsSucceeded,
        long testsFailed,
        List<FailedTestCase> failedCases) {
}
