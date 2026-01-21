package com.jmix.temp3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProductSolver 测试类
 * 
 * @since 2025-01-17
 */
public class ProductSolverTest {

    /**
     * 用例0：测试点，解读1
     * 输入：strReq = " Capacity >=6 where Speed = 5400"
     * 输出：
     * 解1：sd2.qty=1 //优先匹配高速率容量
     * 解2：sd1.qty=2
     * 解3：sd1.qty=1 md1.qty=3 //增配低速率容量
     */
    // @Test
    public void testCase0_CapacityGreaterEqual6Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=6 where Speed = 5400";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 打印前几个解用于调试
        System.out.println("Test Case 0 - Total solutions: " + solutions.size());
        for (int i = 0; i < Math.min(200, solutions.size()); i++) {
            // for (int i = 0; i < Math.min(200, solutions.size()); i++) {
            System.out.println("Solution " + (i + 1) + ": " + solutions.get(i));
        }

        // 验证至少前3个解的顺序和内容
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd2", 1);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }

        if (solutions.size() >= 2) {
            Map<String, Integer> expected2 = new HashMap<>();
            expected2.put("sd1", 2);
            assertSolutionEquals(expected2, solutions.get(1), "Solution 2");
        }

        if (solutions.size() >= 3) {
            Map<String, Integer> expected3 = new HashMap<>();
            expected3.put("sd1", 1);
            expected3.put("md1", 3);
            assertSolutionEquals(expected3, solutions.get(2), "Solution 3");
        }
    }

    /**
     * 用例1：测试点，解读1
     * 输入：strReq = " Capacity >=5 where Speed = 5400"
     * 输出：
     * 解1：sd1.qty=2
     * 解2：sd1.qty=1 md2.qty=1
     */
    // @Test
    public void testCase1_CapacityGreaterEqual5Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=5 where Speed = 5400";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 验证前2个解的顺序和内容
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd1", 2);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }

        if (solutions.size() >= 2) {
            Map<String, Integer> expected2 = new HashMap<>();
            expected2.put("sd1", 1);
            expected2.put("md2", 1);
            assertSolutionEquals(expected2, solutions.get(1), "Solution 2");
        }
    }

    /**
     * 用例2：测试点，解读1
     * 输入：strReq = " Capacity >=7 where Speed = 5400"
     * 输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
     */
    @Test
    public void testCase2_CapacityGreaterEqual7Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=7 where Speed = 5400";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 验证第一个解
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd1", 2);
            expected1.put("md1", 1);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }
    }

    /**
     * 用例3：测试点，解读2
     * 输入：strReq = " Qty >=2 where Speed = 5400"
     * 输出：sd1.qty=2
     */
    @Test
    public void testCase3_QtyGreaterEqual2Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Qty >=2 where Speed = 5400";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 验证第一个解
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd1", 2);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }
    }

    /**
     * 用例4：测试点，解读2
     * 输入：strReq = " Qty >=3 where Speed = 5400"
     * 输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
     */
    @Test
    public void testCase4_QtyGreaterEqual3Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Qty >=3 where Speed = 5400";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 验证第一个解
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd1", 2);
            expected1.put("md1", 1);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }
    }

    /**
     * 用例5：测试点，解读1 - 固态硬盘优先匹配高速率容量
     * 输入：strReq = " Capacity >=5 "
     * 输出：
     * 解1：sd2.qty=1
     * 解2：sd1.qty=2
     */
    // @Test
    public void testCase5_CapacityGreaterEqual5NoSpeedFilter() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=5 ";

        ProductResult result = solver.solve(strReq);
        List<Map<String, Integer>> solutions = result.getSolutions();

        assertFalse(solutions.isEmpty(), "Solutions should not be empty");

        // 验证前2个解的顺序和内容
        if (solutions.size() >= 1) {
            Map<String, Integer> expected1 = new HashMap<>();
            expected1.put("sd2", 1);
            assertSolutionEquals(expected1, solutions.get(0), "Solution 1");
        }

        if (solutions.size() >= 2) {
            Map<String, Integer> expected2 = new HashMap<>();
            expected2.put("sd1", 2);
            assertSolutionEquals(expected2, solutions.get(1), "Solution 2");
        }
    }

    /**
     * 断言解的内容是否相等
     * 
     * @param expected 期望的解
     * @param actual   实际的解
     * @param message  错误消息
     */
    private void assertSolutionEquals(Map<String, Integer> expected, Map<String, Integer> actual, String message) {
        // 创建只包含非零值的期望映射
        Map<String, Integer> expectedFiltered = new HashMap<>();
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            if (entry.getValue() > 0) {
                expectedFiltered.put(entry.getKey(), entry.getValue());
            }
        }

        // 创建只包含非零值的实际映射
        Map<String, Integer> actualFiltered = new HashMap<>();
        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            if (entry.getValue() > 0) {
                actualFiltered.put(entry.getKey(), entry.getValue());
            }
        }

        assertEquals(expectedFiltered.size(), actualFiltered.size(),
                message + " - Solution size mismatch. Expected: " + expectedFiltered + ", Actual: " + actualFiltered);

        for (Map.Entry<String, Integer> entry : expectedFiltered.entrySet()) {
            String key = entry.getKey();
            Integer expectedValue = entry.getValue();
            Integer actualValue = actualFiltered.get(key);
            assertEquals(expectedValue, actualValue,
                    message + " - Part " + key + " quantity mismatch. Expected: " + expectedValue + ", Actual: "
                            + actualValue);
        }
    }
}
