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
    public void testAddTermAndGetParts() {
        // prepare PartVars with test bases
        PartVar pv1 = ofPart("P1");
        PartVar pv2 = ofPart("P2");

        // expr1: coefficient != 1
        PartAlgCPLinearExpr expr1 = new PartAlgCPLinearExpr("expr1");
        expr1.addTerm(pv1, pv1.getQty(), 1);
        expr1.addTerm(pv2, pv2.getQty(), 3);
        log.info(expr1.toString());

        assertEquals("P1.Q*2", expr1.getExprStr());
        assertEquals("%d*2", expr1.getExprTemplate());
        assertEquals("P1.Q_%d*2", expr1.getExprTemplateStr());

    }
}
