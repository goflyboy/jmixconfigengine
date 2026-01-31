package com.jmix.executor.cmodel;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 解决方案结果类
 */
@Data
@NoArgsConstructor
public class SolverResult {

    /**
     * Solver status constants
     */
    public static final String STATUS_OPTIMAL = "OPTIMAL";
    public static final String STATUS_FEASIBLE = "FEASIBLE";

    private List<ModuleInst> solutions = new ArrayList<>();

    private String solverStatus;

    private String message;

    Double objectiveValue;//求解器的opt

    private boolean hasSearchMax = false;// 是否达到搜索的最大值

    private int searchMax = -1;// -1,表示没有设置，搜索的最大值

    private int iterationTimes = 0;// 迭代次数

    /**
     * 是否有解
     * @return
     */
    public boolean hasSolution() {
        return !solutions.isEmpty();
    }

    /**
     * 添加解
     * @param solu
     */
    public void addSolution(ModuleInst solu) {
        this.solutions.add(solu);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("iterationTimes:").append(iterationTimes).append(" searchMax:").append(searchMax)
                .append(" hasSearchMax:").append(hasSearchMax).append("\n");
        sb.append("solutions size:").append(solutions.size()).append("\n");
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("Solu").append(i + 1).append(": ");
            sb.append(solutions.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        sb.append("iterationTimes:").append(iterationTimes).append(" searchMax:").append(searchMax)
                .append(" hasSearchMax:").append(hasSearchMax).append("\n");
        sb.append("solverStatus:").append(solverStatus).append("\n");
        sb.append("solutions size:").append(solutions.size()).append("\n");
        for (int i = 0; i < solutions.size(); i++) {
            sb.append("Solu").append(i + 1).append(": ");
            sb.append(solutions.get(i).toShortString(true)).append("\n");
        }
        return sb.toString();
    }
}
