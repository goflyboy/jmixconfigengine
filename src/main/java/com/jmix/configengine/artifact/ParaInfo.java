package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 参数信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaInfo extends VarInfo {
    /**
     * 参数编码
     */
    private String code;
    
    /**
     * 参数域值
     */
    private long[] domain;
    
    /**
     * 选项信息列表
     */
    private List<ParaOptionInfo> options;
    
    public ParaInfo() {
        // 设置默认变量名
        setVarName(code + "Var");
    }
} 