package com.jmix.tool.packtest;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.scenario.ruletest.ParaIsHiddenTest;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 单文件打包测试
 * 对ParaIsHiddenTest进行打包加载，运行其中用例
 * 
 * @since 2025-09-27
 */
@Slf4j
public class SingleFilePackerTest extends ModuleScenarioTestBase {

    /**
     * 构造SingleFilePackerTest测试类
     */
    public SingleFilePackerTest() {
        super(ParaIsHiddenTest.ParaIsHiddenConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        cfg.setAttachedDebug(false); // 打包模式
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
    }
}
