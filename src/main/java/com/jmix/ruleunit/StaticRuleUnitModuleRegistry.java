package com.jmix.ruleunit;

import com.jmix.executor.bmodel.Module;

/**
 * Simple registry for tests and command-line callers that execute one module.
 */
public final class StaticRuleUnitModuleRegistry implements RuleUnitModuleRegistry {

    private final Module module;

    public StaticRuleUnitModuleRegistry(Module module) {
        this.module = module;
    }

    @Override
    public Module moduleFor(String caseId) {
        return module;
    }
}
