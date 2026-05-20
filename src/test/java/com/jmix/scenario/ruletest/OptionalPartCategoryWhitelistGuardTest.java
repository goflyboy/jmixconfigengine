package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.cp.AlgCPLiteral;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

/**
 * RFC-0009 optional PartCategory guard tests.
 */
public class OptionalPartCategoryWhitelistGuardTest extends ModuleScenarioTestBase {

    public OptionalPartCategoryWhitelistGuardTest() {
        super(OptionalMouseConstraint.class);
    }

    @ModuleAnno(id = 9009L)
    public static class OptionalMouseConstraint extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Core_4:4", "Core_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"}, price = 100)
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"}, price = 200)
        private PartVar cpu8;

        @PartAnno(code = "disk")
        private PartCategoryVar disk;

        @PartAnno(fatherCode = "disk", price = 50)
        private PartVar disk1;

        @PartAnno(code = "mouse", required = false)
        @DAttrAnno1(code = "Level", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Low:1", "High:2"})
        private PartCategoryVar mouse;

        @PartAnno(fatherCode = "mouse", attrs = {"1"}, price = 10)
        private PartVar mouse1;

        @PartAnno(fatherCode = "mouse", attrs = {"2"}, price = 30)
        private PartVar mouse2;

        @CodeRuleAnno()
        private void requiredSelection() {
            model().addExactlyOne(cpu.parts().stream()
                    .map(PartVar::selectedVar)
                    .toArray(AlgCPLiteral[]::new));
            model().addExactlyOne(disk.parts().stream()
                    .map(PartVar::selectedVar)
                    .toArray(AlgCPLiteral[]::new));
        }

        @CodeRuleAnno(leftProObjsStr = "cpu:Select", rightProObjsStr = "mouse:Select")
        private void cpuMouseWhitelist() {
            inCompatible("rule_cpu4_not_mouse2", "cpu:CoreNum=4", "mouse:Level=2");
            inCompatible("rule_cpu8_not_mouse1", "cpu:CoreNum=8", "mouse:Level=1");
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Test
    public void testOptionalMouseAbsentDoesNotBlockMainSolve() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "disk:");

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithSelectedParts("cpu4", "disk1"));
    }

    @Test
    public void testOptionalMouseMentionReturnsAbsentAndPresentBranches() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "disk:",
                "mouse: where Level=1");

        resultAssert().assertSuccess();
        assertTrue(hasSolutionWithoutPartCategory("mouse"));
        assertTrue(hasSolutionWithSelectedParts("cpu4", "mouse1"));
        assertFalse(hasSolutionWithSelectedParts("cpu4", "mouse2"));
    }

    @Test
    public void testOptionalMousePresentWhitelistMustApply() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "disk:",
                "mouse:Sum_Quantity ==1 where Level=2");

        resultAssert().assertNoSolution();
    }

    private boolean hasSolutionWithSelectedParts(String... partCodes) {
        if (getSolutions() == null) {
            return false;
        }
        for (ModuleInst solution : getSolutions()) {
            boolean matched = true;
            for (String partCode : partCodes) {
                if (!hasSelectedPart(solution, partCode)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSelectedPart(ModuleInst solution, String partCode) {
        for (PartInst part : solution.getAllParts()) {
            if (partCode.equals(part.getCode()) && part.isSelected()
                    && part.getQuantity() != null && part.getQuantity() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSolutionWithoutPartCategory(String categoryCode) {
        if (getSolutions() == null) {
            return false;
        }
        for (ModuleInst solution : getSolutions()) {
            if (!hasPartCategory(solution, categoryCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPartCategory(ModuleInst solution, String categoryCode) {
        for (PartCategoryInst partCategory : solution.getAllPartCategorys()) {
            if (categoryCode.equals(partCategory.getCode())) {
                return true;
            }
        }
        return false;
    }
}
