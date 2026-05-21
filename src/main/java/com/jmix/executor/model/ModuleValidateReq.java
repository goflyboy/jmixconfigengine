package com.jmix.executor.model;

import com.jmix.executor.cmodel.ModuleInst;

import lombok.Data;

/**
 * Request for validating an existing module instance.
 */
@Data
public class ModuleValidateReq {
    private Long moduleId;

    private String moduleCode;

    private ModuleInst moduleInst;
}
