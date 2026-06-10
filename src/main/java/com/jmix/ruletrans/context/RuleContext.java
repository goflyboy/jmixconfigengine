package com.jmix.ruletrans.context;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

import java.util.List;

/**
 * Natural-language rule generation context backed by the existing domain model.
 */
public interface RuleContext {

    boolean isModuleLevel();

    Module module();

    List<PartCategory> targetCategories();

    default List<String> categoryCodes() {
        return targetCategories().stream()
                .map(PartCategory::getCode)
                .toList();
    }

    String summary();
}
