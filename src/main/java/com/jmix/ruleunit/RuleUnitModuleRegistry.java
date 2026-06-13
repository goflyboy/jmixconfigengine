package com.jmix.ruleunit;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

/**
 * Resolves product models needed by business JSON case execution.
 */
public interface RuleUnitModuleRegistry {

    Module moduleFor(String caseId);

    default PartCategory partCategoryFor(String caseId, String categoryCode) {
        Module module = moduleFor(caseId);
        if (module == null || categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        return module.getPartCategory(categoryCode);
    }
}
