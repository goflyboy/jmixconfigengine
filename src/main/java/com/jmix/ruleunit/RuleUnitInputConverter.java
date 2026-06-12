package com.jmix.ruleunit;

import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartCategoryInst;
import com.jmix.executor.cmodel.PartInst;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.model.AggregateConditionReq;
import com.jmix.executor.model.InferParasReq;
import com.jmix.executor.model.ModulePostCalcReq;
import com.jmix.executor.model.ModuleValidateReq;
import com.jmix.executor.model.PartCategoryConstraintReq;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts business rule unit inputs into executor request models.
 */
final class RuleUnitInputConverter {

    InferParasReq toInferReq(Long moduleId, Module module, RuleUnitInput input, boolean enumerateAllSolution) {
        InferParasReq req = new InferParasReq();
        req.setModuleId(moduleId);
        req.setModuleCode(module == null ? null : module.getCode());
        req.setEnumerateAllSolution(enumerateAllSolution);
        req.setPreParaInsts(toParaInsts(module, input.parameters()));
        req.setPrePartInsts(toPartInsts(input.parts()));
        req.setPartCategoryConstraintReqs(toPartCategoryReqs(input.partCategories()));
        return req;
    }

    InferParasReq toDiagnosticInferReq(Long moduleId, Module module, RuleUnitInput input) {
        InferParasReq req = toInferReq(moduleId, module, input, false);
        req.setRelaxSolve(true);
        return req;
    }

    ModuleValidateReq toValidateReq(Long moduleId, Module module, RuleUnitInput input) {
        ModuleValidateReq req = new ModuleValidateReq();
        req.setModuleId(moduleId);
        req.setModuleCode(module == null ? null : module.getCode());
        req.setModuleInst(toModuleInst(moduleId, module, input));
        return req;
    }

    ModulePostCalcReq toPostCalcReq(Long moduleId, Module module, RuleUnitInput input) {
        ModulePostCalcReq req = new ModulePostCalcReq();
        req.setModuleId(moduleId);
        req.setModuleCode(module == null ? null : module.getCode());
        req.setSolutions(List.of(toModuleInst(moduleId, module, input)));
        return req;
    }

    RuleUnitInput toInput(com.jmix.ruletrans.testgen.business.BusinessRuleTestCase testCase) {
        return new RuleUnitInput(
                testCase.given().parameters(),
                testCase.given().parts(),
                testCase.given().partCategories());
    }

    private ModuleInst toModuleInst(Long moduleId, Module module, RuleUnitInput input) {
        ModuleInst inst = new ModuleInst();
        inst.setId(moduleId);
        inst.setCode(module == null ? null : module.getCode());
        for (ParaInst paraInst : toParaInsts(module, input.parameters())) {
            inst.addParaInst(paraInst);
        }
        addDefaultPostParameters(module, inst);
        addPartInsts(module, inst, input.parts());
        return inst;
    }

    private List<ParaInst> toParaInsts(Module module, List<RuleUnitParameter> parameters) {
        List<ParaInst> result = new ArrayList<>();
        for (RuleUnitParameter parameter : parameters) {
            if (parameter == null || isBlank(parameter.code())) {
                continue;
            }
            ParaInst paraInst = new ParaInst();
            paraInst.setCode(parameter.code());
            paraInst.setValue(toEngineParaValue(module, parameter));
            if (parameter.hidden() != null) {
                paraInst.setHidden(parameter.hidden());
            }
            result.add(paraInst);
        }
        return result;
    }

    private String toEngineParaValue(Module module, RuleUnitParameter parameter) {
        if (parameter.value() == null) {
            return null;
        }
        if (module == null) {
            return parameter.value();
        }
        return module.getPara(parameter.code())
                .map(para -> ParaTypeHandler.getCodeIdValue(para, parameter.value()))
                .orElse(parameter.value());
    }

