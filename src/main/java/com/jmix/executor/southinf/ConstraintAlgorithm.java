package com.jmix.executor.southinf;

/**
 * Stable entry point implemented by southbound constraint algorithms.
 */
public interface ConstraintAlgorithm {

    AlgorithmDescriptor descriptor();

    void bind(ConstraintContext context);
}
