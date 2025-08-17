package com.jmix.configengine.artifact;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;


/**
 * 模块信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleInfo extends VarInfo<com.jmix.configengine.model.Module> {
    /**
     * 模块编码
     */
    private String code;
    
    /**
     * 参数信息列表
     */
    private List<ParaInfo> paras;
    
    /**
     * 部件信息列表
     */
    private List<PartInfo> parts;
    
    /**
     * 规则信息列表
     */
    private List<RuleInfo> rules;
    
    /**
     * 参数映射表，用于快速查询
     */
    private Map<String, ParaInfo> paraMap = new HashMap<>();
    
    /**
     * 部件映射表，用于快速查询
     */
    private Map<String, PartInfo> partMap = new HashMap<>();
    
    public ModuleInfo() {
        super(); // 调用VarInfo的默认构造函数
    }
    
    /**
     * 构造函数
     */
    public ModuleInfo(com.jmix.configengine.model.Module module) {
        super(module); // 调用VarInfo的构造函数，传入Module（因为Module继承自Extensible）
    }
    
    /**
     * 设置参数列表，同时更新映射表
     */
    public void setParas(List<ParaInfo> paras) {
        this.paras = paras;
        updateParaMap();
    }
    
    /**
     * 设置部件列表，同时更新映射表
     */
    public void setParts(List<PartInfo> parts) {
        this.parts = parts;
        updatePartMap();
    }
    
    /**
     * 更新参数映射表
     */
    private void updateParaMap() {
        paraMap.clear();
        if (paras != null) {
            for (ParaInfo para : paras) {
                paraMap.put(para.getCode(), para);
            }
        }
    }
    
    /**
     * 更新部件映射表
     */
    private void updatePartMap() {
        partMap.clear();
        if (parts != null) {
            for (PartInfo part : parts) {
                partMap.put(part.getCode(), part);
            }
        }
    }
    
    /**
     * 根据编码获取参数信息
     */
    public ParaInfo getPara(String code) {
        return paraMap.get(code);
    }
    
    /**
     * 根据编码获取部件信息
     */
    public PartInfo getPart(String code) {
        return partMap.get(code);
    }
    
    /**
     * 检查是否包含指定编码的参数
     */
    public boolean hasPara(String code) {
        return paraMap.containsKey(code);
    }
    
    /**
     * 检查是否包含指定编码的部件
     */
    public boolean hasPart(String code) {
        return partMap.containsKey(code);
    }
    
    /**
     * 根据父部件编码获取子部件列表
     */
    public List<PartInfo> getChildrenPart(String fatherCode) {
        if (parts == null || fatherCode == null) {
            return new ArrayList<>();
        }
        
        return parts.stream()
                .filter(part -> fatherCode.equals(part.getFatherCode()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有顶级部件（没有父部件的部件）
     */ 
    public List<PartInfo> getTopLevelParts() {
        if (parts == null) {
            return new ArrayList<>();
        }
        
        return parts.stream()
                .filter(part -> part.getFatherCode() == null || part.getFatherCode().isEmpty())
                .collect(Collectors.toList());
    }
} 