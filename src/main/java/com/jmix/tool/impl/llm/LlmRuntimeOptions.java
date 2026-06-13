package com.jmix.tool.impl.llm;

import java.nio.file.Path;

/**
 * Runtime switches for model selection and local LLM cache behavior.
 */
public record LlmRuntimeOptions(
        String modelTag,
        LlmCacheMode cacheMode,
        Path cacheDir) {

    private static final String DEFAULT_MODEL_TAG = "default";
    private static final Path DEFAULT_CACHE_DIR = Path.of(".ruletrans-cache", "llm");

    public LlmRuntimeOptions {
        modelTag = normalizeModelTag(modelTag);
        cacheMode = cacheMode == null ? LlmCacheMode.READ_THROUGH : cacheMode;
        cacheDir = cacheDir == null ? DEFAULT_CACHE_DIR : cacheDir;
    }

    public static LlmRuntimeOptions defaults() {
        return new LlmRuntimeOptions(DEFAULT_MODEL_TAG, LlmCacheMode.READ_THROUGH, DEFAULT_CACHE_DIR);
    }

    public LlmRuntimeOptions withModel(String nextModelTag) {
        return new LlmRuntimeOptions(nextModelTag, cacheMode, cacheDir);
    }

    public LlmRuntimeOptions withCacheMode(LlmCacheMode nextCacheMode) {
        return new LlmRuntimeOptions(modelTag, nextCacheMode, cacheDir);
    }

    public LlmRuntimeOptions withCacheDir(Path nextCacheDir) {
        return new LlmRuntimeOptions(modelTag, cacheMode, nextCacheDir);
    }

    private static String normalizeModelTag(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_MODEL_TAG;
        }
        return value.trim();
    }
}
