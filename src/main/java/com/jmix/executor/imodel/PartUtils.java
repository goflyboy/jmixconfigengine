package com.jmix.executor.imodel;

import java.util.List;

/**
 * 部件工具类
 * 提供部件相关的工具方法
 *
 * @since 2025-01-XX
 */
public final class PartUtils {

    /**
     * 原子部件短字符串中最多显示的代码数量
     */
    private static final int MAX_DISPLAY_CODES = 6;

    /**
     * 私有构造器，防止工具类被实例化
     */
    private PartUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取原子部件的短字符串表示
     * 格式：Size:xx Codes: code1,code2,....（最多MAX_DISPLAY_CODES个代码）
     *
     * @param atomicParts 原子部件列表
     * @return 原子部件的短字符串表示
     */
    public static String toShortString(List<Part> atomicParts) {
        int size = atomicParts.size();
        StringBuilder sb = new StringBuilder();
        sb.append("Size:").append(size);

        if (size > 0) {
            sb.append(" Codes: ");
            int maxCodes = Math.min(size, MAX_DISPLAY_CODES);
            for (int i = 0; i < maxCodes; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                Part part = atomicParts.get(i);
                String code = part.getCode();
                if (code != null && !code.isEmpty()) {
                    sb.append(code);
                }
            }
        }

        return sb.toString();
    }
}
