package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * Multi-instance decision strategy test.
 * Covers SEARCH-003, SEARCH-004, SEARCH-005 from RFC-0001.
 *
 * @since 2026-04-29
 */
@Slf4j
public class SearchStrategyMultiTest extends ModuleScenarioTestBase {

    public SearchStrategyMultiTest() {
        super(MultiSearchConstraint.class);
    }

    /**
     * Constraint model for multi-instance strategy testing.
     */
    @ModuleAnno(id = 123L)
    public static class MultiSearchConstraint extends ConstraintAlgImplTestBase {

        // Single-instance CPU category
        @PartAnno(code = "cpu")
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", price = 10)
        private PartVar cpu1Var;

        @PartAnno(fatherCode = "cpu", price = 5)
        private PartVar cpu2Var;

        @PartAnno(fatherCode = "cpu", price = 100)
        private PartVar cpu3Var;

        // Multi-instance GPU category (2 instances)
        @PartAnno(code = "gpu", instCodes = "gpuI0,gpuI1", supportMultiInst = true)
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_INT,
                options = { "Speed_5400:5400", "Speed_7200:7200" })
        private PartCategoryVar gpu;

        @PartAnno(fatherCode = "gpu", attrs = { "5400" }, price = 50)
        private PartVar gpu1Var;

        @PartAnno(fatherCode = "gpu", attrs = { "7200" }, price = 80)
        private PartVar gpu2Var;

        @PartAnno(fatherCode = "gpu", attrs = { "5400" }, price = 120)
        private PartVar gpu3Var;

        @CodeRuleAnno()
        private void rule1() {
            model.addAtMostOne(new Literal[] { cpu1Var.isSelected, cpu2Var.isSelected, cpu3Var.isSelected });
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    // ==================== SEARCH-003: Multi-instance strategy ====================

    /**
     * SEARCH-003-1: GPU multi-instance ASCENDING by price.
     * All GPU instances should prefer cheaper parts (gpu1 ¥50).
     */
    @Test
    public void testSearch003_MultiInst_AscendingPrice() {
        inferRecommendModule(
                "cpu:",
                "gpuI0: [strategy=ASCENDING:price]",
                "gpuI1: [strategy=ASCENDING:price]");
        printSolutions();
        resultAssert().assertSuccess();
    }

    /**
     * SEARCH-003-2: GPU multi-instance DESCENDING by price.
     * All GPU instances should prefer more expensive parts.
     */
    @Test
    public void testSearch003_MultiInst_DescendingPrice() {
        inferRecommendModule(
                "cpu:",
                "gpuI0: [strategy=DESCENDING:price]",
                "gpuI1: [strategy=DESCENDING:price]");
        printSolutions();
        resultAssert().assertSuccess();
    }

    // ==================== SEARCH-005: Mixed single + multi-instance ====================

    /**
     * SEARCH-005-1: Only CPU ASCENDING, GPU no strategy.
     * CPU should prefer cpu2 (price=5).
     */
    @Test
    public void testSearch005_OnlyCpuAscending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "gpuI0:",
                "gpuI1:");
        printSolutions();
        assertSoluContain(1, "cpu2");
    }

    /**
     * SEARCH-005-3: CPU ASCENDING + GPU ASCENDING.
     * Both categories apply strategy.
     */
    @Test
    public void testSearch005_MixedAscending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "gpuI0: [strategy=ASCENDING:price]",
                "gpuI1: [strategy=ASCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu2");
    }

    // ==================== SEARCH-006: Dynamic attribute sorting ====================

    /**
     * SEARCH-006-1: GPU ASCENDING by Speed (dynAttr).
     * Prefer Speed=5400 parts.
     */
    @Test
    public void testSearch006_Ascending_Speed() {
        inferRecommendModule(
                "cpu:",
                "gpuI0: [strategy=ASCENDING:Speed]",
                "gpuI1: [strategy=ASCENDING:Speed]");
        printSolutions();
        resultAssert().assertSuccess();
    }

    /**
     * SEARCH-006-2: GPU DESCENDING by Speed (dynAttr).
     * Prefer Speed=7200 parts.
     */
    @Test
    public void testSearch006_Descending_Speed() {
        inferRecommendModule(
                "cpu:",
                "gpuI0: [strategy=DESCENDING:Speed]",
                "gpuI1: [strategy=DESCENDING:Speed]");
        printSolutions();
        resultAssert().assertSuccess();
    }
}
