package com.jmix.scenario.hello;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * Hello约束算法测试类
 * 
 * @since 2025-09-23
 */
@Slf4j
public class HelloConstraintTest extends ModuleScenarioTestBase {

    /**
     * 构造HelloConstraintTest测试类
     */
    public HelloConstraintTest() {
        super(HelloConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 测试只有一个解的情况
     */
    @Test
    public void testOnlyOneSolution() {
        // 测试颜色参数推理
        inferParas("tShirt11", 1);

        // 验证第一个解
        solutions(0).assertPara("color").valueEqual("Red")
                .assertPara("size").valueEqual("Small");
        printSolutions();
    }

    /**
     * 测试多解情况
     */
    @Test
    public void testMultiSolution() {
        // "Red-10", "Black-20", "White-30"
        // "Small-10", "Medium-20", "Big-30"
        // if(colorVar.value ==Red && sizeVar.value == Small ) {
        // tShirt11Var.qty = 1;
        // }
        // else {
        // tShirt11Var.qty = 3
        // }
        // 测试颜色参数推理
        inferParas("tShirt11", 3);
        printSolutions();
        // 反推有8个接口 8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);
        // if的解肯定不在其中
        assertSolutionNum("color:Red,size:Small", 0);
        // else的解可能解如下：
        assertSolutionNum("color:Black,size:Big", 1);
        assertSolutionNum("color:White,size:Medium", 1);
        assertSolutionNum("color:Red,size:Medium", 1);
        assertSolutionNum("color:Black,size:Medium", 1);
        assertSolutionNum("color:White,size:Big", 1);
        assertSolutionNum("color:Red,size:Big", 1);
        assertSolutionNum("color:Black,size:Small", 1);
        printSolutions();
    }

}