package com.jmix.configengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Module extends ProgramableObject<Integer> {
    /**
     * 版本信息
     */
    private Long id = 0L;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 模块类型
     */
    private ModuleType type;
    
    /**
     * 参数列表
     */
    private List<Para> paras;
    
    /**
     * 部件列表
     */
    private List<Part> parts;
    
    /**
     * 规则列表
     */
    private List<Rule> rules;
    
    @JsonIgnore
    private Map<String, Para> paraMap = new HashMap<>();
    
    @JsonIgnore
    private Map<String, Part> partMap = new HashMap<>();
    
    @JsonIgnore
    private Map<String, Object> errorMap = new HashMap<>();
    
    /**
     * 初始化方法，建立映射关系提升效率
     */
    public void init() {
        if (paras != null) {
            for (Para para : paras) {
                paraMap.put(para.getCode(), para);
            }
        }
        
        if (parts != null) {
            for (Part part : parts) {
                partMap.put(part.getCode(), part);
            }
        }
    }
} 