package com.jmix.configengine.scenario.hello;

import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.tool.model.ModuleAnno;
import com.jmix.tool.model.ParaAnno;
import com.jmix.tool.model.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

/**
 * Hello模块约束算法
 */
@Slf4j
@ModuleAnno(id = 123L)
public class HelloConstraint extends ConstraintAlgImpl {

    @ParaAnno(defaultValue = "Red", options = { "Red:1", "Black:2", "White:3" })
    private ParaVar colorVar;

    @ParaAnno(defaultValue = "Small", options = { "Small", "Medium", "Big" })
    private ParaVar sizeVar;

    @PartAnno(price = 100L, attrs = { "weight:180g", "size:M" }, extAttrs = { "material:cotton", "brand:Hello" })
    private PartVar tShirt11Var;

    @Override
    protected void initConstraint() {
        addConstraint_rule2();
    }

    /**
     * 条件数量规则：基于颜色和尺寸的部件数量规则
     * 规则内容：如果颜色选择红色且尺寸选择小号，则TShirt11数量为1；否则为3
     */
    public void addConstraint_rule2() {

        // "Red-10", "Black-20", "White-30"
        // "Small-10", "Medium-20", "Big-30"
        // if(colorVar.value ==Red && sizeVar.value == Small ) {
        // TShirt11Var.qty = 1;
        // }
        // else {
        // TShirt11Var.qty = 3
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
        model.addEquality((IntVar) this.tShirt11Var.qty, 1).onlyEnforceIf(redAndSmall);

        // 如果redAndSmall为false，则TShirt11数量为3
        model.addEquality((IntVar) this.tShirt11Var.qty, 3).onlyEnforceIf(redAndSmall.not());
    }
}