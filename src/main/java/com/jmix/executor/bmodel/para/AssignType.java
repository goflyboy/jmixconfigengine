package com.jmix.executor.bmodel.para;

/**
 * Assignment type for parameters.
 * INPUT: manual input
 * CALC: calculated by formula or algorithm
 *
 * Note: keep codes stable for persistence or serialization if needed.
 */
public enum AssignType {
    INPUT(1),
    CALC(2);

    private final int code;

    AssignType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}


