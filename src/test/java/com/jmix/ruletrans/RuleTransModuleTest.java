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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
        FakeLLMInvoker fake = new FakeLLMInvoker(cpuAtMostOneSnippet());
        RuleTransEngine engine = engine(fake);

        String snippet = engine.translate("CPU at most one", RuleContextFactory.partCategory(sampleModule(), "cpu"));

        assertTrue(snippet.contains("@CodeRuleAnno"));
        assertFalse(snippet.contains("package "));
        assertFalse(snippet.contains("class "));
    }

    @Test
    public void testProductStage1IdentifiesAndValidatesCategories() {
        FakeLLMInvoker fake = new FakeLLMInvoker("[\"cpu\", \"drive\"]");
        CategoryIdentifier identifier = new CategoryIdentifier(fake, new PromptBuilder());

        List<String> result = identifier.identify("4-core CPU cannot use 5400 drive", sampleModule());

        assertTrue(result.contains("cpu"));
        assertTrue(result.contains("drive"));
    }

    @Test
    public void testProductStage1RejectsMissingCategory() {
        FakeLLMInvoker fake = new FakeLLMInvoker("[\"missing\"]");
        CategoryIdentifier identifier = new CategoryIdentifier(fake, new PromptBuilder());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> identifier.identify("unknown category", sampleModule()));
        assertTrue(ex.getMessage().contains("missing"));
    }

    @Test
    public void testProductTranslateUsesIdentifiedCategories() {
        FakeLLMInvoker fake = new FakeLLMInvoker("[\"cpu\", \"drive\"]", crossCategorySnippet());
        ProductRuleContext context = RuleContextFactory.product(sampleModule());

        String snippet = engine(fake).translate("4-core CPU cannot use 5400 drive", context);

        assertTrue(snippet.contains("cpu4NotDrive5400"));
        assertTrue(fake.prompts().get(1).contains("\"cpu\""));
        assertTrue(fake.prompts().get(1).contains("\"drive\""));
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
    public void testCompilationErrorDrivesRetry() {
        FakeLLMInvoker fake = new FakeLLMInvoker(cpuAtMostOneBadSnippet(), cpuAtMostOneSnippet());

        RuleTransResult result = engine(fake).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertTrue(result.snippet().contains("addLessOrEqual"));
        assertTrue(fake.prompts().get(1).contains("addLessOrEquals"));
    }

    @Test
    public void testPartCategoryEndToEndGeneratedTestCasesPass() {
        FakeLLMInvoker fake = new FakeLLMInvoker(cpuAtMostOneSnippet(), cpuAtMostOneTestCases());

        RuleTransResult result = engineWithGeneratedCases(fake).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertEquals(2, result.testExecutionResult().testsSucceeded());
    }

    @Test
    public void testProductEndToEndGeneratedTestCasesPass() {
        FakeLLMInvoker fake = new FakeLLMInvoker(
                "[\"cpu\", \"drive\"]",
                crossCategorySnippet(),
                crossCategoryTestCases());

        RuleTransResult result = engineWithGeneratedCases(fake).translateWithRetry(
                "4-core CPU cannot use 5400 drive",
                RuleContextFactory.product(sampleModule()),
                0);

        assertTrue(result.success(), String.valueOf(result.testExecutionResult()));
        assertNotNull(result.testExecutionResult());
        assertEquals(3, result.testExecutionResult().testsSucceeded());
    }

    @Test
    public void testTestFailureDrivesRetry() {
        FakeLLMInvoker fake = new FakeLLMInvoker(
                cpuAtLeastTwoSnippet(),
                cpuAtMostOneTestCases(),
                cpuAtMostOneSnippet());

        RuleTransResult result = engineWithGeneratedCases(fake).translateWithRetry(
                "CPU at most one",
                RuleContextFactory.partCategory(sampleModule(), "cpu"),
                1);

        assertTrue(result.success());
        assertTrue(result.snippet().contains("addLessOrEqual"));
        assertTrue(fake.prompts().get(2).contains("failed tests"));
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
        RuleTransEngine engine = engine(new FakeLLMInvoker(cpuAtMostOneSnippet()));

        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("", RuleContextFactory.partCategory(sampleModule(), "cpu")));
        assertThrows(IllegalArgumentException.class,
                () -> engine.translate("CPU at most one", null));
        assertThrows(CategoryNotFoundException.class,
                () -> new CategoryIdentifier(new FakeLLMInvoker("[]"), new PromptBuilder())
                        .identify("cannot identify", sampleModule()));
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

    private RuleTransEngine engine(FakeLLMInvoker fake) {
        return engine(fake, false);
    }

    private RuleTransEngine engineWithGeneratedCases(FakeLLMInvoker fake) {
        return engine(fake, true);
    }

    private RuleTransEngine engine(FakeLLMInvoker fake, boolean generateCases) {
        RulePromptProjector projector = new RulePromptProjector();
        PromptBuilder promptBuilder = new PromptBuilder(projector);
        RuleSnippetPostProcessor postProcessor = new RuleSnippetPostProcessor();
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        return new RuleTransEngine(
                new CategoryIdentifier(fake, promptBuilder),
                new RuleSnippetGenerator(fake, promptBuilder, postProcessor),
                new RuleSnippetAssembler(tempFileManager),
                new CompilationProcessor(tempFileManager),
                new RuleTestCaseGenerator(generateCases ? fake : null, promptBuilder),
                new TestExecutionProcessor(tempFileManager),
                promptBuilder);
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

    private String cpuAtMostOneBadSnippet() {
        return """
                @CodeRuleAnno(normalNaturalCode = "CPU at most one", fatherCode = "cpu")
                public void ruleCpuAtMostOne() {
                    model().addLessOrEquals(model().sum4Selected("cpu", "", ""), 1);
                }
                """;
    }

    private String cpuAtLeastTwoSnippet() {
        return """
                @CodeRuleAnno(normalNaturalCode = "CPU at most one", fatherCode = "cpu")
                public void ruleCpuAtMostOne() {
                    model().addGreaterOrEqual(model().sum4Selected("cpu", "", ""), 2);
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

    private String cpuAtMostOneTestCases() {
        return """
                {
                  "ruleMethod": "ruleCpuAtMostOne",
                  "cases": [
                    {
                      "id": "cpu_single_valid",
                      "type": "validate",
                      "selectedParts": ["cpu4"],
                      "expectedValid": true
                    },
                    {
                      "id": "cpu_double_invalid",
                      "type": "validate",
                      "selectedParts": ["cpu4", "cpu8"],
                      "expectedValid": false,
                      "expectedViolatedRuleCodes": ["ruleCpuAtMostOne"]
                    }
                  ]
                }
                """;
    }

    private String crossCategoryTestCases() {
        return """
                {
                  "ruleMethod": "cpu4NotDrive5400",
                  "cases": [
                    {
                      "id": "forbidden_pair_invalid",
                      "type": "validate",
                      "selectedParts": ["cpu4", "drive5400"],
                      "expectedValid": false,
                      "expectedViolatedRuleCodes": ["cpu4NotDrive5400"]
                    },
                    {
                      "id": "allowed_pair_valid",
                      "type": "validate",
                      "selectedParts": ["cpu8", "drive7200"],
                      "expectedValid": true
                    },
                    {
                      "id": "recommend_forbidden_pair_no_solution",
                      "type": "recommend",
                      "requests": [
                        "cpu:Sum_Quantity ==1 where CoreNum=4",
                        "drive:Sum_Quantity ==1 where Speed=5400"
                      ],
                      "expectedResult": "NO_SOLUTION"
                    }
                  ]
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

    private static final class FakeLLMInvoker implements LLMInvoker {

        private final Deque<String> responses = new ArrayDeque<>();
        private final List<String> prompts = new ArrayList<>();

        private FakeLLMInvoker(String... responses) {
            this.responses.addAll(Arrays.asList(responses));
        }

        @Override
        public String generate(String systemMessage, String userMessage) {
            prompts.add(userMessage);
            if (responses.isEmpty()) {
                throw new IllegalStateException("No fake LLM response left");
            }
            return responses.removeFirst();
        }

        @Override
        public String getConfigInfo() {
            return "fake";
        }

        private List<String> prompts() {
            return prompts;
        }
    }
}
