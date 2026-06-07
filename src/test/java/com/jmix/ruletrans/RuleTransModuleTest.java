package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.ProductRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.generator.RuleSnippetGenerator;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.ir.RuleTransIrAssessment;
import com.jmix.ruletrans.ir.RuleTransIrEvaluator;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;
import com.jmix.ruletrans.prompt.PartCategoryPromptView;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.prompt.RulePromptProjector;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.impl.llm.LLMInvoker;
import com.jmix.tool.impl.llm.LLMInvokerImpl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

/**
 * RFC-0011 RuleTrans module tests.
 */
public class RuleTransModuleTest {

    private static final String TEMP_RESOURCE_PATH = "target/ruletrans-test-resources";

    @Test
    public void testBuildPartCategoryRuleContextFromAnnotatedClass() {
        Module module = sampleModule();
        PartCategoryRuleContext context = RuleContextFactory.partCategory(module, "cpu");
        PartCategoryPromptView view = new RulePromptProjector().projectPartCategory(context);

        assertTrue(view.dynAttrSchemas().stream().anyMatch(attr -> "CoreNum".equals(attr.code())));
        assertTrue(view.attrParas().stream().anyMatch(attr -> "CoreNum".equals(attr.attrCode())));
        assertFalse(view.atomicParts().isEmpty());
    }

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
    public void testGeneratedSnippetCompiles() {
        PartCategoryRuleContext context = RuleContextFactory.partCategory(sampleModule(), "cpu");
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
        CompilationProcessor processor = new CompilationProcessor(tempFileManager);

        AssembledRuleClass assembled = assembler.assembleCompileUnit(cpuAtMostOneSnippet(), context,
                "RuleTransCompileOk");
        CompilationResult result = processor.compile(assembled);

        assertTrue(result.success(), String.join("\n", result.errors()));
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
    public void testP2IrAssessmentReportsModuleAlgBaseBoundPath() {
        RuleTransIrAssessment assessment = new RuleTransIrEvaluator().assess(
                RuleContextFactory.product(sampleModule(), List.of("cpu", "drive")),
                crossCategorySnippet());

        assertEquals(RuleTransIrAssessment.Status.MODULE_ALG_BASE_BOUND, assessment.status());
        assertFalse(assessment.schemaEmissionReady());
        assertTrue(assessment.targetCategories().contains("cpu"));
        assertFalse(assessment.blockers().isEmpty());
    }

    @Test
    public void testBoundaryInputs() {
        RuleTransEngine engine = engine(realLlm());

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("CPU at most one", null));
    }

    @Test
    public void testSnippetPostProcessorRejectsWholeClass() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.process("package x; public class Bad { " + cpuAtMostOneSnippet() + " }"));
    }

    @Test
    public void testModelHelperRemainsUnchanged() throws Exception {
        String source = java.nio.file.Files.readString(Path.of("src/main/java/com/jmix/tool/ModelHelper.java"));

        assertFalse(source.contains("ruletrans"));
        assertFalse(source.contains("RuleTransEngine"));
    }

    @Test
    public void testTestExecutionProcessorRunsJunitClass() {
        TestExecutionProcessor processor = new TestExecutionProcessor();

        assertDoesNotThrow(() -> assertTrue(processor.execute(PassingJUnitCase.class).success()));
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

    private Module sampleModule() {
        return ModuleGenneratorByAnno.build(SampleRuleTransConstraint.class, TEMP_RESOURCE_PATH);
    }

    private String cpuAtMostOneSnippet() {
        return """
                @CodeRuleAnno(normalNaturalCode = "CPU at most one", fatherCode = "cpu")
                public void ruleCpuAtMostOne() {
                    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                }
                """;
    }

    private String crossCategorySnippet() {
        return """
                @CodeRuleAnno(
                        normalNaturalCode = "4-core CPU cannot use 5400 drive",
                        leftProObjsStr = "cpu:Select",
                        rightProObjsStr = "drive:Select")
                public void cpu4NotDrive5400() {
                    PartAlgCPLinearExpr cpu4Selected = model().sum4Selected("cpu", "", "CoreNum=4")
                            .name("cpu4Selected");
                    PartAlgCPLinearExpr drive5400Selected = model().sum4Selected("drive", "", "Speed=5400")
                            .name("drive5400Selected");
                    model().addLessOrEqual(forbiddenPair(cpu4Selected, drive5400Selected), 1);
                }

                private PartAlgCPLinearExpr forbiddenPair(PartAlgCPLinearExpr left, PartAlgCPLinearExpr right) {
                    return model().newPartLinearExpr("cpu4_drive5400_pair")
                            .addExpr(left, 1)
                            .addExpr(right, 1);
                }
                """;
    }

    @ModuleAnno(id = 8011L)
    public static class SampleRuleTransConstraint extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;

        @CodeRuleAnno(fatherCode = "cpu", attrParaCodes = "CoreNum:Sum")
        public void existingCpuRule() {
        }
    }

    public static class PassingJUnitCase {

        @Test
        public void testPasses() {
            assertTrue(true);
        }
    }

}
