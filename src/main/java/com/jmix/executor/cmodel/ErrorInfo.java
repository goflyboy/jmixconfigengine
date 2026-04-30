package com.jmix.executor.cmodel;

/**
 * PartCategory 实例错误信息
 *
 * @param errorCode    错误码，参考 {@link InstErrorCode}
 * @param errorMessage 错误描述消息
 * @since 2026-04-30
 */
public record ErrorInfo(int errorCode, String errorMessage) {
}
