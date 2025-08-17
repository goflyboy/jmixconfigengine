package com.jmix.configengine;

import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar;
import com.jmix.configengine.artifact.ParaOptionVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;
import java.util.HashMap;
import java.util.Map;

/**
 * T恤衫约束算法实现
 * 继承自ConstraintAlgImpl，实现T恤衫的约束逻辑
 */
public class TShirtConstraint extends ConstraintAlgImpl {
    
    // 参数变量
    public ParaVar ColorVar;
    public ParaVar SizeVar;
    
    // 部件变量
    public PartVar thsirt11Var;
    public PartVar thsirt12Var;
    
    // CP模型
    private CpModel model;
    
    // 不再需要单独的映射，直接使用ParaVar.optionBoolVars
    
    public TShirtConstraint() {
        this.ColorVar = new ParaVar();
        this.SizeVar = new ParaVar();
        this.thsirt11Var = new PartVar();
        this.thsirt12Var = new PartVar();
    }
    public void initModel(CpModel model) {
        this.model = model;
        this.initVariables();
        this.initConstraint();
    }
    
    @Override
    public void initVariables() {
        // 1. 定义属性变量
        this.ColorVar.code = "Color"; 
        this.ColorVar.var = newIntVarFromDomain(model, new long[] {10, 20, 30}, "Color");
        
        // 创建颜色选项
        ParaOptionVar colorOption10 = new ParaOptionVar("Red", 10, model.newBoolVar("Color_" + 10));
        ParaOptionVar colorOption20 = new ParaOptionVar("Black", 20, model.newBoolVar("Color_" + 20));
        ParaOptionVar colorOption30 = new ParaOptionVar("White", 30, model.newBoolVar("Color_" + 30));
        
        this.ColorVar.optionSelectVars.put(10, colorOption10);
        this.ColorVar.optionSelectVars.put(20, colorOption20);
        this.ColorVar.optionSelectVars.put(30, colorOption30);
        
        // 建立属性变量与选项变量之间的关系
        this.ColorVar.optionSelectVars.forEach((optionId, optionVar) -> {
            model.addEquality((IntVar) this.ColorVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
            model.addDifferent((IntVar) this.ColorVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
        });

        this.SizeVar.code = "Size";
        this.SizeVar.var = newIntVarFromDomain(model, new long[] {1, 2, 3}, "Size");
        
        // 创建尺寸选项
        ParaOptionVar sizeOption1 = new ParaOptionVar("Big", 1, model.newBoolVar("Size_" + 1));
        ParaOptionVar sizeOption2 = new ParaOptionVar("Medium", 2, model.newBoolVar("Size_" + 2));
        ParaOptionVar sizeOption3 = new ParaOptionVar("Small", 3, model.newBoolVar("Size_" + 3));
        
        this.SizeVar.optionSelectVars.put(1, sizeOption1);
        this.SizeVar.optionSelectVars.put(2, sizeOption2);
        this.SizeVar.optionSelectVars.put(3, sizeOption3);
        
        // 建立属性变量与选项变量之间的关系
        this.SizeVar.optionSelectVars.forEach((optionId, optionVar) -> {
            model.addEquality((IntVar) this.SizeVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
            model.addDifferent((IntVar) this.SizeVar.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
        });

        this.thsirt11Var.code = "thsirt11";
        this.thsirt11Var.var = model.newIntVar(0, 1000, "thsirt11"); // 范围值TODO

        this.thsirt12Var.code = "thsirt12";
        this.thsirt12Var.var = model.newIntVar(0, 1000, "thsirt12");
    }
    
    @Override
    public void initConstraint() {
        addConstrain_rule1(model, this.ColorVar, this.SizeVar);
        addConstrain_rule2(model, this.thsirt11Var, this.thsirt12Var);
    }

    /**
     * 规则1：颜色和尺寸的约束关系
     * 规则名称：(Color !="Red") Codependent (Size !="Medium")
     */
    public void addConstrain_rule1(CpModel model, ParaVar ColorVar, ParaVar SizeVar) {
        // 确保只有一个颜色选项被选中
        model.addExactlyOne(this.ColorVar.optionSelectVars.values().stream()
            .map(option -> option.getIsSelectedVar())
            .toArray(BoolVar[]::new));
        // 确保只有一个尺寸选项被选中
        model.addExactlyOne(this.SizeVar.optionSelectVars.values().stream()
            .map(option -> option.getIsSelectedVar())
            .toArray(BoolVar[]::new));

        // 4. 定义筛选后的集合 Set1=(A1,A2,A3) =>筛出的结果A1，A2 (filterCodeIds)
        // 左表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
        BoolVar colorNotRed = model.newBoolVar("ColorNotRed");
        model.addBoolOr(new Literal[]{
            this.ColorVar.optionSelectVars.get(10).getIsSelectedVar(),
            this.ColorVar.optionSelectVars.get(20).getIsSelectedVar()
        }).onlyEnforceIf(colorNotRed);
        model.addBoolAnd(new Literal[]{this.ColorVar.optionSelectVars.get(10).getIsSelectedVar().not(),
            this.ColorVar.optionSelectVars.get(20).getIsSelectedVar().not()
        }).onlyEnforceIf(colorNotRed.not());
        
        // set2: Size非中号 (Big=1, Small=3)
        BoolVar sizeNotMedium = model.newBoolVar("SizeNotMedium");
        model.addBoolOr(new Literal[]{
            this.SizeVar.optionSelectVars.get(1).getIsSelectedVar(),
            this.SizeVar.optionSelectVars.get(3).getIsSelectedVar()
        }).onlyEnforceIf(sizeNotMedium);
        model.addBoolAnd(new Literal[]{this.SizeVar.optionSelectVars.get(1).getIsSelectedVar().not(),
            this.SizeVar.optionSelectVars.get(2).getIsSelectedVar().not()
        }).onlyEnforceIf(sizeNotMedium.not());
        
        // 5. 实现Codependent关系
        model.addEquality(colorNotRed, sizeNotMedium);
    }

    /**
     * 规则2：T恤衫数量关系约束
     * 规则名称：TShirt12 = TShirt11*2
     */
    public void addConstrain_rule2(CpModel model, PartVar thsirt11Var, PartVar thsirt12Var) {
        
        // 使用简单的约束：thsirt12 = thsirt11 * 2
        IntVar thsirt11 = (IntVar) thsirt11Var.var;
        IntVar thsirt12 = (IntVar) thsirt12Var.var;
        // OR-Tools doesn't support direct multiplication, so we use addition: thsirt12 = thsirt11 + thsirt11
        // thsirt12 = LinearExpr.weightedSum(
        //     new LinearArgument[] {thsirt11, LinearExpr.constant(2)}, new long[] {1, 2});
        //TODO
    }
    
    /**
     * 设置CP模型
     */
    public void setModel(CpModel model) {
        this.model = model;
    }
} 