package com.jmix.executor.model;

import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;

import lombok.Data;

import java.util.List;

/**
 * 推理参数请求类
 * 包含参数推理所需的所有输入信息
 * 
 * @since 2025-09-22
 */
@Data
public class InferParasReq {

    /**
     * 模块ID
     */
    private Long moduleId;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 主部件实例
     */
    private PartInst mainPartInst;

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
    private String partCategoryCode;
}
