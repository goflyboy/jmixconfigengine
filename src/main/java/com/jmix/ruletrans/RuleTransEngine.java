package com.jmix.ruletrans;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.FailedTestCase;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScenarioClassifier;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruleunit.DefaultRuleUnitTestExecutorService;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;
import com.jmix.ruleunit.RuleUnitTestReport;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;

import com.jmix.executor.bmodel.PartCategory;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.net.URLClassLoader;
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

    /**
     * 将自然语言规则翻译为代码方法体（无重试）。
     *
     * @param naturalLanguage 自然语言描述的规则
     * @param context 规则上下文（包含模块、模块级别信息等）
     * @return 生成的代码方法体
     */
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
        BusinessRuleTestCaseSet businessTestCaseSet = null;

        for (int attempt = 1; attempt <= attemptsLimit; attempt++) {
            methodBody = generateAttemptMethodBody(
                    naturalLanguage, preparedContext, scenario, attempt, methodBody, lastResult, lastTestResult);

            AssembledRuleClass assembled = assembler.assembleCompileUnit(
                    methodBody,
                    preparedContext,
                    scenario,
                    metadata,
                    "RuleTransCandidate" + String.format("%03d", attempt) + "Constraint");
            lastResult = compilationProcessor.compile(assembled);
            if (!lastResult.success()) {
                lastTestResult = null;
                continue;
            }

            if (businessTestCaseSet == null) {
                businessTestCaseSet = testCaseGenerator == null
                        ? BusinessRuleTestCaseSet.empty()
                        : testCaseGenerator.generateBusinessCases(
                                naturalLanguage, preparedContext, scenario, methodBody);
            }
            if (businessTestCaseSet.isEmpty()) {
                return new RuleTransResult(true, methodBody, attempt, lastResult, null, businessTestCaseSet);
            }

            lastTestResult = executeBusinessCases(assembled, businessTestCaseSet);
            if (lastTestResult.success()) {
                return new RuleTransResult(
                        true, methodBody, attempt, lastResult, lastTestResult, businessTestCaseSet);
            }
            if (!hasLikelyRuleLogicError(lastTestResult)) {
                return new RuleTransResult(
                        false, methodBody, attempt, lastResult, lastTestResult, businessTestCaseSet);
            }
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return new RuleTransResult(
                    false, methodBody, attemptsLimit, lastResult, lastTestResult, businessTestCaseSet);
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
        if (!context.isModuleLevel() || !context.targetCategories().isEmpty()) {
            return context;
        }
        List<String> categoryCodes = identifier.identify(naturalLanguage, context.module());
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> context.module().getPartCategory(code))
                .toList();
        return new ModuleRuleContext(context.module(), categories);
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
                    context,
                    naturalLanguage);
        }
        if (lastTestResult != null && !lastTestResult.success()) {
            return generator.generateMethodBodyFromPrompt(
                    promptBuilder.buildTestCorrectionPrompt(
                            naturalLanguage, context, scenario, previousMethodBody, lastTestResult.failedCases()),
                    scenario.sdkProfile(),
                    context,
                    naturalLanguage);
        }
        return generator.generateMethodBody(naturalLanguage, context, scenario);
    }

    private boolean hasLikelyRuleLogicError(TestExecutionResult result) {
        return result.failedCases().stream().anyMatch(failed -> failed.likelyRuleLogicError());
    }

    private TestExecutionResult executeBusinessCases(
            AssembledRuleClass assembled,
            BusinessRuleTestCaseSet businessTestCaseSet) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = generatedClassLoader(originalClassLoader)) {
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> ruleClass = Class.forName(assembled.qualifiedClassName(), true, loader);
            Module module = ModuleGenneratorByAnno.buildModule(ruleClass);
            Result<Void> initResult = ModuleConstraintExecutor.INST.init(ruleTransConstraintConfig());
            if (initResult.getCode() != Result.SUCCESS) {
                return failedExecutionResult("RuleUnit init failed: " + initResult.getMessage());
            }
            Result<Void> addResult = ModuleConstraintExecutor.INST.addModule(module.getId(), module);
            if (addResult.getCode() != Result.SUCCESS) {
                return failedExecutionResult("RuleUnit add module failed: " + addResult.getMessage());
            }
            DefaultRuleUnitTestExecutorService service = new DefaultRuleUnitTestExecutorService(module);
            RuleUnitTestCaseSetReport report = service.executeCaseSet(businessTestCaseSet);
            return toTestExecutionResult(report);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
            return failedExecutionResult(message);
        } finally {
            ModuleConstraintExecutor.INST.fini();
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private URLClassLoader generatedClassLoader(ClassLoader parent) throws Exception {
        URL classesRoot = assembler.tempFileManager().classesRoot().toUri().toURL();
        return new URLClassLoader(new URL[] {classesRoot}, parent);
    }

    private ConstraintConfig ruleTransConstraintConfig() {
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        return config;
    }

    private TestExecutionResult toTestExecutionResult(RuleUnitTestCaseSetReport report) {
        long found = report.caseReports().size();
        long failed = report.caseReports().stream().filter(caseReport -> !caseReport.passed()).count();
        long succeeded = found - failed;
        return new TestExecutionResult(
                report.passed(),
                found,
                succeeded,
                failed,
                report.caseReports().stream()
                        .filter(caseReport -> !caseReport.passed())
                        .map(this::toFailedTestCase)
                        .toList());
    }

    private TestExecutionResult failedExecutionResult(String message) {
        FailedTestCase failed = new FailedTestCase(
                "ruleunit",
                "ruleunit",
                "",
                "",
                "",
                message,
                false);
        return new TestExecutionResult(false, 1, 0, 1, List.of(failed));
    }

    private FailedTestCase toFailedTestCase(RuleUnitTestReport report) {
        return new FailedTestCase(
                report.caseId(),
                report.caseId(),
                "",
                "",
                report.actual() == null ? "" : report.actual().toString(),
                String.join("\n", report.failures()),
                true);
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
