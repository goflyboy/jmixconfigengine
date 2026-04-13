package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 部件分类级算法实现
 * 专注于单个部件分类的约束处理
 * 
 * @since 2025-12-27
 */
@Slf4j
public class SingleInstPartCategoryAlgImpl extends ModuleBaseAlgImpl implements PartCategoryAlgImpl {

    /**
     * 默认构造函数
     */
    public SingleInstPartCategoryAlgImpl() {
        super();
    }

    /**
     * 获取实例ID
     * 
     * @return 实例ID
     */
    public int getInstId() {
        return getPartCategory().getInstId();
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

    @Override
    public List<Part> getAllAtomicParts() {
        return getPartCategory().getAllAtomicParts();
    }

    @Override
    public List<Part> getAtomicParts() {
        return getPartCategory().getAtomicParts();
    }
}
