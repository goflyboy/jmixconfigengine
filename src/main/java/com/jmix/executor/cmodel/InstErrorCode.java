package com.jmix.executor.cmodel;

/**
 * PartCategory 实例错误码常量
 *
 * @since 2026-04-30
 */
public final class InstErrorCode {
    private InstErrorCode() {}

    /** 无错误 */
    public static final int NO_ERROR = 0;

    /** 过滤条件未匹配到任何部件 */
    public static final int FILTER_EMPTY = 1;

    /**
     * 将错误码转换为名称字符串
     */
    public static String toName(int errorCode) {
        return switch (errorCode) {
            case NO_ERROR -> "NO_ERROR";
            case FILTER_EMPTY -> "FILTER_EMPTY";
            default -> "UNKNOWN";
        };
    }
}
