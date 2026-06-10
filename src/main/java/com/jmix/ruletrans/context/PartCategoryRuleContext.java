package com.jmix.ruletrans.context;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

import java.util.List;
import java.util.Objects;

/**
 * Context for rules scoped to one part category.
 */
public final class PartCategoryRuleContext implements RuleContext {

    private final Module module;
    private final PartCategory category;

    public PartCategoryRuleContext(Module module, PartCategory category) {
        this.module = Objects.requireNonNull(module, "module must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public PartCategory category() {
        return category;
    }

    @Override
    public boolean isModuleLevel() {
        return false;
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public List<PartCategory> targetCategories() {
        return List.of(category);
    }

    @Override
    public String summary() {
        return "PartCategory rule context: module=" + module.getCode() + ", category=" + category.getCode();
    }
}
