package com.jmix.temp3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解决方案结果类
 */
class ProductResult {
    private List<Map<String, Integer>> solutions;

    public ProductResult(List<Map<String, Integer>> solutions) {
        this.solutions = solutions;
    }

    public List<Map<String, Integer>> getSolutions() {
        return solutions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("解").append(i + 1).append(": ");
            Map<String, Integer> sol = solutions.get(i);
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sol.entrySet()) {
                if (entry.getValue() > 0) {
                    parts.add(entry.getKey() + ".qty=" + entry.getValue());
                }
            }
            sb.append(String.join("  ", parts)).append("\n");
        }
        return sb.toString();
    }
}
