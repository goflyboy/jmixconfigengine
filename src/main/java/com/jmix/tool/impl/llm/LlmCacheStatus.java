package com.jmix.tool.impl.llm;

/**
 * Diagnostic status for one LLM cache operation.
 */
public enum LlmCacheStatus {
    DISABLED,
    HIT,
    MISS,
    REFRESHED,
    WRITE_FAILED
}
