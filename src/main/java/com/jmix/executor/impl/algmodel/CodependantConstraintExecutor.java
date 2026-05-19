package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.logic.BusinessRelationType;
import com.jmix.executor.bmodel.logic.CodependantRuleSchema;
import com.jmix.executor.bmodel.logic.PairStructRuleSchema;
import com.jmix.executor.bmodel.logic.PartCombination;
import com.jmix.executor.bmodel.logic.PartCombinationType;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleSchema;
import com.jmix.executor.bmodel.logic.TripleStructRuleSchema;
import com.jmix.executor.model.AlgLoaderException;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes structured combination rules against the internal CP model.
 */
@Slf4j
public class CodependantConstraintExecutor {

    private final ModuleAlgImpl moduleAlg;
    private final AlgCPModel model;
    private final StructExprResolver resolver;
    private final StructRuleBuildExpander expander;

    public CodependantConstraintExecutor(ModuleAlgImpl moduleAlg) {
        this.moduleAlg = moduleAlg;
        this.model = moduleAlg.getModel();
        this.resolver = new StructExprResolver(moduleAlg);
        this.expander = new StructRuleBuildExpander(moduleAlg.getModule(), moduleAlg);
    }

    public void execute(Rule rule) {
        RuleSchema rawCode = rule.getRawCode();
        if (rawCode instanceof PairStructRuleSchema pairSchema) {
            executePair(rule, pairSchema);
            return;
        }
        if (rawCode instanceof TripleStructRuleSchema tripleSchema) {
            executeTriple(rule, tripleSchema);
            return;
        }
        CodependantRuleSchema schema = expander.expand(rule);
        executeCodependant(rule.getCode(), schema);
    }

    private void executePair(Rule rule, PairStructRuleSchema schema) {
        if (schema.getRelationType() == null) {
            throw new AlgLoaderException("Pair structured rule relationType cannot be null: " + rule.getCode());
        }
        if (schema.getRelationType() == BusinessRelationType.INCOMPATIBLE) {
            executeCodependant(rule.getCode(), expander.expand(rule));
            return;
        }
        List<PartVarImpl> leftParts = resolver.resolve(schema.getExpr1());
        List<PartVarImpl> rightParts = resolver.resolve(schema.getExpr2());
        addPairRelation(rule.getCode(), schema.getRelationType(), leftParts, rightParts);
    }

    private void executeTriple(Rule rule, TripleStructRuleSchema schema) {
        if (schema.getRelationType() != BusinessRelationType.INCOMPATIBLE) {
            throw new AlgLoaderException("Only ternary incompatible structured rules are supported: "
                    + rule.getCode());
        }
        executeCodependant(rule.getCode(), expander.expand(rule));
    }

    private void executeCodependant(String ruleCode, CodependantRuleSchema schema) {
        validateSchema(ruleCode, schema);
        if (schema.getCombinationType() == PartCombinationType.WHITE) {
            executeWhiteList(ruleCode, schema);
        } else {
            executeBlackList(ruleCode, schema);
        }
    }

    private void executeBlackList(String ruleCode, CodependantRuleSchema schema) {
        int added = 0;
        for (PartCombination combination : schema.getCombinations()) {
            List<PartVarImpl> vars = currentTupleVars(combination.getCodes(schema.getArity()));
            if (vars.size() != schema.getArity()) {
                continue;
            }
            addForbiddenTuple(ruleCode, vars);
            added++;
        }
        log.info("Executed structured blacklist rule: rule={}, constraints={}", ruleCode, added);
    }

    private void executeWhiteList(String ruleCode, CodependantRuleSchema schema) {
        Set<String> allowedTupleKeys = new LinkedHashSet<>();
        for (PartCombination combination : schema.getCombinations()) {
            if (currentTupleVars(combination.getCodes(schema.getArity())).size() == schema.getArity()) {
                allowedTupleKeys.add(StructExprResolver.tupleKey(combination.getCodes(schema.getArity())));
            }
        }

        List<List<PartVarImpl>> dimensions = resolver.currentDimensionParts(schema.getDimensionCategoryCodes());
        List<List<PartVarImpl>> forbiddenTuples = new ArrayList<>();
        collectForbiddenTuples(dimensions, 0, new ArrayList<>(), allowedTupleKeys, forbiddenTuples);
        for (List<PartVarImpl> tuple : forbiddenTuples) {
            addForbiddenTuple(ruleCode, tuple);
        }
        log.info("Executed structured whitelist rule: rule={}, allowed={}, forbiddenConstraints={}",
                ruleCode, allowedTupleKeys.size(), forbiddenTuples.size());
    }

