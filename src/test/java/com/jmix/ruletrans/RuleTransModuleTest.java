package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;
import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

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
import com.jmix.ruletrans.postprocessor.RuleUnitCaseExecutionProcessor;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/**
 * RuleTrans engine and pipeline tests.
 */
public class RuleTransModuleTest {

    @Test
    public void testPartCategoryRuleGenerationReturnsPureMethodBody() {
        RuleTransEngine engine = engine(realLlmInvoker());

        String methodBody = engine.translate(
                "cpu can select at most one part",
                RuleContextFactory.partCategory(sampleModule(), "cpu"));

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

        String methodBody = engine(realLlmInvoker()).translate(
                "cpu CoreNum=4 cannot be selected together with drive Speed=5400",
                context);

        assertTrue(methodBody.contains("cpu"), methodBody);
        assertTrue(methodBody.contains("drive"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
        assertFalse(methodBody.contains("package "), methodBody);
        assertFalse(methodBody.contains("class "), methodBody);
    }

    @Test
    public void testPartCategoryPipelineCompilesMethodBody() {
        RuleTransPipelineResult result = pipeline(realLlmInvoker(), false).execute(new RuleTransRequest(
                // "cpu can select at most one part",
                "CPU最多只能配一个",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1,
                RuleTransPipelineOptions.compileOnly()));

        assertTrue(result.success(), result.messages().toString());
        assertTrue(result.compilationResult().success(), String.join("\n", result.compilationResult().errors()));
        assertTrue(result.methodBody().contains("model().addLessOrEqual"), result.methodBody());
        assertFalse(result.methodBody().contains("@CodeRuleAnno"), result.methodBody());
    }

    @Test
    public void testPartCategoryEndToEndGeneratedTestCasesPass() {
        RuleTransPipelineResult result = pipeline(realLlmInvoker(), true).execute(new RuleTransRequest(
                "cpu can select at most one part",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                0,
                RuleTransPipelineOptions.defaults()));

        assertTrue(result.success(), result.messages().toString());
        assertNotNull(result.ruleUnitReport());
        assertTrue(result.ruleUnitReport().caseReports().stream().anyMatch(report -> report.passed()));
    }

    @Test
    public void testModuleEndToEndGeneratedTestCasesPass() {
        RuleTransPipelineResult result = pipeline(realLlmInvoker(), true).execute(new RuleTransRequest(
                "cpu CoreNum=4 cannot be selected together with drive Speed=5400",
                RuleContextFactory.module(sampleModule()),
                0,
                RuleTransPipelineOptions.defaults()));

        assertTrue(result.success(), result.messages().toString());
        assertNotNull(result.ruleUnitReport());
        assertTrue(result.ruleUnitReport().caseReports().stream().anyMatch(report -> report.passed()));
    }

    @Test
    public void testPartCategoryGeneratedTestCasesPassWithRetryBudget() {
        RuleTransPipelineResult result = pipeline(realLlmInvoker(), true).execute(new RuleTransRequest(
                "cpu can select at most one part",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1,
                RuleTransPipelineOptions.defaults()));

        assertTrue(result.success(), result.messages().toString());
        assertNotNull(result.ruleUnitReport());
        assertTrue(result.ruleUnitReport().passed());
    }

    @Test
    public void testBoundaryInputs() {
        RuleTransEngine engine = engine(null);

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("cpu can select at most one part", null));
    }

    private RuleTransEngine engine(LLMInvoker llmInvoker) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        return new RuleTransEngine(
                new CategoryIdentifier(llmInvoker, promptBuilder),
                new RuleSnippetGenerator(llmInvoker, promptBuilder, postProcessor),
                promptBuilder);
    }

    private RuleTransPipeline pipeline(LLMInvoker llmInvoker, boolean generateCases) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        return new RuleTransPipeline(
                new CategoryIdentifier(llmInvoker, promptBuilder),
                new RuleSnippetGenerator(llmInvoker, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(generateCases ? llmInvoker : null, promptBuilder),
                new RuleUnitCaseExecutionProcessor(tempFileManager),
                promptBuilder);
    }
}
