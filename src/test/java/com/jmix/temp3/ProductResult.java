package com.jmix.temp3;

import java.util.ArrayList;
import java.util.List;

/**
 * 解决方案结果类
 */
class ProductResult {

    private List<Solution> solutions;

    public ProductResult(List<Solution> solutions) {
        this.solutions = solutions;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("解").append(i + 1).append(": ");
            Solution sol = solutions.get(i);
            List<String> parts = new ArrayList<>();
            for (PartResult pr : sol.getParts()) {
                if (pr.isSelected()) {
                    parts.add(pr.getCode() + ".qty=" + pr.getQty());
                }
            }
            sb.append(String.join("  ", parts));
            sb.append(" 目标值: ").append(sol.getObjectValue()).append("\n");
        }
        return sb.toString();
    }
}
