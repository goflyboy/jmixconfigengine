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
public class CalculateRuleIfThenTest extends ModuleScenarioTestBase {
    /**
     * 鏋勯€燙alculateRuleIfThenTest娴嬭瘯绫?
     */
    public CalculateRuleIfThenTest() {
        super(CalculateRuleConstraint.class);
    }

    // ---------------瑙勫垯瀹氫箟start----------------------------------------
    /**
     * 璁＄畻瑙勫垯绾︽潫妯″瀷绫?
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CalculateRuleConstraint extends ModuleAlgBase {

        @ParaAnno(defaultValue = "op11", options = { "op11", "op12", "op13" })
        private ParaVar p1Var;

        @ParaAnno(defaultValue = "op21", options = { "op21", "op22", "op23" })
        private ParaVar p2Var;

        @PartAnno
        private PartVar pt1Var;

        @CodeRuleAnno()
        private void initConstraint() {
            addConstraintRule2();
        }

        /**
         * 娣诲姞绾︽潫瑙勫垯2
         */
        public void addConstraintRule2() {

            // if(p1Var.valueVar() ==op11 && p2Var.valueVar() == op21 ) {
            // pt1Var.quantityVar() = 1;
            // }
            // else {
            // pt1Var.quantityVar() = 3
            // }

            // 鍒涘缓鏉′欢鍙橀噺锛氶鑹叉槸绾㈣壊涓斿昂瀵告槸灏忓彿
            AlgCPBoolVar op11Andop21 = model().newBoolVar("rule2_op11Andop21");

            // 瀹炵幇鏉′欢閫昏緫锛歳edAndSmall = (p1== op11) AND (p2 == op21)
            model().addBoolAnd(new AlgCPLiteral[] {
                    this.p1Var.option("op11").selectedVar(),
                    this.p2Var.option("op21").selectedVar()
            }).onlyEnforceIf(op11Andop21);

            // 濡傛灉涓嶆槸绾㈣壊涓斿皬鍙风殑缁勫悎锛屽垯op11Andop2涓篺alse
            model().addBoolOr(new AlgCPLiteral[] {
                    this.p1Var.option("op11").selectedVar().not(),
                    this.p2Var.option("op21").selectedVar().not()
            }).onlyEnforceIf(op11Andop21.not());

            // 鏍规嵁鏉′欢璁剧疆pt1Var鐨勬暟閲?
            // 濡傛灉op11Andop2涓簍rue锛屽垯pt1Var鏁伴噺涓?
            model().addEquality(pt1Var.quantityVar(), 1).onlyEnforceIf(op11Andop21);

            // 濡傛灉op11Andop2涓篺alse锛屽垯pt1Var鏁伴噺涓?
            model().addEquality(pt1Var.quantityVar(), 3).onlyEnforceIf(op11Andop21.not());
        }
    }

    // ---------------瑙勫垯瀹氫箟end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯瑙勫垯2鐨刬f鏉′欢锛坥p11鍜宱p21锛?
     */
    @Test
    public void testRule2IfOp11Op21() {
        // 娴嬭瘯棰滆壊鍙傛暟鎺ㄧ悊
        inferParas("pt1", 1);

        // 楠岃瘉绗竴涓В
        solutions(0).assertPara("p1").valueEqual("op11")
                .assertPara("p2").valueEqual("op21");
        printSolutions();
    }

    /**
     * 娴嬭瘯瑙勫垯2鐨別lse鏉′欢锛坥p11鍜宱p21锛?
     */
    @Test
    public void testRule2ElseOp11Op21() {
        inferParas("pt1", 3);
        printSolutions();
        // 鍙嶆帹鏈?涓帴鍙?8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(8);
        // if鐨勮В鑲畾涓嶅湪鍏朵腑
        assertSolutionNum("p1:op11,p2:op21", 0);
        // else鐨勮В鍙兘瑙ｅ涓嬶細
        assertSolutionNum("p1:op12,p2:op22", 1);
        printSolutions();
    }

    /**
     * 娴嬭瘯瑙勫垯2鐨勬棤if-else鎯呭喌
     */
    @Test
    public void testRule2NoIfElse() {
        inferParas("pt1", 4);
        printSolutions();
        // 鍙嶆帹鏈?涓帴鍙?8=3*3 - 1
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(0);
        // if鐨勮В鑲畾涓嶅湪鍏朵腑
        assertSolutionNum("p1:op11,p2:op21", 0);
        // else鐨勮В涔熶笉鍦ㄥ叾涓?
        assertSolutionNum("p1:op12,p2:op22", 0);
        printSolutions();
    }

}