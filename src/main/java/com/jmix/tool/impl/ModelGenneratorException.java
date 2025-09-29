package com.jmix.tool.impl;

/**
 * 模型生成相关的运行时异常。
 * 用于封装模型代码生成、编译、运行流程中的错误。
 */
public class ModelGenneratorException extends RuntimeException {

    public ModelGenneratorException(String message) {
        super(message);
    }

    public ModelGenneratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
