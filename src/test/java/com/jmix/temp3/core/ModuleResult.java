package com.jmix.temp3.core;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 解决方案结果类
 */
@Data
public class ModuleResult {

    private List<Solution> solutions = new ArrayList<>();

    private String solverStatus;

    private boolean hasSearchMax = false;// 是否达到搜索的最大值

    private int searchMax = -1;// -1,表示没有设置，搜索的最大值

    private int iterationTimes = 0;// 迭代次数

    public ModuleResult() {
    }

    public List<Solution> getSolutions() {
        return solutions;
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
            sb.append(solutions.get(i).toShortString()).append("\n");
        }
        return sb.toString();
    }
}
