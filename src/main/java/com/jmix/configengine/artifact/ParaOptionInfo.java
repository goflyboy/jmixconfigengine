package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOptionInfo extends VarInfo {
    /**
     * 选项编码ID
     */
    private int codeId;
    
    /**
     * 选项编码
     */
    private String code;
    
    public ParaOptionInfo() {
        // 设置默认变量名
        setVarName(code + "_" + codeId + "_selectVar");
    }
} 