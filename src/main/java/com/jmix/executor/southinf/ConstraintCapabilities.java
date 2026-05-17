package com.jmix.executor.southinf;

/**
 * Capability probe for the active southbound API bridge.
 */
public interface ConstraintCapabilities {

    default boolean supportsPostModuleInstView() {
        return true;
    }
}
