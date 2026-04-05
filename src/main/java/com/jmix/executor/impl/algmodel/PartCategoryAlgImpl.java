package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;

import java.util.List;
import java.util.Map;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
public class PartCategoryAlgImpl extends ModuleBaseAlgImpl {

    private PartCategory partCategory;

    public PartCategoryAlgImpl(PartCategory partCategory, AlgCPModel model, Map<String, Var<?>> varMap) {
        this.partCategory = partCategory;
        this.model = model;
        this.varMap = varMap;
    }

    @Override
    protected List<Part> getParts4Sum() {
        return partCategory.getAtomicParts();
    }

    public PartCategory getPartCategory() {
        return partCategory;
    }

    public String getCategoryCode() {
        return partCategory.getCode();
    }
}
