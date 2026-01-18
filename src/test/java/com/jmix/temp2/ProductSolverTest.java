package com.jmix.temp2;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * ProductSolver 测试类
 * 
 * @since 2025-01-14
 */
@Slf4j
public class ProductSolverTest {

    /**
     * 用例0：测试点，解读1
     * 输入：strReq = " Capacity >=6 where Speed = 5400"
     * 输出：
     * 解1：sd2.qty=1 //优先匹配高速率容量
     * 解2：sd1.qty=2
     * 解3：sd1.qty=1 md1.qty=3 //增配低速率容量
     */
    @Test
    public void testCase0_CapacityGreaterEqual6Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=6 where Speed = 5400";
        
        ProductResult result = solver.solve(strReq);
        
        log.info("Test Case 0 - Input: {}", strReq);
        log.info("Test Case 0 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 0 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 0);
        } else {
            log.error("Test Case 0 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：
        // 解1：sd2.qty=1 //优先匹配高速率容量
        // 解2：sd1.qty=2
        // 解3：sd1.qty=1 md1.qty=3 //增配低速率容量
    }

    /**
     * 用例1：测试点，解读1
     * 输入：strReq = " Capacity >=5 where Speed = 5400"
     * 输出：
     * 解1：sd1.qty=2
     * 解2：sd1.qty=1 md2.qty=1
     */
    @Test
    public void testCase1_CapacityGreaterEqual5Speed5400() {
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=5 where Speed = 5400";
        
        ProductResult result = solver.solve(strReq);
        
        log.info("Test Case 1 - Input: {}", strReq);
        log.info("Test Case 1 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 1 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 1);
        } else {
            log.error("Test Case 1 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：
        // 解1：sd1.qty=2
        // 解2：sd1.qty=1 md2.qty=1
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
        
        log.info("Test Case 2 - Input: {}", strReq);
        log.info("Test Case 2 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 2 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 2);
        } else {
            log.error("Test Case 2 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
    }

    /**
     * 用例3：测试点，解读2
     * 输入：strReq = " Qty >=2 where Speed = 5400"
     * 输出：sd1.qty=2
     */
    @Test
    public void testCase3_QtyGreaterEqual2Speed5400() {
        ProductSolver solver = new ProductSolver();
        // 注意：用户需求中写的是 "Speed Speed = 5400"，这里修正为 "Speed = 5400"
        String strReq = " Qty >=2 where Speed = 5400";
        
        ProductResult result = solver.solve(strReq);
        
        log.info("Test Case 3 - Input: {}", strReq);
        log.info("Test Case 3 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 3 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 3);
        } else {
            log.error("Test Case 3 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：sd1.qty=2
    }

    /**
     * 用例4：测试点，解读2
     * 输入：strReq = " Qty >=3 where Speed = 5400"
     * 输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
     */
    @Test
    public void testCase4_QtyGreaterEqual3Speed5400() {
        ProductSolver solver = new ProductSolver();
        // 注意：用户需求中写的是 "Speed Speed = 5400"，这里修正为 "Speed = 5400"
        String strReq = " Qty >=3 where Speed = 5400";
        
        ProductResult result = solver.solve(strReq);
        
        log.info("Test Case 4 - Input: {}", strReq);
        log.info("Test Case 4 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 4 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 4);
        } else {
            log.error("Test Case 4 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：sd1.qty=2 md1.qty=1 (固态硬盘容量不够，使用机械硬盘补充)
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
        ProductSolver solver = new ProductSolver();
        String strReq = " Capacity >=5 ";
        
        ProductResult result = solver.solve(strReq);
        
        log.info("Test Case 5 - Input: {}", strReq);
        log.info("Test Case 5 - Success: {}", result.isSuccess());
        if (result.isSuccess()) {
            log.info("Test Case 5 - Solutions count: {}", result.getSolutions().size());
            printSolutions(result.getSolutions(), 5);
        } else {
            log.error("Test Case 5 - Error: {}", result.getErrorMessage());
        }
        
        // 预期输出：
        // 解1：sd2.qty=1
        // 解2：sd1.qty=2
    }

    /**
     * 打印解的结果
     */
    private void printSolutions(List<List<ProductResult.PartVarSolution>> solutions, int testCaseIndex) {
        for (int i = 0; i < solutions.size(); i++) {
            List<ProductResult.PartVarSolution> solution = solutions.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append("Test Case ").append(testCaseIndex).append(" - Solution ").append(i + 1).append(": ");
            
            for (int j = 0; j < solution.size(); j++) {
                ProductResult.PartVarSolution partSolution = solution.get(j);
                if (j > 0) {
                    sb.append(" ");
                }
                sb.append(partSolution.getCode()).append(".qty=").append(partSolution.getQty());
            }
            
            log.info(sb.toString());
        }
    }
}

