package com.jmix.configengine.scenario.hello;
 
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.artifact.ParaVar;
import com.jmix.configengine.artifact.PartVar; 
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno; 
import lombok.extern.slf4j.Slf4j;

/**
 * Hello模块约束算法
 */
@Slf4j
@ModuleAnno(id = 123L)
public class HelloConstraint extends ConstraintAlgImpl {
    
    @ParaAnno(
        defaultValue = "Red",
        options = {"Red", "Black", "White"}
    )
    private ParaVar ColorVar;

    @ParaAnno(
        defaultValue = "Small",
        options = {"Small", "Medium", "Big"}
    )
    private ParaVar SizeVar;
    @PartAnno(
        price = 100L,
        attrs = {"weight:180g", "size:M"},
        extAttrs = {"material:cotton", "brand:Hello"}
    )
    private PartVar TShirt11Var;
 
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
        // if(ColorVar.var ==Red && SizeVar.var == Small  ) {
        //     TShirt11Var.var = 1;
        // }
        // else {
        //     TShirt11Var.var = 3
        // }

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
} 