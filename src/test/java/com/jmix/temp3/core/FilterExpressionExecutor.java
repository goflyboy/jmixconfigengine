package com.jmix.temp3.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式执行器（简化实现）
 * 支持条件过滤并记录日志
 */
final public class FilterExpressionExecutor {
    private FilterExpressionExecutor() {
        // 工具类，禁止实例化
    }

    public static List<Part> doSelect(List<Part> parts, String condition) {
        System.out.println(
                "FilterExpressionExecutor: Filtering " + parts.size() + " parts with condition: '" + condition + "'");

        if (condition == null || condition.trim().isEmpty()) {
            System.out.println("FilterExpressionExecutor: No condition specified, returning all parts");
            return new ArrayList<>(parts);
        }

        // 简化解析：只处理 "Speed = 5400" 这样的条件
        List<Part> filtered = new ArrayList<>();
        String[] tokens = condition.trim().split("\\s+");

        if (tokens.length >= 3 && tokens[1].equals("=")) {
            String attribute = tokens[0];
            String value = tokens[2];

            System.out.println("FilterExpressionExecutor: Applying filter - " + attribute + " = " + value);

            for (Part part : parts) {
                if (attribute.equals("Speed")) {
                    if (String.valueOf(part.speed).equals(value)) {
                        filtered.add(part);
                        System.out.println(
                                "FilterExpressionExecutor: Part " + part.code + " matches (speed: " + part.speed + ")");
                    } else {
                        System.out.println("FilterExpressionExecutor: Part " + part.code + " does not match (speed: "
                                + part.speed + ")");
                    }
                }
                // 可以扩展其他属性过滤
            }
        } else {
            System.out.println("FilterExpressionExecutor: Unsupported condition format, returning all parts");
            return new ArrayList<>(parts);
        }

        System.out.println(
                "FilterExpressionExecutor: Filtered " + parts.size() + " parts down to " + filtered.size() + " parts");
        return filtered;
    }
}
