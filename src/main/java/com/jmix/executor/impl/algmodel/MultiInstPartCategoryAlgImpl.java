package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.impl.IPartCategoryInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.southinf.IModuleAlg;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class MultiInstPartCategoryAlgImpl extends ModuleBaseAlgImpl {

    private Map<String, Pair<PartCategoryInput, PartCategoryAlgImpl>> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 默认构造函数
     */
    public MultiInstPartCategoryAlgImpl() {
        super();
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        throw new UnsupportedOperationException("Unimplemented method 'getInstId'");
    }

    public MultiInstPartCategoryInput getMultiInstPartCategoryInput() {
        return (MultiInstPartCategoryInput) getModule();
    }

    public List<PartCategoryInput> getPartCategoryInputs() {
        return getMultiInstPartCategoryInput().getPartCategoryInputs();
    }

    @Override
    protected void initAll(IModuleAlg moduleAlgFile) {
        for (PartCategoryInput partCategoryInput : this.getPartCategoryInputs()) {
            PartCategoryAlgImpl partCategoryAlg = new PartCategoryAlgImpl();
            partCategoryAlg.initData(model, (IModule) partCategoryInput.getFilteredCategory(), partCategoryInput,
                    moduleAlgFile);
            partCategoryAlgs.put(partCategoryInput.getPartCategoryCode(), Pair.of(partCategoryInput, partCategoryAlg));
        }
    }

    protected void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        for (Pair<PartCategoryInput, PartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partCategoryAlg.getSecond().initRules(allRuleMethods, moduleAlgFile, calcStage);
        }
    }

    protected void setInputVariable(IPartCategoryInput partCategoryInput) {
        // 在initAll的initData已经完成了初始化，这里不需要
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
        return getMultiInstPartCategoryInput().getPartCategoryCode() + "[" + "multiInst" + "]";
    }
}
