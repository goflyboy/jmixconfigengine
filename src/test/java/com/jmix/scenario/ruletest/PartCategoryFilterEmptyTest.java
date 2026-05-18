package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
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
 * PartCategory 杩囨护涓虹┖鏃剁殑杈撳嚭澶勭悊娴嬭瘯
 * RFC-0002: PartCategory Filter Empty Handling
 *
 * @since 2026-04-30
 */
@Slf4j
public class PartCategoryFilterEmptyTest extends ModuleScenarioTestBase {

    public PartCategoryFilterEmptyTest() {
        super(FilterEmptyConstraint.class);
    }

    // ---------------妯″瀷瀹氫箟----------------------------------------
    @ModuleAnno(id = 123L)
    public static class FilterEmptyConstraint extends ModuleAlgBase {

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

    // ---------------妯″瀷瀹氫箟缁撴潫----------------------------------------

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    // ==================== ERR-001: 鍗曚釜鍒嗙被杩囨护涓虹┖ ====================

    /**
     * ERR-001-1: cpu 杩囨护涓虹┖锛宒rive 姝ｅ父
     * cpu: CoreNum=8 鏃犲尮閰嶉儴浠讹紝drive: Speed=5400 鍖归厤 drive1(PT1)
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
     * ERR-001-2: drive 杩囨护涓虹┖锛宑pu 姝ｅ父
     * cpu: CoreNum=4 鍖归厤 cpu4锛宒rive: Speed=7200 鏃犲尮閰嶉儴浠?
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
     * ERR-001-3: 涓や釜閮芥甯?
     * cpu: CoreNum=4 鍖归厤 cpu4锛宒rive: Speed=5400 鍖归厤 drive1
     */
    @Test
    public void testErr001_BothOk() {
        inferRecommendModule(
                "cpu: where CoreNum=4",
                "drive: where Speed=5400");
        printSolutions();

        resultAssert().assertCodeEqual(Result.SUCCESS);
        assertSoluContain("cpu4");
        // drive part 鐨?shortCode 鏄?PT1 (auto-generated), code 鏄?drive1
        assertSoluContain("PT1");
    }

    // ==================== ERR-002: 涓や釜鍒嗙被閮借繃婊や负绌?====================

    /**
     * ERR-002-1: 涓や釜閮戒负绌?
     * cpu: CoreNum=8 鏃犲尮閰嶏紝drive: Speed=7200 鏃犲尮閰?
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
