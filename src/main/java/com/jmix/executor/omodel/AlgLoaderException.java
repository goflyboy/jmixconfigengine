package com.jmix.executor.omodel;

/**
 * Compatibility exception for legacy packaged algorithms.
 */
public class AlgLoaderException extends com.jmix.executor.model.AlgLoaderException {
    public AlgLoaderException(String message) {
        super(message);
    }

    public AlgLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
