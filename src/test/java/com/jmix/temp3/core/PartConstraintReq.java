package com.jmix.temp3.core;

import lombok.Data;

/**
 * 请求约束类
 */
@Data
public class PartConstraintReq {
    private String attrCode;
    private String attrComparator;
    private String attrValue;
    private String attrWhereCondition;
}
