package com.jmix.tool.impl.llm;

import java.nio.file.Path;

/**
 * Last-call cache/model metadata exposed to diagnostics decorators.
 */
public record LlmInvocationTrace(
        String modelTag,
        String modelIdentity,
        LlmCacheStatus cacheStatus,
        String cacheKey,
        Path cacheEntryFile) {

    public static LlmInvocationTrace empty() {
        return new LlmInvocationTrace("", "", null, "", null);
    }
}
