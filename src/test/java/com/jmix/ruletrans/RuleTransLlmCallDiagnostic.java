package com.jmix.ruletrans;

import java.nio.file.Path;

/**
 * Test-side diagnostic record for one LLM call.
 */
public record RuleTransLlmCallDiagnostic(
        int index,
        String stage,
        String promptSummary,
        Path fullPromptFile,
        String responseSummary,
        Path fullResponseFile,
        long durationMillis,
        boolean success,
        String errorMessage) {
}
