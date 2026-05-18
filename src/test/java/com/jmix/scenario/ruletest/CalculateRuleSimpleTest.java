package com.jmix.scenario.ruletest;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * Hello绾︽潫绠楁硶娴嬭瘯绫?
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CalculateRuleSimpleTest extends ModuleScenarioTestBase {
    /**
     * 鏋勯€燙alculateRuleSimpleTest娴嬭瘯绫?
     */
    public CalculateRuleSimpleTest() {
        super(CalculateRuleConstraint.class);
    }

    // ---------------瑙勫垯瀹氫箟start----------------------------------------
    /**
     * 绠€鍗曡绠楄鍒欑害鏉熸ā鍨嬬被
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CalculateRuleConstraint extends ModuleAlgBase {

        @ParaAnno(options = { "op11", "op12", "op13" })
        private ParaVar p1Var;

        @PartAnno
        private PartVar pt1Var;

        @CodeRuleAnno
        private void initConstraint() {
            addConstraintRule1();
        }

        /**
         * 娣诲姞绾︽潫瑙勫垯1
         */
        public void addConstraintRule1() {
            // if(p1Var.valueVar() == op11) {
            // pt1Var.quantityVar() = 1;
            // }
            // else {
            // pt1Var.quantityVar() = 3;
            // }

            // 鍒涘缓鏉′欢鍙橀噺锛歱1鏄痮p11
            AlgCPBoolVar op11 = model().newBoolVar("rule1_op11");

            // 瀹炵幇鏉′欢閫昏緫锛歳ule1_op11 = (p1 == op11)
            model().addBoolAnd(new AlgCPLiteral[] {
                    this.p1Var.option("op11").selectedVar()
            }).onlyEnforceIf(op11);

            // 濡傛灉p1涓嶆槸op11锛屽垯rule1_op11涓篺alse
            model().addBoolOr(new AlgCPLiteral[] {
                    this.p1Var.option("op11").selectedVar().not()
            }).onlyEnforceIf(op11.not());

            // 鏍规嵁鏉′欢璁剧疆pt1Var鐨勬暟閲?
            // 濡傛灉rule1_op11涓簍rue锛屽垯pt1Var鏁伴噺涓?
            model().addEquality(pt1Var.quantityVar(), 1).onlyEnforceIf(op11);

            // 濡傛灉rule1_op11涓篺alse锛屽垯pt1Var鏁伴噺涓?
            model().addEquality(pt1Var.quantityVar(), 3).onlyEnforceIf(op11.not());
        }
    }

    // ---------------瑙勫垯瀹氫箟end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯if鏉′欢锛坥p11锛?
     */
    @Test
    public void testIfOp11() {
        // 娴嬭瘯棰滆壊鍙傛暟鎺ㄧ悊
        inferParas("pt1", 1);

        // 楠岃瘉绗竴涓В
        solutions(0).assertPara("p1").valueEqual("op11");
        printSolutions();
    }

    /**
     * 娴嬭瘯else鏉′欢锛坥p11锛?
     */
    @Test
    public void testElseOp11() {
        inferParas("pt1", 3);
        printSolutions();
        // 鍙嶆帹鏈?涓帴鍙?8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);
        // if鐨勮В鑲畾涓嶅湪鍏朵腑
        assertSolutionNum("p1:op11", 0);
        printSolutions();
    }

    /**
     * 娴嬭瘯鏃爄f-else鎯呭喌
     */
    @Test
    public void testNoIfElse() {
        inferParas("pt1", 4);
        printSolutions();
        // 鍙嶆帹鏈?涓帴鍙?8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
        // if鐨勮В鑲畾涓嶅湪鍏朵腑
        assertSolutionNum("p1:op11", 0);
        printSolutions();
    }

}