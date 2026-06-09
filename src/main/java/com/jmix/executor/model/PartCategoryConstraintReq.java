package com.jmix.executor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Standard single PartCategory constraint request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PartCategoryConstraintReq extends PartCategoryConstraintReqBase {

    /**
     * 部件分类代码
     */
    private String partCategoryCode;

    /**
     * 生成短字符串表示
     * 格式：partCategoryCode:attrCode attrComparator attrValue where
     * attrWhereCondition
     * 示例："drive:Sum_Capacity >=5 where Speed like %5400%"
     *
     * @return 短字符串表示
     */
    public String toShortString() {
        StringBuilder sb = new StringBuilder();

        if (partCategoryCode != null && !partCategoryCode.isEmpty()) {
            sb.append(partCategoryCode).append(":");
        }

        if (!getEffectiveAggregateConditions().isEmpty()) {
            for (int i = 0; i < getEffectiveAggregateConditions().size(); i++) {
                if (i > 0) {
                    sb.append(" && ");
                }
                sb.append(getEffectiveAggregateConditions().get(i).toShortString());
            }
        }

        if (getAttrWhereCondition() != null && !getAttrWhereCondition().isEmpty()) {
            sb.append(" where ").append(getAttrWhereCondition());
        }

        return sb.toString();
    }
}
