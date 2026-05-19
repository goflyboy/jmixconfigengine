package com.jmix.executor.southinf;

import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.view.ModuleInstView;
import com.jmix.executor.southinf.view.ParameterInstView;
import com.jmix.executor.southinf.view.PartCategoryInstSumView;
import com.jmix.executor.southinf.view.PartCategoryInstView;
import com.jmix.executor.southinf.view.PartInstView;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;

import java.util.Arrays;
import java.util.List;

public abstract class ModuleAlgBase implements ModuleInstView {
    private RuntimeSupport runtimeSupport;

    public interface RuntimeSupport {
        ModuleCPModel model();

        ModuleInstView currentInst();

        List<PartVar> partVars(String filterCondition);

        void addVarAboutHiddenConstraints(ParaVar... hiddenVars);

        void addVarAboutHiddenConstraints(PartVar... hiddenVars);

        void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);

        void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);

        void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);

        void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr);

        void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr);

        void addControlParaEqual(String sumParaCode, String instSumParaCode);
    }

    public final void bindSouthboundRuntime(RuntimeSupport runtimeSupport) {
        this.runtimeSupport = runtimeSupport;
    }

    public final void clearSouthboundRuntime() {
        this.runtimeSupport = null;
    }

    public final RuntimeSupport southboundRuntime() {
        return runtimeSupport;
    }

    public AlgorithmDescriptor descriptor() {
        AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
        AlgorithmApiVersion apiVersion = getClass().getAnnotation(AlgorithmApiVersion.class);
        if (apiVersion != null) {
            descriptor.setSouthApiVersion(apiVersion.southApiVersion());
            descriptor.setAlgorithmVersion(apiVersion.algorithmVersion());
        }
        descriptor.setAlgorithmId(getClass().getName());
        return descriptor;
    }

    protected final ModuleCPModel model() {
        return runtime().model();
    }

    protected final ModuleInstView currentInst() {
        return runtime().currentInst();
    }

    protected final ParaVar para(String code) {
        return model().para(code);
    }

    protected final PartVar partVar(String code) {
        return model().part(code);
    }

    protected final PartCategoryVar partCategoryVar(String code) {
        return model().partCategory(code);
    }

    protected final List<PartVar> partVars() {
        return partVars("");
    }

    protected final List<PartVar> partVars(String filterCondition) {
        return runtime().partVars(filterCondition);
    }

    protected void addVarAboutHiddenConstraints(ParaVar... hiddenVars) {
        runtime().addVarAboutHiddenConstraints(hiddenVars);
    }

    protected void addVarAboutHiddenConstraints(PartVar... hiddenVars) {
        runtime().addVarAboutHiddenConstraints(hiddenVars);
    }

    protected void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        runtime().addCompatibleConstraintRequires(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                rightParaVar, rightParaFilterOptionCodes);
    }

    protected void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        runtime().addCompatibleConstraintCoDependent(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                rightParaVar, rightParaFilterOptionCodes);
    }

    protected void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        runtime().addCompatibleConstraintInCompatible(ruleCode, leftParaVar, leftParaFilterOptionCodes,
                rightParaVar, rightParaFilterOptionCodes);
    }

    protected void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr) {
        runtime().inCompatible(ruleCode, leftPartsExprStr, rightPartsExprStr);
    }

    protected void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr) {
        runtime().updatePriorityObjectFuntion(ruleCode, expr);
    }

    protected void addControlParaEqual(String sumParaCode, String instSumParaCode) {
        runtime().addControlParaEqual(sumParaCode, instSumParaCode);
    }

    protected List<String> listOf(String... codes) {
        return Arrays.asList(codes);
    }

    private RuntimeSupport runtime() {
        if (runtimeSupport == null) {
            throw new AlgLoaderException("Southbound algorithm runtime is not bound: " + getClass().getName());
        }
        return runtimeSupport;
    }

    @Override
    public String code() {
        return currentInst().code();
    }

    @Override
    public String extAttr(String extAttrKey) {
        return currentInst().extAttr(extAttrKey);
    }

    @Override
    public int extAttr4Int(String extAttrKey) {
        return currentInst().extAttr4Int(extAttrKey);
    }

    @Override
    public String dynAttr(String dynAttrKey) {
        return currentInst().dynAttr(dynAttrKey);
    }

    @Override
    public int dynAttr4Int(String dynAttrKey) {
        return currentInst().dynAttr4Int(dynAttrKey);
    }

    @Override
    public void setDynAttr(String dynAttrKey, String dynAttrValue) {
        currentInst().setDynAttr(dynAttrKey, dynAttrValue);
    }

    @Override
    public Long moduleId() {
        return currentInst().moduleId();
    }

    @Override
    public String instanceConfigId() {
        return currentInst().instanceConfigId();
    }

    @Override
    public int quantity() {
        return currentInst().quantity();
    }

    @Override
    public ParameterInstView parameter(String code) {
        return currentInst().parameter(code);
    }

    @Override
    public PartInstView part(String code) {
        return currentInst().part(code);
    }

    @Override
    public PartCategoryInstView partCategory(String code) {
        return currentInst().partCategory(code);
    }

    @Override
    public PartCategoryInstView partCategory(String code, int instId) {
        return currentInst().partCategory(code, instId);
    }

    @Override
    public PartCategoryInstSumView partCategorySum(String code) {
        return currentInst().partCategorySum(code);
    }
}
