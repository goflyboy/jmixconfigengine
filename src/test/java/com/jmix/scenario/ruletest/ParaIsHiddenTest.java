package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 鍙傛暟闅愯棌娴嬭瘯绫?
 * 娴嬭瘯鍙傛暟闅愯棌鍔熻兘鐨勫悇绉嶅満鏅拰绾︽潫瑙勫垯
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ParaIsHiddenTest extends ModuleScenarioTestBase {
    /**
     * 鏋勯€燩araIsHiddenTest娴嬭瘯绫?
     */
    public ParaIsHiddenTest() {
        super(ParaIsHiddenConstraint.class);
    }

    // --------------start----------------------------------------
    /**
     * 鍙傛暟闅愯棌绾︽潫妯″瀷绫?
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    @AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "para-hidden-2026.05")
    public static class ParaIsHiddenConstraint extends ModuleAlgBase {
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
            // Logic1: p1.hiddenVar() and p2.hiddenVar() are incompatible
            model().addBoolOr(new AlgCPLiteral[] { p1.hiddenVar().not(), p2.hiddenVar().not() });
            addVarAboutHiddenConstraints(p1, p2);

            // Logic2: if p0.valueVar() in (0,1) then p1.hiddenVar()=1 else p2.hiddenVar()=1
            AlgCPBoolVar p0Eq0 = model().newBoolVar("p0_eq_0");
            AlgCPBoolVar p0Eq1 = model().newBoolVar("p0_eq_1");
            model().addEquality(p0.valueVar(), 0).onlyEnforceIf(p0Eq0);
            model().addDifferent(p0.valueVar(), 0).onlyEnforceIf(p0Eq0.not());
            model().addEquality(p0.valueVar(), 1).onlyEnforceIf(p0Eq1);
            model().addDifferent(p0.valueVar(), 1).onlyEnforceIf(p0Eq1.not());

            // p0In01 is true iff p0.valueVar() == 0 or p0.valueVar() == 1
            AlgCPBoolVar p0In01 = model().newBoolVar("p0_in_01");
            model().addBoolOr(new AlgCPLiteral[] { p0Eq0, p0Eq1 }).onlyEnforceIf(p0In01);
            model().addBoolAnd(new AlgCPLiteral[] { p0Eq0.not(), p0Eq1.not() }).onlyEnforceIf(p0In01.not());
            // Ensure not both p0Eq0 and p0Eq1 are true simultaneously
            model().addBoolOr(new AlgCPLiteral[] { p0Eq0.not(), p0Eq1.not() });

            model().addEquality(p1.hiddenVar(), 1).onlyEnforceIf(p0In01);
            model().addEquality(p2.hiddenVar(), 1).onlyEnforceIf(p0In01.not());

            // Logic3: if p1.hiddenVar()=1 then p1.valueVar()=0
            model().addEquality(p1.valueVar(), 0).onlyEnforceIf(p1.hiddenVar());

            // Logic4: if p2.hiddenVar()=1 then p2.valueVar()=0
            model().addEquality(p2.valueVar(), 0).onlyEnforceIf(p2.hiddenVar());

            // Logic5: part1.quantity = p1.valueVar() + p2.valueVar()
            AlgCPLinearExpr sumExpr = model().newLinearExpr("sum_p1_p2");
            sumExpr.addTerm(p1.valueVar(), 1);
            sumExpr.addTerm(p2.valueVar(), 1);
            model().addEquality(part1.quantityVar(), sumExpr);
        }
    }

    // ---------------end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯p0鍊间负0鏃秔1闅愯棌
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
     * 娴嬭瘯p0鍊间负1鏃秔1闅愯棌
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
     * 娴嬭瘯p0鍊间负2鏃秔2闅愯棌
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
     * 娴嬭瘯p2鍒皃art1鐨勬帹鐞?
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
     * 娴嬭瘯p1鍒皃art1鐨勬帹鐞?
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
     * 娴嬭瘯p1鍒皃art1鐨勬棤鏁堣緭鍏?
     */
    @Test
    public void testp1Topart1InvalidInput() {
        // Test when p0=0, p1 should be hidden and p1.var=0
        inferParasByPara("p0", "2", "p1", "3"); // 瓒呭嚭p1鍊煎煙
        printSolutions();
        resultAssert().assertSuccess()
                .assertSolutionSizeEqual(0);

    }

}
