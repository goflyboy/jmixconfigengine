package com.jmix.executor.impl.southbridge;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.impl.algmodel.AlgCPFacadeAdapters;
import com.jmix.executor.impl.algmodel.AlgCPModel;
import com.jmix.executor.impl.algmodel.ModuleAlgImpl;
import com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryAlgImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.ModuleCPModel;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.southinf.view.ModuleInstView;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SouthboundModuleAlgAdapter extends ModuleAlgImpl {
    private final ModuleAlgBase algorithm;
    private final Runtime runtime;

    public SouthboundModuleAlgAdapter(ModuleAlgBase algorithm) {
        this.algorithm = algorithm;
        this.runtime = new Runtime(this);
        this.algorithm.bindSouthboundRuntime(runtime);
    }

    public static void updateRuntimeContext(ModuleAlgBase algorithm, ModuleBaseAlgImpl context) {
        ModuleAlgBase.RuntimeSupport support = algorithm.southboundRuntime();
        if (support instanceof Runtime runtime) {
            runtime.setCurrentContext(context);
        }
    }

    public static Object newPartVarForField(ModuleAlgBase algorithm, PartVarImpl internalPartVar, Field field) {
        return runtime(algorithm).adapter.newPartVarForField(internalPartVar, field);
    }

    public static Object newParaVarForField(ModuleAlgBase algorithm, ParaVarImpl internalParaVar, Field field) {
        return runtime(algorithm).adapter.newParaVarForField(internalParaVar, field);
    }

    private static Runtime runtime(ModuleAlgBase algorithm) {
        ModuleAlgBase.RuntimeSupport support = algorithm.southboundRuntime();
        if (support instanceof Runtime runtime) {
            return runtime;
        }
        throw new IllegalStateException("Southbound runtime is not bound: " + algorithm.getClass().getName());
    }

    @Override
    protected Object moduleAlgFile() {
        return algorithm;
    }

    @Override
    protected void initModelAfter(AlgCPModel model) {
        runtime.bindModel();
    }

    @Override
    protected void afterInitData(IModule module, Object moduleAlg) {
        List<PartCategoryAlgImpl> partCategoryAlgImpls = getPartCategoryAlgs();
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : algorithm.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            fieldMap.put(extractCodeFromFieldName(field.getName()), field);
        }
        for (PartCategoryAlgImpl partCategoryAlgImpl : partCategoryAlgImpls) {
            Field field = fieldMap.get(partCategoryAlgImpl.getCategoryCode());
            if (field != null
                    && com.jmix.executor.southinf.var.PartCategoryVar.class.isAssignableFrom(field.getType())) {
                setVariableField(algorithm, SouthboundVarHandles.partCategory(partCategoryAlgImpl), field);
            }
        }
    }

    @Override
    protected Object newPartVar(PartVarImpl internalPartVar) {
        return SouthboundVarHandles.part(internalPartVar);
    }

    @Override
    protected Object newParaVar(ParaVarImpl internalParaVar) {
        return SouthboundVarHandles.para(internalParaVar);
    }

    @Override
    protected Object newPartVarForField(PartVarImpl internalPartVar, Field field) {
        if (PartVarImpl.class.isAssignableFrom(field.getType())) {
            return internalPartVar;
        }
        return newPartVar(internalPartVar);
    }

    @Override
    protected Object newParaVarForField(ParaVarImpl internalParaVar, Field field) {
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

    public static final class Runtime implements ModuleAlgBase.RuntimeSupport {
        private final SouthboundModuleAlgAdapter adapter;
        private ModuleCPModel model;

        private Runtime(SouthboundModuleAlgAdapter adapter) {
            this.adapter = adapter;
        }

        private void bindModel() {
            this.model = new SouthboundLatestBridge(adapter).model();
        }

        private void setCurrentContext(ModuleBaseAlgImpl context) {
            adapter.currentModule = context == null ? null : context.getModule();
            adapter.currentModuleAlg = context;
        }

        @Override
        public ModuleCPModel model() {
            return model;
        }

        @Override
        public ModuleInstView currentInst() {
            return adapter.currentModuleInstView();
        }

        @Override
        public List<PartVar> partVars(String filterCondition) {
            return adapter.getInternalPartVars(filterCondition).stream()
                    .map(SouthboundVarHandles::part)
                    .collect(Collectors.toList());
        }

        @Override
        public void addVarAboutHiddenConstraints(ParaVar... hiddenVars) {
            for (ParaVar hiddenVar : hiddenVars) {
                adapter.codesOfHiddenConstraint.add(hiddenVar.code());
            }
        }

        @Override
        public void addVarAboutHiddenConstraints(PartVar... hiddenVars) {
            for (PartVar hiddenVar : hiddenVars) {
                adapter.codesOfHiddenConstraint.add(hiddenVar.code());
            }
        }

        @Override
        public void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes) {
            adapter.addCompatibleConstraintRequires(ruleCode, adapter.getParaVar(leftParaVar.code()),
                    leftParaFilterOptionCodes, adapter.getParaVar(rightParaVar.code()), rightParaFilterOptionCodes);
        }

        @Override
        public void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes) {
            adapter.addCompatibleConstraintCoDependent(ruleCode, adapter.getParaVar(leftParaVar.code()),
                    leftParaFilterOptionCodes, adapter.getParaVar(rightParaVar.code()), rightParaFilterOptionCodes);
        }

        @Override
        public void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes) {
            adapter.addCompatibleConstraintInCompatible(ruleCode, adapter.getParaVar(leftParaVar.code()),
                    leftParaFilterOptionCodes, adapter.getParaVar(rightParaVar.code()), rightParaFilterOptionCodes);
        }

        @Override
        public void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr) {
            adapter.inCompatible(ruleCode, leftPartsExprStr, rightPartsExprStr);
        }

        @Override
        public void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr) {
            adapter.updatePriorityObjectFuntion(ruleCode, AlgCPFacadeAdapters.unwrapPartExpr(expr));
        }

        @Override
        public void addControlParaEqual(String sumParaCode, String instSumParaCode) {
            adapter.addControlParaEqual(sumParaCode, instSumParaCode);
        }
    }
}
