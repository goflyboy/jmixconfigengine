package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.PartCategory;

import lombok.extern.slf4j.Slf4j;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class PartCategoryAlgImpl extends ModuleBaseAlgImpl {
    /**
     * 默认构造函数
     */
    public PartCategoryAlgImpl() {
        super();
    }

    public PartCategory getPartCategory() {
        return (PartCategory) getModule();
    }

    public String getCategoryCode() {
        return getPartCategory().getCode();
    }

    @Override
    protected Var<?> newPartVar(PartVar internalPartVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newPartVar'");
    }

    @Override
    protected Var<?> newParaVar(ParaVar internalParaVar) {
        throw new UnsupportedOperationException("Unimplemented method 'newParaVar'");
    }
}
