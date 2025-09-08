package com.jmix.configengine.inf;

/**
 * 推理参数请求类
 */
public class InferParasReq {
    public Long moduleId;
    public String moduleCode;
    public PartInst mainPartInst;
    public java.util.List<ParaInst> preParaInsts;
    public java.util.List<PartInst> prePartInsts;
    public boolean enumerateAllSolution;
}
