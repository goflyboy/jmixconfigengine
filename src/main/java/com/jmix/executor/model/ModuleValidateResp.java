package com.jmix.executor.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result payload for module instance validation.
 */
@Data
public class ModuleValidateResp {
    private boolean valid;

    private List<String> violatedRuleCodes = new ArrayList<>();
}
