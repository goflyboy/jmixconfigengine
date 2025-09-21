package com.jmix.executor.omodel;

/**
 * 算法加载器异常
 * 当算法加载过程中发生错误时抛出此异常
 * 
 * @since 1.0
 */
public class AlgLoaderException extends RuntimeException {

    /**
     * 构造方法
     */
    public AlgLoaderException() {
        super();
    }

    /**
     * 构造方法
     * 
     * @param message 异常消息
     */
    public AlgLoaderException(String message) {
        super(message);
    }

    /**
     * 构造方法
     * 
     * @param message 异常消息
     * @param cause   原因异常
     */
    public AlgLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造方法
     * 
     * @param cause 原因异常
     */
    public AlgLoaderException(Throwable cause) {
        super(cause);
    }
}
