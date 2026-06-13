package com.jmix.tool.impl.llm;

/**
 * Cache behavior for LLM calls.
 */
public enum LlmCacheMode {
    READ_THROUGH,
    REFRESH,
    DISABLED,
    CACHE_ONLY
}
