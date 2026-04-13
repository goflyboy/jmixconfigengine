package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.impl.IPartCategoryInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.southinf.IModuleAlg;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class MultiInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {

    private Map<String, Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl>> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 默认构造函数
     */
    public MultiInstPartCategoryAlgImpl() {
        super();
    }

    public List<SingleInstPartCategoryAlgImpl> getPartCategoryInsts() {
        return new ArrayList<>(partCategoryAlgs.values().stream().map(Pair::getSecond).collect(Collectors.toList()));
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
            SingleInstPartCategoryAlgImpl partCategoryAlg = new SingleInstPartCategoryAlgImpl();
            partCategoryAlg.initData(model, (IModule) partCategoryInput.getFilteredCategory(), partCategoryInput,
                    moduleAlgFile);
            partCategoryAlgs.put(partCategoryInput.getInsKey(), Pair.of(partCategoryInput, partCategoryAlg));
        }
    }

    public void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partCategoryAlg.getSecond().initRules(allRuleMethods, moduleAlgFile, calcStage);
        }

        // 执行当前层的所有实例的规则
        super.executeModuleRules(getMultiInstPartCategoryInput().getAllInstRules(), moduleAlgFile, allRuleMethods,
                calcStage);
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

    @Override
    public String getCategoryCode() {
        return getMultiInstPartCategoryInput().getPartCategoryCode();
    }

    @Override
    public List<Part> getAllAtomicParts() {
        List<Part> allAtomicParts = new ArrayList<>();
        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            allAtomicParts.addAll(partCategoryAlg.getSecond().getAllAtomicParts());
        }
        return allAtomicParts;
    }

    @Override
    public List<Part> getAtomicParts() {
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicParts'");
        // return new ArrayList<>();
    }

    @Override
    public ParaVar getParaVar(String code) {
        throw new UnsupportedOperationException("Unimplemented method 'getParaVar'");
    }

    @Override
    public PartVar getPartVar(String code) {
        throw new UnsupportedOperationException("Unimplemented method 'getPartVar'");
    }

    @Override
    public List<PartVar> getAllPartVars(String filterConditionStr) {
        List<PartVar> partVars = new ArrayList<>();
        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partVars.addAll(partCategoryAlg.getSecond().getAllPartVars(filterConditionStr));
        }
        return partVars;
    }

    public Pair<List<PartVar>, List<PartVar>> filterAllPartVars(String filterConditionStr) {
        Pair<List<PartVar>, List<PartVar>> result = new Pair<>(new ArrayList<>(), new ArrayList<>());
        for (SingleInstPartCategoryAlgImpl partCategoryAlg : this.getPartCategoryInsts()) {
            Pair<List<PartVar>, List<PartVar>> partVars = partCategoryAlg.filterAllPartVars(filterConditionStr);
            result.getFirst().addAll(partVars.getFirst());
            result.getSecond().addAll(partVars.getSecond());
        }
        return result;
    }
}
