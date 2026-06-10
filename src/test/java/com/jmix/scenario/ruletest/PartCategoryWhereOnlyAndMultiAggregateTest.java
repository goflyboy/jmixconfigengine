package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

/**
 * RFC-0012 PartCategory where-only and single IEQ multi aggregate tests.
 */
public class PartCategoryWhereOnlyAndMultiAggregateTest extends ModuleScenarioTestBase {

    public PartCategoryWhereOnlyAndMultiAggregateTest() {
        super(WhereOnlyAndMultiAggregateConstraint.class);
    }

    @ModuleAnno(id = 9012L)
    public static class WhereOnlyAndMultiAggregateConstraint extends ModuleAlgBase {

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400", "Speed_7200:7200"})
        @DAttrAnno2(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Cap_3:3", "Cap_4:4", "Cap_20:20"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "4"}, maxQuantity = 1)
        private PartVar d5400A;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "3"}, maxQuantity = 1)
        private PartVar d5400B;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "3"}, maxQuantity = 1)
        private PartVar d5400C;

        @PartAnno(fatherCode = "drive", attrs = {"7200", "20"}, maxQuantity = 1)
        private PartVar d7200;

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"CoreNum_4:4"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"}, maxQuantity = 1)
        private PartVar cpu4;
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Test
    public void testWhereOnly_DefaultsToQuantityGreaterOrEqualOne() {
        inferRecommendModule("drive:where Speed=5400");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSomeSolutionSelectsAny("d5400A", "d5400B", "d5400C");
        assertNoSolutionSelects("d7200");
    }

    @Test
    public void testSingleIeq_MultiAggregateConditionsShareSameWhere() {
        inferRecommendModule("drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertSomeSolutionSelectsAll("d5400A", "d5400B", "d5400C");
        assertEverySolutionHasDriveQuantity(3);
        assertNoSolutionSelects("d7200");
        assertSolutionsDoNotContain("I1_");
    }

    @Test
    public void testSingleAggregateRequestStillWorks() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSimpleSolutions();

        resultAssert().assertSuccess();
        assertEverySolutionHasDriveQuantity(2);
        assertNoSolutionSelects("d7200");
    }

    @Test
    public void testInvalidMultiAggregateSyntaxFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> inferRecommendModule("drive:Sum_Capacity >=10 && where Speed=5400"));
    }

    @Test
    public void testInvalidMultiAggregateWithSumSumFails() {
        inferRecommendModule("drive:Sum_Capacity >=10 && SumSum_Quantity >=3 where Speed=5400");

        resultAssert()
                .assertCodeEqual(Result.FAILED)
                .assertMessageContains("SumSum");
    }

    @Test
    public void testCrossCategoryMultiAggregateFailsFast() {
        assertThrows(IllegalArgumentException.class,
                () -> inferRecommendModule("drive,cpu:Sum_Quantity >=1 && Sum_Quantity >=2"));
    }

    @Test
    public void testWhereOnly_FilterEmptyAllConstrainedNoSolution() {
        inferRecommendModule("drive:where Speed=3000");
        printSimpleSolutions();

        resultAssert().assertNoSolution();
    }

    @Test
    public void testWhereOnly_FilterEmptyKeepsDiagnostic() {
        inferRecommendModule(
                "drive:where Speed=3000",
                "cpu:where CoreNum=4");
        printSimpleSolutions();

        resultAssert().assertCodeEqual(Result.PARTIAL_SUCCESS);
        assertSoluContain("drive:FILTER_EMPTY");
    }

    private void assertSolutionsDoNotContain(String unexpected) {
        if (getSolutions() == null) {
            return;
        }
        for (ModuleInst solution : getSolutions()) {
            String solutionStr = solution.toShortString(true);
            assertFalse(solutionStr.contains(unexpected),
                    "Solution should not contain " + unexpected + ": " + solutionStr);
        }
    }

    private void assertSomeSolutionSelectsAny(String... partCodes) {
        for (ModuleInst solution : getSolutions()) {
            for (String partCode : partCodes) {
                if (isSelected(solution, partCode)) {
                    return;
                }
            }
        }
        throw new AssertionError("No solution selected any of: " + String.join(",", partCodes));
    }

    private void assertSomeSolutionSelectsAll(String... partCodes) {
        for (ModuleInst solution : getSolutions()) {
            boolean allSelected = true;
            for (String partCode : partCodes) {
                allSelected = allSelected && isSelected(solution, partCode);
            }
            if (allSelected) {
                return;
            }
        }
        throw new AssertionError("No solution selected all of: " + String.join(",", partCodes));
    }

    private void assertNoSolutionSelects(String partCode) {
        for (ModuleInst solution : getSolutions()) {
            assertFalse(isSelected(solution, partCode),
                    "Solution should not select " + partCode + ": " + solution.toShortString(true));
        }
    }

    private void assertEverySolutionHasDriveQuantity(int expectedQuantity) {
        for (ModuleInst solution : getSolutions()) {
            int actualQuantity = quantity(solution, "d5400A")
                    + quantity(solution, "d5400B")
                    + quantity(solution, "d5400C");
            assertEquals(expectedQuantity, actualQuantity,
                    "Unexpected drive quantity in solution: " + solution.toShortString(true));
        }
    }

    private boolean isSelected(ModuleInst solution, String partCode) {
        PartInst part = solution.queryPart("drive", ModuleInst.DEFAULT_INSTANCE_ID, partCode);
        return part != null && (part.isSelected() || quantity(solution, partCode) > 0);
    }

    private int quantity(ModuleInst solution, String partCode) {
        PartInst part = solution.queryPart("drive", ModuleInst.DEFAULT_INSTANCE_ID, partCode);
        return part == null || part.getQuantity() == null ? 0 : part.getQuantity();
    }
}
