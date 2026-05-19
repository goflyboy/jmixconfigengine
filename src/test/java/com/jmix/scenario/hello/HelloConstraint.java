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
 * Hello模块约束算法
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
     * 条件数量规则：基于颜色和尺寸的部件数量规则
     * 规则内容：如果颜色选择红色且尺寸选择小号，则TShirt11数量为1；否则为3
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

        // 创建条件变量：颜色是红色且尺寸是小号
        AlgCPBoolVar redAndSmall = model().newBoolVar("rule2_redAndSmall");

        // 实现条件逻辑：redAndSmall = (Color == Red) AND (Size == Small)
        model().addBoolAnd(new AlgCPLiteral[] {
                this.colorVar.option("Red").selectedVar(),
                this.sizeVar.option("Small").selectedVar()
        }).onlyEnforceIf(redAndSmall);

        // 如果不是红色且小号的组合，则redAndSmall为false
        model().addBoolOr(new AlgCPLiteral[] {
                this.colorVar.option("Red").selectedVar().not(),
                this.sizeVar.option("Small").selectedVar().not()
        }).onlyEnforceIf(redAndSmall.not());

        // 根据条件设置TShirt11的数量
        // 如果redAndSmall为true，则TShirt11数量为1
        model().addEquality(this.tShirt11Var.quantityVar(), 1).onlyEnforceIf(redAndSmall);

        // 如果redAndSmall为false，则TShirt11数量为3
        model().addEquality(this.tShirt11Var.quantityVar(), 3).onlyEnforceIf(redAndSmall.not());
    }
}
