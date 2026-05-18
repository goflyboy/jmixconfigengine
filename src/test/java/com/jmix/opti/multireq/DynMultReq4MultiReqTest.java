package com.jmix.opti.multireq;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.logic.Cardinality;
import com.jmix.executor.bmodel.logic.EffectScope;
import com.jmix.executor.bmodel.logic.PriorityStrategy;
import com.jmix.executor.southinf.cp.AlgCPLinearExpr;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class DynMultReq4MultiReqTest extends ModuleScenarioTestBase {

    // ---------------妯″瀷鐨勫畾涔塻tart----------------------------------------
    @ModuleAnno(id = 123L)
    static public class DynMultReqMultiInstConstraint extends ModuleAlgBase {

        // 纭洏閮ㄤ欢鍒嗙被瀹氫箟--涓ユ牸鎸夊眰绾х粨鏋勫畾涔夛紙椤哄簭寰堥噸瑕侊級锛岄儴浠剁殑attrs涔熻鎸夊畾涔夌殑椤哄簭鏉?
        @PartAnno(supportMultiInst = true)
        @DAttrAnno1(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING, optionExtSchema = "StringUnit", options = {
                "Speed_3000:3000:rpm",
                "Speed_9000:9000:rpm",
                "Speed_5400:5400:rpm",
                "Speed_9900:9900:rpm",
                "Speed_7200a5400:7200/5400:rpm",
                "Speed_7200:7200:rpm" })
        @DAttrAnno2(code = "Capacity", optionExtSchema = "IntegerUnit", options = { "Capacity_1T:1:T",
                "Capacity_2T:2:T",
                "Capacity_3T:3:T",
                "Capacity_6T:6:T",
                "Capacity_9T:9:T",
                "Capacity_4T:4:T" })
        @DAttrAnno3(code = "Type", dynAttrType = DynamicAttributeType.E_STRING, options = { "SD:sd",
                "MD:md" }, instType = 0)
        private PartCategoryVar drive;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "drive", attrs = { "5400", "3", "sd" }, price = 50)
        private PartVar sd1;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "drive", attrs = { "7200", "6", "sd" }, price = 80)
        private PartVar sd2;

        // 鍥烘€佺‖鐩樺疄渚?
        @PartAnno(fatherCode = "drive", attrs = { "9000", "9", "sd" }, price = 90)
        private PartVar sd3;

        // 鏈烘纭洏瀹炰緥1
        @PartAnno(fatherCode = "drive", attrs = { "5400", "1", "md" }, price = 30)
        private PartVar md1;

        // 鏈烘纭洏瀹炰緥2
        @PartAnno(fatherCode = "drive", attrs = { "7200", "2", "md" }, price = 40)
        private PartVar md2;

        // 鏈烘纭洏瀹炰緥3
        @PartAnno(fatherCode = "drive", attrs = { "9000", "3", "md" }, price = 60)
        private PartVar md3;

        // // 鏀瑰姩4锛氬師鏉ュ睘浜庡垎绫?锛堝锛夌殑鍙傛暟锛岀敱浜庡睘浜庢暣涓垎绫?锛屽綋鍓嶅瀹炰緥鍖栵紝鍙兘鎻愬崌鍒颁骇鍝佺骇fatherCode = "drive"-> ""
        // @ParaAnno(fatherCode = "", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar driveSumCapacity;// 杈撳叆鍙傛暟

        // @ParaAnno(fatherCode = "", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar driveSumQuantity; // 杈撳叆鍙傛暟

        // // 鏀瑰姩4锛氬師鏉ュ睘浜庡垎绫?锛堝锛夌殑鍙傛暟锛岀敱浜庡睘浜庢暣涓垎绫?锛屽綋鍓嶅瀹炰緥鍖栵紝鍙兘鎻愬崌鍒颁骇鍝佺骇fatherCode = "drive"-> ""
        // @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumCapacity;// 杈撳叆鍙傛暟

        // @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumQuantity; // 杈撳叆鍙傛暟

        // ==================== CPU閮ㄤ欢鍒嗙被瀹氫箟 ====================
        @PartAnno()
        @DAttrAnno1(code = "CoreNum", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "CoreNum_2:2:core",
                "CoreNum_4:4:core",
                "CoreNum_8:8:core",
                "CoreNum_18:18:core" })
        @DAttrAnno2(code = "Memory", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "Memory_123:123:G",
                "Memory_256:256:G",
                "Memory_512:512:G",
                "Memory_1024:1024:G" })
        @DAttrAnno3(code = "ConfigType", dynAttrType = DynamicAttributeType.E_INT, optionExtSchema = "IntegerUnit", options = {
                "ConfigType_2:2:閰嶇疆",
                "ConfigType_5:5:閰嶇疆" })
        private PartCategoryVar cpu;

        // CPU瀹炰緥1: CoreNum=2, Memory=123, ConfigType=2
        @PartAnno(fatherCode = "cpu", attrs = { "2", "123", "2" }, price = 100)
        private PartVar cpu1;

        // CPU瀹炰緥2: CoreNum=4, Memory=256, ConfigType=2
        @PartAnno(fatherCode = "cpu", attrs = { "4", "256", "2" }, price = 200)
        private PartVar cpu2;

        // CPU瀹炰緥3: CoreNum=8, Memory=512, ConfigType=5
        @PartAnno(fatherCode = "cpu", attrs = { "8", "512", "5" }, price = 400)
        private PartVar cpu3;

        // CPU瀹炰緥4: CoreNum=18, Memory=1024, ConfigType=5
        @PartAnno(fatherCode = "cpu", attrs = { "18", "1024", "5" }, price = 800)
        private PartVar cpu4;

        // @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar cpuSumCores; // 杈撳叆鍙傛暟 TODO:涓嶉渶瑕佸缓绔嬶紝搴旇浠庡紩鎿庡共鎺?TODO

        // @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType =
        // AssignType.INPUT)
        // private ParaVar SumMemory; // 杈撳叆鍙傛暟
        // 鏀瑰姩鐐?锛氬垎绫?(澶氾級澶氫釜璇锋眰鐨勬暣浣撹姹傦紝闇€瑕佹湁鏁翠綋姹囨€?

        // 鏀瑰姩鐐?锛氬垎绫?锛堝崟)->鍒嗙被2锛堝锛夛紝闇€瑕佽缃负鎷嗗垎涓哄鏉¤鍒?Cardinality(1:N)
        @CodeRuleAnno(normalNaturalCode = "cpu4 incompatible with ssd", leftProObjsStr = "cpu:CoreNum|Quantity|Select", rightProObjsStr = "drive:Type|Quantity|Select", leftCardinality = Cardinality.ONE, rightCardinality = Cardinality.MANY)
        private void logicAB1() {
            // TODO锛?code鐨勫姩鎬佹€ф€庝箞淇濋殰
            inCompatible("logicAB1", "cpu:CoreNum=4", "drive:Type=sd");
        }

        @CodeRuleAnno(fatherCode = "cpu", normalNaturalCode = "浠呰兘浣跨敤涓€绉岰PU", leftProObjsStr = "cpu:Selected")
        private void logicA1() {
            AlgCPLinearExpr cpuSelected = model().sum4Selected("").name("cpuSelected");
            model().addLessOrEqual(cpuSelected, 1);
        }

        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "ssd same type max 2", leftProObjsStr = "drive:Select|Quantity", effectScope = EffectScope.SingleInst)
        private void logicB1() {
            // proRule1-natuarl: 鍥烘€佺‖鐩樺繀椤婚厤缃悓涓€绉嶏紝骞朵笖鏈€澶氶厤缃?鍧?
            // proRule1-dsl: 鎷嗗垎涓簆roRule11鍜宲roRule11涓ゆ潯绾︽潫锛堝拰isSelected(S)銆乹ty(Q)鐩稿叧锛?
            // proRule11-cRule: sd1.S + sd2.S <=1
            AlgCPLinearExpr sdTypeSumNum = model().sum4Selected("Type=sd").name("sdTypeSumNum");
            model().addLessOrEqual(sdTypeSumNum, 1);

            // proRule12-cRule: sd1.Q + sd2.Q <= 2
            AlgCPLinearExpr sdTypeSumQty = model().sum4Quantity("Type=sd").name("sdTypeSumQty");
            model().addLessOrEqual(sdTypeSumQty, 2);
        }

        // 鏀瑰姩鐐?锛氬垎绫?(澶氾級澶氫釜璇锋眰鐨勬暣浣撹姹傦紝闇€瑕佹湁鏁翠綋姹囨€?
        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "prefer high capacity drive", strategy = PriorityStrategy.MIN, effectScope = EffectScope.AllInst, attrParaCodes = "Capacity:SumSum,Quantity:SumSum")
        private void logicB2() {
            // 鏀瑰姩鐐?-1锛氬鍔爏um4Quantity鐨刾artCatagoryCodesStr鍙傛暟锛屾敮鎸佸瀹炰緥鐨勬儏鍐?
            PartAlgCPLinearExpr totalCapacity = model().sum4Quantity("Capacity",
                    "").name("totalCapacity");
            // 濡傛灉鏄閲忛渶姹?
            // 鏀瑰姩鐐?锛氭€庝箞鍒ゆ柇 driveSumCapacity鏄惁鏈夎緭鍏ワ紵鍓嶇疆璁＄畻+ 杈撳叆鏉′欢鍒ゆ柇锛?--
            // if (driveSumCapacity.hasInput()) {
            if (drive.sumSumPara("Capacity").hasInput()) {
                int requiredCapacity = drive.sumSumPara("Capacity").inputValue();

                // a1.婊¤冻杈撳叆瀹归噺闇€姹?totalCapacity >= requiredCapacity
                model().addGreaterOrEqual(totalCapacity, requiredCapacity);

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");

                // a2.浣跨敤楂樺閲忕‖鐩?-> "琚€夋嫨閮ㄤ欢鍗曞閲忔€诲拰瓒婂ぇ瓒婂ソ"
                PartAlgCPLinearExpr highCapacityExpr = model().sum4Selected("Capacity", "")
                        .name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -100);

                // a3.鍦ㄦ弧瓒冲閲忛渶姹傜殑鍓嶆彁涓嬶紝瀹归噺瓒婃帴杩戦渶姹傚閲忚秺濂?
                PartAlgCPLinearExpr excessCapacityExpr = model().newPartLinearExpr("excessCapacityExpr");
                excessCapacityExpr.addExpr(totalCapacity, 1);
                excessCapacityExpr.addConstant(-requiredCapacity);
                objectiveExpr.addExpr(excessCapacityExpr, 1);

                // a4.鍦ㄦ弧瓒冲閲忛渶姹傜殑鍓嶆彁涓嬶紝 閰嶇疆鐨勯儴浠舵暟閲忚秺灏戣秺濂?
                PartAlgCPLinearExpr excessQuantityExpr = model().sum4Quantity("", "")
                        .name("excessQuantityExpr");
                objectiveExpr.addExpr(excessQuantityExpr, 800);
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);

            } else {// 缁欑殑鏁伴噺qty鎬绘暟

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                // int requiredQuantity = driveSumQuantity.inputValue();
                int requiredQuantity = drive.sumSumPara("Quantity").inputValue();
                // a1.婊¤冻杈撳叆鎬绘暟閲忛渶姹?totalQuantity >= requiredQuantity
                PartAlgCPLinearExpr totalQuantity = model().sum4Quantity("",
                        "").name("totalQuantity");
                model().addGreaterOrEqual(totalQuantity, requiredQuantity);

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFunQty");

                // a2.浣跨敤楂樺閲忕‖鐩?-> "琚€夋嫨閮ㄤ欢鍗曞閲忔€诲拰瓒婂ぇ瓒婂ソ"
                PartAlgCPLinearExpr highCapacityExpr = model().sum4Selected("Capacity", "")
                        .name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -1);

                // a3.鍦ㄦ弧瓒虫暟閲忛渶姹傜殑鍓嶆彁涓嬶紝鏁伴噺瓒婃帴杩戦渶姹傛暟閲忚秺濂?
                PartAlgCPLinearExpr excessQuantityExpr = model().sum4Quantity("", "")
                        .name("excessQuantityExpr");
                excessQuantityExpr.addConstant(-requiredQuantity);
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);
            }
        }

    }
    // ---------------妯″瀷鐨勫畾涔塭nd----------------------------------------

    public DynMultReq4MultiReqTest() {
        super(DynMultReqMultiInstConstraint.class);
    }

    @Test
    public void oneReq() {
        // Natural-Input: 瑕佹眰鏈烘纭洏瀹归噺>=5T, 瑕佹眰4鏍哥殑CPU鐨勬€诲唴瀛?=512G
        inferRecommendModule("drive:Sum_Capacity >=5 where Speed=5400", "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        // 鍙樺寲鐐?-1锛氳姹備负淇濇寔杈撳叆鐨勭畝娲佽锛屽鏋滀粎杈撳嚭涓€涓疄渚嬶紝鍒欏拰鍘熸潵澶氬崟瀹炰緥涓€鏍凤紝涓嶅姞瀹炰緥鍚?
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
        // assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
    }

    @Test
    public void twoReq() {
        // Natural-Input: 瑕佹眰鏈烘纭洏瀹归噺>=5T, 瑕佹眰鏈烘纭洏瀹归噺>=5T, 瑕佹眰4鏍哥殑CPU鐨勬€诲唴瀛?=512G
        inferRecommendModule("drive:Sum_Capacity >=5 where Speed=5400", "drive:Sum_Capacity >=5 where Speed=5400",
                "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        // 鍙樺寲鐐?-2锛氳姹備负淇濇寔杈撳叆鐨勭畝娲佽锛屽鏋滆緭鍑哄涓疄渚嬶紝鍚庨潰鐨勯渶瑕佸姞涓婂疄鍚嶅悕绉癐1
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*),I1_md1(Q:5,H:0,S:1),I1_sd1(0*)");
        // assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*),I1_md1(Q:5,H:0,S:1),I1_sd1(0*)");
    }

    @Test
    public void sumsumReq() {
        // 娴嬭瘯鎬籹um 鍜?sumsum涔嬮棿鐨勫叧绯伙紵

        // Natural-Input: 瑕佹眰閰嶇疆涓ょ粍Speed=5400锛?鍜屼竴缁凷peed=7200锛屾€诲閲?=10T, 瑕佹眰4鏍哥殑CPU鐨勬€诲唴瀛?=512G
        inferRecommendModule("drive: SumSum_Capacity>=10", "drive: where Speed=5400", "drive: where Speed=7200",
                "cpu:Sum_Memory >=512 where CoreNum=4");
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:2,H:0,S:1),sd1(0*),I1_md2(Q:4,H:0,S:1),I1_sd2(0*)");
        printSimpleSolutions();
    }
}
