package com.jmix.configengine.inf;

import java.util.List;

import lombok.Data;

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
