package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 模块信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleInfo extends VarInfo {
    /**
     * 模块编码
     */
    private String code;
    
    /**
     * 参数信息列表
     */
    private List<ParaInfo> paras;
    
    /**
     * 部件信息列表
     */
    private List<PartInfo> parts;
    
    /**
     * 规则信息列表
     */
    private List<RuleInfo> rules;
    
    public ModuleInfo() {
        // 设置默认变量名
        setVarName(code + "Var");
    }
} 