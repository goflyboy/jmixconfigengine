package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.logic.BusinessRelationType;
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
import com.jmix.tool.bbuilder.anno.PairStructRuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.TripleStructRuleAnno;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RFC-0007 structured combination rule tests.
 */
public class StructCombinationRuleTest extends ModuleScenarioTestBase {

    public StructCombinationRuleTest() {
        super(StructCombinationConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @ModuleAnno(id = 7007L)
    @AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0007-test")
    public static class StructCombinationConstraint extends ModuleAlgBase {

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
        @DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200", "Speed_7000:7000"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"})
        private PartVar drive5400;

        @PartAnno(fatherCode = "drive", attrs = {"7200"})
        private PartVar drive7200;

        @PartAnno(fatherCode = "drive", attrs = {"7000"})
        private PartVar drive7000;

        @PartAnno(code = "monitor")
        @DAttrAnno1(code = "Resolution", options = {"FHD:FHD", "UHD:4K"})
        private PartCategoryVar monitor;

        @PartAnno(fatherCode = "monitor", attrs = {"FHD"})
        private PartVar monitorFhd;

        @PartAnno(fatherCode = "monitor", attrs = {"4K"})
        private PartVar monitor4k;

        @PairStructRuleAnno(
                expr1 = "cpu.CoreNum=4",
                relationType = BusinessRelationType.INCOMPATIBLE,
                expr2 = "drive.Speed=7000")
        public void pairCpu4Drive7000() {
        }

        @PairStructRuleAnno(
                expr1 = "cpu.CoreNum=4",
                relationType = BusinessRelationType.INCOMPATIBLE,
                expr2 = "drive.Speed LIKE 71%")
        public void pairCpu4Drive54Like() {
        }

        @CombinationStructRuleAnno(
                code = "cpu_drive_white",
                combinationType = PartCombinationType.WHITE)
        public void cpuDriveWhite() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_001",
                parentRuleCode = "cpu_drive_white",
                expr1 = "cpu.CoreNum=4",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed=5400")
        public void cpuDriveWhite001() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_002",
                parentRuleCode = "cpu_drive_white",
                expr1 = "cpu.CoreNum=8",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed IN [5400,7200]")
        public void cpuDriveWhite002() {
        }

        @PairStructRuleAnno(
                code = "cpu_drive_white_003",
                parentRuleCode = "cpu_drive_white",
                expr1 = "cpu.CoreNum>8",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed=7200")
        public void cpuDriveWhite003() {
        }

        @CombinationStructRuleAnno(
                code = "cpu_drive_monitor_white",
                combinationType = PartCombinationType.WHITE)
        public void cpuDriveMonitorWhite() {
        }

        @TripleStructRuleAnno(
                code = "cpu_drive_monitor_white_001",
                parentRuleCode = "cpu_drive_monitor_white",
                expr1 = "cpu.CoreNum=8",
                relationType = BusinessRelationType.CO_DEPENDENT,
                expr2 = "drive.Speed=7200",
                expr3 = "monitor.Resolution=4K")
        public void cpuDriveMonitorWhite001() {
        }
    }

    @Test
    public void testAnnotationGeneratedStructRules() {
        Rule parent = getModule().getRule("cpu_drive_white").orElseThrow();
        assertNotNull(parent.getExeSchema());
        CodependantRuleSchema schema = (CodependantRuleSchema) parent.getExeSchema();
        assertEquals(2, schema.getArity());
        assertEquals(4, schema.getCombinations().size());
    }

    @ParameterizedTest
    @CsvSource({
            "cpu4, drive7000, false",
            "cpu4, drive5400, true"
    })
    public void testPairStructRule_Incompatible(String cpuCode, String driveCode, boolean expected) {
        assertEquals(expected, validData(cpuCode, driveCode));
    }

    @ParameterizedTest
    @CsvSource({
            "cpu4, drive5400, true",
            "cpu4, drive7200, false",
            "cpu8, drive5400, true",
            "cpu8, drive7200, true",
            "cpu12, drive5400, false",
            "cpu12, drive7200, true"
    })
    public void testCombinationWhiteList_Pair(String cpuCode, String driveCode, boolean expected) {
        assertEquals(expected, validData(cpuCode, driveCode));
    }

    @ParameterizedTest
    @CsvSource({
            "cpu8, drive7200, monitor4k, true",
            "cpu8, drive5400, monitor4k, false"
    })
    public void testCombinationWhiteList_Triple(String cpuCode, String driveCode, String monitorCode,
            boolean expected) {
        assertEquals(expected, validData(cpuCode, driveCode, monitorCode));
    }

    @Test
    public void testValidateCombinationWhiteList_Pair() {
        assertTrue(validData("cpu4", "drive5400"));
        assertFalse(validData("cpu4", "drive7200"));
        assertTrue(validateData("cpu4", "drive7200").getViolatedRuleCodes().contains("cpu_drive_white"));
    }

    @Test
    public void testValidateCombination_WithQuantityInput() {
        assertTrue(validData(partInst("cpu4", 1), partInst("drive5400", 2)));
    }

    @Test
    public void testCombinationRule_IntersectWithRuntimeFilter() {
        inferRecommendModule("cpu:Sum_Quantity ==1 where CoreNum=4", "drive:Sum_Quantity ==1 where Speed=7200");
        resultAssert().assertNoSolution();
    }

    @Test
    public void testChildRule_NotExecutedIndependently() {
        assertTrue(validData("cpu4", "drive5400"));
        assertFalse(validData("cpu4", "drive7200"));
    }
}
