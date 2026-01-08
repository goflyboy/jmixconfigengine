package com.jmix.executor.imodel;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 动态属性可选值
 * 表示动态属性的一个可选值
 *
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DynamicAttributerOption extends ProgrammableObject<String> {
    // 选项编码ID
    private int codeId;

    // 选项编码值
    private String codeValue;
}