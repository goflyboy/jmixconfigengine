package com.jmix.ruletrans;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.ruletrans.testgen.RuleTransTestCaseSet;

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
        TestExecutionResult lastTestResult = null;
        RuleTransTestCaseSet testCaseSet = null;

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            snippet = generateAttemptSnippet(
                    naturalLanguage, preparedContext, attempt, snippet, lastResult, lastTestResult);

            AssembledRuleClass assembled = assembler.assembleCompileUnit(
                    snippet, preparedContext, "RuleTransCandidate" + String.format("%03d", attempt));
            lastResult = compilationProcessor.compile(assembled);
            if (!lastResult.success()) {
                lastTestResult = null;
                continue;
            }

            if (testCaseSet == null) {
                testCaseSet = testCaseGenerator == null
                        ? RuleTransTestCaseSet.empty()
                        : testCaseGenerator.generate(naturalLanguage, preparedContext, snippet);
            }
            if (testCaseSet.isEmpty()) {
                return new RuleTransResult(true, snippet, attempt, lastResult, null);
            }

            AssembledRuleClass assembledTest = assembler.assembleExecutableTest(
                    snippet,
                    preparedContext,
                    testCaseSet,
                    "RuleTransExecutableTest" + String.format("%03d", attempt));
            CompilationResult testCompilation = compilationProcessor.compile(assembledTest);
            if (!testCompilation.success()) {
                throw new RuleTransException("RuleTrans generated test compilation failed: "
                        + String.join("\n", testCompilation.errors()));
            }
            lastTestResult = testExecutionProcessor.execute(assembledTest);
            if (lastTestResult.success()) {
                return new RuleTransResult(true, snippet, attempt, lastResult, lastTestResult);
            }
            if (!hasLikelyRuleLogicError(lastTestResult)) {
                return new RuleTransResult(false, snippet, attempt, lastResult, lastTestResult);
            }
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return new RuleTransResult(false, snippet, attemptsLimit, lastResult, lastTestResult);
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

    private String generateAttemptSnippet(
            String naturalLanguage,
            RuleContext context,
            int attempt,
            String previousSnippet,
            CompilationResult lastCompilation,
            TestExecutionResult lastTestResult) {
        if (attempt == 1) {
            return generator.generate(naturalLanguage, context);
        }
        if (lastCompilation != null && !lastCompilation.success()) {
            return generator.generateFromPrompt(promptBuilder.buildCompilationCorrectionPrompt(
                    naturalLanguage, context, previousSnippet, lastCompilation));
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return generator.generateFromPrompt(promptBuilder.buildTestCorrectionPrompt(
                    naturalLanguage, context, previousSnippet, lastTestResult.failedCases()));
        }
        return generator.generate(naturalLanguage, context);
    }

    private boolean hasLikelyRuleLogicError(TestExecutionResult result) {
        return result.failedCases().stream().anyMatch(failed -> failed.likelyRuleLogicError());
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
