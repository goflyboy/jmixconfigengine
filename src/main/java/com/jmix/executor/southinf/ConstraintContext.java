package com.jmix.executor.southinf;

import com.jmix.executor.southinf.view.ModuleInstView;

/**
 * Runtime context visible to southbound algorithms.
 * 
 * @deprecated Use {@link ModuleAlgBase} instead, which provides direct access to
 *             {@link ModuleCPModel} and {@link ModuleInstView}.
 */
@Deprecated
public interface ConstraintContext {

    AlgorithmDescriptor descriptor();

    /**
     * @deprecated Use {@link ModuleAlgBase#model()} instead.
     */
    @Deprecated
    ConstraintModel model();

    /**
     * @deprecated Use {@link ModuleAlgBase#model()} instead, which provides
     *             variable lookup methods directly.
     */
    @Deprecated
    ConstraintVarRegistry vars();

    ModuleInstView moduleInst();
}
