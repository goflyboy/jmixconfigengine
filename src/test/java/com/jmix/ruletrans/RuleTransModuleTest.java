package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;
import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * RFC-0011 RuleTrans engine tests.
 */
public class RuleTransModuleTest {

    @Test
    public void testPartCategoryRuleGenerationReturnsPureMethodBody() {
        RuleTransEngine engine = engine(realLlmInvoker());

        String methodBody = engine.translate("cpu 最多只能选择一个部件", RuleContextFactory.partCategory(sampleModule(), "cpu"));

        assertTrue(methodBody.contains("model().addLessOrEqual"), methodBody);
        assertTrue(methodBody.contains("sum4Selected"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
        assertFalse(methodBody.contains("package "), methodBody);
        assertFalse(methodBody.contains("class "), methodBody);
    }

    @Test
    public void testModuleTranslateUsesIdentifiedCategories() {
        ModuleRuleContext context = RuleContextFactory.module(sampleModule());

        String methodBody = engine(realLlmInvoker())
                .translate("cpu 中属性 CoreNum 为 4 的部件不能和 drive 中属性 Speed 为 5400 的部件同时选择", context);

        assertTrue(methodBody.contains("cpu"), methodBody);
        assertTrue(methodBody.contains("drive"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
        assertFalse(methodBody.contains("package "), methodBody);
        assertFalse(methodBody.contains("class "), methodBody);
    }

    @Test
    public void testPartCategoryTranslateWithRetryCompilesMethodBody() {
        RuleTransResult result = engine(realLlmInvoker()).translateWithRetry(
                "cpu 最多只能选择一个部件",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertTrue(result.compilationResult().success(), String.join("\n", result.compilationResult().errors()));
        assertTrue(result.methodBody().contains("model().addLessOrEqual"), result.methodBody());
        assertFalse(result.methodBody().contains("@CodeRuleAnno"), result.methodBody());
    }

    @Test
    public void testPartCategoryEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(realLlmInvoker()).translateWithRetry(
                "cpu 最多只能选择一个部件",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testModuleEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(realLlmInvoker()).translateWithRetry(
                "cpu 中属性 CoreNum 为 4 的部件不能和 drive 中属性 Speed 为 5400 的部件同时选择",
                RuleContextFactory.module(sampleModule()),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testPartCategoryGeneratedTestCasesPassWithRetryBudget() {
        RuleTransResult result = engineWithGeneratedCases(realLlmInvoker()).translateWithRetry(
                "cpu 最多只能选择一个部件",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().success());
    }

    @Test
    public void testBoundaryInputs() {
        RuleTransEngine engine = engine(null);

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("cpu 最多只能选择一个部件", null));
    }

    private RuleTransEngine engine(LLMInvoker llmInvoker) {
        return engine(llmInvoker, false);
    }

    private RuleTransEngine engineWithGeneratedCases(LLMInvoker llmInvoker) {
        return engine(llmInvoker, true);
    }

    private RuleTransEngine engine(LLMInvoker llmInvoker, boolean generateCases) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        return new RuleTransEngine(
                new CategoryIdentifier(llmInvoker, promptBuilder),
                new RuleSnippetGenerator(llmInvoker, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(generateCases ? llmInvoker : null, promptBuilder),
                new TestExecutionProcessor(tempFileManager),
                promptBuilder);
    }

}
