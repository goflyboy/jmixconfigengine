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
            "listOf(optionCode...)",
            "ParaVar.option(optionCode).selectedVar()",
            "ParaVar.valueVar()",
            "ParaVar.hiddenVar()",
            "PartVar.quantityVar()",
            "PartVar.hiddenVar()",
            "model().newBoolVar(name)",
            "model().newLinearExpr(name)",
            "model().newPartLinearExpr(name)",
            "PartAlgCPLinearExpr.addExpr(expr, coefficient)",
            "model().sum4Selected(...)",
            "model().sum4Selected(partCategoryCodes, attrCode, filterCondition)",
            "model().sum4Quantity(...)",
            "model().sum4Quantity(partCategoryCodes, attrCode, filterCondition)",
            "model().addBoolAnd(...)",
            "model().addBoolOr(...)",
            "model().addImplication(...)",
            "model().addEquality(...)",
            "model().addDifferent(...)",
            "model().addLessThan(...)",
            "model().addLessOrEqual(...)",
            "model().addGreaterThan(...)",
            "model().addGreaterOrEqual(...)",
            "inCompatible(ruleCode, left, right)",
            "addCompatibleConstraintRequires(ruleCode, leftPara, leftOptions, rightPara, rightOptions)",
            "addCompatibleConstraintInCompatible(ruleCode, leftPara, leftOptions, rightPara, rightOptions)",
            "addCompatibleConstraintCoDependent(ruleCode, leftPara, leftOptions, rightPara, rightOptions)",
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
