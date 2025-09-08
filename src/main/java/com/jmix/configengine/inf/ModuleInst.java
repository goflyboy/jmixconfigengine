package com.jmix.configengine.inf;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块实例类
 */
@Data
public class ModuleInst {
    //x1:1,x2:2,x3:3,x4:1
    public static final String OTHER_VARIABLES_VALUE_KEY = "OTHER_VARIABLES_VALUE";
    // x1:a1OrA3, x2:b1OrB2OrB3
    public static final String OTHER_VARIABLES_MEMO_KEY = "OTHER_VARIABLES_MEMO";
    public Long id; // module id
    public String code; // module code
    public String instanceConfigId; // instance config id
    public int instanceId; // instance id 默认为0，多个实例，从0开始，如：0，1,2，....
    public Integer quantity; // quantity
    public List<ParaInst> paras; // paras
    public List<PartInst> parts; // parts
    public Map<String,Object> extAttrs=new HashMap<>();
    
    public void addParaInst(ParaInst paraInst){
        paras.add(paraInst);
    }
    
    public void addPartInst(PartInst partInst){
        parts.add(partInst);
    }
}
