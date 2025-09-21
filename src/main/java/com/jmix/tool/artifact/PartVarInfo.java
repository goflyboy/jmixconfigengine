package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Part;

import lombok.Data;
import lombok.EqualsAndHashCode;

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