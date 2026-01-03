package com.jmix.temp;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典序优化（严格优先级）示例
 * 
 * 第一阶段：优先最大化SSD使用
 * 第二阶段：在SSD最优的前提下，最小化成本
 */
public class LexicographicOptimizationTest {
    static {
        Loader.loadNativeLibraries();
    }

    static class Part {
        String code;
        String fatherCode;
        Integer maxQuantity = Integer.MAX_VALUE;
        Long price = 0L;
        List<Map<String, String>> instanceGroups = new ArrayList<>();
    }

    private CpModel model;
    private Map<String, IntVar> partVars = new HashMap<>();
    private List<Part> allParts = new ArrayList<>();
    private List<Part> ssdParts = new ArrayList<>();

    public LexicographicOptimizationTest() {
        model = new CpModel();
        initializeParts();
        createDecisionVariables();
        addConstraints();
    }

    private void initializeParts() {
        // 固态硬盘
        Part ssd1 = createSSD("SSD1", Arrays.asList(
                createInstanceGroup("1", "5400", "2T"),
                createInstanceGroup("2", "7200/5400", "1T")));
        ssd1.price = 500L;

        Part ssd2 = createSSD("SSD2", Arrays.asList(
                createInstanceGroup("1", "7200/5400", "4T")));
        ssd2.price = 800L;

        Part ssd3 = createSSD("SSD3", Arrays.asList(
                createInstanceGroup("1", "9000", "4T")));
        ssd3.price = 1000L;

        // 机械硬盘
        Part hdd1 = createHDD("HDD1", Arrays.asList(
                createInstanceGroup("1", "5400", "2T")), 4);
        hdd1.price = 200L;

        Part hdd2 = createHDD("HDD2", Arrays.asList(
                createInstanceGroup("4", "7200", "2T")), 4);
        hdd2.price = 250L;

        Collections.addAll(allParts, ssd1, ssd2, ssd3, hdd1, hdd2);
        
        // 收集SSD部件
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                ssdParts.add(part);
            }
        }
    }

    private Part createSSD(String code, List<Map<String, String>> groups) {
        Part ssd = new Part();
        ssd.code = code;
        ssd.fatherCode = "SolidStateDrive";
        ssd.maxQuantity = 2;
        ssd.instanceGroups = groups;
        return ssd;
    }

    private Part createHDD(String code, List<Map<String, String>> groups, int maxQty) {
        Part hdd = new Part();
        hdd.code = code;
        hdd.fatherCode = "MechanicalDrive";
        hdd.maxQuantity = maxQty;
        hdd.instanceGroups = groups;
        return hdd;
    }

    private Map<String, String> createInstanceGroup(String groupId, String speed, String capacity) {
        Map<String, String> group = new HashMap<>();
        group.put("实例分组", groupId);
        group.put("转速", speed);
        group.put("容量", capacity);
        return group;
    }

    private void createDecisionVariables() {
        // 部件数量变量
        for (Part part : allParts) {
            String key = part.code;
            IntVar var = model.newIntVar(0, part.maxQuantity, key + "_count");
            partVars.put(key, var);
        }
    }

    private void addConstraints() {
        // 示例约束：总部件数量不超过10
        LinearExprBuilder totalParts = LinearExpr.newBuilder();
        for (Part part : allParts) {
            totalParts.add(partVars.get(part.code));
        }
        model.addLessOrEqual(totalParts.build(), 10);

        // 示例约束：至少需要2个部件
        model.addGreaterOrEqual(totalParts.build(), 2);
    }

    /**
     * 字典序优化求解
     * 第一阶段：最大化SSD使用
     * 第二阶段：在SSD最优的前提下，最小化成本
     */
    public void solveLexicographic() {
        try {
            // 第一阶段：优先最大化SSD使用
            LinearExprBuilder phase1Obj = LinearExpr.newBuilder();
            for (Part part : ssdParts) {
                phase1Obj.add(partVars.get(part.code));
            }
            model.maximize(phase1Obj.build());

            // 求解第一阶段，记录最优值
            CpSolver solver1 = new CpSolver();
            solver1.getParameters().setMaxTimeInSeconds(30);
            CpSolverStatus status1 = solver1.solve(model);

            if (status1 != CpSolverStatus.OPTIMAL && status1 != CpSolverStatus.FEASIBLE) {
                System.out.println("Phase 1: No feasible solution found. Status: " + status1);
                return;
            }

            long maxSsdCount = (long) solver1.objectiveValue();
            System.out.println("=== Phase 1: Maximize SSD Usage ===");
            System.out.println("Status: " + status1);
            System.out.println("Maximum SSD count: " + maxSsdCount);
            printPhase1Solution(solver1);

            // 第二阶段：在SSD最优的前提下，最小化成本
            // 添加约束：SSD数量必须等于第一阶段的最优值
            LinearExprBuilder ssdVars = LinearExpr.newBuilder();
            for (Part part : ssdParts) {
                ssdVars.add(partVars.get(part.code));
            }
            model.addEquality(ssdVars.build(), maxSsdCount);

            // 构建成本目标：最小化总成本
            LinearExprBuilder phase2Obj = LinearExpr.newBuilder();
            for (Part part : allParts) {
                phase2Obj.addTerm(partVars.get(part.code), part.price);
            }
            model.minimize(phase2Obj.build());

            // 求解第二阶段
            CpSolver solver2 = new CpSolver();
            solver2.getParameters().setMaxTimeInSeconds(30);
            CpSolverStatus status2 = solver2.solve(model);

            System.out.println("\n=== Phase 2: Minimize Cost (with SSD optimal) ===");
            System.out.println("Status: " + status2);
            
            if (status2 == CpSolverStatus.OPTIMAL || status2 == CpSolverStatus.FEASIBLE) {
                System.out.println("Minimum cost: " + solver2.objectiveValue());
                printFinalSolution(solver2, maxSsdCount);
            } else {
                System.out.println("Phase 2: No feasible solution found. Status: " + status2);
            }

        } catch (Exception e) {
            System.err.println("Error during lexicographic optimization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printPhase1Solution(CpSolver solver) {
        System.out.println("\nPhase 1 Solution:");
        for (Part part : allParts) {
            long count = solver.value(partVars.get(part.code));
            if (count > 0) {
                System.out.println("  " + part.code + ": " + count + " units");
            }
        }
    }

    private void printFinalSolution(CpSolver solver, long maxSsdCount) {
        System.out.println("\n=== Final Solution (Lexicographic Optimization) ===");
        
        long totalCost = 0;
        long actualSsdCount = 0;
        
        System.out.println("\nSSD Parts:");
        for (Part part : ssdParts) {
            long count = solver.value(partVars.get(part.code));
            if (count > 0) {
                long cost = count * part.price;
                totalCost += cost;
                actualSsdCount += count;
                System.out.println("  " + part.code + ": " + count + " units, price: " + part.price + ", cost: " + cost);
            }
        }
        
        System.out.println("\nHDD Parts:");
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                long count = solver.value(partVars.get(part.code));
                if (count > 0) {
                    long cost = count * part.price;
                    totalCost += cost;
                    System.out.println("  " + part.code + ": " + count + " units, price: " + part.price + ", cost: " + cost);
                }
            }
        }
        
        System.out.println("\n=== Summary ===");
        System.out.println("SSD count: " + actualSsdCount + " (target: " + maxSsdCount + ")");
        System.out.println("Total cost: " + totalCost);
        System.out.println("Objective value: " + solver.objectiveValue());
    }

    @Test
    public void testLexicographicOptimization() {
        LexicographicOptimizationTest test = new LexicographicOptimizationTest();
        test.solveLexicographic();
    }

    public static void main(String[] args) {
        LexicographicOptimizationTest test = new LexicographicOptimizationTest();
        test.solveLexicographic();
    }
}

