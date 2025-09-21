package com.jmix.configengine.inf;

/**
 * 算法执行器异常
 * 当算法执行过程中发生错误时抛出此异常
 * 
 * @author Generated
 * @since 1.0
 */
public class AlgExecutorException extends RuntimeException {

    /**
     * 构造方法
     */
    public AlgExecutorException() {
        super();
    }

    /**
     * 构造方法
     * 
     * @param message 异常消息
     */
    public AlgExecutorException(String message) {
        super(message);
    }

    /**
     * 构造方法
     * 
     * @param message 异常消息
     * @param cause   原因异常
     */
    public AlgExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造方法
     * 
     * @param cause 原因异常
     */
    public AlgExecutorException(Throwable cause) {
        super(cause);
    }
}
