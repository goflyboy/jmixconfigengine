package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PartVarInfo extends VarInfo<com.jmix.configengine.model.Part> {
	private String code;
	private String fatherCode;
	private int maxQuantity = 10000;
	private int minQuantity = 0;
	
	public PartVarInfo() { super(); }
	public PartVarInfo(com.jmix.configengine.model.Part part) { super(part); }
} 