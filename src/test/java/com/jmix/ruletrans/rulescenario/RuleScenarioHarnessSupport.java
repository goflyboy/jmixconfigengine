package com.jmix.ruletrans.rulescenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.postprocessor.TestExecutionResult;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.testgen.RuleTransTestCase;
import com.jmix.ruletrans.testgen.RuleTransTestCaseSet;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

abstract class RuleScenarioHarnessSupport {

    static final String TEMP_RESOURCE_PATH = "target/ruletrans-rulescenario-resources";

    private final RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
    private final RuleTransTempFileManager tempFileManager =
            new RuleTransTempFileManager(Path.of("target/ruletrans-rulescenario"));
    private final RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
    private final CompilationProcessor compilationProcessor = new CompilationProcessor(tempFileManager);
    private final TestExecutionProcessor testExecutionProcessor = new TestExecutionProcessor(tempFileManager);

    protected ProductRuleContext productContext(Class<?> algClass) {
        return RuleContextFactory.fromAnnotatedClass(algClass, TEMP_RESOURCE_PATH);
    }

    protected PartCategoryRuleContext partCategoryContext(Class<?> algClass, String categoryCode) {
        return RuleContextFactory.partCategory(productContext(algClass).module(), categoryCode);
    }

    protected RuleMetadata metadata(
            String ruleCode,
            String naturalLanguage,
            String fatherCode,
            String attrParaCodes) {
        return new RuleMetadata(ruleCode, ruleCode, naturalLanguage, fatherCode, attrParaCodes, "", "");
    }

    protected RuleTransTestCaseSet caseSet(RuleTransTestCase... cases) {
        return new RuleTransTestCaseSet("", List.of(cases));
    }

    protected RuleTransTestCase validateCase(String id, boolean expectedValid, String... selectedParts) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_VALIDATE,
                Arrays.asList(selectedParts),
                expectedValid,
                null,
                null,
                null);
    }

    protected RuleTransTestCase recommendCase(String id, String expectedResult, String... requests) {
        return recommendCase(id, expectedResult, null, null, requests);
    }

    protected RuleTransTestCase recommendCase(
            String id,
            String expectedResult,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedFirstPartQuantities,
            String... requests) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_RECOMMEND,
                null,
                null,
                null,
                Arrays.asList(requests),
                expectedResult,
                null,
                null,
                null,
                expectedSolutionCount,
                null,
                null,
                null,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase inferPartCase(
            String id,
            String partCode,
            int quantity,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_INFER_PART,
                null,
                null,
                null,
                null,
                null,
                partCode,
                quantity,
                preParas,
                expectedSolutionCount,
                expectedConditionCounts,
                expectedFirstParaValues,
                expectedFirstParaHidden,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase inferParaCase(
            String id,
            List<String> preParas,
            Integer expectedSolutionCount,
            Map<String, Integer> expectedConditionCounts,
            Map<String, String> expectedFirstParaValues,
            Map<String, Boolean> expectedFirstParaHidden,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_INFER_PARA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                preParas,
                expectedSolutionCount,
                expectedConditionCounts,
                expectedFirstParaValues,
                expectedFirstParaHidden,
                expectedFirstPartQuantities,
                null,
                null,
                null);
    }

    protected RuleTransTestCase postRecommendCase(
            String id,
            List<String> requests,
            Integer expectedSolutionCount,
            List<String> expectedAllParaNonBlank,
            Map<String, Integer> expectedAllParaMinValues,
            Map<String, String> expectedAllParaValues,
            Map<String, Integer> expectedFirstPartQuantities) {
        return new RuleTransTestCase(
                id,
                RuleTransTestCase.TYPE_POST_RECOMMEND,
                null,
                null,
                null,
                requests,
                null,
                null,
                null,
                null,
                expectedSolutionCount,
                null,
                null,
                null,
                expectedFirstPartQuantities,
                expectedAllParaNonBlank,
                expectedAllParaMinValues,
                expectedAllParaValues);
    }

    protected void assertExecutableScenario(
            String methodBody,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleTransTestCaseSet testCaseSet,
            String className) {
        String processedBody = postProcessor.processMethodBody(methodBody, scenario.sdkProfile());

        AssembledRuleClass compileUnit = assembler.assembleCompileUnit(
                processedBody, context, scenario, metadata, className + "CompileUnit");
        CompilationResult compileResult = compilationProcessor.compile(compileUnit);
        assertTrue(compileResult.success(), String.join("\n", compileResult.errors()));

        AssembledRuleClass executableTest = assembler.assembleExecutableTest(
                processedBody, context, scenario, metadata, testCaseSet, className);
        CompilationResult testCompileResult = compilationProcessor.compile(executableTest);
        assertTrue(testCompileResult.success(), String.join("\n", testCompileResult.errors())
                + "\n" + executableTest.sourceCode());

        TestExecutionResult result = testExecutionProcessor.execute(executableTest);
        assertTrue(result.success(), String.valueOf(result.failedCases()));
        assertTrue(result.testsSucceeded() > 0, String.valueOf(result));
    }
}
