package com.jmix.executor.impl;

import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.PartConstraintReq;

import lombok.Data;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class PartCategoryInput implements IPartCategoryInput {
    /**
     * 实例Id
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;
    /**
     * 求和属性代码
     */
    private String sumAttrCode;

    /**
     * 比较符
     */
    private String comparator;

    /**
     * 左值
     */
    private int leftValue;

    /**
     * 原始部件约束请求
     */
    private PartConstraintReq orgReq;

    /**
     * 过滤后的部件分类
     */
    private PartCategory filteredCategory;

    @Override
    public String getPartCategoryCode() {
        return orgReq.getPartCategoryCode();
    }

    public String getInsKey() {
        return getPartCategoryCode() + "_" + getInstId();
    }
}
