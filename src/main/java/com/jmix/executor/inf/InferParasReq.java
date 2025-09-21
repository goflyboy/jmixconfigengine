package com.jmix.executor.inf;

import lombok.Data;

import java.util.List;

/**
 * 推理参数请求类
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
