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

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * 涓嶅吋瀹规€ц鍒欐祴璇曠被
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CompatibleRuleIncompatibleTest extends ModuleScenarioTestBase {
    // ---------------瑙勫垯瀹氫箟start----------------------------------------
    /**
     * 涓嶅吋瀹规€ц鍒欑害鏉熸ā鍨嬬被
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CompatibleRuleIncompatibleConstraint extends ModuleAlgBase {

        @ParaAnno(options = { "a1", "a2", "a3", "a4", "a5" })
        private ParaVar aVar;

        @ParaAnno(options = { "b1", "b2", "b3", "b4", "b5" })
        private ParaVar bVar;

        @CodeRuleAnno()
        private void initConstraint() {
            // 娉ㄦ剰锛?涓€瀹氳閫夋嫨涓€涓€兼€庝箞鐞嗚В锛堝鏋滀笉閫夋嫨锛屼篃璁剧疆涓€涓彲閫夊€煎氨鍙互琛ㄨ揪锛岃繖鏍锋ā鍨嬬粺涓€浜嗭級
            // A=(a1,a2,a3,a4,a5),aVar.value鐨勫€煎煙
            // B=(b1,b2,b3,b4,b5),bVar.value鐨勫€煎煙
            // 瑙勫垯鍐呭锛?a1,a3) Incompatible
            // (b1,b2,b3)锛屽垯CA=(a1,a3),CB=(b1,b2,b3),NCA=(a2,a4,a5),NCB=(b4,b5)
            // 瑙ｈ锛?
            // 1銆佹鍚?
            // 1.1
            // 濡傛灉aVar.value鍙朇A鐨勪竴涓€?鍦–A涓?锛屽垯bVar.value涓€瀹氫笉鏄疌B鐨勪竴涓€硷紝鍙朇B鍐呯殑鍊兼槸闈炴硶鐨勶紝鍙兘鏄疦CB鍐呯殑鍊硷紝濡?濡傛灉aVar.var=a1,鍒檅Var.var=b4鎴朾5(NCB),涓嶅彲浠ユ槸b1
            // 鎴?b2 鎴?b3(CB)
            // 1.2
            // 濡傛灉aVar.value鍙朜CA涓€涓€?涓嶅湪CA涓?锛屽垯bVar.value鍙互鏄疌B鐨勪竴涓€硷紝涔熷彲浠ユ槸NCB鐨勪竴涓€硷紝濡傦細濡傛灉aVar.var=a2,鍒檅Var.var=b1鎴朾2鎴朾3(CB),涔熷彲浠ユ槸b4鎴朾5(NCB)
            // 2.鍙嶅悜
            // 2.1
            // 濡傛灉bVar.value鍙朇B鐨勪竴涓€?鍦–B涓?锛屽垯aVar.value涓€瀹氫笉鏄疌A鐨勪竴涓€硷紝鍙朇A鍐呯殑鍊兼槸闈炴硶鐨勶紝鍙兘鏄疦CA鍐呯殑鍊硷紝濡?濡傛灉bVar.var=b1,鍒檃Var.var=a2鎴朼4鎴朼5(NCA),涓嶅彲浠ユ槸a1
            // 鎴?a3(CA)
            // 2.2
            // 濡傛灉bVar.value鍙朜CB涓€涓€?涓嶅湪CB涓?锛屽垯aVar.value鍙互鏄疌A鐨勪竴涓€硷紝涔熷彲浠ユ槸NCA鐨勪竴涓€硷紝濡傦細濡傛灉bVar.var=b4,鍒檃Var.var=a1
            // 鎴?a3(CA),涔熷彲浠ユ槸a2 鎴?a4鎴朼5(NCA)
            // 浣跨敤娉涘寲鐨処ncompatible绾︽潫鏂规硶
            addCompatibleConstraintInCompatible("rule1", aVar, Arrays.asList("a1", "a3"), bVar,
                    Arrays.asList("b1", "b2", "b3"));
        }

        /**
         * 娣诲姞鍏煎鎬ц鍒欙細Incompatible鍏崇郴绾︽潫, 鍙傝€冩牱渚?
         * 
         */
        public void addCompatibleConstraintIncompatibleNote() {
            // 鍒涘缓鏉′欢鍙橀噺锛欰鍦–A涓?
            AlgCPBoolVar inCA = model().newBoolVar("inCA");
            model().addBoolOr(new AlgCPLiteral[] {
                    aVar.option("a1").selectedVar(),
                    aVar.option("a3").selectedVar()
            }).onlyEnforceIf(inCA);
            model().addBoolAnd(new AlgCPLiteral[] {
                    aVar.option("a1").selectedVar().not(),
                    aVar.option("a3").selectedVar().not()
            }).onlyEnforceIf(inCA.not());

            // 鍒涘缓鏉′欢鍙橀噺锛欱鍦–B涓?
            AlgCPBoolVar inCB = model().newBoolVar("inCB");
            model().addBoolOr(new AlgCPLiteral[] {
                    bVar.option("b1").selectedVar(),
                    bVar.option("b2").selectedVar(),
                    bVar.option("b3").selectedVar()
            }).onlyEnforceIf(inCB);
            model().addBoolAnd(new AlgCPLiteral[] {
                    bVar.option("b1").selectedVar().not(),
                    bVar.option("b2").selectedVar().not(),
                    bVar.option("b3").selectedVar().not()
            }).onlyEnforceIf(inCB.not());

            // 鍒涘缓鏉′欢鍙橀噺锛欰涓嶅湪CA涓?
            AlgCPBoolVar notInCA = model().newBoolVar("notInCA");
            model().addBoolOr(new AlgCPLiteral[] {
                    aVar.option("a2").selectedVar(),
                    aVar.option("a4").selectedVar(),
                    aVar.option("a5").selectedVar()
            }).onlyEnforceIf(notInCA);
            model().addBoolAnd(new AlgCPLiteral[] {
                    aVar.option("a2").selectedVar().not(),
                    aVar.option("a4").selectedVar().not(),
                    aVar.option("a5").selectedVar().not()
            }).onlyEnforceIf(notInCA.not());

            // 鍒涘缓鏉′欢鍙橀噺锛欱涓嶅湪CB涓?
            AlgCPBoolVar notInCB = model().newBoolVar("notInCB");
            model().addBoolOr(new AlgCPLiteral[] {
                    bVar.option("b4").selectedVar(),
                    bVar.option("b5").selectedVar()
            }).onlyEnforceIf(notInCB);
            model().addBoolAnd(new AlgCPLiteral[] {
                    bVar.option("b4").selectedVar().not(),
                    bVar.option("b5").selectedVar().not()
            }).onlyEnforceIf(notInCB.not());

            // Incompatible 鍙屽悜绾︽潫锛?
            // 姝ｅ悜1.1锛氬鏋淎鍦–A涓紝鍒橞蹇呴』涓嶅湪CB涓紙蹇呴』鍦∟CB涓級
            model().addImplication(inCA, notInCB);

            // 姝ｅ悜1.2锛氬鏋淎涓嶅湪CA涓紝鍒橞鍙互鍦–B涓垨NCB涓紙鏃犵害鏉燂級
            // 杩欎釜绾︽潫涓嶉渶瑕佹樉寮忔坊鍔狅紝鍥犱负榛樿鍏佽

            // 鍙嶅悜2.1锛氬鏋淏鍦–B涓紝鍒橝蹇呴』涓嶅湪CA涓紙蹇呴』鍦∟CA涓級
            model().addImplication(inCB, notInCA);

            // 鍙嶅悜2.2锛氬鏋淏涓嶅湪CB涓紝鍒橝鍙互鍦–A涓垨NCA涓紙鏃犵害鏉燂級
            // 杩欎釜绾︽潫涓嶉渶瑕佹樉寮忔坊鍔狅紝鍥犱负榛樿鍏佽
        }
    }
    // ---------------瑙勫垯瀹氫箟end----------------------------------------

    /**
     * 鏋勯€燙ompatibleRuleIncompatibleTest娴嬭瘯绫?
     */
    public CompatibleRuleIncompatibleTest() {
        super(CompatibleRuleIncompatibleConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯鍦–A涓姹備笉鍦–B涓?
     */
    @Test
    public void testInCARequiresNotInCB() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.1: 濡傛灉A鍦–A涓?a1)锛屽垯B蹇呴』涓嶅湪CB涓?b4鎴朾5)
        inferParasByPara("a", "a1");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 楠岃瘉鎵€鏈夎В涓瑽閮藉湪NCB涓?
        for (int i = 0; i < 2; i++) {
            solutions(i).assertPara("b").valueIn("b4", "b5");
        }

        // 楠岃瘉B鍦–B涓殑瑙ｄ笉瀛樺湪
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 娴嬭瘯鍦–A涓姹備笉鍦–B涓紙a3鎯呭喌锛?
     */
    @Test
    public void testInCARequiresNotInCBA3() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.1: 濡傛灉A鍦–A涓?a3)锛屽垯B蹇呴』涓嶅湪CB涓?b4鎴朾5)
        inferParasByPara("a", "a3");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 楠岃瘉鎵€鏈夎В涓瑽閮藉湪NCB涓?
        for (int i = 0; i < 2; i++) {
            solutions(i).assertPara("b").valueIn("b4", "b5");
        }

        // 楠岃瘉B鍦–B涓殑瑙ｄ笉瀛樺湪
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 娴嬭瘯鍦–A涓笌鍦–B涓粍鍚堟棤鏁?
     */
    @Test
    public void testInCAWithInCBIsInvalid() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.1: 濡傛灉A鍦–A涓?a1)锛屽垯B鍦–B涓?b1銆乥2鎴朾3)鏄笉鍚堟硶鐨?
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
    }

    /**
     * 娴嬭瘯涓嶅湪CA涓厑璁镐换鎰廈
     */
    @Test
    public void testNotInCAAllowsAnyB() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.2: 濡傛灉A涓嶅湪CA涓?a2)锛屽垯B鍙互鏄疌B鎴朜CB涓殑浠绘剰鍊?
        inferParasByPara("a", "a2");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 楠岃瘉B鍙互鍦–B涓?
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 楠岃瘉B涔熷彲浠ュ湪NCB涓?
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 娴嬭瘯涓嶅湪CA涓厑璁镐换鎰廈锛坅4鎯呭喌锛?
     */
    @Test
    public void testNotInCAAllowsAnyBA4() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.2: 濡傛灉A涓嶅湪CA涓?a4)锛屽垯B鍙互鏄疌B鎴朜CB涓殑浠绘剰鍊?
        inferParasByPara("a", "a4");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 楠岃瘉B鍙互鍦–B涓?
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 楠岃瘉B涔熷彲浠ュ湪NCB涓?
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 娴嬭瘯涓嶅湪CA涓厑璁镐换鎰廈锛坅5鎯呭喌锛?
     */
    @Test
    public void testNotInCAAllowsAnyBA5() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.2: 濡傛灉A涓嶅湪CA涓?a5)锛屽垯B鍙互鏄疌B鎴朜CB涓殑浠绘剰鍊?
        inferParasByPara("a", "a5");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 楠岃瘉B鍙互鍦–B涓?
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);

        // 楠岃瘉B涔熷彲浠ュ湪NCB涓?
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);
    }

    /**
     * 娴嬭瘯鍦–B涓姹備笉鍦–A涓?
     */
    @Test
    public void testInCBRequiresNotInCA() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.1: 濡傛灉B鍦–B涓?b1)锛屽垯A蹇呴』涓嶅湪CA涓?a2銆乤4鎴朼5)
        inferParasByPara("b", "b1");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 楠岃瘉A鍦∟CA涓?
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 楠岃瘉A鍦–A涓殑瑙ｄ笉瀛樺湪
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 娴嬭瘯鍦–B涓姹備笉鍦–A涓紙b2銆乥3鎯呭喌锛?
     */
    @Test
    public void testInCBRequiresNotInCAB2B3() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.1: 濡傛灉B鍦–B涓?b2鎴朾3)锛屽垯A蹇呴』涓嶅湪CA涓?a2銆乤4鎴朼5)
        inferParasByPara("b", "b2");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 楠岃瘉A鍦∟CA涓?
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 楠岃瘉A鍦–A涓殑瑙ｄ笉瀛樺湪
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 娴嬭瘯涓嶅湪CB涓厑璁镐换鎰廇
     */
    @Test
    public void testNotInCBAllowsAnyA() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.2: 濡傛灉B涓嶅湪CB涓?b4)锛屽垯A鍙互鏄疌A鎴朜CA涓殑浠绘剰鍊?
        inferParasByPara("b", "b4");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 楠岃瘉A鍙互鍦–A涓?
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 楠岃瘉A涔熷彲浠ュ湪NCA涓?
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }

    /**
     * 娴嬭瘯涓嶅湪CB涓厑璁镐换鎰廇锛坆5鎯呭喌锛?
     */
    @Test
    public void testNotInCBAllowsAnyAB5() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.2: 濡傛灉B涓嶅湪CB涓?b5)锛屽垯A鍙互鏄疌A鎴朜CA涓殑浠绘剰鍊?
        inferParasByPara("b", "b5");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(5);

        // 楠岃瘉A鍙互鍦–A涓?
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);

        // 楠岃瘉A涔熷彲浠ュ湪NCA涓?
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }

    /**
     * 娴嬭瘯Incompatible鍙屽悜绾︽潫
     */
    @Test
    public void testIncompatibleBidirectionalConstraint() {
        // 娴嬭瘯Incompatible鍙屽悜绾︽潫鐨勫畬鏁存€?
        // 楠岃瘉CA鍜孋B缁勪箣闂寸殑鍙屽悜涓嶅吋瀹瑰叧绯?

        // 娴嬭瘯1: A鍦–A涓椂锛孊蹇呴』涓嶅湪CB涓?
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b1", 0);
        assertSolutionNum("b:b2", 0);
        assertSolutionNum("b:b3", 0);
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 娴嬭瘯2: A涓嶅湪CA涓椂锛孊鍙互鍦–B涓垨NCB涓?
        inferParasByPara("a", "a2");
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);
        assertSolutionNum("b:b4", 1);
        assertSolutionNum("b:b5", 1);

        // 娴嬭瘯3: B鍦–B涓椂锛孉蹇呴』涓嶅湪CA涓?
        inferParasByPara("b", "b1");
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);

        // 娴嬭瘯4: B涓嶅湪CB涓椂锛孉鍙互鍦–A涓垨NCA涓?
        inferParasByPara("b", "b4");
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a3", 1);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);
        assertSolutionNum("a:a5", 1);
    }
}