package com.jmix.executor.impl;

import com.jmix.executor.model.InferPartCategoryReq;

import lombok.Data;

import java.util.List;

/**
 * 部件约束数据类
 * 用于存储部件约束的相关信息
 *
 * @since 2025-01-XX
 */
@Data
public class ModuleInput implements IModuleInput {

    private List<PartCategoryInputBase> partCategoryInputs;

    private InferPartCategoryReq partCategoryReq; // 后续待整理
}
