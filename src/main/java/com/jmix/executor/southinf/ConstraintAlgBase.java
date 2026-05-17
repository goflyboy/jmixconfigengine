package com.jmix.executor.southinf;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.impl.algmodel.VarImpl;
import com.jmix.executor.impl.southbridge.SouthboundInstViews;
import com.jmix.executor.impl.southbridge.SouthboundLatestBridge;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.view.ModuleInstView;
import com.jmix.executor.southinf.view.ParameterInstView;
import com.jmix.executor.southinf.view.PartCategoryInstSumView;
import com.jmix.executor.southinf.view.PartCategoryInstView;
import com.jmix.executor.southinf.view.PartInstView;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V1 southbound base class. It exposes stable facade types while reusing the
 * current solver implementation through a bridge.
 */
public abstract class ConstraintAlgBase extends ModuleAlgImpl implements ConstraintAlgorithm, ModuleInstView {

    private ConstraintContext context;

    private ConstraintModel constraintModel;

    private ConstraintVarRegistry varRegistry;

    @Override
    public AlgorithmDescriptor descriptor() {
        if (context != null) {
            return context.descriptor();
        }
        AlgorithmDescriptor descriptor = new AlgorithmDescriptor();
        AlgorithmApiVersion apiVersion = getClass().getAnnotation(AlgorithmApiVersion.class);
        if (apiVersion != null) {
            descriptor.setSouthApiVersion(apiVersion.southApiVersion());
            descriptor.setAlgorithmVersion(apiVersion.algorithmVersion());
        }
        descriptor.setAlgorithmId(getClass().getName());
        return descriptor;
    }

    @Override
    public void bind(ConstraintContext context) {
        this.context = context;
    }

    protected ConstraintModel model() {
        if (context != null) {
            return context.model();
        }
        if (constraintModel == null) {
            constraintModel = new SouthboundLatestBridge(this).model();
        }
        return constraintModel;
    }

    protected ConstraintVarRegistry vars() {
        if (context != null) {
            return context.vars();
        }
        if (varRegistry == null) {
            varRegistry = new SouthboundLatestBridge(this).vars();
        }
        return varRegistry;
    }

    protected ModuleInstView moduleInst() {
        return this;
    }

    protected ParaVar para(String code) {
        return vars().para(code);
    }

    protected PartVar partVar(String code) {
        return vars().part(code);
    }

    protected PartCategoryVar partCategoryVar(String code) {
        return vars().partCategory(code);
    }

    protected List<PartVar> partVars() {
        return partVars("");
    }

    protected List<PartVar> partVars(String filterCondition) {
        return getInternalPartVars(filterCondition).stream()
                .map(PartVar::new)
                .collect(Collectors.toList());
    }

    protected void addVarAboutHiddenConstraints(ParaVar... hiddenVars) {
        super.addVarAboutHiddenConstraints(hiddenVars);
    }

    protected void addVarAboutHiddenConstraints(PartVar... hiddenVars) {
        super.addVarAboutHiddenConstraints(hiddenVars);
    }

    protected void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintRequires(ruleCode, leftParaVar.internal(), leftParaFilterOptionCodes,
                rightParaVar.internal(), rightParaFilterOptionCodes);
    }

    protected void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintCoDependent(ruleCode, leftParaVar.internal(), leftParaFilterOptionCodes,
                rightParaVar.internal(), rightParaFilterOptionCodes);
    }

    protected void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
            List<String> leftParaFilterOptionCodes, ParaVar rightParaVar, List<String> rightParaFilterOptionCodes) {
        super.addCompatibleConstraintInCompatible(ruleCode, leftParaVar.internal(), leftParaFilterOptionCodes,
                rightParaVar.internal(), rightParaFilterOptionCodes);
    }

    @Override
    protected void afterInitData(IModule module, IModuleAlg moduleAlg) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = getPartCategoryAlgs();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : moduleAlg.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            fieldMap.put(extractCodeFromFieldName(field.getName()), field);
        }
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            Field field = fieldMap.get(partCategoryAlgImpl.getCategoryCode());
            if (field != null && PartCategoryVar.class.isAssignableFrom(field.getType())) {
                PartCategoryVar partCategoryVar = new PartCategoryVar(partCategoryAlgImpl);
                setVariableField(moduleAlg, partCategoryVar, field);
            }
        }
    }

    @Override
    protected VarImpl<?> newPartVar(PartVarImpl internalPartVar) {
        return new PartVar(internalPartVar);
    }

    @Override
    protected VarImpl<?> newParaVar(ParaVarImpl internalParaVar) {
        return new ParaVar(internalParaVar);
    }

    @Override
    protected VarImpl<?> newPartVarForField(PartVarImpl internalPartVar, Field field) {
        if (PartVarImpl.class.isAssignableFrom(field.getType())) {
            return internalPartVar;
        }
        return newPartVar(internalPartVar);
    }

    @Override
    protected VarImpl<?> newParaVarForField(ParaVarImpl internalParaVar, Field field) {
        if (ParaVarImpl.class.isAssignableFrom(field.getType())) {
            return internalParaVar;
        }
        return newParaVar(internalParaVar);
    }

    private String extractCodeFromFieldName(String fieldName) {
        if (fieldName.endsWith("Var")) {
            return fieldName.substring(0, fieldName.length() - 3);
        }
        return fieldName;
    }

    private ModuleInstView currentView() {
        try {
            return new SouthboundInstViews.ModuleView(currentModuleInstAccessor());
        } catch (AlgLoaderException ex) {
            throw ex;
        }
    }

    @Override
    public String code() {
        return currentView().code();
    }

    @Override
    public String extAttr(String extAttrKey) {
        return currentView().extAttr(extAttrKey);
    }

    @Override
    public int extAttr4Int(String extAttrKey) {
        return currentView().extAttr4Int(extAttrKey);
    }

    @Override
    public String dynAttr(String dynAttrKey) {
        return currentView().dynAttr(dynAttrKey);
    }

    @Override
    public int dynAttr4Int(String dynAttrKey) {
        return currentView().dynAttr4Int(dynAttrKey);
    }

    @Override
    public void setDynAttr(String dynAttrKey, String dynAttrValue) {
        currentView().setDynAttr(dynAttrKey, dynAttrValue);
    }

    @Override
    public Long moduleId() {
        return currentView().moduleId();
    }

    @Override
    public String instanceConfigId() {
        return currentView().instanceConfigId();
    }

    @Override
    public int quantity() {
        return currentView().quantity();
    }

    @Override
    public ParameterInstView parameter(String code) {
        return currentView().parameter(code);
    }

    @Override
    public PartInstView part(String code) {
        return currentView().part(code);
    }

    @Override
    public PartCategoryInstView partCategory(String code) {
        return currentView().partCategory(code);
    }

    @Override
    public PartCategoryInstView partCategory(String code, int instId) {
        return currentView().partCategory(code, instId);
    }

    @Override
    public PartCategoryInstSumView partCategorySum(String code) {
        return currentView().partCategorySum(code);
    }
}
