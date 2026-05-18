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
 * 鍏煎鎬ц鍒欐祴璇曠被
 * 
 * @since 2025-09-23
 */
@Slf4j
public class CompatibleRuleRequireTest extends ModuleScenarioTestBase {

    /**
     * 鏋勯€燙ompatibleRuleRequireTest娴嬭瘯绫?
     */
    public CompatibleRuleRequireTest() {
        super(CompatibleRuleRequireConstraint.class);
    }

    // ---------------瑙勫垯瀹氫箟start----------------------------------------
    /**
     * 鍏煎鎬ц鍒欒姹傜害鏉熸ā鍨嬬被
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    public static class CompatibleRuleRequireConstraint extends ModuleAlgBase {
        @ParaAnno(options = { "a1", "a2", "a3", "a4" })
        private ParaVar aVar;

        @ParaAnno(options = { "b1", "b2", "b3", "b4" })
        private ParaVar bVar;

        @CodeRuleAnno
        private void initConstraint() {
            // natural code: aVar.valueVar() in (a1,a3) Requires bVar.valueVar() in (b1,b2,b3)
            // A=(a1,a2,a3,a4)
            // B=(b1,b2,b3,b4)
            // 瑙勫垯鍐呭锛?a1,a3) Requires (b1,b2,b3)锛屽垯CA=(a1,a3),CB=(b1,b2,b3)
            // 瑙ｈ锛?
            // 1銆佹鍚?
            // 1.1 鍦–A涓紝濡傛灉aVar.var=a1,鍒檅Var.var=b1 鎴?b2 鎴?b3
            // 1.2 涓嶅湪CA涓紝濡傛灉aVar.var=a1,鍒檅Var.var=b4锛屾槸涓嶅悎娉曠殑
            // 2.鍙嶅悜
            // 2.1 鍦–B涓紝濡傛灉bVar.var=b1,鍒檃Var.var=a1 鎴?a2 鎴?a3 鎴?a4
            // 2.2 涓嶅湪CB涓紝濡傛灉bVar.var=b4,鍒檃Var.var=a2 鎴?a4
            addCompatibleConstraintRequires("rule1", aVar, Arrays.asList("a1", "a3"), bVar,
                    Arrays.asList("b1", "b2", "b3"));
        }

        /**
         * 鍒濆鍖栫害鏉?,鍙傝€?
         */
        protected void initConstraintNote() {
            // 鍒涘缓鏉′欢鍙橀噺锛欰鏄痑1鎴朼3
            AlgCPBoolVar a1OrA3 = model().newBoolVar("a1OrA3");
            model().addBoolOr(new AlgCPLiteral[] {
                    aVar.option("a1").selectedVar(),
                    aVar.option("a3").selectedVar()
            }).onlyEnforceIf(a1OrA3);
            model().addBoolAnd(new AlgCPLiteral[] {
                    aVar.option("a1").selectedVar().not(),
                    aVar.option("a3").selectedVar().not()
            }).onlyEnforceIf(a1OrA3.not());

            // 鍒涘缓鏉′欢鍙橀噺锛欱鏄痓1銆乥2鎴朾3
            AlgCPBoolVar b1OrB2OrB3 = model().newBoolVar("b1OrB2OrB3");
            model().addBoolOr(new AlgCPLiteral[] {
                    bVar.option("b1").selectedVar(),
                    bVar.option("b2").selectedVar(),
                    bVar.option("b3").selectedVar()
            }).onlyEnforceIf(b1OrB2OrB3);
            model().addBoolAnd(new AlgCPLiteral[] {
                    bVar.option("b1").selectedVar().not(),
                    bVar.option("b2").selectedVar().not(),
                    bVar.option("b3").selectedVar().not()
            }).onlyEnforceIf(b1OrB2OrB3.not());

            // 娣诲姞绾︽潫锛氬鏋淎鏄痑1鎴朼3锛屽垯B蹇呴』鏄痓1銆乥2鎴朾3
            model().addImplication(a1OrA3, b1OrB2OrB3);

            // 娉ㄦ剰锛氬弽鍚戠害鏉熶笉闇€瑕佹樉寮忔坊鍔狅紝鍥犱负杩欐槸"Requires"鍏崇郴鐨勫崟鍚戠害鏉?
            // 鍙嶅悜鎺ㄧ悊鐨勭粨鏋滄槸姝ｅ悜绾︽潫鐨勮嚜鐒剁粨鏋?
        }
    }

    // ---------------瑙勫垯瀹氫箟end----------------------------------------
    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    /**
     * 娴嬭瘯A1瑕佹眰B1銆丅2銆丅3
     */
    @Test
    public void testA1RequiresB1B2B3() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.1: 濡傛灉A閫夋嫨a1锛屽垯B蹇呴』鏄痓1銆乥2鎴朾3
        inferParasByPara("a", "a1");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 楠岃瘉鎵€鏈夎В涓瑽閮芥槸b1銆乥2鎴朾3
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 楠岃瘉B=b4鐨勮В涓嶅瓨鍦?
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 娴嬭瘯A3瑕佹眰B1銆丅2銆丅3
     */
    @Test
    public void testA3RequiresB1B2B3() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.1: 濡傛灉A閫夋嫨a3锛屽垯B蹇呴』鏄痓1銆乥2鎴朾3
        inferParasByPara("a", "a3");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(3);

        // 楠岃瘉鎵€鏈夎В涓瑽閮芥槸b1銆乥2鎴朾3
        for (int i = 0; i < 3; i++) {
            solutions(i).assertPara("b").valueIn("b1", "b2", "b3");
        }

        // 楠岃瘉B=b4鐨勮В涓嶅瓨鍦?
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 娴嬭瘯A1涓嶣4缁勫悎鏃犳晥
     */
    @Test
    public void testA1WithB4IsInvalid() {
        // 娴嬭瘯姝ｅ悜瑙勫垯1.2: 濡傛灉A閫夋嫨a1锛屽垯B閫夋嫨b4鏄笉鍚堟硶鐨?
        // 璁剧疆A=a1涓擝=b4锛屽簲璇ユ棤瑙?
        inferParasByPara("a", "a1");
        assertSolutionNum("b:b4", 0);
    }

    /**
     * 娴嬭瘯B1鍏佽浠绘剰A
     */
    @Test
    public void testB1AllowsAnyA() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.1: 濡傛灉B閫夋嫨b1锛屽垯A鍙互鏄痑1銆乤2銆乤3鎴朼4
        inferParasByPara("b", "b1");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4);

        // 楠岃瘉鎵€鏈夎В涓瑼鍙互鏄换鎰忓€?
        assertSolutionNum("a:a1", 1);
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a3", 1);
        assertSolutionNum("a:a4", 1);
    }

    /**
     * 娴嬭瘯B4鍙厑璁窤2銆丄4
     */
    @Test
    public void testB4AllowsA2A4Only() {
        // 娴嬭瘯鍙嶅悜瑙勫垯2.2: 濡傛灉B閫夋嫨b4锛屽垯A鍙兘鏄痑2鎴朼4
        inferParasByPara("b", "b4");

        // A鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(2);

        // 楠岃瘉A鍙兘鏄痑2鎴朼4
        assertSolutionNum("a:a2", 1);
        assertSolutionNum("a:a4", 1);

        // 楠岃瘉A涓嶈兘鏄痑1鎴朼3
        assertSolutionNum("a:a1", 0);
        assertSolutionNum("a:a3", 0);
    }

    /**
     * 娴嬭瘯A2銆丄4鍏佽浠绘剰B
     */
    @Test
    public void testA2A4AllowAnyB() {
        // 娴嬭瘯棰濆鎯呭喌: A閫夋嫨a2鎴朼4鏃讹紝B鍙互鏄换鎰忓€?
        inferParasByPara("a", "a2");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4);

        // 楠岃瘉鎵€鏈夎В涓瑽鍙互鏄换鎰忓€?
        assertSolutionNum("b:b1", 1);
        assertSolutionNum("b:b2", 1);
        assertSolutionNum("b:b3", 1);
        assertSolutionNum("b:b4", 1);

        // 鍚屾牱娴嬭瘯a4
        inferParasByPara("a", "a4");

        // B鏈?绉嶉€夋嫨
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4);
    }
}