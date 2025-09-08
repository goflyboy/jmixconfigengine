package com.jmix.configengine.inf;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模块实例类
 */
@Data
public class ModuleInst {
    public Long id; // module id
    public String code; // module code
    public String instanceConfigId; // instance config id
    public int instanceId; // instance id 默认为0，多个实例，从0开始，如：0，1,2，....
    public Integer quantity; // quantity
    public List<ParaInst> paras; // paras
    public List<PartInst> parts; // parts
    public Map<String,Object> extAttrs;
    
    public void addParaInst(ParaInst paraInst){
        paras.add(paraInst);
    }
    
    public void addPartInst(PartInst partInst){
        parts.add(partInst);
    }
}
