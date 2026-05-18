package com.jmix.tool.autoruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.CompatiableRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 娉ㄥ叆鍏煎鎬ц鍒欐祴璇曠被
 * 娴嬭瘯閫氳繃娉ㄨВ娉ㄥ叆鍏煎鎬ц鍒欑殑鍔熻兘
 *
 * @since 2025-09-22
 */
@Slf4j
public class InjectCompatibleRuleTest extends ModuleScenarioTestBase {
    /**
     * 鏋勯€營njectCompatibleRuleTest娴嬭瘯绫?
     */
    public InjectCompatibleRuleTest() {
        super(InjectCompatibleRuleConstraint.class);
    }

    /**
     * 娉ㄥ叆鍏煎鎬ц鍒欑害鏉熸ā鍨嬬被
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class InjectCompatibleRuleConstraint extends ModuleAlgBase {
        @ParaAnno(options = { "Red", "Black", "White" })
        private ParaVar colorVar;

        @ParaAnno(options = { "Small", "Medium", "Big" })
        private ParaVar sizeVar;

        @CodeRuleAnno(code = "rule1")
        private void rule1() {
            log.info("****************rule1****************");
        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.valueVar() == Red", operator = "Requires", rightExprCode = "sizeVar.valueVar() == Small")
        private void rule2() {

            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-start
            addCompatibleConstraintRequires("rule2", this.colorVar, listOf("Red"), this.sizeVar, listOf("Small"));
            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-end

        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.valueVar() == Black", operator = "CoDependent", rightExprCode = "sizeVar.valueVar() == Medium")
        private void rule3() {

            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-start
            addCompatibleConstraintCoDependent("rule3", this.colorVar, listOf("Black"), this.sizeVar, listOf("Medium"));
            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-end

        }

        @CompatiableRuleAnno(leftExprCode = "colorVar.valueVar() == White", operator = "InCompatible", rightExprCode = "sizeVar.valueVar() == Big")
        private void rule4() {

            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-start
            addCompatibleConstraintInCompatible("rule4", this.colorVar, listOf("White"), this.sizeVar, listOf("Big"));
            // 鑷姩鐢熸垚锛岃鍕跨紪杈?-end

        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯瑙勫垯2鐨凴equires绾︽潫
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
     * 娴嬭瘯瑙勫垯2鐨凜oDependent绾︽潫
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
     * 娴嬭瘯瑙勫垯2鐨処nCompatible绾︽潫
     */
    @Test
    @Disabled
    public void testRule2InCompatible() {
        inferParasByPara("color", "White");
        assertSolutionNum("color:White,size:Big", 0);
    }
}