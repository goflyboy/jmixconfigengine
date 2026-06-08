package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

final class RuleTransTestFixtures {

    static final String TEMP_RESOURCE_PATH = "target/ruletrans-test-resources";

    private RuleTransTestFixtures() {
    }

    static Module sampleModule() {
        return ModuleGenneratorByAnno.build(SampleRuleTransConstraint.class, TEMP_RESOURCE_PATH);
    }

    static String cpuAtMostOneSnippet() {
        return """
                @CodeRuleAnno(normalNaturalCode = "CPU at most one", fatherCode = "cpu")
                public void ruleCpuAtMostOne() {
                    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                }
                """;
    }

    static String cpuAtMostOneMethodBody() {
        return """
                model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                """;
    }

    static String cpu4CannotUseDrive5400MethodBody() {
        return """
                inCompatible(ruleCode, "cpu:CoreNum=4", "drive:Speed=5400");
                """;
    }

    static String postDriveCapacityMethodBody() {
        return """
                int sum = partCategorySum("drive").sumDynAttr4Int("Capacity");
                parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
                partCategory("drive").parameter("pSumCapacity").setValue(String.valueOf(sum));
                """;
    }

    static String crossCategorySnippet() {
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
