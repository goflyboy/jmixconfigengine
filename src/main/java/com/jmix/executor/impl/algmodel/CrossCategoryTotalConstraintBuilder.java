package com.jmix.executor.impl.algmodel;

import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds cross PartCategory aggregate total constraints from runtime branch data.
 */
@Slf4j
public final class CrossCategoryTotalConstraintBuilder {

    public void build(ModuleAlgImpl alg, List<CrossCategoryPartCategoryConstraintReq> reqs) {
        if (alg == null || reqs == null || reqs.isEmpty()) {
            return;
        }
        for (CrossCategoryPartCategoryConstraintReq req : reqs) {
            PartAlgCPLinearExpr expr = buildExpr(alg, req);
            addComparatorConstraint(alg, expr, req);
        }
    }

    private PartAlgCPLinearExpr buildExpr(ModuleAlgImpl alg, CrossCategoryPartCategoryConstraintReq req) {
        String aggregateCode = aggregateCode(req);
        PartAlgCPLinearExpr expr = alg.getModel().newPartLinearExpr(aggregateCode + "_expr");

        for (String categoryCode : req.getPartCategoryCodes()) {
            int beforeTerms = expr.getPartTerms().size();
            for (PartCategoryAlgImpl categoryAlg : resolveCategoryAlgs(alg, categoryCode)) {
                alg.buildSumExprInternal(expr, categoryAlg, req.getAttrCode(), PartVarImpl.QTY_SHORT_NAME,
                        PartVarImpl::getQty, req.getAttrWhereCondition());
            }
            int matchedTerms = expr.getPartTerms().size() - beforeTerms;
            log.info("Cross-category total constraint {} matched {} terms in category {}",
                    aggregateCode, matchedTerms, categoryCode);
        }

        if (expr.isEmpty()) {
            expr.addConstant(0);
        }
        return expr;
    }

    private List<PartCategoryAlgImpl> resolveCategoryAlgs(ModuleAlgImpl alg, String categoryCode) {
        List<PartCategoryAlgImpl> result = new ArrayList<>();
        if (Strings.isBlank(categoryCode)) {
            return result;
        }

        PartCategoryAlgImpl categoryAlg = alg.getPartCategoryAlg(categoryCode);
        if (categoryAlg != null) {
            result.add(categoryAlg);
            return result;
        }

        result.addAll(alg.getPartCategoryAlgByInstPrefix(categoryCode));
        return result;
    }

    private void addComparatorConstraint(ModuleAlgImpl alg, PartAlgCPLinearExpr expr,
            CrossCategoryPartCategoryConstraintReq req) {
        String aggregateCode = aggregateCode(req);
        ComparisonOperator operator = ComparisonOperator.fromSymbol(req.getAttrComparator());
        int leftValue = Integer.parseInt(req.getAttrValue());
        alg.getModel().addRuleSeperator(aggregateCode);
        alg.getModel().setRelax4SysRule(aggregateCode);
        operator.applyConstraint(alg.getModel(), expr, leftValue);
        log.info("Added cross-category total constraint {}: {}",
                aggregateCode, operator.getFormulaString(expr.getExprStr(), leftValue));
    }

    private String aggregateCode(CrossCategoryPartCategoryConstraintReq req) {
        String code = req.getCode();
        if (Strings.isBlank(code)) {
            code = "cross_category_total";
        }
        if (code.startsWith("aggregate_")) {
            return code;
        }
        return "aggregate_" + code;
    }
}
