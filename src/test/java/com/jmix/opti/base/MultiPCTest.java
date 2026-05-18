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
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

@Slf4j
public class MultiPCTest extends ModuleScenarioTestBase {

    // ---------------妯″瀷鐨勫畾涔塻tart----------------------------------------
    @ModuleAnno(id = 123L)
    static public class MultiPCConstraint extends ModuleAlgBase {

        // 纭洏閮ㄤ欢鍒嗙被瀹氫箟--涓ユ牸鎸夊眰绾х粨鏋勫畾涔夛紙椤哄簭寰堥噸瑕侊級锛岄儴浠剁殑attrs涔熻鎸夊畾涔夌殑椤哄簭鏉?
        @PartAnno()
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

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Capacity;// 杈撳叆鍙傛暟

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Quantity; // 杈撳叆鍙傛暟

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

        @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar cpuSumCores; // 杈撳叆鍙傛暟 TODO:涓嶉渶瑕佸缓绔嬶紝搴旇浠庡紩鎿庡共鎺?

        @ParaAnno(fatherCode = "cpu", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Memory; // 杈撳叆鍙傛暟

        @CodeRuleAnno(normalNaturalCode = "cpu4 incompatible with ssd")
        private void logicAB1() {
            inCompatible("logicAB1", "cpu:CoreNum=4", "drive:Type=sd");
        }

        // TODO锛氳繘涓€姝ユ娊鍙栧崟閫夊瀷CPU
        @CodeRuleAnno(fatherCode = "cpu", normalNaturalCode = "浠呰兘浣跨敤涓€绉岰PU")
        private void logicA1() {
            AlgCPLinearExpr cpuSelected = model().sum4Selected("").name("cpuSelected");
            model().addLessOrEqual(cpuSelected, 1);
        }

        @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "ssd same type max 2")
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

