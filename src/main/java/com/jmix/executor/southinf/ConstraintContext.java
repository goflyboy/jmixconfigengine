package com.jmix.executor.southinf;

import com.jmix.executor.southinf.view.ModuleInstView;

/**
 * Runtime context visible to southbound algorithms.
 */
public interface ConstraintContext {

    AlgorithmDescriptor descriptor();

    ConstraintModel model();

    ConstraintVarRegistry vars();

    ModuleInstView moduleInst();
}
