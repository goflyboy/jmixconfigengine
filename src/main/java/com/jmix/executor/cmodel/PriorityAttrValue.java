package com.jmix.executor.cmodel;

import lombok.Data;

/**
 * 优先级属性值
 * 
 * @since 2025-01-XX
 */
@Data
public class PriorityAttrValue {
    /**
     * 属性代码
     */
    private String attrCode;

    /**
     * 最优值
     */
    private double optimalValue;
}
