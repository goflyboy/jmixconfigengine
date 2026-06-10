package com.jmix.ruletrans.rulescenario;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

class ModuleCompatibilityRuleTransScenarioTest extends RuleScenarioHarnessSupport {

    @Test
    void testModuleCrossCategoryIncompatibleRule() {
        RuleContext context = moduleContext(CpuDriveFacts.class, "cpu", "drive");
        RuleScenario scenario = RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.COMPATIBLE);
        RuleMetadata metadata = metadata("ruleCpu4Drive5400Incompatible",
                "cpu 中属性 CoreNum 为 4 的部件不能和 drive 中属性 Speed 为 5400 的部件同时选择", "", "");

        assertNaturalLanguageTranslatesAndExecutes(
                context,
                scenario,
                metadata,
                caseSet(
                        validateCase("cpu4Drive5400Invalid", false, "cpu4", "drive5400"),
                        validateCase("cpu8Drive5400Valid", true, "cpu8", "drive5400"),
                        recommendCase("cpu4AndDrive5400NoSolution", "NO_SOLUTION",
                                "cpu:Sum_Quantity ==1 where CoreNum=4",
                                "drive:Sum_Quantity ==1 where Speed=5400"),
                        recommendCase("cpu8AndDrive5400HasSolution", "HAS_SOLUTION",
                                "cpu:Sum_Quantity ==1 where CoreNum=8",
                                "drive:Sum_Quantity ==1 where Speed=5400")),
                "ModuleCompatibilityScenario");
    }

    @ModuleAnno(id = 811102L)
    public static class CpuDriveFacts extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Core_4:4", "Core_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Speed_5400:5400", "Speed_7200:7200"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;
    }
}
