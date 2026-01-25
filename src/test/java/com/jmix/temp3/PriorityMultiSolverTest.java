package com.jmix.temp3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.temp3.core.PartResult;
import com.jmix.temp3.core.ProductResult;
import com.jmix.temp3.core.Solution;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * ProductSolver 测试类
 * 
 * @since 2025-01-17
 */
public class PriorityMultiSolverTest {

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
        assertSoluSizeEqual(2);
        // printSolu();
        assertSoluContain(1, "parts=[sd1 Q:2, md1 Q:0], OV=400.0, SS=1");
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
        assertSoluContain(1, "parts=[sd1 Q:2, md1 Q:0], OV=900.0, SS=1");
        assertSoluContain("sd1 Q:1, md1 Q:2");
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
        assertSoluContain(1, "parts=[sd1 Q:2, md1 Q:1], OV=901.0, SS=1");
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
        assertSoluContain(1, "parts=[sd1 Q:2, md1 Q:0]");
        assertSoluContain(2, "parts=[sd1 Q:1, md1 Q:1]");
    }

    /**
     * 用例4：测试点，解读2
     * 输入：strReq = " Qty >=3 where Speed = 5400"
     * 输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
     */
    @Test
    public void testCase4_QtyGreaterEqual3Speed5400() {
        String strReq = " Qty >=3 where Speed = 5400";
        solve(strReq);
        assertSoluContain(1, "parts=[sd1 Q:2, md1 Q:1]");
        assertSoluContain("parts=[sd1 Q:2, md1 Q:2]");
    }

    /**
     * 用例5：测试点，解读1 - 固态硬盘优先匹配高速率容量
     * 输入：strReq = " Capacity >=5 "
     * 输出：
     * 解1：sd2.qty=1
     * 解2：sd1.qty=2
     */
    @Test
    public void testCase5_CapacityGreaterEqual5NoSpeedFilter() {
        String strReq = " Capacity >=5 ";
        solve(strReq);
        assertSoluContain(1, "parts=[sd1 Q:0, sd2 Q:1, sd3 Q:0, md1 Q:0, md2 Q:0, md3 Q:0], OV=400.0, SS=1");
        assertSoluContain("sd1 Q:2, sd2 Q:0, sd3 Q:0, md1 Q:0, md2 Q:0, md3 Q:0");// 这个按objfun也是优先的
        assertSoluContain("sd1 Q:2, sd2 Q:0, sd3 Q:0, md1 Q:0, md2 Q:0, md3 Q:0");
        // 26. - 100 * (3 * sd1_Q + 6 * sd2_Q + 9 * sd3_Q) + 1 * (1 * md1_Q + 2 * md2_Q
        // + 3 * md3_Q) + 500 * (1 * (3 * sd1_Q + 6 * sd2_Q + 9 * sd3_Q + 1 * md1_Q + 2
        // * md2_Q + 3 * md3_Q) - 5) + 500 * (1 * sd1_Q + 1 * sd2_Q + 1 * sd3_Q + 1 *
        // md1_Q + 1 * md2_Q + 1 * md3_Q) <= 2000 (addLessOrEqual) L:ObjectiveFun
    }

    /**
     * 用例5：测试点，解读1 - 固态硬盘优先匹配高速率容量，考察点：但是数量满足的要求下，优先使用容量高-
     * 输入：strReq = " Capacity >=2 "
     * 输出：
     * 解1：sd3.qty=2
     * 解2：sd2.qty=2
     */
    @Test
    public void testCase6_CapacityGreaterEqual5NoSpeedFilter() {
        String strReq = " Qty >=2 ";// 固态硬盘最多配2块
        solve(strReq);
        assertSoluContain(1, "parts=[sd1 Q:0, sd2 Q:0, sd3 Q:2, md1 Q:0, md2 Q:0, md3 Q:0], OV=-1800.0");
        assertSoluContain(2, "parts=[sd1 Q:0, sd2 Q:2, sd3 Q:0, md1 Q:0, md2 Q:0, md3 Q:0]");// 这个按objfun也是优先的
        assertSoluContain(3, "parts=[sd1 Q:0, sd2 Q:0, sd3 Q:1, md1 Q:1, md2 Q:0, md3 Q:0]");
        // 26. - 100 * (3 * sd1_Q + 6 * sd2_Q + 9 * sd3_Q) + 1 * (1 * md1_Q + 2 * md2_Q
        // + 3 * md3_Q) + 500 * (1 * (1 * sd1_Q + 1 * sd2_Q + 1 * sd3_Q + 1 * md1_Q + 1
        // * md2_Q + 1 * md3_Q) - 2) <= 1000
    }

    /**
     * 用例5：测试点，解读1 - 固态硬盘优先匹配高速率容量，考察点：但是数量满足的要求下，使用低容量机械硬盘来补充
     * 输入：strReq = " Capacity >=3 " (固态硬盘容量不够，使用机械硬盘补充)
     * 输出：
     * 解1：sd3.qty=2,md1.qty=1
     * 解2：sd3.qty=2,md2.qty=1
     * 解3：sd3.qty=2,md3.qty=1
     */
    @Test
    public void testCase7_CapacityGreaterEqual5NoSpeedFilter() {
        String strReq = " Qty >=3 ";// 固态硬盘最多配2块
        solve(strReq);
        assertSoluContain(1, "parts=[sd1 Q:0, sd2 Q:0, sd3 Q:2, md1 Q:1, md2 Q:0, md3 Q:0], OV=-1799.0");
        assertSoluContain(2, "parts=[sd1 Q:0, sd2 Q:0, sd3 Q:2, md1 Q:0, md2 Q:1, md3 Q:0]");// 这个按objfun也是优先的
        assertSoluContain(3, "parts=[sd1 Q:0, sd2 Q:0, sd3 Q:2, md1 Q:0, md2 Q:0, md3 Q:1]");
        // 26. - 100 * (3 * sd1_Q + 6 * sd2_Q + 9 * sd3_Q) + 1 * (1 * md1_Q + 2 * md2_Q
        // + 3 * md3_Q) + 500 * (1 * (1 * sd1_Q + 1 * sd2_Q + 1 * sd3_Q + 1 * md1_Q + 1
        // * md2_Q + 1 * md3_Q) - 2) <= 1000
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
        PriorityMultiSolver solver = new PriorityMultiSolver();

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

    private void assertSoluContain(int index, String expect) {
        String msg = "Solution at index " + index + " should match expected string";
        assertTrue(result.getSolutions().size() >= index, msg);
        assertStringContains(result.getSolutions().get(index - 1).toShortString(),
                expect,
                msg);
    }

    private void assertStringContains(String real, String expect, String msg) {
        boolean result = real.contains(expect);
        if (!result) {
            assertEquals(expect, real, "contains is false," + msg);
        }
    }

    private void assertSoluContain(String expect) {
        boolean isContain = false;
        for (Solution solu : result.getSolutions()) {
            if (solu.toShortString().contains(expect)) {
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
