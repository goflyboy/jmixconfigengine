package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IModule;
import com.jmix.executor.bmodel.logic.CodependantRuleSchema;
import com.jmix.executor.bmodel.logic.CombinationStructRuleSchema;
import com.jmix.executor.bmodel.logic.PairStructRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombination;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleSchema;
import com.jmix.executor.bmodel.logic.StructExprSchema;
import com.jmix.executor.bmodel.logic.TripleStructRuleSchema;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expands maintenance-time structured rules into executable part combinations.
 */
@Slf4j
public class StructRuleBuildExpander {

    private final IModule module;
    private final StructExprResolver resolver;

    public StructRuleBuildExpander(IModule module, ModuleAlgImpl moduleAlg) {
        this.module = module;
        this.resolver = new StructExprResolver(moduleAlg);
    }

    public CodependantRuleSchema expand(Rule rule) {
        if (rule.getExeSchema() instanceof CodependantRuleSchema codependantRuleSchema) {
            return codependantRuleSchema;
        }
        RuleSchema rawCode = rule.getRawCode();
        CodependantRuleSchema schema;
        if (rawCode instanceof CombinationStructRuleSchema combinationSchema) {
            schema = expandCombination(rule, combinationSchema);
        } else if (rawCode instanceof PairStructRuleSchema pairSchema) {
            schema = expandPair(rule, pairSchema);
        } else if (rawCode instanceof TripleStructRuleSchema tripleSchema) {
            schema = expandTriple(rule, tripleSchema);
        } else {
            throw new AlgLoaderException("Unsupported structured rule schema: " + rule.getCode());
        }
        rule.setExeSchema(schema);
        return schema;
    }

    private CodependantRuleSchema expandCombination(Rule parentRule, CombinationStructRuleSchema schema) {
        validateArity(schema.getArity());
        List<String> dimensions = schema.getDimensionCategoryCodes();
        if (dimensions == null || dimensions.size() != schema.getArity()) {
            throw new AlgLoaderException("Combination rule dimensions do not match arity: " + parentRule.getCode());
        }
        List<Rule> subRules = findSubRules(parentRule, schema);
        Map<String, PartCombination> combinations = new LinkedHashMap<>();
        for (Rule subRule : subRules) {
            List<StructExprSchema> exprs = exprsOf(subRule.getRawCode());
            validateSubRule(parentRule, schema, subRule, exprs);
            for (PartCombination combination : expandExprs(exprs, subRule.getCode())) {
                combinations.put(StructExprResolver.tupleKey(combination.getCodes(schema.getArity())), combination);
            }
        }
        CodependantRuleSchema exeSchema = codependant(schema.getArity(), dimensions, schema.getCombinationType());
        exeSchema.setCombinations(new ArrayList<>(combinations.values()));
        log.info("Expanded combination structured rule: rule={}, combinations={}",
                parentRule.getCode(), exeSchema.getCombinations().size());
        return exeSchema;
    }

    private CodependantRuleSchema expandPair(Rule rule, PairStructRuleSchema schema) {
        List<StructExprSchema> exprs = List.of(schema.getExpr1(), schema.getExpr2());
        CodependantRuleSchema exeSchema = codependant(2,
                List.of(schema.getExpr1().getObjectCode(), schema.getExpr2().getObjectCode()),
                PartCombinationType.BLACK);
        exeSchema.setCombinations(expandExprs(exprs, rule.getCode()));
        return exeSchema;
    }

    private CodependantRuleSchema expandTriple(Rule rule, TripleStructRuleSchema schema) {
        List<StructExprSchema> exprs = List.of(schema.getExpr1(), schema.getExpr2(), schema.getExpr3());
        CodependantRuleSchema exeSchema = codependant(3,
                List.of(schema.getExpr1().getObjectCode(), schema.getExpr2().getObjectCode(),
                        schema.getExpr3().getObjectCode()),
                PartCombinationType.BLACK);
        exeSchema.setCombinations(expandExprs(exprs, rule.getCode()));
        return exeSchema;
    }

