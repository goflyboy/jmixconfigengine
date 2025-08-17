package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.jmix.configengine.model.Extensible;

/**
 * 部件信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartInfo extends VarInfo<com.jmix.configengine.model.Part> {
    /**
     * 部件编码
     */
    private String code;
    
    /**
     * 父部件编码
     */
    private String fatherCode;
    
    /**
     * 最大数量
     */
    private int maxQuantity = 10000;
    
    /**
     * 最小数量
     */
    private int minQuantity = 0;
    
    public PartInfo() {
        super(); // 调用VarInfo的默认构造函数
    }
    
    /**
     * 构造函数
     */
    public PartInfo(com.jmix.configengine.model.Part part) {
        super(part); // 调用VarInfo的构造函数，传入Part（因为Part继承自Extensible）
    }
} 