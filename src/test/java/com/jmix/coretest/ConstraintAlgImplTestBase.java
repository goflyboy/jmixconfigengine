package com.jmix.coretest;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVar;
import com.jmix.executor.impl.algmodel.Var;
import com.jmix.executor.model.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConstraintAlgImplTestBase extends ModuleAlgImpl {

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

        /**
         * reference to internal implementation ParaVar, for delegating runtime values
         * in tests
         */
        public com.jmix.executor.impl.algmodel.ParaVar internal;

        public Integer getInputValue() {
            return internal != null ? internal.getInputValue() : null;
        }

        public Boolean getIsHasInputed() {
            return internal != null ? internal.getIsHasInputed() : Boolean.FALSE;
        }
    }

    @Data
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
        partVar.setBase((Part) internalPartVar.getBase());
        partVar.qty = internalPartVar.getQty();
        partVar.isHidden = internalPartVar.getIsHidden();
        partVar.isSelected = internalPartVar.getIsSelected();
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
        paraVar.internal = internalParaVar;
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

    public List<PartVar> getPartVars(String filtedConditionStr) {
        List<com.jmix.executor.impl.algmodel.PartVar> partVars = super.getInternalPartVars(filtedConditionStr);
        List<PartVar> result = new ArrayList<>();
        for (com.jmix.executor.impl.algmodel.PartVar partVar : partVars) {
            result.add((PartVar) newPartVar(partVar));
        }
        return result;
    }
}
