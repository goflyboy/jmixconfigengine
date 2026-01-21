package com.jmix.tool.artifact;

import com.jmix.executor.bmodel.Module;

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
    /**
     * 模块编码
     */
    private String code;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 参数变量信息列表
     */
    private List<ParaVarInfo> paras;

    /**
     * 部件变量信息列表
     */
    private List<PartVarInfo> parts;

    /**
     * 规则信息列表
     */
    private List<RuleInfo> rules;

    /**
     * 参数变量信息映射，key为参数编码
     */
    private Map<String, ParaVarInfo> paraMap = new HashMap<>();

    /**
     * 部件变量信息映射，key为部件编码
     */
    private Map<String, PartVarInfo> partMap = new HashMap<>();

    /**
     * 默认构造函数
     */
    public ModuleVarInfo() {
        super();
    }

    /**
     * 带模块对象的构造函数
     * 
     * @param module 模块对象
     */
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

    /**
     * 检查是否包含指定代码的参数
     * 
     * @param code 参数代码
     * @return 如果包含则返回true，否则返回false
     */
    public boolean hasPara(String code) {
        return paraMap.containsKey(code);
    }

    /**
     * 检查是否包含指定代码的部件
     * 
     * @param code 部件代码
     * @return 如果包含则返回true，否则返回false
     */
    public boolean hasPart(String code) {
        return partMap.containsKey(code);
    }

    /**
     * 获取指定父级代码的子部件列表
     * 
     * @param fatherCode 父级部件代码
     * @return 子部件列表
     */
    public List<PartVarInfo> getChildrenPart(String fatherCode) {
        if (parts == null || fatherCode == null) {
            return new ArrayList<>();
        }
        return parts.stream().filter(p -> fatherCode.equals(p.getFatherCode())).collect(Collectors.toList());
    }

    /**
     * 获取顶级部件列表（没有父级代码的部件）
     * 
     * @return 顶级部件列表
     */
    public List<PartVarInfo> getTopLevelParts() {
        if (parts == null) {
            return new ArrayList<>();
        }
        return parts.stream().filter(p -> p.getFatherCode() == null || p.getFatherCode().isEmpty())
                .collect(Collectors.toList());
    }
}