package com.jmix.temp;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import org.junit.jupiter.api.Test;

/**
 * 字典序优化示例 - 第二阶段有多个可行解的情况
 * 
 * 问题描述：
 * 选择x, y, z三个变量，满足约束条件
 * 第一阶段：最大化 x + y（优先目标）
 * 第二阶段：在x+y最大的前提下，最小化 z（次要目标）
 * 
 * 设计要点：当x+y达到最大值时，z仍然有多个可能的值，这样第二阶段的最小化才有意义
 */
public class LexicographicOptimizationWithMultipleSolutionsTest {
    static {
        Loader.loadNativeLibraries();
    }

    public void solveLexicographic() {
        try {
            CpModel model = new CpModel();

            // 决策变量：x, y, z 取值范围 [0, 10]
            IntVar x = model.newIntVar(0, 10, "x");
            IntVar y = model.newIntVar(0, 10, "y");
            IntVar z = model.newIntVar(0, 10, "z");

            // 约束条件1：x + y + 3*z <= 30
            // 这个约束设计使得当x+y最大时，z仍然有较大的变化空间
            // 因为x+y+3z≤30，当x+y=20（x=10, y=10）时，z可以是0到3之间的值
            // 当x+y=18时，z可以是0到4之间的值
            LinearExprBuilder constraint1 = LinearExpr.newBuilder();
            constraint1.add(x);
            constraint1.add(y);
            constraint1.addTerm(z, 3);
            model.addLessOrEqual(constraint1.build(), 30);

            // 约束条件2：x + y >= 16
            // 确保x+y有一个最小值要求
            model.addGreaterOrEqual(LinearExpr.sum(new IntVar[] { x, y }), 16);

            // 约束条件3：x <= 10, y <= 10（已经在变量定义中）
            // 约束条件4：z >= 0（已经在变量定义中）

            System.out.println("=== Problem Setup ===");
            System.out.println("Variables: x, y, z ∈ [0, 10]");
            System.out.println("Constraints:");
            System.out.println("  1. x + y + 3*z ≤ 30");
            System.out.println("  2. x + y ≥ 16");
            System.out.println("\nOptimization Goals:");
            System.out.println("  Phase 1: Maximize (x + y)");
            System.out.println("  Phase 2: Minimize z (with x+y at maximum)");

            // ========== 第一阶段：优先最大化 x + y ==========
            LinearExprBuilder phase1Obj = LinearExpr.newBuilder();
            phase1Obj.add(x);
            phase1Obj.add(y);
            model.maximize(phase1Obj.build());

            // 求解第一阶段
            CpSolver solver1 = new CpSolver();
            solver1.getParameters().setMaxTimeInSeconds(10);
            CpSolverStatus status1 = solver1.solve(model);

            if (status1 != CpSolverStatus.OPTIMAL && status1 != CpSolverStatus.FEASIBLE) {
                System.out.println("\nPhase 1: No feasible solution found. Status: " + status1);
                return;
            }

            long maxXPlusY = (long) solver1.objectiveValue();
            System.out.println("\n=== Phase 1: Maximize (x + y) ===");
            System.out.println("Status: " + status1);
            System.out.println("Maximum (x + y): " + maxXPlusY);
            System.out.println("Phase 1 Solution: x=" + solver1.value(x)
                    + ", y=" + solver1.value(y)
                    + ", z=" + solver1.value(z));

            // 计算当x+y=maxXPlusY时，z的可能取值范围
            // 约束：x + y + 3*z ≤ 30
            // 当x+y固定时，z的最大值 = (30 - (x+y)) / 3
            long maxPossibleZPhase1 = (30 - maxXPlusY) / 3;
            System.out.println("Note: When x+y=" + maxXPlusY + ", z can vary from 0 to " + maxPossibleZPhase1
                    + " (based on constraint x+y+3z≤30)");

            // ========== 第二阶段：在x+y最大的前提下，最小化z ==========
            // 添加约束：x + y 必须等于第一阶段的最优值
            model.addEquality(LinearExpr.sum(new IntVar[] { x, y }), maxXPlusY);

            // 构建第二阶段目标：最小化 z
            model.minimize(z);

            // 求解第二阶段：基于新的约束和目标函数，重新完整计算
            CpSolver solver2 = new CpSolver();
            solver2.getParameters().setMaxTimeInSeconds(10);
            CpSolverStatus status2 = solver2.solve(model);

            System.out.println("\n=== Phase 2: Minimize z (with x+y=" + maxXPlusY + ") ===");
            System.out.println("Status: " + status2);

            if (status2 == CpSolverStatus.OPTIMAL || status2 == CpSolverStatus.FEASIBLE) {
                System.out.println("Minimum z: " + solver2.value(z));
                System.out.println("\n=== Final Solution (Lexicographic Optimization) ===");
                System.out.println("x = " + solver2.value(x));
                System.out.println("y = " + solver2.value(y));
                System.out.println("z = " + solver2.value(z));
                System.out.println("x + y = " + (solver2.value(x) + solver2.value(y)) + " (target: " + maxXPlusY + ")");
                System.out.println("Objective value (z): " + solver2.objectiveValue());

                // 验证：展示为什么第二阶段有意义
                System.out.println("\n=== Why Phase 2 Matters ===");
                // 计算在当前x+y值下，z的最大可能值
                // 约束：x + y + 3*z ≤ 30，所以 z ≤ (30 - (x+y)) / 3
                long maxPossibleZ = (30 - maxXPlusY) / 3;
                System.out.println("When x+y=" + maxXPlusY + ", the constraint x+y+3z≤30 allows z to be:");
                System.out.println("  - Maximum z: " + maxPossibleZ);
                System.out.println("  - Minimum z: 0");
                System.out.println("  - Phase 2 found optimal z: " + solver2.value(z));
                if (maxPossibleZ > 0) {
                    System.out.println("  - ✓ Phase 2 optimization is meaningful! (z had " + (maxPossibleZ + 1)
                            + " possible values: 0 to " + maxPossibleZ + ")");
                    System.out
                            .println("  - Phase 2 successfully found the minimum z value among all feasible solutions");
                } else {
                    System.out.println("  - Note: In this case, z only has one possible value (0)");
                }
            } else {
                System.out.println("Phase 2: No feasible solution found. Status: " + status2);
            }

        } catch (Exception e) {
            System.err.println("Error during lexicographic optimization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testLexicographicOptimizationWithMultipleSolutions() {
        LexicographicOptimizationWithMultipleSolutionsTest test = new LexicographicOptimizationWithMultipleSolutionsTest();
        test.solveLexicographic();
    }

    public static void main(String[] args) {
        LexicographicOptimizationWithMultipleSolutionsTest test = new LexicographicOptimizationWithMultipleSolutionsTest();
        test.solveLexicographic();
    }
}
