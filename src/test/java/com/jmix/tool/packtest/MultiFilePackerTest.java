package com.jmix.tool.packtest;

import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
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
        cfg.setLoadType(1);
    }

    /**
     * 测试红色小号T恤（打包模式）
     */
    @Test
    public void testRedSmallTShirtPacked() {
        log.info("Testing red small T-shirt in packed mode");

        // 使用打包模式初始化
        init(false);

        // 测试红色小号组合，应该得到1件T恤
        inferParas("tShirt11", 1, "color", "Red", "size", "Small");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("color").valueEqual("Red").hiddenEqual(false)
                .assertPara("size").valueEqual("Small").hiddenEqual(false)
                .assertPart("tShirt11").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 测试红色中号T恤（打包模式）
     */
    @Test
    public void testRedMediumTShirtPacked() {
        log.info("Testing red medium T-shirt in packed mode");

        // 使用打包模式初始化
        init(false);

        // 测试红色中号组合，应该得到3件T恤
        inferParas("tShirt11", 3, "color", "Red", "size", "Medium");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("color").valueEqual("Red").hiddenEqual(false)
                .assertPara("size").valueEqual("Medium").hiddenEqual(false)
                .assertPart("tShirt11").quantityEqual(3).hiddenEqual(false);
    }

    /**
     * 测试黑色小号T恤（打包模式）
     */
    @Test
    public void testBlackSmallTShirtPacked() {
        log.info("Testing black small T-shirt in packed mode");

        // 使用打包模式初始化
        init(false);

        // 测试黑色小号组合，应该得到3件T恤
        inferParas("tShirt11", 3, "color", "Black", "size", "Small");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("color").valueEqual("Black").hiddenEqual(false)
                .assertPara("size").valueEqual("Small").hiddenEqual(false)
                .assertPart("tShirt11").quantityEqual(3).hiddenEqual(false);
    }

    /**
     * 测试白色大号T恤（打包模式）
     */
    @Test
    public void testWhiteBigTShirtPacked() {
        log.info("Testing white big T-shirt in packed mode");

        // 使用打包模式初始化
        init(false);

        // 测试白色大号组合，应该得到3件T恤
        inferParas("tShirt11", 3, "color", "White", "size", "Big");
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        solutions(0)
                .assertPara("color").valueEqual("White").hiddenEqual(false)
                .assertPara("size").valueEqual("Big").hiddenEqual(false)
                .assertPart("tShirt11").quantityEqual(3).hiddenEqual(false);
    }

    /**
     * 测试所有颜色尺寸组合（打包模式）
     */
    @Test
    public void testAllColorSizeCombinationsPacked() {
        log.info("Testing all color-size combinations in packed mode");

        // 使用打包模式初始化
        init(false);

        String[] colors = { "Red", "Black", "White" };
        String[] sizes = { "Small", "Medium", "Big" };

        for (String color : colors) {
            for (String size : sizes) {
                log.info("Testing combination: {} - {}", color, size);

                // 根据规则，只有红色小号组合得到1件，其他都是3件
                int expectedQuantity = ("Red".equals(color) && "Small".equals(size)) ? 1 : 3;

                inferParas("tShirt11", expectedQuantity, "color", color, "size", size);
                resultAssert()
                        .assertSuccess()
                        .assertSolutionSizeEqual(1);

                solutions(0)
                        .assertPara("color").valueEqual(color).hiddenEqual(false)
                        .assertPara("size").valueEqual(size).hiddenEqual(false)
                        .assertPart("tShirt11").quantityEqual(expectedQuantity).hiddenEqual(false);

                log.info("Combination {} - {} passed: quantity={}", color, size, expectedQuantity);
            }
        }
    }

    /**
     * 测试参数推理（打包模式）
     */
    @Test
    public void testParameterInferencePacked() {
        log.info("Testing parameter inference in packed mode");

        // 使用打包模式初始化
        init(false);

        // 测试通过部件数量推理参数
        inferParas("tShirt11", 1);
        printSolutions();
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);

        // 验证推理结果：只有红色小号组合才能得到1件T恤
        solutions(0)
                .assertPara("color").valueEqual("Red").hiddenEqual(false)
                .assertPara("size").valueEqual("Small").hiddenEqual(false)
                .assertPart("tShirt11").quantityEqual(1).hiddenEqual(false);
    }

    /**
     * 对比测试：调试模式 vs 打包模式
     */
    @Test
    public void testDebugVsPackedMode() {
        log.info("Comparing debug mode vs packed mode for HelloConstraint");

        // 测试调试模式
        init(true);
        inferParas("tShirt11", 1, "color", "Red", "size", "Small");
        int debugSolutionCount = getSolutions() != null ? getSolutions().size() : 0;
        log.info("Debug mode solution count: {}", debugSolutionCount);

        // 清理
        tearDown();

        // 测试打包模式
        init(false);
        inferParas("tShirt11", 1, "color", "Red", "size", "Small");
        int packedSolutionCount = getSolutions() != null ? getSolutions().size() : 0;
        log.info("Packed mode solution count: {}", packedSolutionCount);

        // 验证两种模式的结果一致
        if (debugSolutionCount != packedSolutionCount) {
            throw new AssertionError(String.format(
                    "Solution count mismatch: debug=%d, packed=%d",
                    debugSolutionCount, packedSolutionCount));
        }

        // 验证解决方案内容一致
        if (debugSolutionCount > 0 && packedSolutionCount > 0) {
            // 重新获取调试模式的解决方案
            init(true);
            inferParas("tShirt11", 1, "color", "Red", "size", "Small");
            String debugColor = getSolutions().get(0).getParas().stream()
                    .filter(p -> "color".equals(p.getCode()))
                    .findFirst().map(p -> p.getValue()).orElse("");

            // 重新获取打包模式的解决方案
            tearDown();
            init(false);
            inferParas("tShirt11", 1, "color", "Red", "size", "Small");
            String packedColor = getSolutions().get(0).getParas().stream()
                    .filter(p -> "color".equals(p.getCode()))
                    .findFirst().map(p -> p.getValue()).orElse("");

            if (!debugColor.equals(packedColor)) {
                throw new AssertionError(String.format(
                        "Solution content mismatch: debug color=%s, packed color=%s",
                        debugColor, packedColor));
            }
        }

        log.info("Debug and packed modes produce consistent results for HelloConstraint");
    }

    /**
     * 测试模块打包功能
     */
    @Test
    public void testModulePacking() {
        log.info("Testing module packing functionality");

        // 使用打包模式初始化，这会触发打包过程
        init(false);

        // 验证模块已正确加载
        if (getModule() == null) {
            throw new AssertionError("Module not loaded after packing");
        }

        // 验证模块基本信息
        if (!"Hello".equals(getModule().getCode())) {
            throw new AssertionError("Module code mismatch: expected=Hello, actual=" + getModule().getCode());
        }

        if (getModule().getId() == null || getModule().getId() != 123L) {
            throw new AssertionError("Module ID mismatch: expected=123, actual=" + getModule().getId());
        }

        log.info("Module packing test passed: code={}, id={}", getModule().getCode(), getModule().getId());
    }
}
