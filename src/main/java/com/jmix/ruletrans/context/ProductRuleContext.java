package com.jmix.ruletrans.context;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

import java.util.List;
import java.util.Objects;

/**
 * Context for product-level rules, optionally narrowed to identified categories.
 */
public final class ProductRuleContext implements RuleContext {

    private final Module module;
    private final List<PartCategory> selectedCategories;

    public ProductRuleContext(Module module, List<PartCategory> selectedCategories) {
        this.module = Objects.requireNonNull(module, "module must not be null");
        this.selectedCategories = selectedCategories == null ? List.of() : List.copyOf(selectedCategories);
    }

    @Override
    public boolean isProductLevel() {
        return true;
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public List<PartCategory> targetCategories() {
        return selectedCategories;
    }

    @Override
    public String summary() {
        String codes = categoryCodes().isEmpty() ? "<all categories>" : String.join(",", categoryCodes());
        return "Product rule context: module=" + module.getCode() + ", categories=" + codes;
    }
}
