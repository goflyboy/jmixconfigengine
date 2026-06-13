package com.jmix.ruleunit;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.Result;
import com.jmix.ruletrans.testgen.business.RuleUnitDiagnostic;

import java.util.List;

/**
 * Default single-rule facade backed by the production ModuleConstraintExecutor.
 */
public final class DefaultModuleConstraintExecutor4SingleRule implements ModuleConstraintExecutor4SingleRule {

    private final ModuleConstraintExecutor executor;
    private final RuleUnitInputConverter inputConverter = new RuleUnitInputConverter();
    private final RuleUnitResultConverter resultConverter = new RuleUnitResultConverter();

    public DefaultModuleConstraintExecutor4SingleRule() {
        this(ModuleConstraintExecutor.INST);
    }

    public DefaultModuleConstraintExecutor4SingleRule(ModuleConstraintExecutor executor) {
        this.executor = executor == null ? ModuleConstraintExecutor.INST : executor;
    }

    @Override
    public RuleUnitActualResult testAssignment(Long moduleId, Module module, RuleUnitInput input) {
        Result<List<ModuleInst>> result = executor.inferParas(
                inputConverter.toInferReq(moduleId, module, input, true));
        return resultConverter.fromInferenceResult(module, result);
    }

    @Override
    public RuleUnitActualResult testAssignment(Long moduleId, PartCategory partCategory, RuleUnitInput input) {
        return testAssignment(moduleId, (Module) null, input);
    }

    @Override
    public RuleUnitActualResult testCompatibility(Long moduleId, Module module, RuleUnitInput input) {
        return validate(moduleId, module, input);
    }

    @Override
    public RuleUnitActualResult testCompatibility(Long moduleId, PartCategory partCategory, RuleUnitInput input) {
        return validate(moduleId, null, input);
    }

    @Override
    public RuleUnitActualResult testPriority(Long moduleId, Module module, RuleUnitInput input) {
        Result<List<ModuleInst>> result = executor.inferParas(
                inputConverter.toInferReq(moduleId, module, input, true));
        return resultConverter.fromInferenceResult(module, result);
    }

    @Override
    public RuleUnitActualResult testPriority(Long moduleId, PartCategory partCategory, RuleUnitInput input) {
        return testPriority(moduleId, (Module) null, input);
    }

    @Override
    public RuleUnitActualResult testPostAssignment(Long moduleId, Module module, RuleUnitInput input) {
        Result<List<ModuleInst>> result = executor.postCalculate(
                inputConverter.toPostCalcReq(moduleId, module, input));
        return resultConverter.fromInferenceResult(module, result);
    }

    private RuleUnitActualResult validate(Long moduleId, Module module, RuleUnitInput input) {
        Result<ModuleValidateResp> result = executor.validate(inputConverter.toValidateReq(moduleId, module, input));
        RuleUnitActualResult actual = resultConverter.fromValidationResult(result);
        if (Boolean.TRUE.equals(actual.compatible()) || !actual.diagnostics().isEmpty()) {
            return actual;
        }

        Result<List<ModuleInst>> relaxedResult = executor.inferParas(
                inputConverter.toDiagnosticInferReq(moduleId, module, input));
        List<RuleUnitDiagnostic> diagnostics = resultConverter.diagnosticsFromInferenceResult(relaxedResult).stream()
                .filter(diagnostic -> module != null && module.getRule(diagnostic.ruleCode()).isPresent())
                .toList();
        if (diagnostics.isEmpty()) {
            return actual;
        }
        return new RuleUnitActualResult(
                actual.engineResultCode(),
                actual.message(),
                actual.compatible(),
                actual.parameters(),
                actual.parts(),
                actual.solutions(),
                diagnostics);
    }
}
