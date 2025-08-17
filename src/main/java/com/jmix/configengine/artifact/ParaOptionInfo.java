package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.jmix.configengine.model.Extensible;

/**
 * 参数选项信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOptionInfo extends VarInfo<com.jmix.configengine.model.ParaOption> {
    /**
     * 选项编码ID
     */
    private int codeId;
    
    /**
     * 选项编码
     */
    private String code;
    
    public ParaOptionInfo() {
        super(); // 调用VarInfo的默认构造函数
    }
    
    /**
     * 构造函数
     */
    public ParaOptionInfo(com.jmix.configengine.model.ParaOption option) {
        super(option); // 调用VarInfo的构造函数，传入ParaOption（因为ParaOption继承自Extensible）
    }
} 