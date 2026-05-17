package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;

/**
 * PartCategory function interface.
 * 
 * @deprecated Use {@link PartCategoryCPModel} instead.
 */
@Deprecated
public interface IPartCategoryFunction {
    PartAlgCPLinearExpr sum4Selected(String filtedConditionStr);
    PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr);
}
