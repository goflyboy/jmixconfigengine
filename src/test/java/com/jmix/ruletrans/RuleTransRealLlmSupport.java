package com.jmix.ruletrans;

import com.jmix.tool.impl.llm.LLMInvoker;
import com.jmix.tool.impl.llm.LLMInvokerImpl;

/**
 * Shared real LLM invoker for RuleTrans integration-style tests.
 */
public final class RuleTransRealLlmSupport {

    private RuleTransRealLlmSupport() {
    }

    public static LLMInvoker realLlmInvoker() {
        return Holder.INSTANCE;
    }

    private static final class Holder {

        private static final LLMInvoker INSTANCE = new LLMInvokerImpl();
    }
}