    private void collectForbiddenTuples(List<List<PartVarImpl>> dimensions, int index, List<PartVarImpl> tuple,
            Set<String> allowedTupleKeys, List<List<PartVarImpl>> forbiddenTuples) {
        if (index == dimensions.size()) {
            List<String> codes = tuple.stream().map(PartVarImpl::getCode).toList();
            if (!allowedTupleKeys.contains(StructExprResolver.tupleKey(codes))) {
                forbiddenTuples.add(new ArrayList<>(tuple));
            }
            return;
        }
        for (PartVarImpl partVar : dimensions.get(index)) {
            tuple.add(partVar);
            collectForbiddenTuples(dimensions, index + 1, tuple, allowedTupleKeys, forbiddenTuples);
            tuple.remove(tuple.size() - 1);
        }
    }

    private void addPairRelation(String ruleCode, BusinessRelationType relationType, List<PartVarImpl> leftParts,
            List<PartVarImpl> rightParts) {
        switch (relationType) {
            case REQUIRES -> {
                for (PartVarImpl left : leftParts) {
                    for (PartVarImpl right : rightParts) {
                        model.addImplication(left.getIsSelected(), right.getIsSelected());
                    }
                }
            }
            case CO_DEPENDENT -> {
                for (PartVarImpl left : leftParts) {
                    for (PartVarImpl right : rightParts) {
                        model.addEquality(left.getIsSelected(), right.getIsSelected());
                    }
                }
            }
            case INCOMPATIBLE -> throw new AlgLoaderException("Unexpected incompatible relation path: " + ruleCode);
            default -> throw new AlgLoaderException("Unsupported pair structured relation: " + relationType);
        }
        log.info("Executed pair structured relation rule: rule={}, relation={}, left={}, right={}",
                ruleCode, relationType, leftParts.size(), rightParts.size());
    }

    private List<PartVarImpl> currentTupleVars(List<String> partCodes) {
        List<PartVarImpl> vars = new ArrayList<>();
        for (String partCode : partCodes) {
            PartVarImpl partVar = findCurrentPartVar(partCode);
            if (partVar == null) {
                return List.of();
            }
            vars.add(partVar);
        }
        return vars;
    }

    private PartVarImpl findCurrentPartVar(String partCode) {
        for (PartCategoryAlgImpl categoryAlg : moduleAlg.getPartCategoryAlgs()) {
            for (PartVarImpl partVar : categoryAlg.getAllPartVars("")) {
                if (partCode.equals(partVar.getCode())) {
                    return partVar;
                }
            }
        }
        return null;
    }

    private void addForbiddenTuple(String ruleCode, List<PartVarImpl> vars) {
        AlgCPLinearExpr expr = model.newLinearExpr(ruleCode + "_tuple_" + tupleName(vars));
        for (PartVarImpl var : vars) {
            expr.addTerm(var.getIsSelected(), 1);
        }
        model.addLessOrEqual(expr, vars.size() - 1);
    }

    private String tupleName(List<PartVarImpl> vars) {
        return String.join("_", vars.stream().map(PartVarImpl::getCode).toList());
    }

    private void validateSchema(String ruleCode, CodependantRuleSchema schema) {
        if (schema == null) {
            throw new AlgLoaderException("Structured executable schema cannot be null: " + ruleCode);
        }
        if (schema.getArity() != 2 && schema.getArity() != 3) {
            throw new AlgLoaderException("Only binary and ternary structured rules are supported: " + ruleCode);
        }
        if (schema.getDimensionCategoryCodes() == null || schema.getDimensionCategoryCodes().size() != schema
                .getArity()) {
            throw new AlgLoaderException("Structured rule dimensions do not match arity: " + ruleCode);
        }
        if (schema.getCombinationType() == null) {
            schema.setCombinationType(PartCombinationType.BLACK);
        }
    }
}
