package com.jmix.ruletrans.sdk;

import java.util.List;

/**
 * Instance-view SDK surface for POST rules.
 */
public final class PostSdkContext implements SdkContext {

    private static final List<String> ALLOWED_APIS = List.of(
            "currentInst()",
            "parameter(code).value()",
            "parameter(code).setValue(value)",
            "part(code)",
            "part(code).setQuantity(quantity)",
            "part(code).setDynAttr(key, value)",
            "partCategory(code)",
            "partCategory(code).parameter(code).setValue(value)",
            "partCategory(code).part(code).setQuantity(quantity)",
            "partCategory(code).part(code).setDynAttr(key, value)",
            "partCategory(code).sumQuantity()",
            "partCategorySum(code)",
            "partCategorySum(code).sumDynAttr4Int(attr)",
            "partCategorySum(code).dynAttr(attr)",
            "partCategorySum(code).dynAttrs(attr)");

    private static final List<String> FORBIDDEN_APIS = List.of(
            "model()",
            "CP variables",
            "partVar(code)",
            "partCategoryVar(code)",
            "para(code)",
            "onlyEnforceIf(...)",
            "model().addLessOrEqual(...)",
            "inCompatible(...)",
            "addCompatibleConstraint...",
            "model().setObjectExpr(...)",
            "updatePriorityObjectFuntion(...)");

    @Override
    public SdkProfile profile() {
        return SdkProfile.POST;
    }

    @Override
    public List<String> allowedApis() {
        return ALLOWED_APIS;
    }

    @Override
    public List<String> forbiddenApis() {
        return FORBIDDEN_APIS;
    }
}
