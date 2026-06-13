package com.jmix.ruletrans;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Low-level natural-language rule translation collaborator.
 */
@Slf4j
public final class RuleTransEngine {

    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleScenarioClassifier scenarioClassifier;

    public RuleTransEngine(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            PromptBuilder promptBuilder) {
        this(identifier, generator, promptBuilder, new RuleScenarioClassifier());
    }

    public RuleTransEngine(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            PromptBuilder promptBuilder,
            RuleScenarioClassifier scenarioClassifier) {
        this.identifier = identifier;
        this.generator = generator;
        this.scenarioClassifier = scenarioClassifier == null ? new RuleScenarioClassifier() : scenarioClassifier;
    }

    /**
     * Translates a natural-language rule into a Java method body without retry or execution.
     *
     * @param naturalLanguage rule described in natural language
     * @param context target rule context
     * @return generated Java method body
     */
    public String translate(String naturalLanguage, RuleContext context) {
        validate(naturalLanguage, context);
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        RuleScenario scenario = scenarioClassifier.classify(naturalLanguage, preparedContext);
        return generator.generateMethodBody(naturalLanguage, preparedContext, scenario);
    }

    private RuleContext prepareContext(String naturalLanguage, RuleContext context) {
        if (!context.isModuleLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = identifier.identify(naturalLanguage, context.module());
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ModuleRuleContext(context.module(), categories);
    }

    private void validate(String naturalLanguage, RuleContext context) {
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            log.warn("RuleTrans request rejected: naturalLanguage must not be blank");
            throw new IllegalArgumentException("naturalLanguage must not be blank");
        }
        if (context == null) {
            log.warn("RuleTrans request rejected: context must not be null, naturalLanguage={}", naturalLanguage);
            throw new IllegalArgumentException("context must not be null");
        }
    }
}
