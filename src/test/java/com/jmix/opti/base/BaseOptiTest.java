package com.jmix.opti.base;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrInherit;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class BaseOptiTest extends ModuleScenarioTestBase {

    // ---------------妯″瀷鐨勫畾涔塻tart----------------------------------------
    @ModuleAnno(id = 123L)
    static public class BaseOptiConstraint extends ModuleAlgBase {
        // 纭洏閮ㄤ欢鍒嗙被瀹氫箟--涓ユ牸鎸夊眰绾х粨鏋勫畾涔夛紙椤哄簭寰堥噸瑕侊級锛岄儴浠剁殑attrs涔熻鎸夊畾涔夌殑椤哄簭鏉?
        @PartAnno(code = "drive")
        @DAttrAnno2(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING, optionExtSchema = "StringUnit", options = {
                "Speed_3000:3000:rpm",
                "Speed_9000:9000:rpm",
                "Speed_5400:5400:rpm",
                "Speed_9900:9900:rpm",
                "Speed_7200a5400:7200/5400:rpm",
                "Speed_7200:7200:rpm" }, instType = 0)
        @DAttrAnno3(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_1T:1:T",
                "Capacity_2T:2:T",
                "Capacity_3T:3:T",
                "Capacity_6T:6:T",
                "Capacity_9T:9:T",
                "Capacity_4T:4:T" }, instType = 0)
        private PartCategoryVar drive;

        // 鍥烘€佺‖鐩橀儴浠跺垎绫伙紝缁ф壙driveVar骞堕噸鍐欏睘鎬?
        @PartAnno(code = "sd", fatherCode = "drive")
        @DAttrInherit(fatherCode = "driveVar")
        private PartCategoryVar sd;

        // 鏈烘纭洏閮ㄤ欢鍒嗙被锛岀户鎵縟riveVar
        @PartAnno(code = "md", fatherCode = "drive")
        @DAttrInherit(fatherCode = "driveVar")
        private PartCategoryVar md;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "sd", attrs = { "5400", "3" })
        private PartVar sd1;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "sd", attrs = { "7200", "6" })
        private PartVar sd2;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "sd", attrs = { "9000", "9" })
        private PartVar sd3;

        // 鏈烘纭洏瀹炰緥1
        @PartAnno(fatherCode = "md", attrs = { "5400", "1" })
        private PartVar md1;

        // 鏈烘纭洏瀹炰緥2
        @PartAnno(fatherCode = "md", attrs = { "7200", "2" })
        private PartVar md2;

        // 鏈烘纭洏瀹炰緥3
        @PartAnno(fatherCode = "md", attrs = { "9000", "3" })
        private PartVar md3;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Capacity;// 杈撳叆鍙傛暟

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Quantity; // 杈撳叆鍙傛暟

        // proRule1:鍥烘€佺‖鐩樺繀椤婚厤缃悓涓€绉嶏紝骞朵笖鏈€澶氶厤缃?鍧?
        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "ssd same type max 2")
        private void rule1() {
            // proRule1-natuarl: 鍥烘€佺‖鐩樺繀椤婚厤缃悓涓€绉嶏紝骞朵笖鏈€澶氶厤缃?鍧?
            // proRule1-dsl: 鎷嗗垎涓簆roRule11鍜宲roRule11涓ゆ潯绾︽潫锛堝拰isSelected(S)銆乹ty(Q)鐩稿叧锛?
            // proRule11-cRule: sd1.S + sd2.S <=1
            AlgCPLinearExpr sumSelected = model().sum4Selected("fatherCode=sd");
            model().addLessOrEqual(sumSelected, 1);

            // proRule12-cRule: sd1.Q + sd2.Q <= 2
            AlgCPLinearExpr sumQty = model().sum4Quantity("fatherCode=sd");
            model().addLessOrEqual(sumQty, 2);
        }

        // // proRule2:鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
        // @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode =
        // "鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺", attrCode = "capacityWeight", strategy =
        // PriorityStrategy.MAX, type = PriorityType.SELECT)
        // private void rule2() {
        // // proRule2-natuarl: 鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
        // // proRule2-dsl: 閫夋嫨鐨勯儴浠禼apacityWeight鎬诲拰瓒婂ぇ瓒婂ソ( 鍜宷ty(Q) * capacityWeight 鐩稿叧)
        // // proRule2-expr: sd1.S*110 +sd2.S*120 + md1.S*13
        // // proRule2-step1: maximum(expr) ->
        // // proRule2-step2: expr >= 200*(1-30%) = 84
        // } // 浼樺厛浣跨敤鍥烘€佺‖鐩橈細濡傛灉鍥烘€佺‖鐩樺閲忓凡瓒冲锛岄檺鍒舵満姊扮‖鐩樹娇鐢?TODO锛屾€庝箞琛ㄨ揪

        // proRule2:鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺", strategy = PriorityStrategy.MIN)
        private void rule2() {
            // proRule2-natuarl: 鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
            // proRule2-dsl: 閫夋嫨鐨勯儴浠禼apacityWeight鎬诲拰瓒婂ぇ瓒婂ソ( 鍜宷ty(Q) * capacityWeight 鐩稿叧)
            // proRule2-expr: sd1.S*110 +sd2.S*120 + md1.S*13
            // proRule2-step1: maximum(expr) ->
            // proRule2-step2: expr >= 200*(1-30%) = 84
            applyPriorityRule();

        } // 浼樺厛浣跨敤鍥烘€佺‖鐩橈細濡傛灉鍥烘€佺‖鐩樺閲忓凡瓒冲锛岄檺鍒舵満姊扮‖鐩樹娇鐢?TODO锛屾€庝箞琛ㄨ揪

        // 瑙勫垯1: 鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
        private void applyPriorityRule() {
            List<PartVar> partVars = partVars("");

            PartAlgCPLinearExpr ssTotalCapacity = model().sum4Quantity("Capacity", "fatherCode=sd");
            PartAlgCPLinearExpr mechTotalCapacity = model().sum4Quantity("Capacity", "fatherCode=md");
            // 濡傛灉鏄閲忛渶姹?
            // if ("Capacity".equals(req.getAttrCode())) {
            if (Sum_Capacity.hasInput()) {
                int requiredCapacity = Sum_Capacity.inputValue();
                // 鍒涘缓鍥烘€佺‖鐩樻槸鍚﹁冻澶熺殑甯冨皵鍙橀噺
                AlgCPBoolVar ssSufficient = model().newBoolVar(
                        "ssSufficient");

                // 瀹氫箟锛氬鏋滃浐鎬佺‖鐩樺閲?>= 闇€姹傚閲忥紝鍒?ssSufficient = true
                model().addGreaterOrEqual(ssTotalCapacity,
                        requiredCapacity).onlyEnforceIf(ssSufficient);
                model().addLessThan(ssTotalCapacity,
                        requiredCapacity).onlyEnforceIf(ssSufficient.not());

                // 瑙勫垯1.1: 濡傛灉鍥烘€佺‖鐩樿冻澶燂紝鍒欑姝娇鐢ㄦ満姊扮‖鐩?
                List<PartVar> mechanicalParts = partVars("fatherCode=md");
                for (PartVar pv : mechanicalParts) {
                    model().addEquality(pv.quantityVar(), 0).onlyEnforceIf(ssSufficient);
                }

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");

                // 鍩虹鐩爣: 鏈€澶у寲SSD浣跨敤锛堣礋鏉冮噸锛?-瀹归噺瓒婂ぇ瓒婂ソ
                objectiveExpr.addExpr(ssTotalCapacity, -100);

                // HDD鎯╃綒 = HDD瀹归噺 * 鎯╃綒绯绘暟S
                objectiveExpr.addExpr(mechTotalCapacity, 1);

                // 3. 鎯╃綒杩囧害閰嶇疆锛堥噸瑕侊紒锛?
                PartAlgCPLinearExpr totalCapacityExpr = model().sum4Selected("Capacity", "").name("totalCapacityExpr");

                // 鍒涘缓杩囧害閰嶇疆鍙橀噺绾︽潫锛歟xcessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr tExpr = model().newPartLinearExpr("excessCapacityExpr");
                tExpr.addExpr(totalCapacityExpr, 1);
                tExpr.addConstant(-requiredCapacity);
                // 2. 杩囧害閰嶇疆鎯╃綒
                objectiveExpr.addExpr(tExpr, 500); // 鎯╃綒杩囧害閰嶇疆

                // 4. 鎯╃綒浣跨敤澶?涓浂浠讹紙榧撳姳绠€娲侀厤缃級
                // 鎬婚浂浠舵暟閲忔儵缃?
                PartAlgCPLinearExpr totalPartsExpr = model().sum4Quantity("").name("totalPartsExpr");

                objectiveExpr.addExpr(totalPartsExpr, 500); // 闆朵欢鏁伴噺鎯╃綒
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("rule2", objectiveExpr);

            } else {// 缁欑殑鏁伴噺qty鎬绘暟

                PartAlgCPLinearExpr ssTotalQty = model().sum4Quantity("fatherCode=sd");
                PartAlgCPLinearExpr mechTotalQty = model().sum4Quantity("fatherCode=md");

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                int requiredQty = Sum_Quantity.inputValue();
                // 鍒涘缓鍥烘€佺‖鐩樻槸鍚﹁冻澶熺殑甯冨皵鍙橀噺
                AlgCPBoolVar ssSufficientQty = (AlgCPBoolVar) model().newBoolVar(
                        "ssSufficientQty");
                // 瀹氫箟锛氬鏋滃浐鎬佺‖鐩樺閲?>= 闇€姹傚閲忥紝鍒?ssSufficient = true
                model().addGreaterOrEqual(ssTotalQty,
                        requiredQty).onlyEnforceIf(ssSufficientQty);
                model().addLessThan(ssTotalQty,
                        requiredQty).onlyEnforceIf(ssSufficientQty.not());
                // 瑙勫垯1.1: 濡傛灉鍥烘€佺‖鐩樿冻澶燂紝鍒欑姝娇鐢ㄦ満姊扮‖鐩?
                List<PartVar> mechanicalParts = partVars("fatherCode=md");
                for (PartVar pv : mechanicalParts) {
                    model().addEquality(pv.quantityVar(), 0).onlyEnforceIf(ssSufficientQty);
                }

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFunQty");

                // 鍩虹鐩爣: 鏈€澶у寲SSD浣跨敤锛堣礋鏉冮噸锛?-瀹归噺瓒婂ぇ瓒婂ソ
                objectiveExpr.addExpr(ssTotalCapacity, -100);
                // objectiveExpr.addExpr(ssTotalQty, -100);
                // HDD鎯╃綒 = HDD瀹归噺 * 鎯╃綒绯绘暟S锛屽閲忚秺灏忚秺濂?
                objectiveExpr.addExpr(mechTotalCapacity, 1);
                // objectiveExpr.addExpr(mechTotalQty, 1);

                // 3. 鎯╃綒杩囧害閰嶇疆锛堥噸瑕侊紒锛?
                PartAlgCPLinearExpr totalQtyExpr = model().sum4Quantity("");

                // 鍒涘缓杩囧害閰嶇疆鍙橀噺 绾︽潫锛歟xcessCapacity = totalCapacity - requiredCapacity
                PartAlgCPLinearExpr excessQyExpr = model().newPartLinearExpr("excessQyExpr");
                excessQyExpr.addExpr(totalQtyExpr, 1);
                excessQyExpr.addConstant(-requiredQty);
                // 2. 杩囧害閰嶇疆鎯╃綒
                objectiveExpr.addExpr(excessQyExpr, 500); // 鎯╃綒杩囧害閰嶇疆

                model().setObjectExpr(objectiveExpr); // 鍒唌inimize/adddGreaterxx
                // model().minimize(objectiveExpr); // 璁剧疆鐩爣鍑芥暟涓烘渶灏忓寲锛堝洜涓篠SD鏈夎礋鏉冮噸锛?
                updatePriorityObjectFuntion("rule2", objectiveExpr);
            }
        }

    }
    // ---------------妯″瀷鐨勫畾涔塭nd----------------------------------------

    public BaseOptiTest() {
        super(BaseOptiConstraint.class);
    }

    // 瑕佹眰5400閫熺巼鐨勭‖鐩?鍧?
    @Test
    public void firstBase() {
        // 娴嬭瘯鐐癸細鐖跺眰category锛?琛ㄨ揪寮?
        inferRecommend("drive", "drive:Sum_Quantity ==2 where Speed=5400");
        // Print solutions for debugging
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1)");
        assertSoluContain(3, "md1(Q:2,H:0,S:1),sd1(0*)");

        // Req:
        // drive:Sum_Quantity ==2 where Speed=5400
        // md1.Q* + sd1.Q* == 2

        // proRule1:鍥烘€佺‖鐩樺繀椤婚厤缃悓涓€绉嶏紝骞朵笖鏈€澶氶厤缃?鍧?
        // sd1.S <= 1
        // sd1.Q <= 2

        // proRule2-鍥烘€佺‖鐩樹紭鍏堝尮閰嶉珮閫熺巼瀹归噺锛岀敤鏈烘纭洏澧為厤浣庨€熺巼瀹归噺
        // objfun 1*(-100*3*sd1.Q_1 + 1*1*md1.Q_1 + 500*(1*(1*sd1.Q_1 + 1*md1.Q_1) - 2))
    }

    // 鐢ㄤ緥0锛?娴嬭瘯鐐癸紝瑙ｈ1
    // 杈撳叆锛?
    // strReq = " Capacity >=6 where Speed = 5400"
    // 杈撳嚭锛?
    // 瑙?锛?sd1.quantityVar()=2
    // 瑙?锛?sd1.quantityVar()=1 md1.quantityVar()=3 //澧為厤浣庨€熺巼瀹归噺
    // ...
    @Test
    public void testCase0_CapacityGreaterEqual6Speed5400() {
        inferRecommend("drive", "drive:Sum_Capacity >=6 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:3,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase1_CapacityGreaterEqual5Speed5400() {
        inferRecommend("drive", "drive:Sum_Capacity >=5 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:2,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase2_CapacityGreaterEqual7Speed5400() {
        inferRecommend("drive", "drive:Sum_Capacity >=7 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),sd1(Q:2,H:0,S:1)");
    }

    @Test
    public void testCase3_QtyGreaterEqual3Speed5400() {
        inferRecommend("drive", "drive:Sum_Quantity >=3 where Speed = 5400");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),sd1(Q:2,H:0,S:1)");
        assertSoluContain("md1(Q:2,H:0,S:1),sd1(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase5_CapacityGreaterEqual5NoSpeedFilter() {
        inferRecommend("drive", "drive:Sum_Capacity >=5");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),md2(0*),md3(0*),sd1(Q:2,H:0,S:1),sd2(0*),sd3(0*)");
        // issue: sd1.Q=1,md2.Q=2 涓轰粈涔堟病鏈夊嚭鏉?
    }

    @Test
    public void testCase6_QuantityGreaterEqual2NoSpeedFilter() {
        inferRecommend("drive", "drive:Sum_Quantity >=2");
        printSimpleSolutions();
        assertSoluContain(1, "md1(0*),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(0*),md2(0*),md3(0*),sd1(0*),sd2(Q:2,H:0,S:1),sd3(0*)");
        assertSoluContain("md1(Q:1,H:0,S:1),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:1,H:0,S:1)");
    }

    @Test
    public void testCase7_QuantityGreaterEqual3NoSpeedFilter() {
        inferRecommend("drive", "drive:Sum_Quantity >=3");
        printSimpleSolutions();
        assertSoluContain(1, "md1(Q:1,H:0,S:1),md2(0*),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(2, "md1(0*),md2(Q:1,H:0,S:1),md3(0*),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
        assertSoluContain(3, "md1(0*),md2(0*),md3(Q:1,H:0,S:1),sd1(0*),sd2(0*),sd3(Q:2,H:0,S:1)");
    }

    // 瑕佹眰5400閫熺巼鐨勫浐鎬佺‖鐩?鍧?
    @Test
    public void testNoSpeedRequirement() {
        inferRecommend("drive", "drive:Sum_Quantity ==2 where Speed=3000");
        // resultAssert().assertSolutionSizeEqual(0);
    }
}
