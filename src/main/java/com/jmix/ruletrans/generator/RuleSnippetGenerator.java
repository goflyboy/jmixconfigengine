package com.jmix.ruletrans.generator;

import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.sdk.SdkProfile;
import com.jmix.tool.impl.llm.LLMInvoker;

/**
 * Generates Java rule method bodies through the existing LLM invoker.
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
        return generateMethodBody(naturalLanguage, context, null);
    }

    public String generateMethodBody(String naturalLanguage, RuleContext context, RuleScenario scenario) {
        String prompt = promptBuilder.buildGeneratePrompt(naturalLanguage, context, scenario);
        SdkProfile sdkProfile = sdkProfile(scenario);
        return generateMethodBodyFromPrompt(prompt, sdkProfile);
    }

    public String generateFromPrompt(String prompt) {
        return generateMethodBodyFromPrompt(prompt, SdkProfile.CONSTRAINT);
    }

    public String generateMethodBodyFromPrompt(String prompt, SdkProfile sdkProfile) {
        try {
            String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
            return postProcessor.processMethodBody(response, sdkProfile);
        } catch (RuleTransException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleTransException("LLM rule method body generation failed: " + e.getMessage(), e);
        }
    }

    private SdkProfile sdkProfile(RuleScenario scenario) {
        return scenario == null ? SdkProfile.CONSTRAINT : scenario.sdkProfile();
    }
}
