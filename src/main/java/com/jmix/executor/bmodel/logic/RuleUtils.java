package com.jmix.executor.bmodel.logic;

import com.jmix.executor.bmodel.base.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class RuleUtils {
    public static Pair<List<Rule>, List<Rule>> splitRules(List<Rule> rules) {
        // 单实例规则仅作用在每个实例上
        List<Rule> singleInstRules = rules.stream().filter(rule -> rule.getEffectScope() == EffectScope.SingleInst)
                .collect(Collectors.toList());
        // AllInst作用在所有的实例上
        List<Rule> allInstRules = rules.stream().filter(rule -> rule.getEffectScope() == EffectScope.AllInst)
                .collect(Collectors.toList());
        return Pair.of(singleInstRules, allInstRules);
    }
}
