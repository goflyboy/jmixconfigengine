package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 鏁存暟鍙傛暟娴嬭瘯绫?
 * 娴嬭瘯鏁存暟绫诲瀷鍙傛暟鐨勫悇绉嶇害鏉熷拰璁＄畻鍔熻兘
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ParaIntegerTest extends ModuleScenarioTestBase {

    /**
     * 鏋勯€燩araIntegerTest娴嬭瘯绫?
     */
    public ParaIntegerTest() {
        super(ParaIntegerConstraint.class);
    }

    // --------------start----------------------------------------
    /**
     * 鏁存暟鍙傛暟绾︽潫妯″瀷绫?
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class ParaIntegerConstraint extends ModuleAlgBase {
        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p1;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar part1;

        @CodeRuleAnno
        private void initConstraint() {
            // part1.quantity = p1.valueVar() + p2.valueVar()
            AlgCPLinearExpr sumExpr = model().newLinearExpr("sum_p1_p2");
            sumExpr.addTerm(p1.valueVar(), 1);
            sumExpr.addTerm(p2.valueVar(), 1);
            model().addEquality(part1.quantityVar(), sumExpr);
        }
    }

    // --------------end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯澶氳В鎯呭喌
     */
    @Test
    public void testMultipleSolutions() {
        inferParas("part1", 3);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4);
        assertSolutionNum("p1:0,p2:3", 1);
        assertSolutionNum("p1:1,p2:2", 1);
        assertSolutionNum("p1:2,p2:1", 1);
        assertSolutionNum("p1:3,p2:0", 1);
    }

    /**
     * 娴嬭瘯鏃犺В鎯呭喌
     */
    @Test
    public void testNoSolution() {
        inferParas("part1", 4);
        resultAssert().assertSuccess()
                .assertSolutionSizeEqual(0);
    }

    /**
     * 娴嬭瘯鍙傛暟椹卞姩鎺ㄧ悊
     */
    @Test
    public void testParaDrivenInference() {
        inferParasByPara("p1", "2", "p2", "1");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPart("part1").quantityEqual(3);
    }

    /**
     * 娴嬭瘯闆舵暟閲忔儏鍐?
     */
    @Test
    public void testZeroQuantity() {
        inferParas("part1", 0);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("p1").valueEqual("0")
                .assertPara("p2").valueEqual("0");
    }

    /**
     * 娴嬭瘯澶氬弬鏁版帹鐞?
     */
    @Test
    public void testMultipleParaInference() {
        // 浣跨敤鍙彉鍙傛暟鐗堟湰锛歩nferParasByPara(String paraCode1, String value1, String paraCode2,
        // String value2, ...)
        inferParasByPara("p1", "2", "p2", "1");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPart("part1").quantityEqual(3);
    }
}