    private List<PartInst> toPartInsts(List<RuleUnitPart> parts) {
        List<PartInst> result = new ArrayList<>();
        for (RuleUnitPart part : parts) {
            if (part == null || isBlank(part.code())) {
                continue;
            }
            PartInst partInst = new PartInst();
            partInst.setCode(part.code());
            Integer quantity = part.quantity();
            if (quantity == null && Boolean.TRUE.equals(part.isSelected())) {
                quantity = 1;
            }
            partInst.setQuantity(quantity);
            partInst.setSelected(part.isSelected() != null ? part.isSelected() : quantity != null && quantity > 0);
            if (part.hidden() != null) {
                partInst.setHidden(part.hidden());
            }
            if (!part.attrs().isEmpty()) {
                partInst.setSelectAttrValue(part.attrs());
            }
            result.add(partInst);
        }
        return result;
    }

    private List<PartCategoryConstraintReq> toPartCategoryReqs(List<RuleUnitPartCategory> categories) {
        List<PartCategoryConstraintReq> result = new ArrayList<>();
        for (RuleUnitPartCategory category : categories) {
            if (category == null || isBlank(category.category())) {
                continue;
            }
            PartCategoryConstraintReq req = new PartCategoryConstraintReq();
            req.setPartCategoryCode(category.category());
            if (!category.where().isEmpty()) {
                req.setAttrWhereCondition(toWhereExpr(category.where()));
            }
            if (!isBlank(category.aggregate())) {
                req.addAggregateCondition(toAggregateCondition(category));
            } else if (!category.where().isEmpty()) {
                req.addAggregateCondition(defaultQuantityCondition());
            }
            result.add(req);
        }
        return result;
    }

    private AggregateConditionReq toAggregateCondition(RuleUnitPartCategory category) {
        AggregateConditionReq condition = new AggregateConditionReq();
        String aggregate = category.aggregate();
        String attrTypeName = "Sum";
        String attrCode = aggregate;
        int separator = aggregate.indexOf('_');
        if (separator > 0 && separator < aggregate.length() - 1) {
            attrTypeName = aggregate.substring(0, separator);
            attrCode = aggregate.substring(separator + 1);
        }
        try {
            condition.setAttrType(AttrParaType.valueOf(attrTypeName));
        } catch (IllegalArgumentException e) {
            throw new RuleTransException("Unsupported aggregate type: " + attrTypeName, e);
        }
        condition.setAttrCode(attrCode);
        condition.setComparator(isBlank(category.operator()) ? "==" : category.operator());
        condition.setAttrValue(String.valueOf(category.value()));
        return condition;
    }

    private AggregateConditionReq defaultQuantityCondition() {
        AggregateConditionReq condition = new AggregateConditionReq();
        condition.setAttrType(AttrParaType.Sum);
        condition.setAttrCode("Quantity");
        condition.setComparator(">=");
        condition.setAttrValue("1");
        condition.setDefaulted(true);
        return condition;
    }

