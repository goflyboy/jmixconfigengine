package com.jmix.ruletrans;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.ruletrans.testgen.RuleTransTestCaseSet;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Orchestrates natural-language rule translation.
 */
@Slf4j
public final class RuleTransEngine {

    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleSnippetAssembler assembler;
    private final CompilationProcessor compilationProcessor;
    private final RuleTestCaseGenerator testCaseGenerator;
    private final TestExecutionProcessor testExecutionProcessor;
    private final PromptBuilder promptBuilder;
    private final RuleScenarioClassifier scenarioClassifier;

    public RuleTransEngine(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            RuleSnippetAssembler assembler,
            CompilationProcessor compilationProcessor,
            RuleTestCaseGenerator testCaseGenerator,
            TestExecutionProcessor testExecutionProcessor,
            PromptBuilder promptBuilder) {
        this(identifier, generator, assembler, compilationProcessor, testCaseGenerator,
                testExecutionProcessor, promptBuilder, new RuleScenarioClassifier());
    }

    public RuleTransEngine(
            CategoryIdentifier identifier,
            RuleSnippetGenerator generator,
            RuleSnippetAssembler assembler,
            CompilationProcessor compilationProcessor,
            RuleTestCaseGenerator testCaseGenerator,
            TestExecutionProcessor testExecutionProcessor,
            PromptBuilder promptBuilder,
            RuleScenarioClassifier scenarioClassifier) {
        this.identifier = identifier;
        this.generator = generator;
        this.assembler = assembler;
        this.compilationProcessor = compilationProcessor;
        this.testCaseGenerator = testCaseGenerator;
        this.testExecutionProcessor = testExecutionProcessor;
        this.promptBuilder = promptBuilder;
        this.scenarioClassifier = scenarioClassifier == null ? new RuleScenarioClassifier() : scenarioClassifier;
    }

    public String translate(String naturalLanguage, RuleContext context) {
        validate(naturalLanguage, context);
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        RuleScenario scenario = scenarioClassifier.classify(naturalLanguage, preparedContext);
        return generator.generateMethodBody(naturalLanguage, preparedContext, scenario);
    }

    public RuleTransResult translateWithRetry(String naturalLanguage, RuleContext context, int maxRetries) {
        validate(naturalLanguage, context);
        RuleContext preparedContext = prepareContext(naturalLanguage, context);
        RuleScenario scenario = scenarioClassifier.classify(naturalLanguage, preparedContext);
        RuleMetadata metadata = RuleMetadata.from(naturalLanguage, preparedContext, scenario);
        int attemptsLimit = Math.max(1, maxRetries + 1);
        String methodBody = null;
        CompilationResult lastResult = null;
        TestExecutionResult lastTestResult = null;
        RuleTransTestCaseSet testCaseSet = null;

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            methodBody = generateAttemptMethodBody(
                    naturalLanguage, preparedContext, scenario, attempt, methodBody, lastResult, lastTestResult);

            AssembledRuleClass assembled = assembler.assembleCompileUnit(
                    methodBody,
                    preparedContext,
                    scenario,
                    metadata,
                    "RuleTransCandidate" + String.format("%03d", attempt));
            lastResult = compilationProcessor.compile(assembled);
            if (!lastResult.success()) {
                lastTestResult = null;
                continue;
            }

            if (testCaseSet == null) {
                testCaseSet = testCaseGenerator == null
                        ? RuleTransTestCaseSet.empty()
                        : testCaseGenerator.generate(naturalLanguage, preparedContext, scenario, methodBody);
            }
            if (testCaseSet.isEmpty()) {
                return new RuleTransResult(true, methodBody, attempt, lastResult, null);
            }

            AssembledRuleClass assembledTest = assembler.assembleExecutableTest(
                    methodBody,
                    preparedContext,
                    scenario,
                    metadata,
                    testCaseSet,
                    "RuleTransExecutableTest" + String.format("%03d", attempt));
            CompilationResult testCompilation = compilationProcessor.compile(assembledTest);
            if (!testCompilation.success()) {
                String message = "RuleTrans generated test compilation failed: "
                        + String.join("\n", testCompilation.errors());
                log.error("RuleTrans generated test compilation failed, naturalLanguage={}, attempt={}, "
                                + "testClass={}, sourceFile={}, errors={}",
                        naturalLanguage,
                        attempt,
                        assembledTest.qualifiedClassName(),
                        testCompilation.sourceFile(),
                        testCompilation.errors());
                throw new RuleTransException(message);
            }
            lastTestResult = testExecutionProcessor.execute(assembledTest);
            if (lastTestResult.success()) {
                return new RuleTransResult(true, methodBody, attempt, lastResult, lastTestResult);
            }
            if (!hasLikelyRuleLogicError(lastTestResult)) {
                return new RuleTransResult(false, methodBody, attempt, lastResult, lastTestResult);
            }
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return new RuleTransResult(false, methodBody, attemptsLimit, lastResult, lastTestResult);
        }
        List<String> errors = lastResult == null ? List.of() : lastResult.errors();
        String message = "RuleTrans compilation retry limit exceeded: " + String.join("\n", errors);
        log.error("RuleTrans compilation retry limit exceeded, naturalLanguage={}, attemptsLimit={}, errors={}",
                naturalLanguage, attemptsLimit, errors);
        throw new RuleTransException(message);
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

    private String generateAttemptMethodBody(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            int attempt,
            String previousMethodBody,
            CompilationResult lastCompilation,
            TestExecutionResult lastTestResult) {
        if (attempt == 1) {
            return generator.generateMethodBody(naturalLanguage, context, scenario);
        }
        if (lastCompilation != null && !lastCompilation.success()) {
            return generator.generateMethodBodyFromPrompt(
                    promptBuilder.buildCompilationCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastCompilation),
                    scenario.sdkProfile(),
                    context);
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return generator.generateMethodBodyFromPrompt(
                    promptBuilder.buildTestCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastTestResult.failedCases()),
                    scenario.sdkProfile(),
                    context);
        }
        return generator.generateMethodBody(naturalLanguage, context, scenario);
    }

    private boolean hasLikelyRuleLogicError(TestExecutionResult result) {
        return result.failedCases().stream().anyMatch(failed -> failed.likelyRuleLogicError());
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
