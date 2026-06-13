package com.jmix.tool.impl.llm;

/**
 * Optional interface for decorators that can expose metadata for the latest call.
 */
public interface LlmInvocationTraceProvider {

    LlmInvocationTrace lastInvocationTrace();
}
