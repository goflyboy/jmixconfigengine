package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 参数定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Para extends ProgrammableObject<String> {
    /**
     * 参数类型
     */
    private ParaType type;
    
    /**
     * 枚举类型下的具体枚举值
     */
    private List<ParaOption> options;
    
    /**
     * Range类型的最小值
     */
    private String minValue;
    
    /**
     * Range类型的最大值
     */
    private String maxValue;
} 