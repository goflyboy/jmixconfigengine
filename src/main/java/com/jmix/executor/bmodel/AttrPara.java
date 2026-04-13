package com.jmix.executor.bmodel;

import lombok.Data;

/**
 * 属性参数类，用于描述需要对哪些属性进行汇总计算
 * 
 * @since 2026-04-13
 */
@Data
public class AttrPara {
    /**
     * 属性编码
     */
    private String attrCode;
    
    /**
     * 参数类型，默认为SUM
     */
    private AttrParaType type = AttrParaType.SUM;
}