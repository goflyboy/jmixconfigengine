package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 参数信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaInfo extends VarInfo<com.jmix.configengine.model.Para> {
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
        super(); // 调用VarInfo的默认构造函数
    }
    
    /**
     * 构造函数
     */
    public ParaInfo(com.jmix.configengine.model.Para para) {
        super(para); // 调用VarInfo的构造函数，传入Para（因为Para继承自Extensible）
    }
} 