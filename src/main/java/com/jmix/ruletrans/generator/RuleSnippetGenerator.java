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

    private static final int LLM_GENERATION_ATTEMPTS = 3;

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
        return generateMethodBodyFromPrompt(prompt, sdkProfile, context);
    }

    public String generateFromPrompt(String prompt) {
        return generateMethodBodyFromPrompt(prompt, SdkProfile.CONSTRAINT);
    }

    public String generateMethodBodyFromPrompt(String prompt, SdkProfile sdkProfile) {
        return generateMethodBodyFromPrompt(prompt, sdkProfile, null);
    }

    public String generateMethodBodyFromPrompt(String prompt, SdkProfile sdkProfile, RuleContext context) {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= LLM_GENERATION_ATTEMPTS; attempt++) {
            try {
                String response = llmInvoker.generate(PromptBuilder.SYSTEM_MESSAGE, prompt);
                if (response == null || response.isBlank()) {
                    throw new RuleTransException("LLM returned empty response");
                }
                return postProcessor.processMethodBody(response, sdkProfile, context);
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        String message = lastFailure == null || lastFailure.getMessage() == null
                ? "unknown error"
                : lastFailure.getMessage();
        throw new RuleTransException("LLM rule method body generation failed after "
                + LLM_GENERATION_ATTEMPTS + " attempts: " + message, lastFailure);
    }

    private SdkProfile sdkProfile(RuleScenario scenario) {
        return scenario == null ? SdkProfile.CONSTRAINT : scenario.sdkProfile();
    }
}
