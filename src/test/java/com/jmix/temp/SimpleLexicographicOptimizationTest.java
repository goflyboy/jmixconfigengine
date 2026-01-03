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
 * 简单的字典序优化（严格优先级）示例
 * 
 * 问题：选择x, y, z三个变量，满足约束条件
 * 第一阶段：最大化 x + y（优先目标）
 * 第二阶段：在x+y最大的前提下，最小化 z（次要目标）
 */
public class SimpleLexicographicOptimizationTest {
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

            // 约束条件：x + y + z <= 15
            model.addLessOrEqual(LinearExpr.sum(new IntVar[] { x, y, z }), 15);

            // 约束条件：x + y >= 5
            model.addGreaterOrEqual(LinearExpr.sum(new IntVar[] { x, y }), 5);

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
                System.out.println("Phase 1: No feasible solution found. Status: " + status1);
                return;
            }

            long maxXPlusY = (long) solver1.objectiveValue();
            System.out.println("=== Phase 1: Maximize (x + y) ===");
            System.out.println("Status: " + status1);
            System.out.println("Maximum (x + y): " + maxXPlusY);
            System.out.println("Phase 1 Solution: x=" + solver1.value(x) +
                    ", y=" + solver1.value(y) +
                    ", z=" + solver1.value(z));

            // ========== 第二阶段：在x+y最大的前提下，最小化z ==========
            // 注意：第二阶段会基于新的约束重新完整计算
            // 1. 我们在同一个model对象上添加了新约束：x + y = maxXPlusY
            // 2. 修改了目标函数：从最大化(x+y)改为最小化z
            // 3. 创建新的求解器，重新完整求解（包含所有原有约束 + 新约束）

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
                System.out.println("\n=== Final Solution ===");
                System.out.println("x = " + solver2.value(x));
                System.out.println("y = " + solver2.value(y));
                System.out.println("z = " + solver2.value(z));
                System.out.println("x + y = " + (solver2.value(x) + solver2.value(y)) + " (target: " + maxXPlusY + ")");
                System.out.println("Objective value (z): " + solver2.objectiveValue());
            } else {
                System.out.println("Phase 2: No feasible solution found. Status: " + status2);
            }

        } catch (Exception e) {
            System.err.println("Error during lexicographic optimization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testSimpleLexicographicOptimization() {
        SimpleLexicographicOptimizationTest test = new SimpleLexicographicOptimizationTest();
        test.solveLexicographic();
    }

    public static void main(String[] args) {
        SimpleLexicographicOptimizationTest test = new SimpleLexicographicOptimizationTest();
        test.solveLexicographic();
    }
}
