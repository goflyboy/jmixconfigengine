package com.jmix.configengine.scenario.tshirt;

import com.jmix.configengine.artifact.*;
import com.google.ortools.sat.*;

/**
 * 自动生成的约束类：TShirtConstraint
 */
public class TShirtConstraint extends ConstraintAlgImpl {
    
    private ParaVar ColorVar;
    private ParaVar SizeVar;
    
    private PartVar TShirt11Var;
    private PartVar TShirt12Var;

    @Override
    public void initVariables() {
        //参数创建
            //参数Color创建
            this.ColorVar = new ParaVar();
            this.ColorVar.code = "Color";
            this.ColorVar.var = newIntVarFromDomain(model, new long[] {10,20,30}, "Color");
            
            //为参数可选值创建“是否选择的var"
            this.ColorVar.optionSelectVars.put(10, new ParaOptionVar("Red", 10, model.newBoolVar("Color"+"_" + 10)));
            this.ColorVar.optionSelectVars.put(20, new ParaOptionVar("Black", 20, model.newBoolVar("Color"+"_" + 20)));
            this.ColorVar.optionSelectVars.put(30, new ParaOptionVar("White", 30, model.newBoolVar("Color"+"_" + 30)));
        // 建立属性变量与选项变量之间的关系
        this.ColorVar.optionSelectVars.forEach((optionId, optionVar) -> {
            model.addEquality((IntVar) this.ColorVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
            model.addDifferent((IntVar) this.ColorVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
        });
            //参数Size创建
            this.SizeVar = new ParaVar();
            this.SizeVar.code = "Size";
            this.SizeVar.var = newIntVarFromDomain(model, new long[] {1,2,3}, "Size");
            
            //为参数可选值创建“是否选择的var"
            this.SizeVar.optionSelectVars.put(1, new ParaOptionVar("Big", 1, model.newBoolVar("Size"+"_" + 1)));
            this.SizeVar.optionSelectVars.put(2, new ParaOptionVar("Medium", 2, model.newBoolVar("Size"+"_" + 2)));
            this.SizeVar.optionSelectVars.put(3, new ParaOptionVar("Small", 3, model.newBoolVar("Size"+"_" + 3)));
        // 建立属性变量与选项变量之间的关系
        this.SizeVar.optionSelectVars.forEach((optionId, optionVar) -> {
            model.addEquality((IntVar) this.SizeVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
            model.addDifferent((IntVar) this.SizeVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
        });
        
        //部件创建
            //部件TShirt11创建
            this.TShirt11Var = new PartVar();
            this.TShirt11Var.code = "TShirt11";
            this.TShirt11Var.var = model.newIntVar(0, 1000, "TShirt11");
            //部件TShirt12创建
            this.TShirt12Var = new PartVar();
            this.TShirt12Var.code = "TShirt12";
            this.TShirt12Var.var = model.newIntVar(0, 1000, "TShirt12");
    }

    @Override
    public void initConstraint() {
        // 这里应该初始化约束，实际实现中需要CPModel
        addConstraint_rule1();
        addConstraint_rule2();
        addConstraint_rule3();
    }
    
    /**
     * 未知规则类型：颜色和尺寸兼容关系规则
     * 规则内容：如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号
     */
    public void addConstraint_rule1() {
        System.out.println("未知规则类型: 颜色和尺寸兼容关系规则");
    }
    /**
     * 兼容性规则：部件数量关系规则
     * 规则内容：装饰部件TShirt12的数量必须等于主体部件TShirt11数量的2倍
     */
    public void addConstraint_rule2() {
        // TODO: 实现兼容性约束TODO 
        System.out.println("添加兼容性约束: 部件数量关系规则");
    }
           
       //===================================类型1===================================
    /**
     * 未知规则类型：颜色选择规则
     * 规则内容：颜色参数必须且只能选择一个选项
     */
    public void addConstraint_rule3() {
        System.out.println("未知规则类型: 颜色选择规则");
    }
    
    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        TShirtConstraint constraint = new TShirtConstraint();
        constraint.initVariables();
        constraint.initConstraint();
        
        constraint.addConstraint_rule1();
        constraint.addConstraint_rule2();
        constraint.addConstraint_rule3();
        
        System.out.println("约束规则执行完成");
    }
    @Override
    protected void initModelAfter(CpModel model) {
    }
}