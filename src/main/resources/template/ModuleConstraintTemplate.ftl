package ${module.packageName};

import com.jmix.configengine.artifact.*;
import com.google.ortools.sat.*;

/**
 * 自动生成的约束类：${module.code}Constraint
 */
public class ${module.code}Constraint extends ConstraintAlgImpl {
    
    <#--参数变量声明 -->
    <#list module.paras as para>
    private ParaVar ${para.varName};
    </#list>
    
    <#--Part变量声明 -->
    <#list module.parts as part>
    private PartVar ${part.varName};
    </#list>

    @Override
    public void initVariables() {
        //参数创建
        <#list module.paras as para>
            //参数${para.code}创建
            this.${para.varName} = new ParaVar();
            this.${para.varName}.code = "${para.code}";
            this.${para.varName}.var = newIntVarFromDomain(model, new long[] {${para.optionIdsStr}}, "${para.code}");
            
            //为参数可选值创建“是否选择的var"
            <#list para.options as option>
            this.${para.varName}.optionSelectVars.put(${option.codeId}, new ParaOptionVar("${option.code}", ${option.codeId}, model.newBoolVar("${para.code}"+"_" + ${option.codeId})));
            </#list>
        // 建立属性变量与选项变量之间的关系
        this.${para.varName}.optionSelectVars.forEach((optionId, optionVar) -> {
            model.addEquality((IntVar) this.${para.varName}.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar());
            model.addDifferent((IntVar) this.${para.varName}.var, optionId).onlyEnforceIf(optionVar.getIsSelectedVar().not());
        });
        </#list>
        
        //部件创建
        <#list module.parts as part>
            //部件${part.code}创建
            this.${part.varName} = new PartVar();
            this.${part.varName}.code = "${part.code}";
            this.${part.varName}.var = model.newIntVar(0, 1000, "${part.code}");
        </#list>
    }

    @Override
    public void initConstraint() {
        // 这里应该初始化约束，实际实现中需要CPModel
        <#list module.rules as rule>
        addConstraint_${rule.code}();
        </#list>
    }
    
    <#--生成约束定义 -->
    <#list module.rules as rule>
        //===================================调试信息===================================
        // 规则: ${rule.code} - ${rule.name}
        // isCompatibleRule(): ${rule.isCompatibleRule()?string}
        // rule.left??: ${(rule.left??)?string}
        // rule.right??: ${(rule.right??)?string}
        // 条件判断结果: ${(rule.isCompatibleRule() && rule.left?? && rule.right??)?string}
        //===================================调试信息===================================
        
        <#if rule.isCompatibleRule() && rule.left?? && rule.right??>
        //===================================类型1===================================
    /**
     * 兼容性规则：${rule.name}
     * 规则内容：${rule.normalNaturalCode}
     */
    public void addConstraint_${rule.code}() {
            // left:确保只有一个颜色选项被选中,part要考虑TODO
            model.addExactlyOne(this.${rule.left.varName}.optionSelectVars.values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));
            // right:确保只有一个颜色选项被选中,part要考虑TODO
            model.addExactlyOne(this.${rule.right.varName}.optionSelectVars.values().stream()
                .map(option -> option.getIsSelectedVar())
                .toArray(BoolVar[]::new));

            // 4. 定义筛选后的集合 Set1=(A1,A2,A3) =>筛出的结果A1，A2 (filterCodeIds)
            // 左表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
            BoolVar leftCond = model.newBoolVar("${rule.code}" + "_" + "leftCond");
            <#if rule.leftFilterCodes?? && rule.leftFilterCodes?size gt 0>
            model.addBoolOr(new Literal[]{
                <#list rule.leftFilterCodes as filterCode>
                this.${rule.left.varName}.getParaOptionByCode("${filterCode}").getIsSelectedVar()<#if filterCode_has_next>,</#if>
                </#list>
            }).onlyEnforceIf(leftCond);
            model.addBoolAnd(new Literal[]{
                <#list rule.leftFilterCodes as filterCode>
                this.${rule.left.varName}.getParaOptionByCode("${filterCode}").getIsSelectedVar().not()<#if filterCode_has_next>,</#if>
                </#list>
            }).onlyEnforceIf(leftCond.not());
            </#if>
            
            // 右表达式：对Color.options执行filter("Color !="Red")的结果为：(Black=10, White=20)
            BoolVar rightCond = model.newBoolVar("${rule.code}" + "_" + "rightCond");
            <#if rule.rightFilterCodes?? && rule.rightFilterCodes?size gt 0>
            model.addBoolOr(new Literal[]{
                <#list rule.rightFilterCodes as filterCode>
                this.${rule.right.varName}.getParaOptionByCode("${filterCode}").getIsSelectedVar()<#if filterCode_has_next>,</#if>
                </#list>
            }).onlyEnforceIf(rightCond);
            model.addBoolAnd(new Literal[]{
                <#list rule.rightFilterCodes as filterCode>
                this.${rule.right.varName}.getParaOptionByCode("${filterCode}").getIsSelectedVar().not()<#if filterCode_has_next>,</#if>
                </#list>
            }).onlyEnforceIf(rightCond.not());
            </#if>
            
            // 5. 实现Codependent关系
            <#if rule.leftFilterCodes?? && rule.leftFilterCodes?size gt 0 && rule.rightFilterCodes?? && rule.rightFilterCodes?size gt 0>
            model.addEquality(leftCond, rightCond);
            </#if>
    }


        //===================================类型2===================================
        <#elseif rule.isCalculateRule()>
    /**
     * 兼容性规则：${rule.name}
     * 规则内容：${rule.normalNaturalCode}
     */
    public void addConstraint_${rule.code}() {
        // TODO: 实现兼容性约束TODO 
        System.out.println("添加兼容性约束: ${rule.name}");
    }
           
       //===================================类型1===================================
        <#else>
    /**
     * 未知规则类型：${rule.name}
     * 规则内容：${rule.normalNaturalCode}
     */
    public void addConstraint_${rule.code}() {
        System.out.println("未知规则类型: ${rule.name}");
    }
        </#if>
    </#list>
    
    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
        ${module.code}Constraint constraint = new ${module.code}Constraint();
        constraint.initVariables();
        constraint.initConstraint();
        
        <#list module.rules as rule> 
        constraint.addConstraint_${rule.code}();
        </#list>
        
        System.out.println("约束规则执行完成");
    }
    @Override
    protected void initModelAfter(CpModel model) {
    }
}