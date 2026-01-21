package com.jmix.scenario.hello;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.executor.bmodel.anno.CodeRuleAnno;
import com.jmix.executor.bmodel.anno.ModuleAnno;
import com.jmix.executor.bmodel.anno.ParaAnno;
import com.jmix.executor.bmodel.anno.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

/**
 * Hello模块约束算法
 * 
 * @since 2025-09-23
 */
@ModuleAnno(id = 123L)
public class HelloConstraint extends ConstraintAlgImplTestBase {

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
        // if(colorVar.value ==Red && sizeVar.value == Small ) {
        // tShirt11Var.qty = 1;
        // }
        // else {
        // tShirt11Var.qty = 3
        // }

        // 创建条件变量：颜色是红色且尺寸是小号
        BoolVar redAndSmall = newBoolVar("rule2_redAndSmall");

        // 实现条件逻辑：redAndSmall = (Color == Red) AND (Size == Small)
        model.addBoolAnd(new Literal[] {
                this.colorVar.getParaOptionByCode("Red").getIsSelectedVar(),
                this.sizeVar.getParaOptionByCode("Small").getIsSelectedVar()
        }).onlyEnforceIf(redAndSmall);

        // 如果不是红色且小号的组合，则redAndSmall为false
        model.addBoolOr(new Literal[] {
                this.colorVar.getParaOptionByCode("Red").getIsSelectedVar().not(),
                this.sizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
        }).onlyEnforceIf(redAndSmall.not());

        // 根据条件设置TShirt11的数量
        // 如果redAndSmall为true，则TShirt11数量为1
        model.addEquality(this.tShirt11Var.qty, 1).onlyEnforceIf(redAndSmall);

        // 如果redAndSmall为false，则TShirt11数量为3
        model.addEquality(this.tShirt11Var.qty, 3).onlyEnforceIf(redAndSmall.not());
    }
}