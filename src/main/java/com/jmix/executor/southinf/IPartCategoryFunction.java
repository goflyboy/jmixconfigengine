package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.PartAlgCPLinearExprImpl;

/**
 * PartCategory function interface.
 * 
 * @deprecated Use {@link PartCategoryCPModel} instead.
 */
@Deprecated
public interface IPartCategoryFunction {
    PartAlgCPLinearExprImpl sum4Selected(String filtedConditionStr);
    PartAlgCPLinearExprImpl sum4Quantity(String filtedConditionStr);
}
