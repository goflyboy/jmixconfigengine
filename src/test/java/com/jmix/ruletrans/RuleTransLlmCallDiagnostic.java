package com.jmix.ruletrans;

import com.jmix.tool.impl.llm.LlmCacheStatus;

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
        String errorMessage,
        String modelTag,
        String modelIdentity,
        LlmCacheStatus cacheStatus,
        String cacheKey,
        Path cacheEntryFile) {
}
