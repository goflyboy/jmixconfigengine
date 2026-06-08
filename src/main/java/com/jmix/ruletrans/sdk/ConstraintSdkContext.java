package com.jmix.ruletrans.sdk;

import java.util.List;

/**
 * Constraint-model SDK surface for non-POST rules.
 */
public final class ConstraintSdkContext implements SdkContext {

    private static final List<String> ALLOWED_APIS = List.of(
            "model()",
            "para(code)",
            "partVar(code)",
            "partCategoryVar(code)",
            "partVars(filter)",
            "model().sum4Selected(...)",
            "model().sum4Quantity(...)",
            "model().addBoolAnd(...)",
            "model().addBoolOr(...)",
            "model().addImplication(...)",
            "model().addEquality(...)",
            "model().addLessOrEqual(...)",
            "inCompatible(ruleCode, left, right)",
            "addCompatibleConstraintRequires(...)",
            "addCompatibleConstraintInCompatible(...)",
            "addCompatibleConstraintCoDependent(...)",
            "addVarAboutHiddenConstraints(...)",
            "model().setObjectExpr(...)",
            "updatePriorityObjectFuntion(ruleCode, expr)");

    private static final List<String> FORBIDDEN_APIS = List.of(
            "currentInst()",
            "parameter(...).setValue(...)",
            "partCategorySum(...)",
            "part(...).setQuantity(...)",
            "part(...).setDynAttr(...)",
            "partCategory(...).parameter(...).setValue(...)",
            "setDynAttr(...)");

    @Override
    public SdkProfile profile() {
        return SdkProfile.CONSTRAINT;
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
