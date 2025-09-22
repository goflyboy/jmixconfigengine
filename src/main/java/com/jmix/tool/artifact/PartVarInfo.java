package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Part;

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
    private String code;
    private String fatherCode;
    private int maxQuantity = 10000;
    private int minQuantity = 0;

    public PartVarInfo() {
        super();
    }

    public PartVarInfo(Part part) {
        super(part);
    }
}