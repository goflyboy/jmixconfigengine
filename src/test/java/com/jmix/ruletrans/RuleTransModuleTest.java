package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.ProductRuleContext;
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
import com.jmix.tool.impl.llm.LLMInvokerImpl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * RFC-0011 RuleTrans engine tests.
 */
public class RuleTransModuleTest {

    @Test
    public void testPartCategoryRuleGenerationReturnsPureSnippet() {
        RuleTransEngine engine = engine(realLlm());

        String snippet = engine.translate("CPU at most one", RuleContextFactory.partCategory(sampleModule(), "cpu"));

        assertTrue(snippet.contains("@CodeRuleAnno"), snippet);
        assertTrue(snippet.contains("cpu"), snippet);
        assertFalse(snippet.contains("package "), snippet);
        assertFalse(snippet.contains("class "), snippet);
    }

    @Test
    public void testProductTranslateUsesIdentifiedCategories() {
        ProductRuleContext context = RuleContextFactory.product(sampleModule());

        String snippet = engine(realLlm()).translate("4-core CPU cannot use 5400 drive", context);

        assertTrue(snippet.contains("@CodeRuleAnno"), snippet);
        assertTrue(snippet.contains("cpu"), snippet);
        assertTrue(snippet.contains("drive"), snippet);
        assertFalse(snippet.contains("package "), snippet);
        assertFalse(snippet.contains("class "), snippet);
    }

    @Test
    public void testPartCategoryTranslateWithRetryCompilesRealSnippet() {
        RuleTransResult result = engine(realLlm()).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertTrue(result.compilationResult().success(), String.join("\n", result.compilationResult().errors()));
        assertTrue(result.snippet().contains("@CodeRuleAnno"), result.snippet());
    }

    @Test
    public void testPartCategoryEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(realLlm()).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testProductEndToEndGeneratedTestCasesPass() {
        RuleTransResult result = engineWithGeneratedCases(realLlm()).translateWithRetry(
                "4-core CPU cannot use 5400 drive",
                RuleContextFactory.product(sampleModule()),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().testsSucceeded() > 0);
    }

    @Test
    public void testPartCategoryGeneratedTestCasesPassWithRetryBudget() {
        RuleTransResult result = engineWithGeneratedCases(realLlm()).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertTrue(result.testExecutionResult().success());
    }

    @Test
    public void testBoundaryInputs() {
        RuleTransEngine engine = engine(realLlm());

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("CPU at most one", null));
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

    private LLMInvoker realLlm() {
        return RealLlmHolder.INSTANCE;
    }

    private static final class RealLlmHolder {
        private static final LLMInvoker INSTANCE = new LLMInvokerImpl();
    }
}
