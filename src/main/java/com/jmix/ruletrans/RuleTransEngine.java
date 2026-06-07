package com.jmix.ruletrans;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;

import java.util.List;

/**
 * Orchestrates natural-language rule translation.
 */
public final class RuleTransEngine {

    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleSnippetAssembler assembler;
    private final CompilationProcessor compilationProcessor;
    private final RuleTestCaseGenerator testCaseGenerator;
    private final TestExecutionProcessor testExecutionProcessor;
    private final PromptBuilder promptBuilder;

    public RuleTransEngine(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            RuleSnippetAssembler assembler,
            CompilationProcessor compilationProcessor,
            RuleTestCaseGenerator testCaseGenerator,
            TestExecutionProcessor testExecutionProcessor,
            PromptBuilder promptBuilder) {
        this.identifier = identifier;
        this.generator = generator;
        this.assembler = assembler;
        this.compilationProcessor = compilationProcessor;
        this.testCaseGenerator = testCaseGenerator;
        this.testExecutionProcessor = testExecutionProcessor;
        this.promptBuilder = promptBuilder;
    }

    public String translate(String naturalLanguage, RuleContext context) {
        validate(naturalLanguage, context);
        return generator.generate(naturalLanguage, prepareContext(naturalLanguage, context));
    }

    public RuleTransResult translateWithRetry(String naturalLanguage, RuleContext context, int maxRetries) {
        validate(naturalLanguage, context);
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        int attemptsLimit = Math.max(1, maxRetries + 1);
        String snippet = null;
        CompilationResult lastResult = null;

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            snippet = attempt == 1
                    ? generator.generate(naturalLanguage, preparedContext)
                    : generator.generateFromPrompt(promptBuilder.buildCompilationCorrectionPrompt(
                            naturalLanguage, preparedContext, snippet, lastResult));

            AssembledRuleClass assembled = assembler.assembleCompileUnit(
                    snippet, preparedContext, "RuleTransCandidate" + String.format("%03d", attempt));
            lastResult = compilationProcessor.compile(assembled);
            if (lastResult.success()) {
                if (testCaseGenerator != null) {
                    testCaseGenerator.generate(naturalLanguage, preparedContext, snippet);
                }
                return new RuleTransResult(true, snippet, attempt, lastResult, null);
            }
        }
        throw new RuleTransException("RuleTrans compilation retry limit exceeded: "
                + String.join("\n", lastResult == null ? List.of() : lastResult.errors()));
    }

    public TestExecutionProcessor testExecutionProcessor() {
        return testExecutionProcessor;
    }

    private RuleContext prepareContext(String naturalLanguage, RuleContext context) {
        if (!context.isProductLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = identifier.identify(naturalLanguage, context.module());
        List<com.jmix.executor.bmodel.PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ProductRuleContext(context.module(), categories);
    }

    private void validate(String naturalLanguage, RuleContext context) {
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            throw new IllegalArgumentException("naturalLanguage must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }
}
