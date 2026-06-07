package com.jmix.ruletrans.testgen;

import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.tool.impl.llm.LLMInvoker;

/**
 * Generates and parses structured P1 test cases using the existing LLM invoker.
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
        String prompt = promptBuilder.buildTestCasePrompt(naturalLanguage, context, snippet);
        try {
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return RuleTransTestCaseSet.fromJson(response);
        } catch (RuleTransException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleTransException("LLM test case generation failed: " + e.getMessage(), e);
        }
    }
}
