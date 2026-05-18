package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.impl.IModuleInput;
import com.jmix.executor.impl.PartCategoryInput;
import com.jmix.executor.impl.PartCategoryInputBase;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * 閮ㄤ欢鍒嗙被绾х畻娉曞疄鐜?
 * 涓撴敞浜庡崟涓儴浠跺垎绫荤殑绾︽潫澶勭悊
 * 
 * @since 2025-12-27
 */
@Slf4j
public class SingleInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {
    /**
     * 瀹炰緥ID
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

    /**
     * 榛樿鏋勯€犲嚱鏁?
     */
    public SingleInstPartCategoryAlgImpl() {
        super();
    }

    @Override
    protected void initData(AlgCPModel model, IModule module, IModuleInput moduleInput,
            Object moduleAlgFile) {
        PartCategoryInput input = (PartCategoryInput) moduleInput;
        this.instId = input.getInstId();
        super.initData(model, module, moduleInput, moduleAlgFile);
    }

    @Override
    public void initInput(Object moduleAlgFile) {
        PartCategoryInput input = (PartCategoryInput) (this.moduleInput);
        newAttrParaVar(input.getSumAttrParas());
        super.initInput(moduleAlgFile);
        setPartCategoryInput(input);
    }

    public void initRules(Map<String, Method> allRuleMethods, Object moduleAlgFile, CalcStage calcStage) {
        super.buildPriorityConstraint(getModule()); // 鍏堟瀯寤烘湰閮ㄤ欢鍒嗙被鐨勪紭鍏堢被瑙勫垯
        super.initRules(allRuleMethods, moduleAlgFile, calcStage);
    }

    /**
     * 鑾峰彇瀹炰緥ID
     * 
     * @return 瀹炰緥ID
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
    protected Object newPartVar(PartVarImpl internalPartVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newPartVar'");
    }

    @Override
    protected Object newParaVar(ParaVarImpl internalParaVar) {
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

    public ParaVarImpl getSumSumParaByAttr(String attrCode) {
        // 濡傛灉涓€涓敮鎸佸瀹炰緥鐨勫垎绫伙紝浠呭垵濮嬪寲浜嗕竴涓垎绫伙紝鍒欓渶瑕佹妸璁块棶sumsum杞彉涓簊um鐨勮闂?
        return getSumParaByAttr(attrCode);
    }

    public ParaVarImpl getSumParaByAttr(String attrCode) {
        ParaVarImpl paraVar = super.getParaVar(AttrParaType.Sum.name() + AttrPara.CODE_SEPARATOR + attrCode);
        if (paraVar == null) {
            log.error("ParaVarImpl not found for attrCode: {}", attrCode);
            throw new AlgLoaderException("ParaVarImpl not found for attrCode: " + attrCode);
        }
        return paraVar;
    }

    public ParaVarImpl getSumParaByAttrInternal(String attrCode) {
        ParaVarImpl paraVar = paraMap.get(AttrParaType.Sum.name() + AttrPara.CODE_SEPARATOR + attrCode);
        return paraVar;
    }

    protected void sumFunConstraint(ModuleBaseAlgImpl moduleBaseAlgImplTmp,
            PartCategoryInputBase partConstraintTmp) {
        SingleInstPartCategoryAlgImpl singleInstPartCategoryAlgImpl = (SingleInstPartCategoryAlgImpl) moduleBaseAlgImplTmp;
        PartCategoryInput partConstraint = (PartCategoryInput) partConstraintTmp;
        PartAlgCPLinearExpr sumFunExpr = buildSumExpr(
                singleInstPartCategoryAlgImpl,
                partConstraint.getSumAttrCode(), "Q",
                PartVarImpl::getQty, "");
        // 搴旂敤绾︽潫
        ComparisonOperator operator = ComparisonOperator.fromSymbol(partConstraint.getComparator());
        operator.applyConstraint(model, sumFunExpr, partConstraint.getLeftValue());
        log.info("Priority-Added sum constraint: {} for {}",
                sumFunExpr.getExprStr(),
                partConstraint.getOrgReq() != null ? partConstraint.getOrgReq().toString() : "null");
    }
}
