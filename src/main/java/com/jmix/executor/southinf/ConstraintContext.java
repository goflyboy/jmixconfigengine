package com.jmix.executor.southinf;

/**
 * Runtime context visible to southbound algorithms.
 * This is a simplified context that delegates to the new southbound interfaces.
 */
public interface ConstraintContext {

    AlgorithmDescriptor descriptor();

    /**
     * @deprecated Use {@link com.jmix.executor.southinf.ModuleCPModel} instead.
     */
    @Deprecated
    ConstraintModel model();

    /**
     * @deprecated Use {@link com.jmix.executor.southinf.ModuleCPModel} instead,
     *             which provides variable lookup methods directly.
     */
    @Deprecated
    ConstraintVarRegistry vars();

    com.jmix.executor.southinf.view.ModuleInstView moduleInst();
}
