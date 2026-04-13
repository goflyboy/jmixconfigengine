package com.jmix.executor.impl;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.para.Para;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class MultiInstPartCategoryInput implements IPartCategoryInput, IModule {
    private List<PartCategoryInput> partCategoryInputs = new ArrayList<>();
    private List<Rule> allInstRules = new ArrayList<>();
    private List<Para> sumsumAttrParas = new ArrayList<>();

    public List<PartCategoryInput> getPartCategoryInputs() {
        return partCategoryInputs;
    }

    public void addPartCategoryInput(PartCategoryInput partCategoryInput) {
        partCategoryInputs.add(partCategoryInput);
    }

    @Override
    public List<Para> getParas() {
        return sumsumAttrParas;
    }

    @Override
    public Optional<Para> getPara(String code) {
        return sumsumAttrParas.stream().filter(para -> para.getCode().equals(code)).findFirst();
    }

    @Override
    public List<Rule> getRules() {
        return allInstRules;
    }

    @Override
    public Optional<Rule> getRule(String code) {
        return allInstRules.stream().filter(rule -> rule.getCode().equals(code)).findFirst();
    }

    @Override
    public boolean hasPriorityRule() {
        return allInstRules.stream()
                .anyMatch(rule -> RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName()));
    }

    @Override
    public List<Rule> getPriorityRules() {
        return allInstRules.stream()
                .filter(rule -> RuleTypeConstants.isPriorityRule(rule.getRuleSchemaTypeFullName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Rule> getRules(CalcStage calcStage) {
        return allInstRules.stream()
                .filter(rule -> rule.getCalcStage() == calcStage)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getDynAttr() {
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttr'");
    }

    @Override
    public void setDynAttr(Map<String, String> dynAttr) {
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttr'");
    }

    @Override
    public void setAttr(String key, String value) {
        throw new UnsupportedOperationException("Unimplemented method 'setAttr'");
    }

    @Override
    public String getAttr(String key) {
        throw new UnsupportedOperationException("Unimplemented method 'getAttr'");
    }

    @Override
    public List<DynamicAttribute> getDynAttrSchemas() {
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttrSchemas'");
    }

    @Override
    public void setDynAttrSchemas(List<DynamicAttribute> dynAttrSchemas) {
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttrSchemas'");
    }

    @Override
    public DynamicAttribute getDynAttrSchema(String code) {
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttrSchema'");
    }

    @Override
    public void setDynAttrSchema(String code, DynamicAttribute dynAttrSchema) {
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttrSchema'");
    }

    @Override
    public IPart getPart(String code) {
        throw new UnsupportedOperationException("Unimplemented method 'getPart'");
    }

    @Override
    public List<Rule> getAllRules() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllRules'");
    }

    @Override
    public List<Para> getAllParas() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllParas'");
    }

    @Override
    public List<IPart> getAllParts() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllParts'");
    }

    @Override
    public List<Part> getAllAtomicParts() {
        throw new UnsupportedOperationException("Unimplemented method 'getAllAtomicParts'");
    }

    @Override
    public List<Part> getAtomicParts() {
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicParts'");
    }

    @Override
    public List<Part> getAllAtomicParts(String partCategoryCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getAllAtomicParts'");
    }

    @Override
    public boolean hasAllPriorityRule() {
        throw new UnsupportedOperationException("Unimplemented method 'hasAllPriorityRule'");
    }

    @Override
    public PartCategory getPartCategory(String categoryCode) {
        throw new UnsupportedOperationException("Unimplemented method 'getPartCategory'");
    }

    @Override
    public List<Rule> queryPriorityRules() {
        throw new UnsupportedOperationException("Unimplemented method 'queryPriorityRules'");
    }

    @Override
    public List<Rule> getAllRules(CalcStage calcStage) {
        throw new UnsupportedOperationException("Unimplemented method 'getAllRules'");
    }

    @Override
    public String getPartCategoryCode() {
        return partCategoryInputs.get(0).getPartCategoryCode();
    }
}
