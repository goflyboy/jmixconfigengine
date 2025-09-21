package com.jmix.configengine.scenario.autoruletest;

import com.jmix.configengine.scenario.base.CodeRuleAnno;
import com.jmix.configengine.scenario.base.CompatiableRuleAnno;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.artifact.ConstraintAlgImpl;
import com.jmix.executor.impl.artifact.ParaVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class InjectCompatibleRuleTest extends ModuleScenarioTestBase {

    @ModuleAnno(id = 123L)
    static public class InjectCompatibleRuleConstraint extends ConstraintAlgImpl {
        @ParaAnno(options = { "Red", "Black", "White" })
        private ParaVar ColorVar;

        @ParaAnno(options = { "Small", "Medium", "Big" })
        private ParaVar SizeVar;

        // @Override
        // protected void initConstraint() {//不能有这个代码
        // }
        @CodeRuleAnno(code = "rule1")
        private void rule1() {
            System.out.println("****************rule1****************");
        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == Red", operator = "Requires", rightExprCode = "SizeVar.value == Small")
        private void rule2() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintRequires("rule2", this.ColorVar, listOf("Red"), this.SizeVar, listOf("Small"));
            // 自动生成，请勿编辑--end

        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == Black", operator = "CoDependent", rightExprCode = "SizeVar.value == Medium")
        private void rule3() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintCoDependent("rule3", this.ColorVar, listOf("Black"), this.SizeVar, listOf("Medium"));
            // 自动生成，请勿编辑--end

        }

        @CompatiableRuleAnno(leftExprCode = "ColorVar.value == White", operator = "InCompatible", rightExprCode = "SizeVar.value == Big")
        private void rule4() {

            // 自动生成，请勿编辑--start
            addCompatibleConstraintInCompatible("rule4", this.ColorVar, listOf("White"), this.SizeVar, listOf("Big"));
            // 自动生成，请勿编辑--end

        }
    }

    public InjectCompatibleRuleTest() {
        super(InjectCompatibleRuleConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1);
    }

    @Test
    public void testRule2Requires() {
        inferParasByPara("Color", "Red");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("Color").valueEqual("Red")
                .assertPara("Size").valueEqual("Small");
    }

    @Test
    public void testRule2CoDependent() {
        inferParasByPara("Color", "Black");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("Color").valueEqual("Black")
                .assertPara("Size").valueEqual("Medium");
    }

    @Test
    public void testRule2InCompatible() {
        inferParasByPara("Color", "White");
        assertSolutionNum("Color:White,Size:Big", 0);
    }
}