package com.jmix.temp3;

import java.util.List;

/**
 * 解决方案结果类
 */
class ProductResult {

    private List<Solution> solutions;
    private String solverStatus;

    public ProductResult(List<Solution> solutions, String solverStatus) {
        this.solutions = solutions;
        this.solverStatus = solverStatus;
    }

    public List<Solution> getSolutions() {
        return solutions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("solverStatus:").append(solverStatus).append("\n");
        sb.append("solutions size:").append(solutions.size()).append("\n");
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("Solu").append(i + 1).append(": ");
            sb.append(solutions.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append("solverStatus:").append(solverStatus).append("\n");
        sb.append("solutions size:").append(solutions.size()).append("\n");
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("Solu").append(i + 1).append(": ");
            sb.append(solutions.get(i).toShortString()).append("\n");
        }
        return sb.toString();
    }
}
