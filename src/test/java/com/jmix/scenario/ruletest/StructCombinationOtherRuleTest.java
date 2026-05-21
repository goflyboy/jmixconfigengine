package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.logic.CodependantRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.CombinationStructRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PairStructRuleAnno;
import com.jmix.tool.bbuilder.anno.TripleStructRuleAnno;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RFC-0007 structured combination rule tests for blacklist and edge scenarios.
 */
public class StructCombinationOtherRuleTest extends ModuleScenarioTestBase {

    public StructCombinationOtherRuleTest() {
        super(StructCombinationOtherConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @ModuleAnno(id = 7008L)
    @AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0007-blacklist-test")
    public static class StructCombinationOtherConstraint extends ModuleAlgBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8", "Core_12:12"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"})
        private PartVar cpu4;

        @PartAnno(fatherCode = "cpu", attrs = {"8"})
        private PartVar cpu8;

        @PartAnno(fatherCode = "cpu", attrs = {"12"})
        private PartVar cpu12;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200", "Speed_9000:9000"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;

        @PartAnno(fatherCode = "drive", attrs = {"9000"})
        private PartVar drive9000;

        @PartAnno(code = "monitor")
        @DAttrAnno1(code = "Resolution", options = {"FHD:FHD", "UHD:4K"})
        private PartCategoryVar monitor;

        @PartAnno(fatherCode = "monitor", attrs = {"FHD"})
        private PartVar monitorFhd;

        @PartAnno(fatherCode = "monitor", attrs = {"4K"})
        private PartVar monitor4k;

        @CombinationStructRuleAnno(
                code = "cpu_drive_black",
                combinationType = PartCombinationType.BLACK)
        public void cpuDriveBlack() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_black_001",
                parentRuleCode = "cpu_drive_black",
                expr1 = "cpu.CoreNum=4",
                expr2 = "drive.Speed=7200")
        public void cpuDriveBlack001() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_black_duplicate",
                parentRuleCode = "cpu_drive_black",
                expr1 = "cpu.CoreNum=4",
                expr2 = "drive.Speed=7200")
        public void cpuDriveBlackDuplicate() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_black_no_match",
                parentRuleCode = "cpu_drive_black",
                expr1 = "cpu.CoreNum=99",
                expr2 = "drive.Speed=9000")
        public void cpuDriveBlackNoMatch() {
        }

        @CombinationStructRuleAnno(
                code = "cpu_drive_monitor_black",
                combinationType = PartCombinationType.BLACK)
        public void cpuDriveMonitorBlack() {
        }

        @TripleStructRuleAnno(
                code = "cpu_drive_monitor_black_001",
                parentRuleCode = "cpu_drive_monitor_black",
                expr1 = "cpu.CoreNum=8",
                expr2 = "drive.Speed=5400",
                expr3 = "monitor.Resolution=4K")
        public void cpuDriveMonitorBlack001() {
        }
    }

    @Test
    public void testBlackListGeneratedStructRules() {
        CodependantRuleSchema schema = exeSchema("cpu_drive_black");
        assertEquals(PartCombinationType.BLACK, schema.getCombinationType());
        assertEquals(1, schema.getCombinations().size());
    }

    @ParameterizedTest
    @CsvSource({
            "cpu4, drive7200, false",
            "cpu4, drive5400, true",
            "cpu8, drive7200, true",
            "cpu12, drive9000, true"
    })
    public void testCombinationBlackList_Pair(String cpuCode, String driveCode, boolean expected) {
        assertEquals(expected, validData(cpuCode, driveCode));
    }

    @ParameterizedTest
    @CsvSource({
            "cpu8, drive5400, monitor4k, false",
            "cpu8, drive5400, monitorFhd, true",
            "cpu8, drive7200, monitor4k, true"
    })
    public void testCombinationBlackList_Triple(String cpuCode, String driveCode, String monitorCode,
            boolean expected) {
        assertEquals(expected, validData(cpuCode, driveCode, monitorCode));
    }

    @Test
    public void testValidateCombinationBlackList_Pair() {
        assertFalse(validData("cpu4", "drive7200"));
        assertTrue(validateData("cpu4", "drive7200").getViolatedRuleCodes().contains("cpu_drive_black"));
    }

    @Test
    public void testValidateCombinationBlackList_Triple() {
        assertFalse(validData("cpu8", "drive5400", "monitor4k"));
        assertTrue(validateData("cpu8", "drive5400", "monitor4k")
                .getViolatedRuleCodes().contains("cpu_drive_monitor_black"));
    }

    @Test
    public void testBlackListGenerationRejectsForbiddenPair() {
        inferRecommendModule("cpu:Sum_Quantity ==1 where CoreNum=4", "drive:Sum_Quantity ==1 where Speed=7200");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testBlackListGenerationAllowsUnmatchedPair() {
        inferRecommendModule("cpu:Sum_Quantity ==1 where CoreNum=4", "drive:Sum_Quantity ==1 where Speed=5400");
        resultAssert().assertSuccess();
    }

    @Test
    public void testNoMatchBlackListChildDoesNotRejectExistingParts() {
        assertTrue(validData("cpu12", "drive9000"));
    }

    private CodependantRuleSchema exeSchema(String ruleCode) {
        Rule parent = getModule().getRule(ruleCode).orElseThrow();
        return (CodependantRuleSchema) parent.getExeSchema();
    }
}
