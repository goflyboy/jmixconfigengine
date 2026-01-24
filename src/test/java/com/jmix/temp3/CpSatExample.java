package com.jmix.temp3;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

/**
 * 使用示例：展示如何使用包装类进行CP-SAT建模和日志记录
 */
public class CpSatExample {

    public static void main(String[] args) {
        // 加载 OR-Tools 库
        Loader.loadNativeLibraries();

        // 创建模型跟踪器
        CpModelTracker tracker = new CpModelTracker();

        // 创建变量
        IntVar ssTotalCapacity = tracker.newIntVar(0, 10000,
                "Total solid-state storage capacity");
        IntVar mechTotalCapacity = tracker.newIntVar(0, 20000,
                "Total mechanical storage capacity");
        IntVar ssdHighSpeedCapacity = tracker.newIntVar(0, 8000,
                "High-speed SSD capacity");

        // 创建跟踪的目标函数
        TrackedLinearExpr objective = tracker.newTrackedExpr("Objective");

        // 添加目标项（每个项都有描述）
        objective.addTerm(ssTotalCapacity, -10000);
        objective.addTerm(mechTotalCapacity, 1);
        objective.addTerm(ssdHighSpeedCapacity, -5000);

        // 设置目标函数
        tracker.setObjective(objective, false, "Storage capacity optimization");

        // 添加约束
        // 创建约束表达式
        TrackedLinearExpr totalCapacityExpr = tracker.newTrackedExpr("TotalCapacity");
        totalCapacityExpr.addTerm(ssTotalCapacity, 1);
        totalCapacityExpr.addTerm(mechTotalCapacity, 1);

        // 添加容量限制约束
        TrackerConstraint capacityConstraint = tracker.addGreaterOrEqual(totalCapacityExpr, 15000);

        // 打印模型摘要
        tracker.printModelSummary();

        // 求解
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10);
        CpSolverStatus status = solver.solve(tracker.getModel());

        // 分析结果
        printSolution(solver, tracker, status);

        // // 手动计算并验证目标值
        // if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
        // verifyObjective(solver, tracker);
        // }
    }

    /**
     * 打印求解结果
     */
    private static void printSolution(CpSolver solver, CpModelTracker tracker, CpSolverStatus status) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SOLUTION RESULTS");
        System.out.println("=".repeat(60));

        System.out.println("Status: " + status);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("\nVariable values:");
            for (String varName : tracker.getAllVariables().stream()
                    .map(IntVar::getName)
                    .sorted()
                    .toList()) {
                IntVar var = tracker.getVariable(varName);
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

    /**
     * 手动验证目标函数值
     */
    private static void verifyObjective(CpSolver solver, CpModelTracker tracker) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MANUAL OBJECTIVE VERIFICATION");
        System.out.println("=".repeat(60));

        // 手动计算目标函数值（根据我们构建目标函数时的结构）
        long ssValue = solver.value(tracker.getVariable("SS_Total_Capacity"));
        long mechValue = solver.value(tracker.getVariable("Mech_Total_Capacity"));
        long ssdValue = solver.value(tracker.getVariable("SSD_HighSpeed_Capacity"));

        long calculatedObj = (-10000 * ssValue) + (1 * mechValue) + (-5000 * ssdValue);

        double solverObj = solver.objectiveValue();

        System.out.printf("SS_Total_Capacity: %d * -10000 = %d%n", ssValue, -10000 * ssValue);
        System.out.printf("Mech_Total_Capacity: %d * 1 = %d%n", mechValue, mechValue);
        System.out.printf("SSD_HighSpeed_Capacity: %d * -5000 = %d%n", ssdValue, -5000 * ssdValue);
        System.out.printf("Calculated objective: %d%n", calculatedObj);
        System.out.printf("Solver objective: %.0f%n", solverObj);

        if (Math.abs(calculatedObj - solverObj) < 0.1) {
            System.out.println("✓ Objective verification PASSED");
        } else {
            System.out.println("✗ Objective verification FAILED");
        }
    }
}
