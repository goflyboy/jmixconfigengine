package com.jmix.temp3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // 用例0： 测试点，解读1
    // 输入：
    // strReq = " Capacity >=6 where Speed = 5400"
    // 输出：
    // 解1： sd1.qty=2
    // 解2： sd1.qty=1 md1.qty=3 //增配低速率容量
    // ...
    @Test
    public void testCase0_CapacityGreaterEqual6Speed5400() {
        String strReq = " Capacity >=6 where Speed = 5400";
        solve(strReq);
        assertSoluSizeEqual(9);
        // printSolu();
        assertSoluEqual(1, "parts=[sd1 Q:2, md1 Q:0], OV=-600.0");
    }

    /**
     * 用例1：测试点，解读1
     * 输入：strReq = " Capacity >=5 where Speed = 5400"
     * 输出：
     * 解1： sd1.qty=2
     * 解2： sd1.qty=1 md1.qty=2
     */
    @Test
    public void testCase1_CapacityGreaterEqual5Speed5400() {
        String strReq = " Capacity >=5 where Speed = 5400";
        solve(strReq);
        assertSoluEqual(1, "parts=[sd1 Q:2, md1 Q:0], OV=-600.0");
        assertSoluContain("parts=[sd1 Q:1, md1 Q:2], OV=-298.0");
    }

    /**
     * 用例2：测试点，解读1
     * 输入：strReq = " Capacity >=7 where Speed = 5400"
     * 输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
     */
    @Test
    public void testCase2_CapacityGreaterEqual7Speed5400() {
        String strReq = " Capacity >=7 where Speed = 5400";
        solve(strReq);
        assertSoluEqual(1, "parts=[sd1 Q:2, md1 Q:1], OV=-599.0");
    }

    /**
     * 用例3：测试点，解读2
     * 输入：strReq = " Qty >=2 where Speed = 5400"
     * 输出：sd1.qty=2
     */
    @Test
    public void testCase3_QtyGreaterEqual2Speed5400() {
        String strReq = " Qty >=2 where Speed = 5400";
        solve(strReq);
        assertSoluEqual(1, "parts=[sd1 Q:2, md1 Q:1], OV=-599.0");
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
        List<Solution> solutions = result.getSolutions();

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
        List<Solution> solutions = result.getSolutions();

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

    ProductResult result = null;

    /**
     * 求解并打印调试信息
     *
     * @param strReq       需求字符串
     * @param testCaseName 测试用例名称
     * @return 解列表
     */
    private ProductResult solve(String strReq) {
        ProductSolver solver = new ProductSolver();

        result = solver.solve(strReq);
        System.out.println("strReq:" + strReq);
        System.out.println(result.toShortString());
        return result;
    }

    private void printSolu() {
        System.out.println(result.toString());
    }

    private void assertSoluSizeEqual(int size) {
        assertEquals(size, result.getSolutions().size(), "Solutions size should be " + size);
    }

    private void assertSoluEqual(int index, String expect) {
        assertEquals(expect, result.getSolutions().get(index - 1).toShortString(),
                "Solution at index " + index + " should match expected string");
    }

    private void assertSoluContain(String expect) {
        boolean isContain = false;
        for (Solution solu : result.getSolutions()) {
            if (solu.toShortString().equals(expect)) {
                isContain = true;
                break;
            }
        }
        assertTrue(isContain,
                "Solution contain " + expect);
    }

    /**
     * 断言解的内容是否相等
     *
     * @param expected 期望的解
     * @param actual   实际的解
     * @param message  错误消息
     */
    private void assertSolutionEquals(Map<String, Integer> expected, Solution actual, String message) {
        // 创建只包含非零值的期望映射
        Map<String, Integer> expectedFiltered = new HashMap<>();
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            if (entry.getValue() > 0) {
                expectedFiltered.put(entry.getKey(), entry.getValue());
            }
        }

        // 创建只包含选中部件的实际映射
        Map<String, Integer> actualFiltered = new HashMap<>();
        for (PartResult pr : actual.getParts()) {
            if (pr.isSelected()) {
                actualFiltered.put(pr.getCode(), pr.getQty());
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
