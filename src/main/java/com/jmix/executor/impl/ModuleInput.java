package com.jmix.executor.impl;

import com.jmix.executor.cmodel.ErrorInfo;
import com.jmix.executor.cmodel.ParaInst;
import com.jmix.executor.cmodel.PartInst;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2026-4-17
 */
@Data
public class ModuleInput implements IModuleInput {

    /**
     * 模块ID
     */
    private Long moduleId;

    /**
     * 模块编码
     */
    private String moduleCode;

    /**
     * 主部件实例
     */
    private PartInst mainPartInst;

    /**
     * 前置参数实例列表
     */
    private List<ParaInst> preParaInsts;

    /**
     * 前置部件实例列表
     */
    private List<PartInst> prePartInsts;

    /**
     * 是否枚举所有解, false，仅返回一个可行解，true，则返回所有的可选解
     */
    private boolean enumerateAllSolution = false;

    /**
     * 最大solution的个数
     */
    private int maxSolutionNum = 10;

    /**
     * 部件分类输入列表
     */
    private List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();

    /**
     * 部件分类过滤错误信息映射，key 为 partCategoryCode
     */
    private Map<String, ErrorInfo> partCategoryErrorInfoMap = new LinkedHashMap<>();

    /**
     * 部件分类实例列表
     */
    // private List<PartCategoryInst> partCategorys = new ArrayList<>();
}
