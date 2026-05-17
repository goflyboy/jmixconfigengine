package com.jmix.coretest;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.ParaOptionVarImpl;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.impl.algmodel.VarImpl;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.IModuleAlg;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConstraintAlgImplTestBase extends ModuleAlgImpl {

    /**
     * 为了简化测试用例中算法的表达，类似 p1.value + p2.value 的表达，而不是
     * p1.getValue() + p2.getValue()。和 ParaVarImpl/Part 类似，但不是继承，而是内部类。
     */
    static public class ParaVar extends VarImpl<Para> {

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
        public Map<Integer, ParaOptionVarImpl> optionSelectVars = new HashMap<>();

        /**
         * 根据代码ID获取参数选项变量
         *
         * @param codeId 选项的代码ID
         * @return 对应的参数选项变量，如果不存在则返回null
         */
        public ParaOptionVarImpl getParaOptionByCodeId(Integer codeId) {
            return optionSelectVars.get(codeId);
        }

        /**
         * 根据代码获取参数选项变量
         *
         * @param code 选项的代码
         * @return 对应的参数选项变量
         * @throws AlgLoaderException 如果找不到对应的选项
         */
        public ParaOptionVarImpl getParaOptionByCode(String code) {
            for (ParaOptionVarImpl option : optionSelectVars.values()) {
                if (code != null && code.equals(option.getCode())) {
                    return option;
                }
            }
            throw new AlgLoaderException("ParaOptionVar not found for code: " + code);
        }

        /**
         * Reference to internal implementation for delegating runtime values in tests.
         */
        public ParaVarImpl internal;

        public Integer getInputValue() {
            return internal != null ? internal.getInputValue() : null;
        }

        public Boolean getIsHasInputed() {
            return internal != null ? internal.getHasInputed() : Boolean.FALSE;
        }

        public void setIsHasInputed(Boolean hasInputed) {
            if (internal != null) {
                internal.setHasInputed(hasInputed);
            }
        }
    }

    @Data
    public class PartVar extends VarImpl<Part> {
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

    /**
     * 初始化Module后
     *
     * @param module
     * @param moduleAlg
     */
    protected void afterInitData(IModule module, IModuleAlg moduleAlg) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = this.getPartCategoryAlgs();

        // 获取当前类的所有字段
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            fieldMap.put(field.getName(), field);
        }

        // 遍历partCategoryAlgImpls，创建PartCategoryVar并设置到对应的字段上
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            String categoryCode = partCategoryAlgImpl.getCategoryCode();
            Field field = fieldMap.get(categoryCode);

            if (field != null && PartCategoryVar.class.isAssignableFrom(field.getType())) {
                try {
                    // 创建PartCategoryVar
                    PartCategoryVar partCategoryVar = new PartCategoryVar();
                    partCategoryVar.setAlgImpl((ModuleBaseAlgImpl) partCategoryAlgImpl);

                    // 设置到字段上
                    field.set(this, partCategoryVar);
                    log.info("Set PartCategoryVar for field: {}, categoryCode: {}", field.getName(), categoryCode);
                } catch (IllegalAccessException e) {
                    log.error("Failed to set PartCategoryVar for field: {}", field.getName(), e);
                }
            }
        }
    }

    public class PartCategoryVar extends PartVar {
        /**
         * 引用到内部算法实现
         */
        private ModuleBaseAlgImpl algImpl;

        public void setAlgImpl(ModuleBaseAlgImpl algImpl) {
            this.algImpl = algImpl;
        }

        public ParaVar getSumSumParaByAttr(String attrCode) {
            ParaVarImpl tmpVar = algImpl.getSumSumParaByAttr(attrCode);
            return toParaVar(tmpVar);
        }

        public ParaVar getSumParaByAttr(String attrCode) {
            ParaVarImpl tmpVar = algImpl.getSumParaByAttr(attrCode);
            return toParaVar(tmpVar);
        }
    }

    /**
     * 创建部件变量，继承类可以重载
     *
     * @param internalPartVar 内部部件变量
     * @return 创建的部件变量
     */
    protected VarImpl<?> newPartVar(PartVarImpl internalPartVar) {
        PartVar partVar = new PartVar();
        partVar.setBase((Part) internalPartVar.getBase());
        partVar.qty = internalPartVar.getQty();
        partVar.isHidden = internalPartVar.getIsHidden();
        partVar.isSelected = internalPartVar.getIsSelected();
        return partVar;
    }

    /**
     * 创建参数变量，继承类可以重载
     *
     * @param internalParaVar 内部参数变量
     * @return 创建的参数变量
     */
    protected VarImpl<?> newParaVar(ParaVarImpl internalParaVar) {
        return toParaVar(internalParaVar);
    }

    /**
     * 将内部ParaVar转换为测试用的ParaVar
     *
     * @param internalParaVar 内部参数变量
     * @return 转换后的参数变量
     */
    public static ParaVar toParaVar(ParaVarImpl internalParaVar) {
        if (internalParaVar == null) {
            return null;
        }
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
        List<PartVarImpl> partVars = super.getInternalPartVars(filtedConditionStr);
        List<PartVar> result = new ArrayList<>();
        for (PartVarImpl partVar : partVars) {
            result.add((PartVar) newPartVar(partVar));
        }
        return result;
    }
}
