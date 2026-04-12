package com.jmix.executor.bmodel;

import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.logic.CalcStage;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 部件分类
 * 表示部件的分类定义，继承自Part
 *
 * @since 2025-12-27
 */
@Data
@Slf4j
public class MultiInstPartCategory implements IModule {

    private List<PartCategory> partCategories = new ArrayList<>();

    public void addPartCategory(PartCategory partCategory) {
        partCategories.add(partCategory);
    }

    @Override
    public List<Para> getParas() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParas'");
    }

    @Override
    public Optional<Para> getPara(String code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPara'");
    }

    @Override
    public List<Rule> getRules() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRules'");
    }

    @Override
    public Optional<Rule> getRule(String code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRule'");
    }

    @Override
    public boolean hasPriorityRule() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasPriorityRule'");
    }

    @Override
    public List<Rule> getPriorityRules() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPriorityRules'");
    }

    @Override
    public List<Rule> getRules(CalcStage calcStage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRules'");
    }

    @Override
    public Map<String, String> getDynAttr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttr'");
    }

    @Override
    public void setDynAttr(Map<String, String> dynAttr) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttr'");
    }

    @Override
    public void setAttr(String key, String value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setAttr'");
    }

    @Override
    public String getAttr(String key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttr'");
    }

    @Override
    public List<DynamicAttribute> getDynAttrSchemas() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttrSchemas'");
    }

    @Override
    public void setDynAttrSchemas(List<DynamicAttribute> dynAttrSchemas) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttrSchemas'");
    }

    @Override
    public DynamicAttribute getDynAttrSchema(String code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDynAttrSchema'");
    }

    @Override
    public void setDynAttrSchema(String code, DynamicAttribute dynAttrSchema) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDynAttrSchema'");
    }

    @Override
    public IPart getPart(String code) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPart'");
    }

    @Override
    public List<Rule> getAllRules() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllRules'");
    }

    @Override
    public List<Para> getAllParas() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllParas'");
    }

    @Override
    public List<IPart> getAllParts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllParts'");
    }

    @Override
    public List<Part> getAllAtomicParts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllAtomicParts'");
    }

    @Override
    public List<Part> getAtomicParts() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAtomicParts'");
    }

    @Override
    public List<Part> getAllAtomicParts(String partCategoryCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllAtomicParts'");
    }

    @Override
    public boolean hasAllPriorityRule() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasAllPriorityRule'");
    }

    @Override
    public PartCategory getPartCategory(String categoryCode) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPartCategory'");
    }

    @Override
    public List<Rule> queryPriorityRules() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryPriorityRules'");
    }

    @Override
    public List<Rule> getAllRules(CalcStage calcStage) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllRules'");
    }

}
