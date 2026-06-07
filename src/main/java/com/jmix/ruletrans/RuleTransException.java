package com.jmix.ruletrans;

/**
 * RuleTrans pipeline exception.
 */
public class RuleTransException extends RuntimeException {

    public RuleTransException(String message) {
        super(message);
    }

    public RuleTransException(String message, Throwable cause) {
        super(message, cause);
    }
}
