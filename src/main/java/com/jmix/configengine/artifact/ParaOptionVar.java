package com.jmix.configengine.artifact;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

/**
 * 参数选项变量
 * 用于表示参数选项的选中状态和相关信息
 */
@Data
public class ParaOptionVar {
    /**
     * 参数选项编码
     */
    public String code;
    
    /**
     * 
     */
    public int codeId;
    
    /**
     * 参数选项的选中状态（布尔变量）
     */
    public BoolVar isSelectedVar;

    /**
     * 默认构造函数
     */
    public ParaOptionVar() {
    }
    
    /**
     * 构造函数
     * @param code 选项编码
     * @param codeId 选项编码ID
     */
    public ParaOptionVar(String code, int codeId) {
        this.code = code;
        this.codeId = codeId;
    }
    
    /**
     * 构造函数
     * @param code 选项编码
     * @param codeId 选项编码ID
     * @param isSelectedVar 选中状态变量
     */
    public ParaOptionVar(String code, int codeId, BoolVar isSelectedVar) {
        this.code = code;
        this.codeId = codeId;
        this.isSelectedVar = isSelectedVar;
    }     
} 