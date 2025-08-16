package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部件信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartInfo extends VarInfo {
    /**
     * 部件编码
     */
    private String code;
    
    /**
     * 最大数量
     */
    private int maxQuantity = 10000;
    
    /**
     * 最小数量
     */
    private int minQuantity = 0;
    
    public PartInfo() {
        // 设置默认变量名
        setVarName(code + "Var");
    }
} 