package com.jmix.tool.impl.llm;

/**
 * File-backed cache entry for one LLM response.
 */
public record LlmCacheEntry(
        int schemaVersion,
        String key,
        String stage,
        String modelTag,
        String modelIdentity,
        String createdAt,
        String updatedAt,
        int hitCount,
        Request request,
        Response response,
        Diagnostics diagnostics) {

    public LlmCacheEntry withHit(String updatedAt) {
        return new LlmCacheEntry(
                schemaVersion,
                key,
                stage,
                modelTag,
                modelIdentity,
                createdAt,
                updatedAt,
                hitCount + 1,
                request,
                response,
                diagnostics);
    }

    public record Request(String systemMessage, String userMessage) {
    }

    public record Response(String text) {
    }

    public record Diagnostics(long durationMillis, String sourceConfigInfo) {
    }
}
