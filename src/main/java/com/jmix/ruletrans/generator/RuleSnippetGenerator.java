package com.jmix.ruletrans.generator;

import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.impl.llm.LLMInvoker;

/**
 * Generates Java rule snippets through the existing LLM invoker.
 */
public final class RuleSnippetGenerator {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;
    private final RuleSnippetPostProcessor postProcessor;

    public RuleSnippetGenerator(
            LLMInvoker llmInvoker,
            PromptBuilder promptBuilder,
            RuleSnippetPostProcessor postProcessor) {
        this.llmInvoker = llmInvoker;
        this.promptBuilder = promptBuilder;
        this.postProcessor = postProcessor;
    }

    public String generate(String naturalLanguage, RuleContext context) {
        String prompt = promptBuilder.buildGeneratePrompt(naturalLanguage, context);
        return generateFromPrompt(prompt);
    }

    public String generateFromPrompt(String prompt) {
        try {
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return postProcessor.process(response);
        } catch (RuleTransException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleTransException("LLM rule snippet generation failed: " + e.getMessage(), e);
        }
    }
}
