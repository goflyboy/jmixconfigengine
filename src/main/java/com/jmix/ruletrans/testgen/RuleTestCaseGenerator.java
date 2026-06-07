package com.jmix.ruletrans.testgen;

import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.impl.llm.LLMInvoker;

/**
 * Placeholder P1 test case generator using the existing LLM invoker.
 */
public final class RuleTestCaseGenerator {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;

    public RuleTestCaseGenerator(LLMInvoker llmInvoker, PromptBuilder promptBuilder) {
        this.llmInvoker = llmInvoker;
        this.promptBuilder = promptBuilder;
    }

    public RuleTransTestCaseSet generate(String naturalLanguage, RuleContext context, String snippet) {
        if (llmInvoker == null) {
            return RuleTransTestCaseSet.empty();
        }
        // P1 execution is intentionally conservative until generated case schema is stabilized.
        promptBuilder.buildTestCasePrompt(naturalLanguage, context, snippet);
        return RuleTransTestCaseSet.empty();
    }
}
