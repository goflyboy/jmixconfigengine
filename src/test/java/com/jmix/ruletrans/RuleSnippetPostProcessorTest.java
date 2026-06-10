package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.cpuAtMostOneSnippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.sdk.SdkProfile;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

public class RuleSnippetPostProcessorTest {

    @Test
    public void testRejectsWholeClass() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.process("package x; public class Bad { " + cpuAtMostOneSnippet() + " }"));
    }

    @Test
    public void testAcceptsPlainMethodBody() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                """);

        assertEquals("model().addLessOrEqual(model().sum4Selected(\"cpu\", \"\", \"\"), 1);", methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"));
    }

    @Test
    public void testRejectsAnnotationAndMethodDeclarationOnNewPath() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class, () -> processor.processMethodBody(cpuAtMostOneSnippet()));
    }

    @Test
    public void testExtractsLegacySnippetToMethodBody() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processLegacySnippetToMethodBody(cpuAtMostOneSnippet(), SdkProfile.CONSTRAINT);

        assertTrue(methodBody.contains("model().addLessOrEqual"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
    }

    @Test
    public void testRejectsConstraintMethodBodyUsingPostApi() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        parameter("p1").setValue("1");
                        """, SdkProfile.CONSTRAINT));
    }

    @Test
    public void testRejectsPostMethodBodyUsingConstraintApi() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                        """, SdkProfile.POST));
    }

    @Test
    public void testNormalizesAggregateFilterStringOnly() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                model().addGreaterOrEqual(model().sum4Quantity("disk*", "Capacity", "PortRate=='10G'"), 100);
                String message = "left == 'right'";
                """);

        assertTrue(methodBody.contains("sum4Quantity(\"disk*\", \"Capacity\", \"PortRate=10G\")"), methodBody);
        assertTrue(methodBody.contains("String message = \"left == 'right'\";"), methodBody);
    }

    @Test
    public void testRejectsMixedWildcardAndCommaAggregateCategoryCodes() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        model().addGreaterOrEqual(
                                model().sum4Quantity("disk*,mainBoard", "Capacity", "PortRate=10G"), 100);
                        """));
    }

    @Test
    public void testRejectsUnterminatedMarkdownCodeFence() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        ```java
                        model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                        """));
    }

    @Test
    public void testRejectsPostPartCategorySumQuantityApi() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        int qty = partCategorySum("drive").sumQuantity();
                        parameter("total").setValue(String.valueOf(qty));
                        """, SdkProfile.POST));
    }

    @Test
    public void testNormalizesAggregateAddTermToAddExpr() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                PartAlgCPLinearExpr objExpr = model().newPartLinearExpr("priority");
                objExpr.addTerm(model().sum4Quantity("accelerator", "Score", ""), -1000);
                objExpr.addTerm(model().sum4Quantity("accelerator", "", ""), 1);
                """);

        assertTrue(methodBody.contains("objExpr.addExpr(model().sum4Quantity(\"accelerator\", \"Score\", \"\"), -1000);"),
                methodBody);
        assertTrue(methodBody.contains("objExpr.addExpr(model().sum4Quantity(\"accelerator\", \"\", \"\"), 1);"),
                methodBody);
    }

    @Test
    public void testNormalizesPartLinearExprVariableAddTermToAddExpr() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                PartAlgCPLinearExpr total = model().sum4Quantity("drive", "Capacity", "");
                PartAlgCPLinearExpr objExpr = model().newPartLinearExpr("priority");
                objExpr.addTerm(total, -100);
                """);

        assertTrue(methodBody.contains("objExpr.addExpr(total, -100);"), methodBody);
    }

    @Test
    public void testNormalizesPartCategoryAggregateOverloadsFromModuleShape() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                model().addGreaterOrEqual(model().sum4Quantity("drive", "Capacity", ""), para("Sum_Capacity").valueVar());
                PartAlgCPLinearExpr objExpr = model().newPartLinearExpr("drivePriorityObj");
                objExpr.addExpr(model().sum4Quantity("drive", "", ""), 1000);
                objExpr.addExpr(model().sum4Quantity("drive*", "Capacity", ""), -1);
                """, SdkProfile.CONSTRAINT, RuleContextFactory.partCategory(RuleTransTestFixtures.sampleModule(), "drive"));

        assertTrue(methodBody.contains(
                "model().addGreaterOrEqual(model().sum4Quantity(\"Capacity\", \"\"), para(\"Sum_Capacity\").valueVar());"),
                methodBody);
        assertTrue(methodBody.contains("objExpr.addExpr(model().sum4Quantity(\"\", \"\"), 1000);"),
                methodBody);
        assertTrue(methodBody.contains("objExpr.addExpr(model().sum4Quantity(\"Capacity\", \"\"), -1);"),
                methodBody);
    }

    @Test
    public void testNormalizesPartCategoryInputParameterAndTwoArgAggregateShape() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                PartAlgCPLinearExpr totalQuantity = model().sum4Quantity("drive", "");
                PartAlgCPLinearExpr weightedCapacity = model().sum4Quantity("drive", "Capacity");
                model().addGreaterOrEqual(weightedCapacity, para("Sum_Capacity").valueVar());
                """, SdkProfile.CONSTRAINT,
                RuleContextFactory.partCategory(
                        RuleContextFactory.fromAnnotatedClass(InputParameterAggregateFacts.class,
                                RuleTransTestFixtures.TEMP_RESOURCE_PATH).module(),
                        "drive"));

        assertTrue(methodBody.contains("PartAlgCPLinearExpr totalQuantity = model().sum4Quantity(\"\", \"\");"),
                methodBody);
        assertTrue(methodBody.contains("PartAlgCPLinearExpr weightedCapacity = model().sum4Quantity(\"Capacity\", \"\");"),
                methodBody);
        assertTrue(methodBody.contains(
                "model().addGreaterOrEqual(weightedCapacity, para(\"Sum_Capacity\").inputValue());"),
                methodBody);
    }

    @ModuleAnno(id = 811113L)
    public static class InputParameterAggregateFacts extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_1:1", "Cap_3:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"3"})
        private PartVar sd1;

        @ParaAnno(fatherCode = "drive", code = "Sum_Capacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Capacity;
    }
}
