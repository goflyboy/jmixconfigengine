package com.jmix.executor.model;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;

import lombok.Data;

/**
 * Aggregate condition carried by one PartCategory request.
 */
@Data
public class AggregateConditionReq {

    private AttrParaType attrType = AttrParaType.Sum;

    private String attrCode;

    private String comparator;

    private String attrValue;

    private boolean defaulted;

    public String toShortString() {
        StringBuilder builder = new StringBuilder();
        builder.append(attrType.name())
                .append(AttrPara.CODE_SEPARATOR)
                .append(attrCode);
        if (comparator != null && !comparator.isEmpty()) {
            builder.append(" ").append(comparator);
        }
        if (attrValue != null && !attrValue.isEmpty()) {
            builder.append(attrValue);
        }
        return builder.toString();
    }
}
