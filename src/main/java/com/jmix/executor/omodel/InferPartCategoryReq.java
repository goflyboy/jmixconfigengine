package com.jmix.executor.omodel;

import lombok.Data;

import java.util.List;

/**
 * 推理部件分类请求类
 * 包含部件分类推理所需的所有输入信息
 * 
 * @since 2025-01-XX
 */
@Data
public class InferPartCategoryReq {
    /**
     * 前置参数实例列表
     */
    private List<ParaInst> preParaInsts;

    /**
     * 前置部件实例列表
     */
    private List<PartInst> prePartInsts;

    /**
     * 部件约束请求列表
     */
    private List<PartConstraintReq> partConstraintReqs;

    /**
     * 是否枚举所有解, false，仅返回一个可行解，true，则返回所有的可选解
     */
    private boolean enumerateAllSolution = false;

    /**
     * 部件分类代码
     */
    private String partCatagoryCode;
}

