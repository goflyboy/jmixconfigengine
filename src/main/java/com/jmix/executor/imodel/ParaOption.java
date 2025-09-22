package com.jmix.executor.imodel;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项
 * 表示参数的一个可选值
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOption extends ProgrammableObject<String> {
    /**
     * 选项编码ID
     */
    private int codeId;
}