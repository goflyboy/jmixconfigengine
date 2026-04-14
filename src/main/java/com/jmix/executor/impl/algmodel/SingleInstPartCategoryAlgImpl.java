package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.southinf.IModuleAlg;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class SingleInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {
    /**
     * 实例ID
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

    /**
     * 默认构造函数
     */
    public SingleInstPartCategoryAlgImpl() {
        super();
    }

    @Override
    protected void initData(AlgCPModel model, IModule module, PartCategoryInputBase partCategoryInput,
            IModuleAlg moduleAlgFile) {
        PartCategoryInput input = (PartCategoryInput) partCategoryInput;
        this.instId = input.getInstId();
        super.initData(model, module, partCategoryInput, moduleAlgFile);
        newAttrParaVar(input.getSumAttrParas());
        super.initInput(model, module, partCategoryInput, moduleAlgFile);
    }

    public void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        super.buildPriorityConstraint(getModule()); // 先构建本部件分类的优先类规则
        super.initRules(allRuleMethods, moduleAlgFile, calcStage);
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        return getPartCategory().isEnumMutiInst() ? getPartCategory().getEnumInstId() : instId;
    }

    public PartCategory getPartCategory() {
        return (PartCategory) getModule();
    }

    public String getCategoryCode() {
        return getPartCategory().getCode();
    }

    @Override
    protected Var<?> newPartVar(PartVar internalPartVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newPartVar'");
    }

    @Override
    protected Var<?> newParaVar(ParaVar internalParaVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newParaVar'");
    }

    @Override
    public String toString() {
        return getPartCategory().getCode() + "[" + getPartCategory().getEnumInstId() + "]";
    }

    @Override
    public List<Part> getAllAtomicParts() {
        return getPartCategory().getAllAtomicParts();
    }

    @Override
    public List<Part> getAtomicParts() {
        return getPartCategory().getAtomicParts();
    }
}