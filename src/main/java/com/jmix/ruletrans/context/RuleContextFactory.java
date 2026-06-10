package com.jmix.ruletrans.context;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.ruletrans.CategoryNotFoundException;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;

import java.util.List;

/**
 * Factory methods for RuleTrans contexts.
 */
public final class RuleContextFactory {

    private RuleContextFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static PartCategoryRuleContext partCategory(Module module, String categoryCode) {
        validateModule(module);
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new IllegalArgumentException("categoryCode must not be blank");
        }
        PartCategory category = module.getPartCategory(categoryCode);
        if (category == null) {
            throw new CategoryNotFoundException("PartCategory not found: " + categoryCode);
        }
        return new PartCategoryRuleContext(module, category);
    }

    public static ModuleRuleContext module(Module module) {
        validateModule(module);
        return new ModuleRuleContext(module, List.of());
    }

    public static ModuleRuleContext module(Module module, List<String> categoryCodes) {
        validateModule(module);
        if (categoryCodes == null || categoryCodes.isEmpty()) {
            return module(module);
        }
        List<PartCategory> categories = categoryCodes.stream()
                .map(code -> findCategory(module, code))
                .toList();
        return new ModuleRuleContext(module, categories);
    }

    public static ModuleRuleContext fromAnnotatedClass(Class<?> algClass, String tempResourcePath) {
        if (algClass == null) {
            throw new IllegalArgumentException("algClass must not be null");
        }
        Module module = ModuleGenneratorByAnno.build(algClass, tempResourcePath);
        module.init();
        return module(module);
    }

    private static void validateModule(Module module) {
        if (module == null) {
            throw new IllegalArgumentException("module must not be null");
        }
        module.init();
    }

    private static PartCategory findCategory(Module module, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new CategoryNotFoundException("PartCategory code must not be blank");
        }
        PartCategory category = module.getPartCategory(code);
        if (category == null) {
            throw new CategoryNotFoundException("PartCategory not found: " + code);
        }
        return category;
    }
}
