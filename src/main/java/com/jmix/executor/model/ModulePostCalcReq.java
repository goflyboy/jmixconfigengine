package com.jmix.executor.model;

import com.jmix.executor.cmodel.ModuleInst;

import lombok.Data;

import java.util.List;

/**
 * 独立后置计算请求
 *
 * @since 2026-05-03
 */
@Data
public class ModulePostCalcReq {

    private Long moduleId;

    private String moduleCode;

    private List<ModuleInst> solutions;
}
