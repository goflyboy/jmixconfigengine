package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class PostRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testPostWritesModuleCategoryAndPartInstanceState() {
        RuleContext context = moduleContext(PostFacts.class, "drive");
        RuleScenario scenario = RuleScenario.post(RuleScope.PRODUCT, RuleFamily.POST);
        RuleMetadata metadata = metadata("rulePostDriveWriteBack",
                "POST 阶段将 drive 的 Capacity 总和写回产品参数 pDriveSumCapacity 和 drive 分类参数 pSumCapacity；将 drive 的总数量写回产品参数 pDriveQuantity；并将 drive 分类下部件 md1 的数量设置为 2",
                "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        postRecommendCase("postWriteBacksAreVisible",
                                List.of("drive:Sum_Quantity ==1 where Speed=5400"),
                                null,
                                List.of("pDriveSumCapacity", "pDriveQuantity", "pSumCapacity"),
                                Map.of("pDriveSumCapacity", 1),
                                null,
                                Map.of("md1", 2))),
                "PostWriteBackScenario");
    }

    @ModuleAnno(id = 811112L)
    public static class PostFacts extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Capacity_1:1", "Capacity_3:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "1"})
        private PartVar md1;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "3"})
        private PartVar sd1;

        @ParaAnno(fatherCode = "drive", code = "Sum_Quantity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar driveSumQuantity;

        @ParaAnno(code = "pDriveSumCapacity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveSumCapacity;

        @ParaAnno(code = "pDriveQuantity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveQuantity;

        @ParaAnno(fatherCode = "drive", code = "pSumCapacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pSumCapacity;
    }
}
