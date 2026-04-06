package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;

public interface IPartCategoryFunction {
    PartAlgCPLinearExpr newPartLinearExpr(String name);

    PartAlgCPLinearExpr sum4Selected(String filtedConditionStr);

    PartAlgCPLinearExpr sum4Selected(String cofAttrCode, String filtedConditionStr);

    PartAlgCPLinearExpr sum4Selected(String partCategoryCodesStr, String cofAttrCode, String filtedConditionStr);

    PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr);

    PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr);

    PartAlgCPLinearExpr sum4Quantity(String partCategoryCodesStr, String cofAttrCode, String filtedConditionStr);

}