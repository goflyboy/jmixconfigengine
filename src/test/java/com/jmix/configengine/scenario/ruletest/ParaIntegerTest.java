package com.jmix.configengine.scenario.ruletest;

import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.ParaType;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ParaAnno;
import com.jmix.tool.model.PartAnno;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

/**
 * 整数参数测试类
 * 测试整数类型参数的各种约束和计算功能
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ParaIntegerTest extends ModuleScenarioTestBase {

    /**
     * 构造ParaIntegerTest测试类
     */
    public ParaIntegerTest() {
        super(ParaIntegerConstraint.class);
    }

    // --------------start----------------------------------------
    @ModuleAnno(id = 123L)
    private static class ParaIntegerConstraint extends ConstraintAlgImpl {
        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p1;

        @ParaAnno(type = ParaType.INTEGER, defaultValue = "0", minValue = "0", maxValue = "50")
        private ParaVar p2;

        @PartAnno(maxQuantity = 3)
        private PartVar part1;

        @Override
        protected void initConstraint() {
            // part1.quantity = p1.value + p2.value
            model.addEquality(part1.qty,
                    LinearExpr.sum(new IntVar[] { p1.value, p2.value }));
        }
    }

    // --------------end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    /**
     * 测试多解情况
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
     * 测试无解情况
     */
    @Test
    public void testNoSolution() {
        inferParas("part1", 4);
        resultAssert().assertNoSolution();
    }

    /**
     * 测试参数驱动推理
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
     * 测试零数量情况
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
     * 测试多参数推理
     */
    @Test
    public void testMultipleParaInference() {
        // 使用可变参数版本：inferParasByPara(String paraCode1, String value1, String paraCode2,
        // String value2, ...)
        inferParasByPara("p1", "2", "p2", "1");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPart("part1").quantityEqual(3);
    }
}