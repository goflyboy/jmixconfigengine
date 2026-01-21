package com.jmix.tool.bbuilder.impl;

import com.jmix.executor.bmodel.Part;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部件变量信息类
 * 包含部件的变量信息，用于代码生成
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PartVarInfo extends VarInfo<Part> {
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

    /**
     * 默认构造函数
     */
    public PartVarInfo() {
        super();
    }

    /**
     * 带参数的构造函数
     * 
     * @param part 部件对象
     */
    public PartVarInfo(Part part) {
        super(part);
    }
}