    private List<PartCombination> expandExprs(List<StructExprSchema> exprs, String sourceRuleCode) {
        List<List<String>> partCodeSets = new ArrayList<>();
        for (StructExprSchema expr : exprs) {
            partCodeSets.add(resolver.resolveCodes(expr));
        }
        List<PartCombination> result = new ArrayList<>();
        expandCartesian(partCodeSets, 0, new ArrayList<>(), sourceRuleCode, result);
        return result;
    }

    private void expandCartesian(List<List<String>> partCodeSets, int index, List<String> tuple,
            String sourceRuleCode, List<PartCombination> result) {
        if (index == partCodeSets.size()) {
            result.add(toCombination(tuple, sourceRuleCode));
            return;
        }
        for (String partCode : partCodeSets.get(index)) {
            tuple.add(partCode);
            expandCartesian(partCodeSets, index + 1, tuple, sourceRuleCode, result);
            tuple.remove(tuple.size() - 1);
        }
    }

    private PartCombination toCombination(List<String> tuple, String sourceRuleCode) {
        if (tuple.size() == 2) {
            return new PartCombination(tuple.get(0), tuple.get(1), sourceRuleCode);
        }
        if (tuple.size() == 3) {
            return new PartCombination(tuple.get(0), tuple.get(1), tuple.get(2), sourceRuleCode);
        }
        throw new AlgLoaderException("Unsupported structured rule arity: " + tuple.size());
    }

    private List<Rule> findSubRules(Rule parentRule, CombinationStructRuleSchema schema) {
        Map<String, Rule> rulesByCode = new LinkedHashMap<>();
        for (Rule rule : module.getAllRules()) {
            rulesByCode.put(rule.getCode(), rule);
        }

        List<Rule> subRules = new ArrayList<>();
        if (schema.getSubRuleCodes() != null && !schema.getSubRuleCodes().isEmpty()) {
            for (String subRuleCode : schema.getSubRuleCodes()) {
                Rule subRule = rulesByCode.get(subRuleCode);
                if (subRule == null) {
                    throw new AlgLoaderException("Combination sub rule not found: " + subRuleCode);
                }
                subRules.add(subRule);
            }
        }
        for (Rule rule : module.getAllRules()) {
            if (parentRule.getCode().equals(rule.getParentRuleCode()) && !subRules.contains(rule)) {
                subRules.add(rule);
            }
        }
        if (subRules.isEmpty()) {
            throw new AlgLoaderException("Combination rule has no sub rules: " + parentRule.getCode());
        }
        return subRules;
    }

    private List<StructExprSchema> exprsOf(RuleSchema schema) {
        if (schema instanceof PairStructRuleSchema pairSchema) {
            return List.of(pairSchema.getExpr1(), pairSchema.getExpr2());
        }
        if (schema instanceof TripleStructRuleSchema tripleSchema) {
            return List.of(tripleSchema.getExpr1(), tripleSchema.getExpr2(), tripleSchema.getExpr3());
        }
        throw new AlgLoaderException("Combination sub rule must be pair or triple structured rule");
    }

    private void validateSubRule(Rule parentRule, CombinationStructRuleSchema schema, Rule subRule,
            List<StructExprSchema> exprs) {
        if (exprs.size() != schema.getArity()) {
            throw new AlgLoaderException("Combination sub rule arity mismatch: " + subRule.getCode());
        }
        for (int i = 0; i < exprs.size(); i++) {
            String actualCategoryCode = exprs.get(i).getObjectCode();
            String expectedCategoryCode = schema.getDimensionCategoryCodes().get(i);
            if (!expectedCategoryCode.equals(actualCategoryCode)) {
                throw new AlgLoaderException("Combination sub rule dimension mismatch: parent="
                        + parentRule.getCode() + ", sub=" + subRule.getCode());
            }
        }
    }

    private CodependantRuleSchema codependant(int arity, List<String> dimensions, PartCombinationType type) {
        validateArity(arity);
        CodependantRuleSchema schema = new CodependantRuleSchema();
        schema.setArity(arity);
        schema.setDimensionCategoryCodes(new ArrayList<>(dimensions));
        schema.setCombinationType(type == null ? PartCombinationType.BLACK : type);
        return schema;
    }

    private void validateArity(int arity) {
        if (arity != 2 && arity != 3) {
            throw new AlgLoaderException("Only binary and ternary structured rules are supported: " + arity);
        }
    }
}
