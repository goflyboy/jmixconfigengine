package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.StructCompareOperator;
import com.jmix.executor.bmodel.logic.StructExprSchema;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Resolves structured expressions against the current runtime part variables.
 */
@Slf4j
public class StructExprResolver {

    private final ModuleAlgImpl moduleAlg;

    public StructExprResolver(ModuleAlgImpl moduleAlg) {
        this.moduleAlg = moduleAlg;
    }

    public List<PartVarImpl> resolve(StructExprSchema expr) {
        validateExpr(expr);
        PartCategoryAlgImpl categoryAlg = moduleAlg.getPartCategoryAlg(expr.getObjectCode());
        if (categoryAlg == null) {
            log.error("Part category algorithm not found for structured expression: {}", expr.getObjectCode());
            throw new AlgLoaderException("Part category algorithm not found: " + expr.getObjectCode());
        }
        List<PartVarImpl> candidates = categoryAlg.getAllPartVars("");
        List<PartVarImpl> resolved = filter(candidates, expr);
        log.info("Resolved structured expression: category={}, attr={}, operator={}, values={}, count={}",
                expr.getObjectCode(), expr.getAttrCode(), expr.getOperator(), expr.getValues(), resolved.size());
        return resolved;
    }

    public List<String> resolveCodes(StructExprSchema expr) {
        return resolve(expr).stream()
                .map(PartVarImpl::getCode)
                .toList();
    }

    private List<PartVarImpl> filter(List<PartVarImpl> candidates, StructExprSchema expr) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> values = expr.getValues() == null ? List.of() : expr.getValues();
        if (values.isEmpty()) {
            throw new AlgLoaderException("Structured expression values cannot be empty: " + expr.getObjectCode());
        }

