package com.jmix.executor.impl.algmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jmix.executor.bmodel.Part;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * Test for PartAlgCPLinearExpr: build two expressions using addTerm and verify
 * the string/template parts.
 */
@Slf4j
public class PartAlgCPLinearExprTest {
    AlgCPModel model = new AlgCPModel();

    private PartVar ofPart(String partCode) {
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
    public void testAddTermConstant_basic() {
        // prepare PartVars with test bases
        PartVar pv1 = ofPart("P1");
        PartVar pv2 = ofPart("P2");

        PartAlgCPLinearExpr expr1 = new PartAlgCPLinearExpr("expr1");
        expr1.addTerm(pv1, pv1.getQty(), 1);
        expr1.addTerm(pv2, pv2.getQty(), 3);
        expr1.addConstant(5);
        log.info(expr1.toString());

        assertEquals("1*P1.Q + 3*P2.Q + 5", expr1.getExprStr());
        assertEquals("1*%d + 3*%d + 5", expr1.getExprTemplate());
        assertEquals("1*P1.Q_%d + 3*P2.Q_%d + 5", expr1.getExprTemplateStr());
        assertEquals("P1,P2", expr1.getPartTermsStr());
    }

    @Test
    public void testAddTermConstant_negative() {
        // prepare PartVars with test bases
        PartVar pv1 = ofPart("P1");
        PartVar pv2 = ofPart("P2");

        PartAlgCPLinearExpr expr1 = new PartAlgCPLinearExpr("expr1");
        expr1.addTerm(pv1, pv1.getQty(), -1);
        expr1.addTerm(pv2, pv2.getQty(), -3);
        expr1.addConstant(5);
        log.info(expr1.toString());

        assertEquals("-1*P1.Q - 3*P2.Q + 5", expr1.getExprStr());
        assertEquals("-1*%d - 3*%d + 5", expr1.getExprTemplate());
        assertEquals("-1*P1.Q_%d - 3*P2.Q_%d + 5", expr1.getExprTemplateStr());
        assertEquals("P1,P2", expr1.getPartTermsStr());
    }

    @Test
    public void testAddExpr() {
        // prepare PartVars with test bases
        PartVar pv1 = ofPart("P1");
        PartVar pv2 = ofPart("P2");
        PartVar pv3 = ofPart("P3");

        PartAlgCPLinearExpr expr1 = new PartAlgCPLinearExpr("expr1");
        expr1.addTerm(pv1, pv1.getQty(), 1);
        expr1.addTerm(pv2, pv2.getQty(), 3);
        expr1.addConstant(5);
        log.info(expr1.toString());

        PartAlgCPLinearExpr expr2 = new PartAlgCPLinearExpr("expr2");
        expr2.addTerm(pv3, pv1.getQty(), -30);
        log.info(expr2.toString());

        PartAlgCPLinearExpr expr = new PartAlgCPLinearExpr("expr");
        expr.addExpr(expr1, 1);
        expr.addExpr(expr2, -30);
        log.info(expr.toString());

        assertEquals("1*(1*P1.Q + 3*P2.Q + 5) - 30*(-30*P3.Q)", expr.getExprStr());
        assertEquals("1*(1*%d + 3*%d + 5) - 30*(-30*%d)", expr.getExprTemplate());
        assertEquals("1*(1*P1.Q_%d + 3*P2.Q_%d + 5) - 30*(-30*P3.Q_%d)", expr.getExprTemplateStr());
        assertEquals(3, expr.getPartTerms().size());
        assertEquals("P1,P2,P3", expr.getPartTermsStr());
    }

    @Test
    public void testAddExprNest() {
        // prepare PartVars with test bases
        PartVar pv1 = ofPart("P1");
        PartVar pv2 = ofPart("P2");
        PartVar pv3 = ofPart("P3");

        PartAlgCPLinearExpr expr1 = new PartAlgCPLinearExpr("expr1");
        expr1.addTerm(pv1, pv1.getQty(), 1);
        expr1.addTerm(pv2, pv2.getQty(), 3);
        expr1.addConstant(5);
        log.info(expr1.toString());

        PartAlgCPLinearExpr expr2 = new PartAlgCPLinearExpr("expr2");
        expr2.addTerm(pv3, pv1.getQty(), -30);
        log.info(expr2.toString());

        PartAlgCPLinearExpr expr11 = new PartAlgCPLinearExpr("expr11");
        expr11.addExpr(expr1, 11);
        expr11.addExpr(expr2, -30);
        log.info(expr11.toString());

        PartAlgCPLinearExpr expr12 = new PartAlgCPLinearExpr("expr12");
        expr12.addConstant(100);
        expr12.addExpr(expr11, 500);
        log.info(expr12.toString());

        assertEquals("100 + 500*(11*(1*P1.Q + 3*P2.Q + 5 - 30*P3.Q))", expr12.getExprStr());
        assertEquals("100 + 500*(11*(1*%d + 3*%d + 5 - 30*%d))", expr12.getExprTemplate());
        assertEquals("100 + 500*(11*(1*P1.Q_%d + 3*P2.Q_%d + 5 - 30*P3.Q_%d))", expr12.getExprTemplateStr());
        assertEquals("P1,P2,P3", expr12.getPartTermsStr());
    }
    // TODO 是有其它场景
}
