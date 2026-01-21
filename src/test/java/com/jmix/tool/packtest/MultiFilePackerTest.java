package com.jmix.tool.packtest;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.scenario.hello.HelloConstraint;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * 多文件打包测试
 * 对HelloConstraint进行打包加载，运行其中用例
 * 
 * @since 2025-09-27
 */
@Slf4j
public class MultiFilePackerTest extends ModuleScenarioTestBase {

    /**
     * 构造MultiFilePackerTest测试类
     */
    public MultiFilePackerTest() {
        super(HelloConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        cfg.setAttachedDebug(false); // 打包模式
    }

    /**
     * 测试只有一个解的情况
     */
    @Test
    public void testOnlyOneSolution() {
        // 测试颜色参数推理
        inferParas("tShirt11", 1);

        // 使用resultAssert验证执行结果
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
    }
}
