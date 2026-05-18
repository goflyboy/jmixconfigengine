package com.jmix.scenario.hello;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

/**
 * Hello妯″潡绾︽潫绠楁硶
 * 
 * @since 2025-09-23
 */
@ModuleAnno(id = 123L)
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "hello-2026.05")
public class HelloConstraint extends ModuleAlgBase {

    @ParaAnno(defaultValue = "Red", options = { "Red:1", "Black:2", "White:3" })
    private ParaVar colorVar;

    @ParaAnno(defaultValue = "Small", options = { "Small", "Medium", "Big" })
    private ParaVar sizeVar;

    @PartAnno(price = 100L)
    private PartVar tShirt11Var;

    /**
     * 鏉′欢鏁伴噺瑙勫垯锛氬熀浜庨鑹插拰灏哄鐨勯儴浠舵暟閲忚鍒?
     * 瑙勫垯鍐呭锛氬鏋滈鑹查€夋嫨绾㈣壊涓斿昂瀵搁€夋嫨灏忓彿锛屽垯TShirt11鏁伴噺涓?锛涘惁鍒欎负3
     */
    @CodeRuleAnno()
    public void addConstraintRule2() {

        // "Red-10", "Black-20", "White-30"
        // "Small-10", "Medium-20", "Big-30"
        // if(colorVar.valueVar() ==Red && sizeVar.valueVar() == Small ) {
        // tShirt11Var.quantityVar() = 1;
        // }
        // else {
        // tShirt11Var.quantityVar() = 3
        // }

        // 鍒涘缓鏉′欢鍙橀噺锛氶鑹叉槸绾㈣壊涓斿昂瀵告槸灏忓彿
        AlgCPBoolVar redAndSmall = model().newBoolVar("rule2_redAndSmall");

        // 瀹炵幇鏉′欢閫昏緫锛歳edAndSmall = (Color == Red) AND (Size == Small)
        model().addBoolAnd(new AlgCPLiteral[] {
                this.colorVar.option("Red").selectedVar(),
                this.sizeVar.option("Small").selectedVar()
        }).onlyEnforceIf(redAndSmall);

        // 濡傛灉涓嶆槸绾㈣壊涓斿皬鍙风殑缁勫悎锛屽垯redAndSmall涓篺alse
        model().addBoolOr(new AlgCPLiteral[] {
                this.colorVar.option("Red").selectedVar().not(),
                this.sizeVar.option("Small").selectedVar().not()
        }).onlyEnforceIf(redAndSmall.not());

        // 鏍规嵁鏉′欢璁剧疆TShirt11鐨勬暟閲?
        // 濡傛灉redAndSmall涓簍rue锛屽垯TShirt11鏁伴噺涓?
        model().addEquality(this.tShirt11Var.quantityVar(), 1).onlyEnforceIf(redAndSmall);

        // 濡傛灉redAndSmall涓篺alse锛屽垯TShirt11鏁伴噺涓?
        model().addEquality(this.tShirt11Var.quantityVar(), 3).onlyEnforceIf(redAndSmall.not());
    }
}
