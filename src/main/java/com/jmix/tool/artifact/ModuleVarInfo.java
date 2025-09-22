package com.jmix.tool.artifact;

import com.jmix.executor.imodel.Module;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块变量信息类
 * 包含模块的所有变量信息，用于代码生成
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleVarInfo extends VarInfo<Module> {
    private String code;

    private String packageName;

    private List<ParaVarInfo> paras;

    private List<PartVarInfo> parts;

    private List<RuleInfo> rules;

    private Map<String, ParaVarInfo> paraMap = new HashMap<>();

    private Map<String, PartVarInfo> partMap = new HashMap<>();

    public ModuleVarInfo() {
        super();
    }

    public ModuleVarInfo(Module module) {
        super(module);
    }

    /**
     * 设置参数列表并更新参数映射
     * 
     * @param paras 参数列表
     */
    public void setParas(List<ParaVarInfo> paras) {
        this.paras = paras;
        updateParaMap();
    }

    /**
     * 设置部件列表并更新部件映射
     * 
     * @param parts 部件列表
     */
    public void setParts(List<PartVarInfo> parts) {
        this.parts = parts;
        updatePartMap();
    }

    private void updateParaMap() {
        paraMap.clear();
        if (paras != null) {
            for (ParaVarInfo p : paras) {
                paraMap.put(p.getCode(), p);
            }
        }
    }

    private void updatePartMap() {
        partMap.clear();
        if (parts != null) {
            for (PartVarInfo p : parts) {
                partMap.put(p.getCode(), p);
            }
        }
    }

    /**
     * 根据代码获取参数信息
     * 
     * @param code 参数代码
     * @return 参数信息，如果不存在则返回null
     */
    public ParaVarInfo getPara(String code) {
        return paraMap.get(code);
    }

    /**
     * 根据代码获取部件信息
     * 
     * @param code 部件代码
     * @return 部件信息，如果不存在则返回null
     */
    public PartVarInfo getPart(String code) {
        return partMap.get(code);
    }

    public boolean hasPara(String code) {
        return paraMap.containsKey(code);
    }

    public boolean hasPart(String code) {
        return partMap.containsKey(code);
    }

    public List<PartVarInfo> getChildrenPart(String fatherCode) {
        if (parts == null || fatherCode == null) {
            return new ArrayList<>();
        }
        return parts.stream().filter(p -> fatherCode.equals(p.getFatherCode())).collect(Collectors.toList());
    }

    public List<PartVarInfo> getTopLevelParts() {
        if (parts == null) {
            return new ArrayList<>();
        }
        return parts.stream().filter(p -> p.getFatherCode() == null || p.getFatherCode().isEmpty())
                .collect(Collectors.toList());
    }
}