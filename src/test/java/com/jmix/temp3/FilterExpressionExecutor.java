package com.jmix.temp3;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式执行器（简化实现）
 */
final class FilterExpressionExecutor {
    private FilterExpressionExecutor() {
        // 工具类，禁止实例化
    }

    public static List<Part> doSelect(List<Part> parts, String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return new ArrayList<>(parts);
        }

        // 简化解析：只处理 "Speed = 5400" 这样的条件
        List<Part> filtered = new ArrayList<>();
        String[] tokens = condition.trim().split("\\s+");

        if (tokens.length >= 3 && tokens[1].equals("=")) {
            String attribute = tokens[0];
            String value = tokens[2];

            for (Part part : parts) {
                if (attribute.equals("Speed")) {
                    if (String.valueOf(part.speed).equals(value)) {
                        filtered.add(part);
                    }
                }
                // 可以扩展其他属性过滤
            }
        }

        return filtered;
    }
}
