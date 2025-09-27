package com.jmix.tool.packtest;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
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
        cfg.setLoadType(1);
    }

    /**
     * 测试p0值为0时p1隐藏（打包模式）
     */
    @Test
    public void testp0Value0p1HiddenPacked() {
        log.info("Testing p0=0, p1 should be hidden in packed mode");

        // 使用打包模式初始化
        init(false);

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
                .assertPart("part1").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试p0值为1时p1隐藏（打包模式）
     */
    @Test
    public void testp0Value1p1HiddenPacked() {
        log.info("Testing p0=1, p1 should be hidden in packed mode");

        // 使用打包模式初始化
        init(false);

        // Test when p0=1, p1 should be hidden and p1.var=0
        inferParas("part1", 1, "p0", "1");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("p0").valueEqual(1).hiddenEqual(false)
                .assertPara("p1").valueEqual(0).hiddenEqual(true)
                .assertPara("p2").valueEqual(1).hiddenEqual(false)
                .assertPart("part1").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试p0值为2时p2隐藏（打包模式）
     */
    @Test
    public void testp0Value2p2HiddenPacked() {
        log.info("Testing p0=2, p2 should be hidden in packed mode");

        // 使用打包模式初始化
        init(false);

        // Test when p0=2, p2 should be hidden and p2.var=0
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
     * 测试p2到part1的推理（打包模式）
     */
    @Test
    public void testp2Topart1Packed() {
        log.info("Testing p2 to part1 inference in packed mode");

        // 使用打包模式初始化
        init(false);

        // Test when p0=1, p1 should be hidden and p1.var=0
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
     * 测试p1到part1的推理（打包模式）
     */
    @Test
    public void testp1Topart1Packed() {
        log.info("Testing p1 to part1 inference in packed mode");

        // 使用打包模式初始化
        init(false);

        // Test when p0=2, p1 should be hidden and p1.var=0
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
     * 测试p1到part1的无效输入（打包模式）
     */
    @Test
    public void testp1Topart1InvalidInputPacked() {
        log.info("Testing p1 to part1 invalid input in packed mode");

        // 使用打包模式初始化
        init(false);

        // Test when p0=2, p1 should be hidden and p1.var=0
        inferParasByPara("p0", "2", "p1", "3"); // 超出p1值域
        printSolutions();
        resultAssert().assertNoSolution();
    }

    /**
     * 对比测试：调试模式 vs 打包模式
     */
    @Test
    public void testDebugVsPackedMode() {
        log.info("Comparing debug mode vs packed mode");

        // 测试调试模式
        init(true);
        inferParas("part1", 1, "p0", "0");
        int debugSolutionCount = getSolutions() != null ? getSolutions().size() : 0;
        log.info("Debug mode solution count: {}", debugSolutionCount);

        // 清理
        tearDown();

        // 测试打包模式
        init(false);
        inferParas("part1", 1, "p0", "0");
        int packedSolutionCount = getSolutions() != null ? getSolutions().size() : 0;
        log.info("Packed mode solution count: {}", packedSolutionCount);

        // 验证两种模式的结果一致
        if (debugSolutionCount != packedSolutionCount) {
            throw new AssertionError(String.format(
                    "Solution count mismatch: debug=%d, packed=%d",
                    debugSolutionCount, packedSolutionCount));
        }

        log.info("Debug and packed modes produce consistent results");
    }
}
