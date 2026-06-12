package com.jmix.ruleunit;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

/**
 * Single-rule unit facade over the main ModuleConstraintExecutor.
 */
public interface ModuleConstraintExecutor4SingleRule {

    RuleUnitActualResult testAssignment(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testAssignment(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testCompatibility(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testCompatibility(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testPriority(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testPriority(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testPostAssignment(
            Long moduleId,
            Module module,
            RuleUnitInput input);
}
