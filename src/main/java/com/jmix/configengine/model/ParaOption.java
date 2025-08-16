package com.jmix.configengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOption extends ProgramableObject<String> {
    /**
     * 选项编码ID
     */
    private int codeId;
} 