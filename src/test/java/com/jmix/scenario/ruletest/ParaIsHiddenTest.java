package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.imodel.anno.CodeRuleAnno;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.ParaAnno;
import com.jmix.executor.imodel.anno.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 参数隐藏测试类
 * 测试参数隐藏功能的各种场景和约束规则
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ParaIsHiddenTest extends ModuleScenarioTestBase {
    /**
     * 构造ParaIsHiddenTest测试类
     */
    public ParaIsHiddenTest() {
        super(ParaIsHiddenConstraint.class);
    }

    // --------------start----------------------------------------
    /**
     * 参数隐藏约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class ParaIsHiddenConstraint extends ConstraintAlgImplTestBase {
        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "3")
        private ParaVar p0;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar p1;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "2")
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar part1;

        @CodeRuleAnno
        private void initConstraint() {
            // Logic1: p1.isHidden and p2.isHidden are incompatible
            model.addBoolOr(new Literal[] { p1.isHidden.not(), p2.isHidden.not() });
            addVarAboutHiddenConstraints(p1, p2);

            // Logic2: if p0.value in (0,1) then p1.isHidden=1 else p2.isHidden=1
            BoolVar p0Eq0 = model.newBoolVar("p0_eq_0");
            BoolVar p0Eq1 = model.newBoolVar("p0_eq_1");
            model.addEquality(p0.value, 0).onlyEnforceIf(p0Eq0);
            model.addDifferent(p0.value, 0).onlyEnforceIf(p0Eq0.not());
            model.addEquality(p0.value, 1).onlyEnforceIf(p0Eq1);
            model.addDifferent(p0.value, 1).onlyEnforceIf(p0Eq1.not());

            // p0In01 is true iff p0.value == 0 or p0.value == 1
            BoolVar p0In01 = model.newBoolVar("p0_in_01");
            model.addBoolOr(new Literal[] { p0Eq0, p0Eq1 }).onlyEnforceIf(p0In01);
            model.addBoolAnd(new Literal[] { p0Eq0.not(), p0Eq1.not() }).onlyEnforceIf(p0In01.not());
            // Ensure not both p0Eq0 and p0Eq1 are true simultaneously
            model.addBoolOr(new Literal[] { p0Eq0.not(), p0Eq1.not() });

            model.addEquality(p1.isHidden, 1).onlyEnforceIf(p0In01);
            model.addEquality(p2.isHidden, 1).onlyEnforceIf(p0In01.not());

            // Logic3: if p1.isHidden=1 then p1.value=0
            model.addEquality(p1.value, 0).onlyEnforceIf(p1.isHidden);

            // Logic4: if p2.isHidden=1 then p2.value=0
            model.addEquality(p2.value, 0).onlyEnforceIf(p2.isHidden);

            // Logic5: part1.quantity = p1.value + p2.value
            model.addEquality(part1.qty,
                    LinearExpr.sum(new IntVar[] { p1.value, p2.value }));
        }
    }

    // ---------------end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 测试p0值为0时p1隐藏
     */
    @Test
    public void testp0Value0p1Hidden() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParas("part1", 1, "p0", "0");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(0).hiddenEqual(false)
                .assertPara("p1").valueEqual(0).hiddenEqual(true)
                .assertPara("p2").valueEqual(1).hiddenEqual(false)
                .assertPara("p2").valueEqual(1).hiddenEqual(false)
                .assertPart("part1").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试p0值为1时p1隐藏
     */
    @Test
    public void testp0Value1p1Hidden() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParas("part1", 1, "p0", "1");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(1).hiddenEqual(false)
                .assertPara("p1").valueEqual(0).hiddenEqual(true)
                .assertPara("p2").valueEqual(1).hiddenEqual(false)
                .assertPara("p2").valueEqual(1).hiddenEqual(false)
                .assertPart("part1").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试p0值为2时p2隐藏
     */
    @Test
    public void testp0Value2p2Hidden() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParas("part1", 1, "p0", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(2).hiddenEqual(false)
                .assertPara("p1").valueEqual(1).hiddenEqual(false)
                .assertPara("p2").valueEqual(0).hiddenEqual(true)
                .assertPart("part1").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试p2到part1的推理
     */
    @Test
    public void testp2Topart1() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParasByPara("p0", "1", "p2", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(1).hiddenEqual(false)
                .assertPara("p1").valueEqual(0).hiddenEqual(true)
                .assertPara("p2").valueEqual(2).hiddenEqual(false)
                .assertPart("part1").quantityEqual(2).hiddenEqual(false);
    }

    /**
     * 测试p1到part1的推理
     */
    @Test
    public void testp1Topart1() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParasByPara("p0", "2", "p1", "2");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(2).hiddenEqual(false)
                .assertPara("p1").valueEqual(2).hiddenEqual(false)
                .assertPara("p2").valueEqual(0).hiddenEqual(true)
                .assertPart("part1").quantityEqual(2).hiddenEqual(false);
    }

    /**
     * 测试p1到part1的无效输入
     */
    @Test
    public void testp1Topart1InvalidInput() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParasByPara("p0", "2", "p1", "3"); // 超出p1值域
        printSolutions();
        resultAssert().assertNoSolution();

    }

}