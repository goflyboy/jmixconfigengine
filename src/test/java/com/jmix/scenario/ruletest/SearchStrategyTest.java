package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPLiteral;

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
    public static class SearchConstraint extends ModuleAlgBase {

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
            model().addExactlyOne(new AlgCPLiteral[] { cpu1Var.selectedVar(), cpu2Var.selectedVar(), cpu3Var.selectedVar() });
            model().addExactlyOne(new AlgCPLiteral[] { disk1Var.selectedVar(), disk2Var.selectedVar() });
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    // ==================== SEARCH-001: Single category strategy ====================

    /**
     * SEARCH-001-1: CPU ASCENDING by price.
     * First solution should contain CPU2 (price=5, cheapest) selected with qty=1.
     */
    @Test
    public void testSearch001_AscendingPrice_Cpu() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk:");
        printSolutions();
        assertSoluContain(1, "cpu2(Q:1,H:0,S:1)");
    }

    /**
     * SEARCH-001-2: CPU DESCENDING by price.
     * First solution should contain CPU3 (price=100, most expensive) selected with qty=1.
     */
    @Test
    public void testSearch001_DescendingPrice_Cpu() {
        inferRecommendModule(
                "cpu: [strategy=DESCENDING:price]",
                "disk:");
        printSolutions();
        assertSoluContain(1, "cpu3(Q:1,H:0,S:1)");
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
     * First solution: cpu2 selected (price=5, cheapest), PT2 selected (price=30, cheaper disk).
     * PT1=disk1(price=50, code len>4), PT2=disk2(price=30, code len>4).
     */
    @Test
    public void testSearch002_CpuAscending_DiskAscending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk: [strategy=ASCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu2(Q:1,H:0,S:1)");
        assertSoluContain(1, "PT2(Q:1,H:0,S:1)");
    }

    /**
     * SEARCH-002-2: CPU DESCENDING + Disk DESCENDING.
     * First solution: cpu3 selected (price=100, most expensive), PT1 selected (price=50, expensive disk).
     */
    @Test
    public void testSearch002_CpuDescending_DiskDescending() {
        inferRecommendModule(
                "cpu: [strategy=DESCENDING:price]",
                "disk: [strategy=DESCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu3(Q:1,H:0,S:1)");
        assertSoluContain(1, "PT1(Q:1,H:0,S:1)");
    }

    /**
     * SEARCH-002-3: CPU ASCENDING + Disk DESCENDING.
     * First solution: cpu2 selected (price=5), PT1 selected (price=50, expensive disk first).
     */
    @Test
    public void testSearch002_CpuAscending_DiskDescending() {
        inferRecommendModule(
                "cpu: [strategy=ASCENDING:price]",
                "disk: [strategy=DESCENDING:price]");
        printSolutions();
        assertSoluContain(1, "cpu2(Q:1,H:0,S:1)");
        assertSoluContain(1, "PT1(Q:1,H:0,S:1)");
    }
}
