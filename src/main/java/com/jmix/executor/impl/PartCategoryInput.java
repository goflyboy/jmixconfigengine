package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.cmodel.ModuleInst;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class PartCategoryInput extends PartCategoryInputBase {
    /**
     * 实例Id
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

    /**
     * 过滤后的部件分类
     */
    private PartCategory filteredCategory;

    /**
     * 求和属性参数列表
     */
    private List<AttrPara> sumAttrParas = new ArrayList<>();

    @Override
    public String getPartCategoryCode() {
        return filteredCategory.getCode();
    }

    public String getInsKey() {
        return getPartCategoryCode() + "_" + getInstId();
    }
}
