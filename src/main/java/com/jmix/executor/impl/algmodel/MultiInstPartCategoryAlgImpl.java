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
public class MultiInstPartCategoryAlgImpl extends ModuleBaseAlgImpl {

    /**
     * 默认构造函数
     */
    public MultiInstPartCategoryAlgImpl() {
        super();
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        throw new UnsupportedOperationException("Unimplemented method 'getInstId'");
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

    @Override
    public String toString() {
        return getPartCategory().getCode() + "[" + getPartCategory().getInstId() + "]";
    }
}
