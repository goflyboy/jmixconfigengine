package com.jmix.executor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * Cross PartCategory total constraint request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrossCategoryPartCategoryConstraintReq extends PartCategoryConstraintReqBase {

    /**
     * Stable aggregate constraint code.
     */
    private String code;

    /**
     * Logical PartCategory codes participating in this aggregate total.
     */
    private List<String> partCategoryCodes;
}
