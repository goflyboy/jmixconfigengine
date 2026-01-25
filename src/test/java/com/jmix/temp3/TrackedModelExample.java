package com.jmix.temp3;

import com.jmix.temp3.core.CpModelTracker;
import com.jmix.temp3.core.TrackedLinearExpr;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

/**
 * 使用示例：展示如何使用包装类进行CP-SAT建模和日志记录
 */
public class TrackedModelExample {

    public static void main(String[] args) {
        // 加载 OR-Tools 库
        Loader.loadNativeLibraries();

        // 创建模型跟踪器
        CpModelTracker model = new CpModelTracker();

        // 创建变量
        IntVar ssTotalCapacity = model.newIntVar(0, 10000,
                "ssTotalCapacity");
        IntVar mechTotalCapacity = model.newIntVar(0, 20000,
                "mechTotalCapacity");
        IntVar ssdHighSpeedCapacity = model.newIntVar(0, 8000,
                "ssdHighSpeedCapacity");

        // 创建跟踪的目标函数
        TrackedLinearExpr objective = model.newTrackedExpr("Objective");

        // 添加目标项（每个项都有描述）
        objective.addTerm(ssTotalCapacity, -10000);
        objective.addTerm(mechTotalCapacity, 1);
        objective.addTerm(ssdHighSpeedCapacity, -5000);

        // 设置目标函数
        model.setObjective(objective, false, "Storage capacity optimization");

        // 添加约束
        // 创建约束表达式
        TrackedLinearExpr totalCapacityExpr = model.newTrackedExpr("TotalCapacity");
        totalCapacityExpr.addTerm(ssTotalCapacity, 1);
        totalCapacityExpr.addTerm(mechTotalCapacity, 1);

        // 添加容量限制约束
        model.addGreaterOrEqual(totalCapacityExpr, 15000);

        // 打印模型摘要
        model.printModelSummary();

        // 求解
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10);
        CpSolverStatus status = solver.solve(model.getModel());

        // 分析结果
        printSolution(solver, model, status);

    }

    /**
     * 打印求解结果
     */
    private static void printSolution(CpSolver solver, CpModelTracker model, CpSolverStatus status) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SOLUTION RESULTS");
        System.out.println("=".repeat(60));

        System.out.println("Status: " + status);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("\nVariable values:");
            for (String varName : model.getAllVariables().stream()
                    .map(IntVar::getName)
                    .sorted()
                    .toList()) {
                IntVar var = model.getVariable(varName);
                long value = solver.value(var);
                System.out.printf("  %s = %d%n", varName, value);
            }

            System.out.printf("\nObjective value: %.2f%n", solver.objectiveValue());
            System.out.printf("Best bound: %.2f%n", solver.bestObjectiveBound());
        }

        System.out.println("\nSolver statistics:");
        System.out.println("  Conflicts: " + solver.numConflicts());
        System.out.println("  Branches: " + solver.numBranches());
        System.out.println("  Wall time: " + solver.wallTime() + " s");
    }

}
