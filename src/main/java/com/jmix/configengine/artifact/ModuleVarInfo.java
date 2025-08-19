package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.*;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleVarInfo extends VarInfo<com.jmix.configengine.model.Module> {
	private String code;
	private String packageName;
	private List<ParaVarInfo> paras;
	private List<PartVarInfo> parts;
	private List<RuleInfo> rules;
	private Map<String, ParaVarInfo> paraMap = new HashMap<>();
	private Map<String, PartVarInfo> partMap = new HashMap<>();
	
	public ModuleVarInfo() { super(); }
	public ModuleVarInfo(com.jmix.configengine.model.Module module) { super(module); }
	
	public void setParas(List<ParaVarInfo> paras) { this.paras = paras; updateParaMap(); }
	public void setParts(List<PartVarInfo> parts) { this.parts = parts; updatePartMap(); }
	
	private void updateParaMap() {
		paraMap.clear();
		if (paras != null) { for (ParaVarInfo p : paras) { paraMap.put(p.getCode(), p); } }
	}
	private void updatePartMap() {
		partMap.clear();
		if (parts != null) { for (PartVarInfo p : parts) { partMap.put(p.getCode(), p); } }
	}
	public ParaVarInfo getPara(String code) { return paraMap.get(code); }
	public PartVarInfo getPart(String code) { return partMap.get(code); }
	public boolean hasPara(String code) { return paraMap.containsKey(code); }
	public boolean hasPart(String code) { return partMap.containsKey(code); }
	public List<PartVarInfo> getChildrenPart(String fatherCode) {
		if (parts == null || fatherCode == null) return new ArrayList<>();
		return parts.stream().filter(p -> fatherCode.equals(p.getFatherCode())).collect(Collectors.toList());
	}
	public List<PartVarInfo> getTopLevelParts() {
		if (parts == null) return new ArrayList<>();
		return parts.stream().filter(p -> p.getFatherCode() == null || p.getFatherCode().isEmpty()).collect(Collectors.toList());
	}
} 