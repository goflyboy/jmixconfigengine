package com.jmix.executor.southinf;

import com.jmix.executor.impl.algmodel.PartAlgCPLinearExpr;

public class PartCategoryFunction extends ConstraintFunction implements IPartCategoryFunction {
    IPartCategoryFunction impl;

    @Override
    public PartAlgCPLinearExpr sum4Selected(String filtedConditionStr) {
        return impl.sum4Selected(filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr sum4Selected(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        return impl.sum4Selected(partCategoryCodesStr, cofAttrCode, filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr sum4Quantity(String filtedConditionStr) {
        return impl.sum4Quantity(filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr sum4Quantity(String partCategoryCodesStr, String cofAttrCode,
            String filtedConditionStr) {
        return impl.sum4Quantity(partCategoryCodesStr, cofAttrCode, filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr sum4Selected(String cofAttrCode, String filtedConditionStr) {
        return impl.sum4Selected(cofAttrCode, filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr sum4Quantity(String cofAttrCode, String filtedConditionStr) {
        return impl.sum4Quantity(cofAttrCode, filtedConditionStr);
    }

    @Override
    public PartAlgCPLinearExpr newPartLinearExpr(String name) {
        return impl.newPartLinearExpr(name);
    }
}
