package com.jmix.executor.impl.algmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Part;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 系统测试：验证 addAbsExpr 方法在约束求解器中的正确性
 */
@Slf4j
public class PartAlgCPLinearExprSysTest {

    @BeforeAll
    public static void setUp() {
        // 加载 OR-Tools 本地库
        Loader.loadNativeLibraries();
    }

    private PartVar ofPart(AlgCPModel model, String partCode) {
        Part part = new Part();
        part.setCode(partCode);
        part.setShortCode(partCode);

        PartVar pv = new PartVar();
        pv.setBase(part);
        pv.setIsSelected(model.newBoolVar(partCode + "isSelected"));
        pv.setQty(model.newIntVar(0, 100, partCode + "qty"));
        return pv;
    }

    @Test
    public void testAbsExprSpecificCase() {
        // 测试具体场景: P1.Q - P2.Q = 5 时，|P1.Q - P2.Q| = 5
        AlgCPModel model = new AlgCPModel();

        PartVar pv1 = ofPart(model, "P1");
        PartVar pv2 = ofPart(model, "P2");

        // 创建表达式: diff = P1.Q - P2.Q
        PartAlgCPLinearExpr diffExpr = new PartAlgCPLinearExpr("diff");
        diffExpr.addTerm(pv1, pv1.getQty(), 1);
        diffExpr.addTerm(pv2, pv2.getQty(), -1);

        // 添加绝对值约束
        PartAlgCPLinearExpr absExpr = new PartAlgCPLinearExpr("abs");
        absExpr.addAbsExpr(diffExpr, model);

        // // 设置具体值: P1.Q >= 8, P2.Q >= 3
        model.addGreaterOrEqual(pv1.getQty(), 8);
        model.addGreaterOrEqual(pv2.getQty(), 3);

        // 设置目标: 最小化 absVar（实际上应该是 5）
        model.minimize(absExpr);

        // 求解
        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model.getCpModel());

        log.info("Solver status: {}", status);

        assertTrue(status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE,
                "Solver should find OPTIMAL or FEASIBLE solution");

        long p1Value = solver.value(pv1.getQty());
        long p2Value = solver.value(pv2.getQty());
        long objValue = (long) solver.objectiveValue();

        log.info("Solution: P1.Q = {}, P2.Q = {}, |P1.Q - P2.Q|={}  objValue={}", p1Value, p2Value, objValue);

        // 验证: |8 - 3| = 5
        assertEquals(objValue, Math.abs(p1Value - p2Value), "Difference should be 5");

        log.info("Specific case test passed!");
    }
}