        return switch (expr.getOperator()) {
            case IN -> union(candidates, expr, values);
            case NOT_IN -> notIn(candidates, expr, values);
            default -> filterBySingleCondition(candidates, expr, values.get(0));
        };
    }

    private List<PartVarImpl> union(List<PartVarImpl> candidates, StructExprSchema expr, List<String> values) {
        Map<String, PartVarImpl> result = new LinkedHashMap<>();
        for (String value : values) {
            for (PartVarImpl partVar : filterBySingleCondition(candidates, expr, value)) {
                result.put(partVar.getCode(), partVar);
            }
        }
        return new ArrayList<>(result.values());
    }

    private List<PartVarImpl> notIn(List<PartVarImpl> candidates, StructExprSchema expr, List<String> values) {
        List<PartVarImpl> result = new ArrayList<>(candidates);
        for (String value : values) {
            result = filterBySingleCondition(result, expr, value);
        }
        return result;
    }

    private List<PartVarImpl> filterBySingleCondition(List<PartVarImpl> candidates, StructExprSchema expr,
            String value) {
        String filterExpr = toFilterExpr(expr, value);
        return FilterExpressionExecutor.doSelect(candidates, filterExpr);
    }

    private String toFilterExpr(StructExprSchema expr, String value) {
        return switch (expr.getOperator()) {
            case EQ, IN -> expr.getAttrCode() + "=" + value;
            case NE, NOT_IN -> expr.getAttrCode() + "!=" + value;
            case GT -> expr.getAttrCode() + ">" + value;
            case GE -> expr.getAttrCode() + ">=" + value;
            case LT -> expr.getAttrCode() + "<" + value;
            case LE -> expr.getAttrCode() + "<=" + value;
            case LIKE -> expr.getAttrCode() + " like \"" + value + "\"";
            case NOT_LIKE -> expr.getAttrCode() + " not like \"" + value + "\"";
        };
    }

    public List<List<PartVarImpl>> currentDimensionParts(List<String> dimensionCategoryCodes) {
        List<List<PartVarImpl>> dimensions = new ArrayList<>();
        for (String categoryCode : dimensionCategoryCodes) {
            PartCategoryAlgImpl categoryAlg = moduleAlg.getPartCategoryAlg(categoryCode);
            if (categoryAlg == null) {
                log.error("Part category algorithm not found for structured rule dimension: {}", categoryCode);
                throw new AlgLoaderException("Part category algorithm not found: " + categoryCode);
            }
            dimensions.add(categoryAlg.getAllPartVars(""));
        }
        return dimensions;
    }

    public static boolean matches(PartVarImpl partVar, StructExprSchema expr) {
        if (partVar == null || expr == null) {
            return false;
        }
        List<String> values = expr.getValues() == null ? List.of() : expr.getValues();
        return switch (expr.getOperator()) {
            case EQ -> values.stream().anyMatch(value -> compare(partVar, expr.getAttrCode(), value) == 0);
            case NE -> values.stream().allMatch(value -> compare(partVar, expr.getAttrCode(), value) != 0);
            case GT -> !values.isEmpty() && compare(partVar, expr.getAttrCode(), values.get(0)) > 0;
            case GE -> !values.isEmpty() && compare(partVar, expr.getAttrCode(), values.get(0)) >= 0;
            case LT -> !values.isEmpty() && compare(partVar, expr.getAttrCode(), values.get(0)) < 0;
            case LE -> !values.isEmpty() && compare(partVar, expr.getAttrCode(), values.get(0)) <= 0;
            case IN -> values.stream().anyMatch(value -> compare(partVar, expr.getAttrCode(), value) == 0);
            case NOT_IN -> values.stream().noneMatch(value -> compare(partVar, expr.getAttrCode(), value) == 0);
            case LIKE -> !values.isEmpty() && like(getAttr(partVar, expr.getAttrCode()), values.get(0));
            case NOT_LIKE -> !values.isEmpty() && !like(getAttr(partVar, expr.getAttrCode()), values.get(0));
        };
    }

    public static String tupleKey(List<String> codes) {
        return String.join("\u001F", codes);
    }

    public static LinkedHashSet<String> tupleKeys(List<? extends List<String>> tuples) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (List<String> tuple : tuples) {
            keys.add(tupleKey(tuple));
        }
        return keys;
    }

    private static int compare(PartVarImpl partVar, String attrCode, String value) {
        String attrValue = getAttr(partVar, attrCode);
        try {
            double left = Double.parseDouble(attrValue);
            double right = Double.parseDouble(value);
            return Double.compare(left, right);
        } catch (NumberFormatException e) {
            return attrValue.compareTo(value);
        }
    }

    private static String getAttr(PartVarImpl partVar, String attrCode) {
        if ("code".equals(attrCode)) {
            return partVar.getCode();
        }
        if ("fatherCode".equals(attrCode) && partVar.getBase() instanceof Part part) {
            return part.getFatherCode();
        }
        String value = partVar.getAttr(attrCode);
        if (value != null) {
            return value;
        }
        throw new AlgLoaderException("Part attribute not found: part=" + partVar.getCode() + ", attr=" + attrCode);
    }

    private static boolean like(String value, String pattern) {
        String regex = pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("*", "\\*")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("%", ".*")
                .replace("_", ".");
        return value.matches("^" + regex + "$");
    }

    private void validateExpr(StructExprSchema expr) {
        if (expr == null) {
            throw new AlgLoaderException("Structured expression cannot be null");
        }
        if (!RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY.equals(expr.getObjectType())) {
            throw new AlgLoaderException("Only PartCategory structured expressions are supported: "
                    + expr.getObjectType());
        }
        if (expr.getObjectCode() == null || expr.getObjectCode().isEmpty()) {
            throw new AlgLoaderException("Structured expression objectCode cannot be empty");
        }
        if (expr.getAttrCode() == null || expr.getAttrCode().isEmpty()) {
            throw new AlgLoaderException("Structured expression attrCode cannot be empty");
        }
        if (expr.getOperator() == null) {
            throw new AlgLoaderException("Structured expression operator cannot be null");
        }
    }
}