    private String toWhereExpr(Map<String, String> where) {
        List<String> clauses = new ArrayList<>();
        for (Map.Entry<String, String> entry : where.entrySet()) {
            if (isBlank(entry.getKey())) {
                continue;
            }
            clauses.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(" and ", clauses);
    }

    private void addDefaultPostParameters(Module module, ModuleInst inst) {
        if (module == null) {
            return;
        }
        Set<String> existingCodes = inst.getParas().stream()
                .map(ParaInst::getCode)
                .collect(Collectors.toSet());
        for (Para para : module.getParas()) {
            if (!existingCodes.contains(para.getCode())) {
                inst.addParaInst(toDefaultParaInst(para));
            }
        }
    }

    private ParaInst toDefaultParaInst(Para para) {
        ParaInst paraInst = new ParaInst();
        paraInst.setCode(para.getCode());
        if (para.getDefaultValue() != null) {
            paraInst.setValue(String.valueOf(para.getDefaultValue()));
        }
        return paraInst;
    }

    private void addPartInsts(Module module, ModuleInst inst, List<RuleUnitPart> parts) {
        if (module == null) {
            for (PartInst partInst : toPartInsts(parts)) {
                inst.addPartInst(partInst);
            }
            return;
        }

        Map<String, List<PartInst>> partInstsByParent = new LinkedHashMap<>();
        for (RuleUnitPart part : parts) {
            PartInst partInst = toPartInst(part);
            if (partInst == null) {
                continue;
            }
            Part modelPart = findAtomicPart(module, partInst.getCode());
            String parentCode = modelPart == null ? null : modelPart.getFatherCode();
            if (isBlank(parentCode)) {
                inst.addPartInst(partInst);
                continue;
            }
            partInstsByParent.computeIfAbsent(parentCode, key -> new ArrayList<>()).add(partInst);
        }

        for (Map.Entry<String, List<PartInst>> entry : partInstsByParent.entrySet()) {
            PartCategoryInst categoryInst = ensurePartCategoryInst(module, inst, entry.getKey());
            for (PartInst partInst : entry.getValue()) {
                categoryInst.addPartInst(partInst);
            }
        }
    }

    private PartCategoryInst ensurePartCategoryInst(Module module, ModuleInst inst, String categoryCode) {
        PartCategoryInst categoryInst = findPartCategoryInst(inst, categoryCode);
        PartCategory category = module.getPartCategory(categoryCode);
        if (categoryInst == null) {
            categoryInst = new PartCategoryInst();
            categoryInst.setCode(categoryCode);
            if (category != null) {
                categoryInst.setShortCode(category.getShortCode());
            }
            Optional<PartCategory> parent = parentCategory(module, categoryCode);
            if (parent.isPresent()) {
                ensurePartCategoryInst(module, inst, parent.get().getCode()).addPartCategoryInst(categoryInst);
            } else {
                inst.addPartCategoryInst(categoryInst);
            }
        }
        addDefaultCategoryParameters(category, categoryInst);
        return categoryInst;
    }

    private Part findAtomicPart(Module module, String partCode) {
        for (Part part : module.getAllAtomicParts()) {
            if (partCode.equals(part.getCode())) {
                return part;
            }
        }
        return null;
    }

    private PartCategoryInst findPartCategoryInst(ModuleInst inst, String categoryCode) {
        for (PartCategoryInst categoryInst : inst.getAllPartCategorys()) {
            if (categoryCode.equals(categoryInst.getCode())) {
                return categoryInst;
            }
        }
        return null;
    }

    private Optional<PartCategory> parentCategory(Module module, String categoryCode) {
        PartCategory category = module.getPartCategory(categoryCode);
        if (category == null || isBlank(category.getFatherCode())) {
            return Optional.empty();
        }
        return Optional.ofNullable(module.getPartCategory(category.getFatherCode()));
    }

    private void addDefaultCategoryParameters(PartCategory category, PartCategoryInst categoryInst) {
        if (category == null) {
            return;
        }
        Set<String> existingCodes = categoryInst.getParas().stream()
                .map(ParaInst::getCode)
                .collect(Collectors.toSet());
        for (Para para : category.getParas()) {
            if (!existingCodes.contains(para.getCode())) {
                categoryInst.addParaInst(toDefaultParaInst(para));
            }
        }
    }

    private PartInst toPartInst(RuleUnitPart part) {
        if (part == null || isBlank(part.code())) {
            return null;
        }
        PartInst partInst = new PartInst();
        partInst.setCode(part.code());
        Integer quantity = part.quantity();
        if (quantity == null && Boolean.TRUE.equals(part.isSelected())) {
            quantity = 1;
        }
        partInst.setQuantity(quantity);
        partInst.setSelected(part.isSelected() != null ? part.isSelected() : quantity != null && quantity > 0);
        if (part.hidden() != null) {
            partInst.setHidden(part.hidden());
        }
        if (!part.attrs().isEmpty()) {
            partInst.setSelectAttrValue(part.attrs());
        }
        return partInst;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
