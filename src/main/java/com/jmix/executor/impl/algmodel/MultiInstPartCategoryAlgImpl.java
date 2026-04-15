package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
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

    @Override
    public int getInstId() {
        return ModuleInst.INVALID_INSTANCE_ID;
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

    protected void initData(AlgCPModel model, IModule module, PartCategoryInputBase partCategoryInput,
            IModuleAlg moduleAlgFile) {
        super.initData(model, module, partCategoryInput, moduleAlgFile);
        for (PartCategoryInput partCategoryInputInst : this.getPartCategoryInputs()) {
            SingleInstPartCategoryAlgImpl partCategoryAlg = new SingleInstPartCategoryAlgImpl();
            partCategoryAlg.initData(model, (IModule) partCategoryInputInst.getFilteredCategory(),
                    partCategoryInputInst,
                    moduleAlgFile);
            partCategoryAlgs.put(partCategoryInputInst.getInsKey(), Pair.of(partCategoryInputInst, partCategoryAlg));
        }
    }

    protected void setSumSumToSumAttrParaRelation() {
        // 设置 总总 和 总变量的关系
        List<AttrPara> sumSumAttrParas = getMultiInstPartCategoryInput().getSumSumAttrParas();
        for (AttrPara attrPara : sumSumAttrParas) {
            // 有一个已经输入，则汇总也设置数据
            // 如果汇总已经输入，一输入的为准
            // 总总参，和总参的关系，后续支持写逻辑（多个总总之间issue, 总参和下面的变量的关系）
            Pair<Boolean, Integer> hasSetSumPara = hasSetSumPara(attrPara);
            if (hasSetSumPara.getFirst()) {
                ParaVar sumSumParaVar = getSumSumParaByAttr(attrPara.getAttrCode());
                if (!sumSumParaVar.getHasInputed()) {
                    sumSumParaVar.setInputValue(hasSetSumPara.getSecond());
                    sumSumParaVar.setHasInputed(true);
                }
            }
        }
    }

    // 判断本总总参在其子类是否已经设置, 有一个设置了，则为设置
    private Pair<Boolean, Integer> hasSetSumPara(AttrPara sumSumPara) {
        boolean hasSet = false;
        int sumSumValue = 0;
        ParaVar pv = null;
        for (SingleInstPartCategoryAlgImpl partCategoryAlg : getPartCategoryInsts()) {
            pv = partCategoryAlg.getSumParaByAttrInternal(sumSumPara.getAttrCode());
            if (pv != null && pv.getHasInputed()) {
                hasSet = true;
                sumSumValue += pv.getInputValue();
            } else {
                // throw exception,log error
                log.warn("SumSumPara " + sumSumPara.getAttrCode()
                        + " is not set or not inputed in the child PartCategoryAlg");
                continue;
            }
        }
        return new Pair<>(hasSet, sumSumValue);
    }

    // @Override
    // protected void initAll(IModuleAlg moduleAlgFile) {
    // for (PartCategoryInput partCategoryInput : this.getPartCategoryInputs()) {
    // SingleInstPartCategoryAlgImpl partCategoryAlg = new
    // SingleInstPartCategoryAlgImpl();
    // partCategoryAlg.initData(model, (IModule)
    // partCategoryInput.getFilteredCategory(), partCategoryInput,
    // moduleAlgFile);
    // partCategoryAlgs.put(partCategoryInput.getInsKey(),
    // Pair.of(partCategoryInput, partCategoryAlg));
    // }
    // }

    public void initRules(Map<String, Method> allRuleMethods, IModuleAlg moduleAlgFile, CalcStage calcStage) {
        super.buildPriorityConstraint(getMultiInstPartCategoryInput());

        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partCategoryAlg.getSecond().initRules(allRuleMethods, moduleAlgFile, calcStage);
        }

        // 执行当前层的所有实例的规则
        super.executeModuleRules(getMultiInstPartCategoryInput().getAllInstRules(), moduleAlgFile, allRuleMethods,
                calcStage);
    }

    @Override
    public void initInput(IModuleAlg moduleAlgFile) {
        MultiInstPartCategoryInput multiInstPartCategoryInput = (MultiInstPartCategoryInput) this.moduleInput;
        newAttrParaVar(multiInstPartCategoryInput.getSumSumAttrParas());
        super.initInput(moduleAlgFile);
        setPartCategoryInput(multiInstPartCategoryInput);
        for (PartCategoryAlgImpl partCategoryAlg : this.getPartCategoryInsts()) {
            partCategoryAlg.initInput(moduleAlgFile);
        }
        setSumSumToSumAttrParaRelation();
    }

    protected void sumFunConstraint(ModuleBaseAlgImpl moduleBaseAlgImplTmp,
            PartCategoryInputBase partConstraintTmp) {
        MultiInstPartCategoryAlgImpl multiInstPartCategoryAlgImpl = (MultiInstPartCategoryAlgImpl) moduleBaseAlgImplTmp;
        PartCategoryInput partConstraint = (PartCategoryInput) partConstraintTmp;
        PartAlgCPLinearExpr sumFunExpr = new PartAlgCPLinearExpr(
                multiInstPartCategoryAlgImpl.getCategoryCode() + "_" + multiInstPartCategoryAlgImpl.getInstId()
                        + "_sumFunConstraint");
        List<SingleInstPartCategoryAlgImpl> partCategoryAlgImpls = multiInstPartCategoryAlgImpl.getPartCategoryInsts();
        for (SingleInstPartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            buildSumExprInternal(sumFunExpr, partCategoryAlgImpl,
                    partConstraint.getSumAttrCode(), "Q",
                    PartVar::getQty, "");
        }
        // 应用约束
        ComparisonOperator operator = ComparisonOperator.fromSymbol(partConstraint.getComparator());
        operator.applyConstraint(model, sumFunExpr, partConstraint.getLeftValue());
        log.info("Priority-Added sum constraint: {} for {}",
                sumFunExpr.getExprStr(),
                partConstraint.getOrgReq() != null ? partConstraint.getOrgReq().toString() : "null");
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
        return super.getParaVar(code);
    }

    public ParaVar getSumSumParaByAttr(String attrCode) {
        return super.getSumSumParaByAttr(attrCode);
    }

    public ParaVar getSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumParaByAttr'");
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
