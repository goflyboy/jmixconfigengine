package com.jmix.ruleunit;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.model.ModuleValidateResp;
import com.jmix.executor.model.Result;
import com.jmix.ruletrans.testgen.business.RuleUnitDiagnostic;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitSolution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts executor outputs into business-facing actual results.
 */
final class RuleUnitResultConverter {

    RuleUnitActualResult fromInferenceResult(Module module, Result<List<ModuleInst>> result) {
        List<ModuleInst> solutions = result.getData() == null ? List.of() : result.getData();
        List<RuleUnitSolution> businessSolutions = new ArrayList<>();
        for (int i = 0; i < solutions.size(); i++) {
            businessSolutions.add(toSolution(module, solutions.get(i), i + 1));
        }
        RuleUnitSolution first = businessSolutions.isEmpty() ? null : businessSolutions.get(0);
        boolean hasSolution = !businessSolutions.isEmpty()
                && (result.getCode() == Result.SUCCESS || result.getCode() == Result.PARTIAL_SUCCESS);
        return new RuleUnitActualResult(
                result.getCode(),
                result.getMessage(),
                hasSolution,
                first == null ? List.of() : first.parameters(),
                first == null ? List.of() : first.parts(),
                businessSolutions,
                toDiagnostics(result));
    }

    RuleUnitActualResult fromValidationResult(Result<ModuleValidateResp> result) {
        ModuleValidateResp resp = result.getData();
        boolean compatible = result.getCode() == Result.SUCCESS && resp != null && resp.isValid();
        List<RuleUnitDiagnostic> diagnostics = resp == null
                ? List.of()
                : resp.getViolatedRuleCodes().stream()
                        .map(code -> new RuleUnitDiagnostic(code, null))
                        .toList();
        return new RuleUnitActualResult(
                result.getCode(),
                result.getMessage(),
                compatible,
                List.of(),
                List.of(),
                List.of(),
                diagnostics);
    }

    List<RuleUnitDiagnostic> diagnosticsFromInferenceResult(Result<List<ModuleInst>> result) {
        return toDiagnostics(result);
    }

    private RuleUnitSolution toSolution(Module module, ModuleInst solution, int rank) {
        return new RuleUnitSolution(
                rank,
                effectivePartInsts(solution).stream()
                        .map(this::toPart)
                        .toList(),
                allParaInsts(solution).stream()
                        .map(paraInst -> toParameter(module, paraInst))
                        .toList());
    }

    private List<PartInst> effectivePartInsts(ModuleInst solution) {
        Map<String, PartInst> byCode = new LinkedHashMap<>();
        for (PartInst partInst : solution.getAllParts()) {
            if (partInst == null || partInst.getCode() == null) {
                continue;
            }
            byCode.merge(partInst.getCode(), partInst, this::moreMeaningfulPart);
        }
        return new ArrayList<>(byCode.values());
    }

    private PartInst moreMeaningfulPart(PartInst current, PartInst candidate) {
        if (partScore(candidate) > partScore(current)) {
            return candidate;
        }
        return current;
    }

    private int partScore(PartInst partInst) {
        int score = 0;
        if (partInst.isSelected()) {
            score += 2;
        }
        Integer quantity = partInst.getQuantity();
        if (quantity != null && quantity > 0) {
            score += 1 + quantity;
        }
        return score;
    }

    private RuleUnitPart toPart(PartInst partInst) {
        return new RuleUnitPart(
                partInst.getCode(),
                partInst.getQuantity(),
                partInst.isSelected(),
                partInst.isHidden(),
                partInst.getSelectAttrValue());
    }

    private RuleUnitParameter toParameter(Module module, ParaInst paraInst) {
        return new RuleUnitParameter(
                paraInst.getCode(),
                toDisplayParaValue(module, paraInst),
                paraInst.isHidden());
    }

    private String toDisplayParaValue(Module module, ParaInst paraInst) {
        if (paraInst.getValue() == null || paraInst.getValue().isBlank() || module == null) {
            return paraInst.getValue();
        }
        Optional<Para> para = module.getPara(paraInst.getCode());
        if (para.isEmpty()) {
            return paraInst.getValue();
        }
        return ParaTypeHandler.getDisplayValue(para.get(), paraInst.getValue());
    }

    private List<ParaInst> allParaInsts(ModuleInst solution) {
        List<ParaInst> result = new ArrayList<>();
        if (solution.getParas() != null) {
            result.addAll(solution.getParas());
        }
        solution.getAllPartCategorys().forEach(category -> {
            if (category.getParas() != null) {
                result.addAll(category.getParas());
            }
        });
        return result;
    }

    private List<RuleUnitDiagnostic> toDiagnostics(Result<List<ModuleInst>> result) {
        if (result.getSolverResult() == null || result.getSolverResult().getDiagnosticConstraints() == null) {
            return List.of();
        }
        return result.getSolverResult().getDiagnosticConstraints().stream()
                .map(dc -> new RuleUnitDiagnostic(dc.getCode(), dc.getDescription()))
                .toList();
    }
}
