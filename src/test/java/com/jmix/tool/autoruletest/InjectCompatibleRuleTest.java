package com.jmix.tool.autoruletest;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.imodel.anno.CodeRuleAnno;
import com.jmix.executor.imodel.anno.CompatiableRuleAnno;
import com.jmix.executor.imodel.anno.ModuleAnno;
import com.jmix.executor.imodel.anno.ParaAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 注入兼容性规则测试类
 * 测试通过注解注入兼容性规则的功能
 * 
 * @since 2025-09-22
 */
@Slf4j
public class InjectCompatibleRuleTest extends ModuleScenarioTestBase {
    /**
     * 构造InjectCompatibleRuleTest测试类
     */
    public InjectCompatibleRuleTest() {
        super(InjectCompatibleRuleConstraint.class);
    }

    /**
     * 注入兼容性规则约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class InjectCompatibleRuleConstraint extends ConstraintAlgImplTestBase {
        @ParaAnno(options = { "Red", "Black", "White" })
        private ParaVar colorVar;

        @ParaAnno(options = { "Small", "Medium", "Big" })
        private ParaVar sizeVar;

        @CodeRuleAnno(code = "rule1")
        private void rule1() {
            log.info("****************rule1****************");
        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.value == Red", operator = "Requires", rightExprCode = "sizeVar.value == Small")
        private void rule2() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintRequires("rule2", this.colorVar, listOf("Red"), this.sizeVar, listOf("Small"));
            // 自动生成，请勿编辑--end

        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.value == Black", operator = "CoDependent", rightExprCode = "sizeVar.value == Medium")
        private void rule3() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintCoDependent("rule3", this.colorVar, listOf("Black"), this.sizeVar, listOf("Medium"));
            // 自动生成，请勿编辑--end

        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.value == White", operator = "InCompatible", rightExprCode = "sizeVar.value == Big")
        private void rule4() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintInCompatible("rule4", this.colorVar, listOf("White"), this.sizeVar, listOf("Big"));
            // 自动生成，请勿编辑--end

        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 测试规则2的Requires约束
     */
    @Test
    @Disabled
    public void testRule2Requires() {
        inferParasByPara("color", "Red");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("color").valueEqual("Red")
                .assertPara("size").valueEqual("Small");
    }

    /**
     * 测试规则2的CoDependent约束
     */
    @Test
    @Disabled
    public void testRule2CoDependent() {
        inferParasByPara("color", "Black");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("color").valueEqual("Black")
                .assertPara("size").valueEqual("Medium");
    }

    /**
     * 测试规则2的InCompatible约束
     */
    @Test
    @Disabled
    public void testRule2InCompatible() {
        inferParasByPara("color", "White");
        assertSolutionNum("color:White,size:Big", 0);
    }
}