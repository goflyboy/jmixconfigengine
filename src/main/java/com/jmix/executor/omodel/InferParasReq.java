package com.jmix.executor.omodel;

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

    private Long moduleId;

    private String moduleCode;

    private PartInst mainPartInst;

    private List<ParaInst> preParaInsts;

    private List<PartInst> prePartInsts;

    private boolean enumerateAllSolution;
}
