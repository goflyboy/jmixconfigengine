package com.jmix.executor.impl.algmodel;

import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;

import lombok.extern.slf4j.Slf4j;

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
            PartCategoryAlgImpl categoryAlg = alg.getPartCategoryAlg(categoryCode);
            if (categoryAlg != null) {
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
        return "aggregate_" + String.join("_", req.getPartCategoryCodes());
    }
}
