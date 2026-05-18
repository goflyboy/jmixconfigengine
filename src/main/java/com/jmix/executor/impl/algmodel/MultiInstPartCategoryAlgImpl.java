package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.IModuleInput;
import com.jmix.executor.impl.MultiInstPartCategoryInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 閮ㄤ欢鍒嗙被绾х畻娉曞疄鐜?
 * 涓撴敞浜庡崟涓儴浠跺垎绫荤殑绾︽潫澶勭悊
 * 
 * @since 2025-12-27
 */
@Slf4j
public class MultiInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {

    private Map<String, Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl>> partCategoryAlgs = new LinkedHashMap<>();

    /**
     * 榛樿鏋勯€犲嚱鏁?
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

    protected void initData(AlgCPModel model, IModule module,
            IModuleInput partCategoryInput,
            Object moduleAlgFile) {
        super.initData(model, module, partCategoryInput, moduleAlgFile);
        for (PartCategoryInput partCategoryInputInst : this.getPartCategoryInputs()) {
            SingleInstPartCategoryAlgImpl partCategoryAlg = new SingleInstPartCategoryAlgImpl();
            if (partCategoryAlgs.containsKey(partCategoryInputInst.getInsKey())) {
                log.error("PartCategoryInputInst " + partCategoryInputInst.getInsKey()
                        + " already exists, skipping");
                throw new AlgLoaderException("PartCategoryInputInst " + partCategoryInputInst.getInsKey()
                        + " already exists, skipping");
            }
            partCategoryAlgs.put(partCategoryInputInst.getInsKey(), Pair.of(partCategoryInputInst, partCategoryAlg));
            partCategoryAlg.initData(model, (IModule) partCategoryInputInst.getFilteredCategory(),
                    partCategoryInputInst,
                    moduleAlgFile);
        }
    }

    protected void setSumSumToSumAttrParaRelation() {
        // 璁剧疆 鎬绘€?鍜?鎬诲彉閲忕殑鍏崇郴
        List<AttrPara> sumSumAttrParas = getMultiInstPartCategoryInput().getSumSumAttrParas();
        for (AttrPara attrPara : sumSumAttrParas) {
            // 鏈変竴涓凡缁忚緭鍏ワ紝鍒欐眹鎬讳篃璁剧疆鏁版嵁
            // 濡傛灉姹囨€诲凡缁忚緭鍏ワ紝涓€杈撳叆鐨勪负鍑?
            // 鎬绘€诲弬锛屽拰鎬诲弬鐨勫叧绯伙紝鍚庣画鏀寔鍐欓€昏緫锛堝涓€绘€讳箣闂磇ssue, 鎬诲弬鍜屼笅闈㈢殑鍙橀噺鐨勫叧绯伙級
            Pair<Boolean, Integer> hasSetSumPara = hasSetSumPara(attrPara);
            if (hasSetSumPara.getFirst()) {
                ParaVarImpl sumSumParaVar = getSumSumParaByAttr(attrPara.getAttrCode());
                if (!sumSumParaVar.getHasInputed()) {
                    sumSumParaVar.setInputValue(hasSetSumPara.getSecond());
                    sumSumParaVar.setHasInputed(true);
                }
            }
        }
    }

    // 鍒ゆ柇鏈€绘€诲弬鍦ㄥ叾瀛愮被鏄惁宸茬粡璁剧疆, 鏈変竴涓缃簡锛屽垯涓鸿缃?
    private Pair<Boolean, Integer> hasSetSumPara(AttrPara sumSumPara) {
        boolean hasSet = false;
        int sumSumValue = 0;
        ParaVarImpl pv = null;
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

    public void initRules(Map<String, Method> allRuleMethods, Object moduleAlgFile, CalcStage calcStage) {
        super.buildPriorityConstraint(getMultiInstPartCategoryInput());

        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partCategoryAlg.getSecond().initRules(allRuleMethods, moduleAlgFile, calcStage);
        }

        // 鎵ц褰撳墠灞傜殑鎵€鏈夊疄渚嬬殑瑙勫垯
        super.executeModuleRules(getMultiInstPartCategoryInput().getAllInstRules(), moduleAlgFile, allRuleMethods,
                calcStage);
    }

    @Override
    public void initInput(Object moduleAlgFile) {
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
        MultiInstPartCategoryInput partConstraint = (MultiInstPartCategoryInput) partConstraintTmp;
        PartAlgCPLinearExpr sumFunExpr = new PartAlgCPLinearExpr(
                multiInstPartCategoryAlgImpl.getCategoryCode() + "_" + multiInstPartCategoryAlgImpl.getInstId()
                        + "_sumFunConstraint");
        List<SingleInstPartCategoryAlgImpl> partCategoryAlgImpls = multiInstPartCategoryAlgImpl.getPartCategoryInsts();
        for (SingleInstPartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            buildSumExprInternal(sumFunExpr, partCategoryAlgImpl,
                    partConstraint.getSumAttrCode(), "Q",
                    PartVarImpl::getQty, "");
        }
        // 搴旂敤绾︽潫
        ComparisonOperator operator = ComparisonOperator.fromSymbol(partConstraint.getComparator());
        operator.applyConstraint(model, sumFunExpr, partConstraint.getLeftValue());
        log.info("Priority-Added sum constraint: {} for {}",
                sumFunExpr.getExprStr(),
                partConstraint.getOrgReq() != null ? partConstraint.getOrgReq().toString() : "null");
    }

    @Override
    protected Object newPartVar(PartVarImpl internalPartVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newPartVar'");
    }

    @Override
    protected Object newParaVar(ParaVarImpl internalParaVar) {
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
    public ParaVarImpl getParaVar(String code) {
        return super.getParaVar(code);
    }

    public ParaVarImpl getSumSumParaByAttr(String attrCode) {
        ParaVarImpl paraVar = super.getParaVar(AttrParaType.SumSum.name() + AttrPara.CODE_SEPARATOR + attrCode);
        if (paraVar == null) {
            log.error("ParaVarImpl not found for attrCode: {}", attrCode);
            throw new AlgLoaderException("ParaVarImpl not found for attrCode: " + attrCode);
        }
        return paraVar;
    }

    public ParaVarImpl getSumParaByAttr(String attrCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getSumParaByAttr'");
    }

    @Override
    public PartVarImpl getPartVar(String code) {
        throw new UnsupportedOperationException("Unimplemented method 'getPartVar'");
    }

    @Override
    public List<PartVarImpl> getAllPartVars(String filterConditionStr) {
        List<PartVarImpl> partVars = new ArrayList<>();
        for (Pair<PartCategoryInput, SingleInstPartCategoryAlgImpl> partCategoryAlg : partCategoryAlgs.values()) {
            partVars.addAll(partCategoryAlg.getSecond().getAllPartVars(filterConditionStr));
        }
        return partVars;
    }

    public Pair<List<PartVarImpl>, List<PartVarImpl>> filterAllPartVars(String filterConditionStr) {
        Pair<List<PartVarImpl>, List<PartVarImpl>> result = new Pair<>(new ArrayList<>(), new ArrayList<>());
        for (SingleInstPartCategoryAlgImpl partCategoryAlg : this.getPartCategoryInsts()) {
            Pair<List<PartVarImpl>, List<PartVarImpl>> partVars = partCategoryAlg.filterAllPartVars(filterConditionStr);
            result.getFirst().addAll(partVars.getFirst());
            result.getSecond().addAll(partVars.getSecond());
        }
        return result;
    }
}
