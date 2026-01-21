package com.jmix.tool.bbuilder.impl;

import com.jmix.executor.bmodel.DynamicAttributerOption;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数选项信息
 * 包含参数选项的变量信息，用于代码生成
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParaOptionVarInfo extends VarInfo<DynamicAttributerOption> {
    /**
     * 选项编码ID
     */
    private int codeId;

    /**
     * 选项编码
     */
    private String code;

    /**
     * 默认构造函数
     */
    public ParaOptionVarInfo() {
        super(); // 调用VarInfo的默认构造函数
    }

    /**
     * 带参数选项对象的构造函数
     * 
     * @param option 参数选项对象
     */
    public ParaOptionVarInfo(DynamicAttributerOption option) {
        super(option); // 调用VarInfo的构造函数，传入ParaOption（因为ParaOption继承自Extensible）
    }
}