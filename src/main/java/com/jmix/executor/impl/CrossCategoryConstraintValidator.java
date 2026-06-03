package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.base.BaseType;
import com.jmix.executor.impl.algmodel.ComparisonOperator;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.impl.util.FilterExpressionExecutor.FilterCondition;
import com.jmix.executor.model.CrossCategoryPartCategoryConstraintReq;

import lombok.extern.slf4j.Slf4j;

import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates cross PartCategory aggregate input before CP model creation.
 */
@Slf4j
public final class CrossCategoryConstraintValidator {

    private static final int MIN_CATEGORY_COUNT = 2;

    private CrossCategoryConstraintValidator() {
    }

    public static List<CrossCategoryPartCategoryConstraintReq> validate(
            List<CrossCategoryPartCategoryConstraintReq> reqs, Module module) {
        List<CrossCategoryPartCategoryConstraintReq> normalized = new ArrayList<>();
        if (reqs == null || reqs.isEmpty()) {
            return normalized;
        }
        for (CrossCategoryPartCategoryConstraintReq req : reqs) {
            validateReq(req, module);
            normalized.add(req);
        }
        return normalized;
    }

    private static void validateReq(CrossCategoryPartCategoryConstraintReq req, Module module) {
        if (req == null) {
            throw new IllegalArgumentException("Invalid cross-category total request: request is null");
        }

        List<String> categoryCodes = deduplicateCategoryCodes(req);
        req.setPartCategoryCodes(categoryCodes);
        if (categoryCodes.size() < MIN_CATEGORY_COUNT) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: partCategoryCodes must contain at least two categories");
        }

        String attrCode = normalizeAttrCode(req.getAttrCode());
        if (Strings.isBlank(attrCode)) {
            throw new IllegalArgumentException("Invalid cross-category total request: attrCode is required");
        }
        req.setAttrCode(attrCode);
        validateComparatorAndValue(req);

        for (String categoryCode : categoryCodes) {
            PartCategory category = findCategory(module, categoryCode);
            DynamicAttribute attr = getAttribute(category, attrCode,
                    "Invalid cross-category total request: attr " + attrCode
                            + " is not defined on category " + categoryCode);
            if (!isNumeric(attr)) {
                throw new IllegalArgumentException(
                        "Invalid cross-category total request: attr " + attrCode
                                + " is not numeric on category " + categoryCode);
            }
        }

        List<String> whereAttrs = parseWhereAttrCodes(req.getAttrWhereCondition());
        for (String whereAttr : whereAttrs) {
            for (String categoryCode : categoryCodes) {
                PartCategory category = findCategory(module, categoryCode);
                getAttribute(category, whereAttr,
                        "Invalid cross-category total request: where attr " + whereAttr
                                + " is not defined on category " + categoryCode);
            }
        }
    }

    private static List<String> deduplicateCategoryCodes(CrossCategoryPartCategoryConstraintReq req) {
        if (req.getPartCategoryCodes() == null || req.getPartCategoryCodes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: partCategoryCodes is required");
        }
        Set<String> deduped = new LinkedHashSet<>();
        for (String code : req.getPartCategoryCodes()) {
            if (Strings.isBlank(code)) {
                continue;
            }
            String trimmed = code.trim();
            if (!deduped.add(trimmed)) {
                log.warn("Duplicate category {} ignored in cross-category total request", trimmed);
            }
        }
        return new ArrayList<>(deduped);
    }

    private static PartCategory findCategory(Module module, String categoryCode) {
        PartCategory category = module.getPartCategory(categoryCode);
        if (category == null) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: category " + categoryCode + " does not exist");
        }
        return category;
    }

    private static DynamicAttribute getAttribute(PartCategory category, String attrCode, String errorMessage) {
        try {
            return category.getAttribute(attrCode, category.getDynAttrSchemas());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(errorMessage, ex);
        }
    }

    private static boolean isNumeric(DynamicAttribute attr) {
        if (attr == null || attr.getDynAttrType() == null) {
            return false;
        }
        BaseType baseType = attr.getDynAttrType().getBaseType();
        return baseType == BaseType.INT || baseType == BaseType.FLOAT || baseType == BaseType.DOUBLE;
    }

    private static void validateComparatorAndValue(CrossCategoryPartCategoryConstraintReq req) {
        if (Strings.isBlank(req.getAttrComparator())) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: attrComparator is required");
        }
        ComparisonOperator.fromSymbol(req.getAttrComparator().trim());
        req.setAttrComparator(req.getAttrComparator().trim());

        if (Strings.isBlank(req.getAttrValue())) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: attrValue is required");
        }
        try {
            Integer.parseInt(req.getAttrValue().trim());
            req.setAttrValue(req.getAttrValue().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: attrValue must be an integer", ex);
        }
    }

    private static String normalizeAttrCode(String attrCode) {
        if (Strings.isBlank(attrCode)) {
            return attrCode;
        }
        String trimmed = attrCode.trim();
        if (!trimmed.contains(AttrPara.CODE_SEPARATOR)) {
            return trimmed;
        }
        String[] parts = trimmed.split(AttrPara.CODE_SEPARATOR, 2);
        try {
            AttrParaType.valueOf(parts[0]);
            return parts[1];
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }

    private static List<String> parseWhereAttrCodes(String whereCondition) {
        List<String> attrCodes = new ArrayList<>();
        if (Strings.isBlank(whereCondition)) {
            return attrCodes;
        }
        Optional<FilterCondition> conditionOpt = FilterExpressionExecutor.parseFilterExpression(whereCondition);
        if (!conditionOpt.isPresent()) {
            throw new IllegalArgumentException(
                    "Invalid cross-category total request: invalid where condition " + whereCondition);
        }
        collectAttrCodes(conditionOpt.get(), attrCodes);
        return attrCodes;
    }

    private static void collectAttrCodes(FilterCondition condition, List<String> attrCodes) {
        if (condition.isCompound()) {
            for (FilterCondition subCondition : condition.getSubConditions()) {
                collectAttrCodes(subCondition, attrCodes);
            }
            return;
        }
        if (!Strings.isBlank(condition.getFieldName())) {
            attrCodes.add(condition.getFieldName());
        }
    }
}
