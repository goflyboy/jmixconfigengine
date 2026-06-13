package com.jmix.tool.impl.llm;

/**
 * Creates real LLM invokers from configured model profiles.
 */
public final class LLMInvokerFactory {

    private LLMInvokerFactory() {
    }

    public static LLMInvoker create(String modelTag) {
        return new LLMInvokerImpl(profile(modelTag));
    }

    public static LlmModelProfile profile(String modelTag) {
        return LlmModelRegistry.load().resolve(modelTag);
    }
}
