package com.jmix.configengine.scenario.tshirt;

import com.jmix.configengine.artifact.*;
import com.google.ortools.sat.*;
import java.util.*;

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
            this.ColorVar = createParaVar("Color");
            //参数Size创建
            this.SizeVar = createParaVar("Size");
        
        //部件创建
            //部件TShirt11创建
            this.TShirt11Var = createPartVar("TShirt11");
            //部件TShirt12创建
            this.TShirt12Var = createPartVar("TShirt12");
    }

    @Override
    public void initConstraint() {
        // 这里应该初始化约束，实际实现中需要CPModel
        addConstraint_rule1();
        addConstraint_rule2();
        addConstraint_rule3();
    }
    
        //===================================调试信息===================================
        // 规则: rule1 - 颜色和尺寸兼容关系规则
        // isCompatibleRule(): true
        // rule.left??: true
        // rule.right??: true
        // 条件判断结果: true
        //===================================调试信息===================================
        
        //===================================类型1===================================
    /**
     * 兼容性规则：颜色和尺寸兼容关系规则
     * 规则内容：如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号
     */
    public void addConstraint_rule1() {
        addCompatibleConstraint("rule1", this.ColorVar,
        Arrays.asList(
                    "Big",
                    "Small"
        ), this.SizeVar, Arrays.asList(
                    "Red"
        ));
    }
    /**
     * 兼容性规则：颜色和尺寸兼容关系规则
     * 规则内容：如果颜色选择红色，则尺寸必须选择大号或小号，不能选择中号
     */
    public void addConstraint_rule1_comment() { 
            // left:确保只有一个颜色选项被选中,part要考虑TODO
            model.addExactlyOne(this.ColorVar.optionSelectVars.values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));
            // right:确保只有一个颜色选项被选中,part要考虑TODO
            model.addExactlyOne(this.SizeVar.optionSelectVars.values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));

            // 4. 定义筛选后的集合 Set1=(A1,A2,A3) =>筛出的结果A1，A2 (filterCodeIds)
            // 左表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
            BoolVar leftCond = model.newBoolVar("rule1" + "_" + "leftCond");
            model.addBoolOr(new Literal[]{
                this.ColorVar.getParaOptionByCode("Red").getIsSelectedVar()
            }).onlyEnforceIf(leftCond);
            model.addBoolAnd(new Literal[]{
                this.ColorVar.getParaOptionByCode("Red").getIsSelectedVar().not()
            }).onlyEnforceIf(leftCond.not());
            
            // 右表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
            BoolVar rightCond = model.newBoolVar("rule1" + "_" + "rightCond");
            model.addBoolOr(new Literal[]{
                this.SizeVar.getParaOptionByCode("Big").getIsSelectedVar(),
                this.SizeVar.getParaOptionByCode("Small").getIsSelectedVar()
            }).onlyEnforceIf(rightCond);
            model.addBoolAnd(new Literal[]{
                this.SizeVar.getParaOptionByCode("Big").getIsSelectedVar().not(),
                this.SizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
            }).onlyEnforceIf(rightCond.not());
            
            // 5. 实现Codependent关系
            model.addEquality(leftCond, rightCond);
    }


        //===================================类型2===================================
        //===================================调试信息===================================
        // 规则: rule2 - 部件数量关系规则
        // isCompatibleRule(): false
        // rule.left??: false
        // rule.right??: false
        // 条件判断结果: false
        //===================================调试信息===================================
        
    /**
     * 条件数量规则：基于颜色和尺寸的部件数量规则
     * 规则内容：如果颜色选择红色且尺寸选择小号，则TShirt11数量为1；否则为3
     */
    public void addConstraint_rule2() {
        // 创建条件变量：颜色是红色且尺寸是小号
        BoolVar redAndSmall = model.newBoolVar("rule2_redAndSmall");
        
        // 实现条件逻辑：redAndSmall = (Color == Red) AND (Size == Small)
        model.addBoolAnd(new Literal[]{
            this.ColorVar.getParaOptionByCode("Red").getIsSelectedVar(),
            this.SizeVar.getParaOptionByCode("Small").getIsSelectedVar()
        }).onlyEnforceIf(redAndSmall);
        
        // 如果不是红色且小号的组合，则redAndSmall为false
        model.addBoolOr(new Literal[]{
            this.ColorVar.getParaOptionByCode("Red").getIsSelectedVar().not(),
            this.SizeVar.getParaOptionByCode("Small").getIsSelectedVar().not()
        }).onlyEnforceIf(redAndSmall.not());
        
        // 根据条件设置TShirt11的数量
        // 如果redAndSmall为true，则TShirt11数量为1
        model.addEquality((IntVar)this.TShirt11Var.var, 1).onlyEnforceIf(redAndSmall);
        
        // 如果redAndSmall为false，则TShirt11数量为3
        model.addEquality((IntVar)this.TShirt11Var.var, 3).onlyEnforceIf(redAndSmall.not());
    }
           
       //===================================类型1===================================
        //===================================调试信息===================================
        // 规则: rule3 - 颜色选择规则
        // isCompatibleRule(): false
        // rule.left??: false
        // rule.right??: false
        // 条件判断结果: false
        //===================================调试信息===================================
        
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