        @PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "prefer high capacity drive", strategy = PriorityStrategy.MIN)
        private void logicB2() {

            PartAlgCPLinearExpr totalCapacity = model().sum4Quantity("Capacity", "").name("totalCapacity");
            // 濡傛灉鏄閲忛渶姹?
            if (Sum_Capacity.hasInput()) {
                int requiredCapacity = Sum_Capacity.inputValue();

                // a1.婊¤冻杈撳叆瀹归噺闇€姹?totalCapacity >= requiredCapacity
                model().addGreaterOrEqual(totalCapacity, requiredCapacity);

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");

                // a2.浣跨敤楂樺閲忕‖鐩?-> "琚€夋嫨閮ㄤ欢鍗曞閲忔€诲拰瓒婂ぇ瓒婂ソ"
                PartAlgCPLinearExpr highCapacityExpr = model().sum4Selected("Capacity", "").name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -100);

                // a3.鍦ㄦ弧瓒冲閲忛渶姹傜殑鍓嶆彁涓嬶紝瀹归噺瓒婃帴杩戦渶姹傚閲忚秺濂?
                PartAlgCPLinearExpr excessCapacityExpr = model().newPartLinearExpr("excessCapacityExpr");
                excessCapacityExpr.addExpr(totalCapacity, 1);
                excessCapacityExpr.addConstant(-requiredCapacity);
                objectiveExpr.addExpr(excessCapacityExpr, 1);

                // a4.鍦ㄦ弧瓒冲閲忛渶姹傜殑鍓嶆彁涓嬶紝 閰嶇疆鐨勯儴浠舵暟閲忚秺灏戣秺濂?
                PartAlgCPLinearExpr excessQuantityExpr = model().sum4Quantity("", "").name("excessQuantityExpr");
                objectiveExpr.addExpr(excessQuantityExpr, 800);
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);

            } else {// 缁欑殑鏁伴噺qty鎬绘暟

                // int requiredQty = Integer.parseInt(req.getAttrValue());
                int requiredQuantity = Sum_Quantity.inputValue();
                // a1.婊¤冻杈撳叆鎬绘暟閲忛渶姹?totalQuantity >= requiredQuantity
                PartAlgCPLinearExpr totalQuantity = model().sum4Quantity("", "").name("totalQuantity");
                model().addGreaterOrEqual(totalQuantity, requiredQuantity);

                // 鍒涘缓鐩爣鍑芥暟
                PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFunQty");

                // a2.浣跨敤楂樺閲忕‖鐩?-> "琚€夋嫨閮ㄤ欢鍗曞閲忔€诲拰瓒婂ぇ瓒婂ソ"
                PartAlgCPLinearExpr highCapacityExpr = model().sum4Selected("Capacity", "").name("highCapacityExpr");
                objectiveExpr.addExpr(highCapacityExpr, -1);

                // a3.鍦ㄦ弧瓒虫暟閲忛渶姹傜殑鍓嶆彁涓嬶紝鏁伴噺瓒婃帴杩戦渶姹傛暟閲忚秺濂?
                PartAlgCPLinearExpr excessQuantityExpr = model().sum4Quantity("", "").name("excessQuantityExpr");
                excessQuantityExpr.addConstant(-requiredQuantity);
                model().setObjectExpr(objectiveExpr);
                updatePriorityObjectFuntion("logicB2", objectiveExpr);
            }
        }

    }
    // ---------------妯″瀷鐨勫畾涔塭nd----------------------------------------

    public MultiPCTest() {
        super(MultiPCConstraint.class);
    }

    // 鐢ㄤ緥缁村害璁捐
    // 缁村害1-DA: 鍒嗙被杈撳叆(Input鍙傛暟)
    // DA1-绗竴涓湁瑕佹眰锛堝墠 锛?
    // DA2-绗簩涓湁瑕佹眰锛堝悗锛?
    // DA3 涓や釜閮芥湁瑕佹眰
    // 缁村害2-DB锛氳鍒欙紝瑙勫垯绫诲瀷锛屽眰绾э紝澶氫釜浼樺厛绾ц鍒?
    // DB1-鍒嗙被-闈炰紭鍏圠ogicB2
    // DB2-鍒嗙被-浼樺厛LogicA1,LogicA2
    // DB3-鍒嗙被闂?LogicAB锛屽乏鍙虫槸鍚︾┖锛孡eft,right
    @Test
    public void mixInCompatibleLeftYRightY() {
        // Natural-Input: 瑕佹眰5400閫熺巼鐨勭‖鐩樺閲?=5T, 瑕佹眰4鏍哥殑CPU鐨勬€诲唴瀛?=512G
        // DSL-Input: drive:Sum_Capacity >=5 where Speed=5400, cpu:Sum_Memory >=512
        // where CoreNum=4
        // Struct-Input: req1F,req1C(sd1.Q*3 + md1*1 >=5), req2F,req2C(cpu2.Q*256 >=512)
        // 娴嬭瘯鐐癸細
        // 缁村害1-DA:DA3
        // 缁村害2-DB:DB3(Left-Y,Right-Y)

        // 棰勬湡缁撴灉锛?
        // 瑙?锛?cpu2.Q=2 md1.Q=5
        // --杩囨护锛屾墽琛宺eq1F,req2F: -->drive:sd1,md1, cpu:cpu2
        // --浜у搧瑙勫垯瀹炰緥鍖?
        // ----LogicA1:cpu2.S<=1
        // ----LogicB1:sd1.S +md1.S <=1,sd1.Q<=2 --鐢熸晥
        // ----LogicB2: (cpu2.S) InCompatible (sd1.S) (Left-Y,Right-Y)
        // ----鐩爣鍑芥暟锛?xxx
        // --鍏抽敭鎵ц杩囩▼锛?
        // ----1. "4鏍哥殑CPU鐨勬€诲唴瀛?=512G" -->cpu2.Q =2
        // ---LogicAB1(鎺掗櫎sd1) + "5400閫熺巼"(sd1,md1),鍙湁md1锛屾弧瓒宠姹?
        // --鏍规嵁req1C,鐨刴d1.Q = 5
        inferRecommendModule("drive:Sum_Capacity >=5 where Speed=5400", "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        assertSoluContain(1, "cpu2(Q:2,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
        // assertSoluContain("cpu2(Q:20,H:0,S:1),md1(Q:5,H:0,S:1),sd1(0*)");
    }

    @Test
    public void mixInCompatibleLeftYRightN() {
        // Natural-Input: 瑕佹眰鏈烘纭洏瀹归噺>=5T, 瑕佹眰4鏍哥殑CPU鐨勬€诲唴瀛?=512G

        inferRecommendModule("drive:Sum_Capacity >=5 where Type=md", "cpu:Sum_Memory >=512 where CoreNum=4");
        printSimpleSolutions();
        assertSoluContain(1, "cpu2(Q:20,H:0,S:1),md1(0*),md2(Q:1,H:0,S:1),md3(Q:1,H:0,S:1)");
        // TODO: 鎬庝箞璁ヽpu1浠?寮€濮?
    }
}
