package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.cpuAtMostOneMethodBody;
import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.ModuleRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

public class RuleSnippetAssemblerTest {

    @Test
    public void testGeneratedMethodBodyCompiles() {
        PartCategoryRuleContext context = RuleContextFactory.partCategory(sampleModule(), "cpu");
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
        CompilationProcessor processor = new CompilationProcessor(tempFileManager);

        AssembledRuleClass assembled = assembler.assembleCompileUnit(cpuAtMostOneMethodBody(), context,
                "RuleTransCompileOk");
        CompilationResult result = processor.compile(assembled);

        assertTrue(assembled.sourceCode().contains("@CodeRuleAnno"), assembled.sourceCode());
        assertTrue(assembled.sourceCode().contains("public void"), assembled.sourceCode());
        assertTrue(result.success(), String.join("\n", result.errors()));
    }

    @Test
    public void testPriorityAggregateExpressionCanCompareWithParameterValueVar() {
        ModuleRuleContext baseContext = RuleContextFactory.fromAnnotatedClass(
                PriorityExpressionFacts.class,
                "target/ruletrans-test-resources");
        ModuleRuleContext context = RuleContextFactory.module(baseContext.module(), List.of("accelerator"));
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
        CompilationProcessor processor = new CompilationProcessor(tempFileManager);
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.PRIORITY);
        RuleMetadata metadata = new RuleMetadata(
                "rulePriorityParameter",
                "rulePriorityParameter",
                "accelerator 的总数量必须至少达到整数参数 target，并优先选择属性 Score 更高的部件",
                "",
                "",
                "",
                "");

        AssembledRuleClass assembled = assembler.assembleCompileUnit("""
                PartAlgCPLinearExpr totalQty = model().sum4Quantity("accelerator", "", "");
                model().addGreaterOrEqual(totalQty, para("target").valueVar());

                PartAlgCPLinearExpr totalWeightedScore = model().sum4Quantity("accelerator", "Score", "");
                PartAlgCPLinearExpr objExpr = model().newPartLinearExpr("priority");
                objExpr.addExpr(totalWeightedScore, -1000);
                objExpr.addExpr(totalQty, 1);
                model().setObjectExpr(objExpr);
                updatePriorityObjectFuntion(ruleCode, objExpr);
                """, context, scenario, metadata, "RuleTransPriorityExpressionCompileOk");
        CompilationResult result = processor.compile(assembled);

        assertTrue(result.success(), String.join("\n", result.errors()) + "\n" + assembled.sourceCode());
    }

    @ModuleAnno(id = 801101L)
    public static class PriorityExpressionFacts extends ModuleAlgBase {

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar target;

        @PartAnno(code = "accelerator")
        @DAttrAnno1(code = "Score", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Score_10:10", "Score_1:1"})
        private PartCategoryVar accelerator;

        @PartAnno(fatherCode = "accelerator", attrs = {"10"}, maxQuantity = 1)
        private PartVar fast;

        @PartAnno(fatherCode = "accelerator", attrs = {"1"}, maxQuantity = 1)
        private PartVar economy;
    }
}
