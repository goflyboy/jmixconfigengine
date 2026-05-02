package com.jmix.scenario.ruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * PartCategory 过滤为空时的输出处理测试
 * RFC-0002: PartCategory Filter Empty Handling
 *
 * @since 2026-04-30
 */
@Slf4j
public class PartCategoryFilterEmptyTest extends ModuleScenarioTestBase {

    public PartCategoryFilterEmptyTest() {
        super(FilterEmptyConstraint.class);
    }

    // ---------------模型定义----------------------------------------
    @ModuleAnno(id = 123L)
    public static class FilterEmptyConstraint extends ConstraintAlgImplTestBase {

        @PartAnno(code = "cpu")
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"CoreNum_4:4", "CoreNum_8:8"})
        private PartCategoryVar cpu;

        @PartAnno(fatherCode = "cpu", attrs = {"4"}, price = 100)
        private PartVar cpu4;

        @PartAnno(code = "drive")
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400"}, price = 50)
        private PartVar drive1;
    }

    // ---------------模型定义结束----------------------------------------

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    // ==================== ERR-001: 单个分类过滤为空 ====================

    /**
     * ERR-001-1: cpu 过滤为空，drive 正常
     * cpu: CoreNum=8 无匹配部件，drive: Speed=5400 匹配 drive1(PT1)
     */
    @Test
    public void testErr001_CpuFilterEmpty_DriveOk() {
        inferRecommendModule(
                "cpu: where CoreNum=8",
                "drive: where Speed=5400");
        printSolutions();

        resultAssert().assertCodeEqual(Result.PARTIAL_SUCCESS);
        assertSoluContain("cpu:FILTER_EMPTY");
    }

    /**
     * ERR-001-2: drive 过滤为空，cpu 正常
     * cpu: CoreNum=4 匹配 cpu4，drive: Speed=7200 无匹配部件
     */
    @Test
    public void testErr001_CpuOk_DriveFilterEmpty() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "drive: where Speed=7200");
        printSolutions();

        resultAssert().assertCodeEqual(Result.PARTIAL_SUCCESS);
        assertSoluContain("drive:FILTER_EMPTY");
    }

    /**
     * ERR-001-3: 两个都正常
     * cpu: CoreNum=4 匹配 cpu4，drive: Speed=5400 匹配 drive1
     */
    @Test
    public void testErr001_BothOk() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "drive: where Speed=5400");
        printSolutions();

        resultAssert().assertCodeEqual(Result.SUCCESS);
        assertSoluContain("cpu4");
        // drive part 的 shortCode 是 PT1 (auto-generated), code 是 drive1
        assertSoluContain("PT1");
    }

    // ==================== ERR-002: 两个分类都过滤为空 ====================

    /**
     * ERR-002-1: 两个都为空
     * cpu: CoreNum=8 无匹配，drive: Speed=7200 无匹配
     */
    @Test
    public void testErr002_BothFilterEmpty() {
        inferRecommendModule(
                "cpu: where CoreNum=8",
                "drive: where Speed=7200");
        printSolutions();

        resultAssert().assertCodeEqual(Result.NO_SOLUTION);
    }
}
