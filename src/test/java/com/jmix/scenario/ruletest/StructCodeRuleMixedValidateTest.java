package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.CombinationStructRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PairStructRuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

/**
 * Validator should execute the full rule model, including mixed Struct and Code rules.
 */
public class StructCodeRuleMixedValidateTest extends ModuleScenarioTestBase {

    public StructCodeRuleMixedValidateTest() {
        super(MixedValidateConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @ModuleAnno(id = 7017L)
    @AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0007-mixed-validate-test")
    public static class MixedValidateConstraint extends ModuleAlgBase {

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

        @CombinationStructRuleAnno(
                code = "mixed_cpu_drive_white",
                combinationType = PartCombinationType.WHITE)
        public void cpuDriveWhite() {
        }

        @PairStructRuleAnno(
                code = "mixed_cpu_drive_white_001",
                parentRuleCode = "mixed_cpu_drive_white",
                expr1 = "cpu.CoreNum=4",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed=5400")
        public void cpu4Drive5400() {
        }

        @PairStructRuleAnno(
                code = "mixed_cpu_drive_white_002",
                parentRuleCode = "mixed_cpu_drive_white",
                expr1 = "cpu.CoreNum=8",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed IN [5400,7200]")
        public void cpu8DriveAny() {
        }

        @CodeRuleAnno(leftProObjsStr = "cpu:Select", rightProObjsStr = "drive:Select")
        public void cpu8NotDrive5400() {
            model().addImplication(cpu8.selectedVar(), drive5400.selectedVar().not());
        }
    }

    @Test
    public void testMixedRules_PassBothStructAndCode() {
        assertTrue(validData("cpu4", "drive5400"));
        assertTrue(validData("cpu8", "drive7200"));
    }

    @Test
    public void testMixedRules_StructViolationReported() {
        ModuleValidateResp resp = validateData("cpu4", "drive7200");
        assertFalse(resp.isValid());
        assertTrue(resp.getViolatedRuleCodes().contains("mixed_cpu_drive_white"));
    }

    @Test
    public void testMixedRules_CodeRuleViolationReported() {
        ModuleValidateResp resp = validateData("cpu8", "drive5400");
        assertFalse(resp.isValid());
        assertTrue(resp.getViolatedRuleCodes().contains("cpu8NotDrive5400"));
    }
}
