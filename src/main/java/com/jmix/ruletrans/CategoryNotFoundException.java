package com.jmix.ruletrans;

/**
 * Thrown when module-level category identification cannot be mapped back to a module.
 */
public class CategoryNotFoundException extends RuleTransException {

    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
