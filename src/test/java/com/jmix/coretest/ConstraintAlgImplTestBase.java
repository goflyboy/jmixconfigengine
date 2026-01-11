package com.jmix.coretest;

import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVar;
import com.jmix.executor.impl.algmodel.Var;
import com.jmix.executor.omodel.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConstraintAlgImplTestBase extends ConstraintAlgImpl {

    /**
     * 为了简化测试用例中算法的表达，类似:p1.value + p2.value 表达，不是 p1.getValue() + p2.getValue()
     * 和@link com.jmix.executor.impl.algmodel.ParaVar/Part 类似，但不是继承，而是内部类
     */
    static public class ParaVar extends Var<Para> {

        /**
         * 参数值状态
         */
        public IntVar value;

        /**
         * 兼容旧模型的隐藏布尔变量
         * 0-显示，1-隐藏
         */
        public BoolVar isHidden;

        /**
         * 参数可选值的选中状态(CodeId -> ParaOptionVar)
         */
        public Map<Integer, ParaOptionVar> optionSelectVars = new HashMap<>();

        /**
         * 根据代码ID获取参数选项变量
         * 
         * @param codeId 选项的代码ID
         * @return 对应的参数选项变量，如果不存在则返回null
         */
        public ParaOptionVar getParaOptionByCodeId(Integer codeId) {
            return optionSelectVars.get(codeId);
        }

        /**
         * 根据代码获取参数选项变量
         * 
         * @param code 选项的代码
         * @return 对应的参数选项变量
         * @throws AlgLoaderException 如果找不到对应的选项
         */
        public ParaOptionVar getParaOptionByCode(String code) {
            for (ParaOptionVar option : optionSelectVars.values()) {
                if (code != null && code.equals(option.getCode())) {
                    return option;
                }
            }
            throw new AlgLoaderException("ParaOptionVar not found for code: " + code);
        }
    }

    public class PartVar extends Var<Part> {
        /**
         * 部件的数量值
         */
        public IntVar qty;

        /**
         * 显示隐藏属性
         */
        public BoolVar isHidden;

        /**
         * 子部件选中状态(Part.code -> BoolVar)
         */
        // public Map<String, BoolVar> subPartSelectedVars = new HashMap<>();

        /**
         * 是否选中
         */
        public BoolVar isSelected;
    }

    public class PartCategoryVar extends PartVar {
    }

    /**
     * 创建部件变量, 继承类可以重载
     *
     * @param internalPartVar 内部部件变量
     * @return 创建的部件变量
     */
    protected Var<?> newPartVar(com.jmix.executor.impl.algmodel.PartVar internalPartVar) {
        PartVar partVar = new PartVar();
        partVar.setBase(internalPartVar.getBase());
        partVar.qty = internalPartVar.getQty();
        partVar.isHidden = internalPartVar.getIsHidden();
        partVar.isSelected = internalPartVar.getIsSelected();

        // // 初始化子部件选中状态映射 TODO
        // for (com.jmix.executor.impl.algmodel.PartVar subPart :
        // internalPartVar.getSubParts()) {
        // partVar.subPartSelectedVars.put(subPart.getCode(),
        // newBoolVar(subPart.getCode() + "_selected"));
        // }
        return partVar;
    }

    /**
     * 创建参数变量, 继承类可以重载
     * 
     * @param internalParaVar 内部参数变量
     * @return 创建的参数变量
     */
    protected Var<?> newParaVar(com.jmix.executor.impl.algmodel.ParaVar internalParaVar) {
        ParaVar paraVar = new ParaVar();
        paraVar.setBase(internalParaVar.getBase());
        paraVar.value = internalParaVar.getValue();
        paraVar.isHidden = internalParaVar.getIsHidden();
        paraVar.optionSelectVars = internalParaVar.getOptionSelectVars();
        return paraVar;
    }

    public void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintInCompatible(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    public void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintCoDependent(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    public void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes,
            ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintRequires(ruleCode, super.getParaVar(leftParaVar.getCode()),
                leftParaFilterOptionCodes, super.getParaVar(rightParaVar.getCode()), rightParaFilterOptionCodes);
    }

    // /**
    // * 通用的部件求和方法，根据指定的变量获取函数计算总和
    // *
    // * @param cofAttrCode 属性代码，如果为null或空则使用默认值1
    // * @param varGetter 从PartVar获取LinearArgument的函数（如getIsSelected或getQty）
    // * @param varName 变量名称（如"isSelected"或"qty"），用于构建字符串表达式
    // * @return 求和后的LinearExpr表达式
    // */
    // private LinearExpr sum4Parts(String cofAttrCode,
    // Function<com.jmix.executor.impl.algmodel.PartVar, LinearArgument> varGetter,
    // String varName) {
    // com.jmix.executor.impl.algmodel.PartVar partVar = null;
    // List<Part> atomicParts = super.getModule().getAtomicParts();
    // LinearArgument[] sumTerms = new LinearArgument[atomicParts.size()];
    // List<String> sumTermStrings = new ArrayList<>();
    // int index = 0;
    // boolean isWithoutAttr = Strings.isNullOrEmpty(cofAttrCode);
    // for (Part part : atomicParts) {
    // partVar = super.getPartVar(part.getCode());
    // int attrValue = isWithoutAttr ? 1 :
    // Integer.parseInt(part.getAttr(cofAttrCode));
    // sumTerms[index] = LinearExpr.term(varGetter.apply(partVar), attrValue);
    // sumTermStrings.add(partVar.getBase().getShortCode() + "." + varName + "*" +
    // attrValue);
    // index++;
    // }
    // String sumFormulaBase = String.join(" + ", sumTermStrings);
    // log.info("Sum formula: {}", sumFormulaBase);
    // return LinearExpr.sum(sumTerms);
    // }

    // public LinearExpr sum4Selected() {
    // return sum4Selected(null);
    // }

    // public LinearExpr sum4Selected(String cofAttrCode) {
    // return sum4Parts(cofAttrCode,
    // com.jmix.executor.impl.algmodel.PartVar::getIsSelected, "S");
    // }

    // public LinearExpr sum4Quantity() {
    // return sum4Quantity(null);
    // }

    // public LinearExpr sum4Quantity(String cofAttrCode) {
    // return sum4Parts(cofAttrCode,
    // com.jmix.executor.impl.algmodel.PartVar::getQty, "Q");
    // }

}
