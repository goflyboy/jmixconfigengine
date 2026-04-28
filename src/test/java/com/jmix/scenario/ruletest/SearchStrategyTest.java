package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * Decision strategy test - single instance category scenarios.
 * Covers SEARCH-001 and SEARCH-002 from RFC-0001.
 *
 * @since 2026-04-29
 */
@Slf4j
public class SearchStrategyTest extends ModuleScenarioTestBase {

    public SearchStrategyTest() {
        super(SearchConstraint.class);
    }

    /**
     * Constraint model for search strategy testing.
     * Single-instance CPU and Disk categories with price attributes.
     * Constraint: at most one part selected per category.
     */
    @ModuleAnno(id = 123L)
    public static class SearchConstraint extends ConstraintAlgImplTestBase {

        @PartAnno(code = "cpu")
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", price = 10)
        private PartVar cpu1Var;

        @PartAnno(fatherCode = "cpu", price = 5)
        private PartVar cpu2Var;

        @PartAnno(fatherCode = "cpu", price = 100)
        private PartVar cpu3Var;

        @PartAnno(code = "disk")
        private PartCategoryVar disk;

        @PartAnno(fatherCode = "disk", price = 50)
        private PartVar disk1Var;

        @PartAnno(fatherCode = "disk", price = 30)
        private PartVar disk2Var;

        @CodeRuleAnno()
        private void rule1() {
            model.addExactlyOne(new Literal[] { cpu1Var.isSelected, cpu2Var.isSelected, cpu3Var.isSelected });
            model.addExactlyOne(new Literal[] { disk1Var.isSelected, disk2Var.isSelected });
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    // ==================== SEARCH-001: Single category strategy ====================

    /**
     * SEARCH-001-1: CPU ASCENDING by price.
     * First solution should contain CPU2 (price=5, cheapest).
     */
    @Test
    public void testSearch001_AscendingPrice_Cpu() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk:");
        printSolutions();
        assertSoluContain(1, "cpu2");
    }

    /**
     * SEARCH-001-2: CPU DESCENDING by price.
     * First solution should contain CPU3 (price=100, most expensive).
     */
    @Test
    public void testSearch001_DescendingPrice_Cpu() {
        inferRecommendModule(
                "cpu: [strategy=DESCENDING:price]",
                "disk:");
        printSolutions();
        assertSoluContain(1, "cpu3");
    }

    /**
     * SEARCH-001-3: No strategy (regression test).
     * Solutions should still be found without strategy.
     */
    @Test
    public void testSearch001_NoStrategy_Regression() {
        inferRecommendModule("cpu:", "disk:");
        printSolutions();
        resultAssert().assertSuccess();
    }

    // ==================== SEARCH-002: Multi-category strategy combinations ====================

    /**
     * SEARCH-002-1: CPU ASCENDING + Disk ASCENDING.
     * First solution should contain CPU2(price=5).
     * Disk ASCENDING: cheaper disk (ds2, price=30) preferred.
     */
    @Test
    public void testSearch002_CpuAscending_DiskAscending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk: [strategy=ASCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu2");
        // ds1=PT1 (price=50), ds2=PT2 (price=30), ASCENDING prefers ds2
        assertSoluContain(1, "PT2");
    }

    /**
     * SEARCH-002-2: CPU DESCENDING + Disk DESCENDING.
     * First solution should contain CPU3(price=100).
     * Disk DESCENDING: expensive disk (ds1, price=50) preferred.
     */
    @Test
    public void testSearch002_CpuDescending_DiskDescending() {
        inferRecommendModule(
                "cpu: [strategy=DESCENDING:price]",
                "disk: [strategy=DESCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu3");
        // ds1=PT1 (price=50), ds2=PT2 (price=30), DESCENDING prefers ds1
        assertSoluContain(1, "PT1");
    }

    /**
     * SEARCH-002-3: CPU ASCENDING + Disk DESCENDING.
     * First solution should contain CPU2(price=5).
     * Disk DESCENDING: expensive disk (ds1, price=50) preferred.
     */
    @Test
    public void testSearch002_CpuAscending_DiskDescending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk: [strategy=DESCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu2");
        assertSoluContain(1, "PT1");
    }
}
