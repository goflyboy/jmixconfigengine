package com.jmix.executor.imodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.imodel.rule.PriorityRuleSchema;
import com.jmix.executor.imodel.rule.RuleTypeConstants;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * PriorityConstraint测试类
 * 
 * @since 2025-01-XX
 */
public class PriorityConstraintTest {

    /**
     * 测试PriorityConstraint基本属性设置
     */
    @Test
    public void testPriorityConstraintCreation() {
        PriorityConstraint constraint = new PriorityConstraint();

        // 创建Rule对象
        Rule rule = createTestRule("rule1", PriorityType.SELECT, PriorityStrategy.MAX, "capacity");
        constraint.setRule(rule);
        constraint.setAttrCode("capacity");
        constraint.setExprStr("sd1.S*110 + sd2.S*120 + md1.S*13");
        constraint.setExprTemplate("%d*110 + %d*120 + %d*13");
        constraint.setExprTemplateStr("sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13");

        // 验证属性设置
        assertNotNull(constraint.getRule());
        assertEquals("rule1", constraint.getRule().getCode());
        assertEquals("capacity", constraint.getAttrCode());
        PriorityRuleSchema schema = (PriorityRuleSchema) constraint.getRule().getRawCode();
        assertEquals(PriorityType.SELECT, schema.getPriorityType());
        assertEquals(PriorityStrategy.MAX, schema.getPriorityStrategy());
        assertEquals("sd1.S*110 + sd2.S*120 + md1.S*13", constraint.getExprStr());
        assertEquals("%d*110 + %d*120 + %d*13", constraint.getExprTemplate());
        assertEquals("sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13", constraint.getExprTemplateStr());
    }

    /**
     * 创建测试用的Rule对象
     * 
     * @param ruleCode 规则代码
     * @param type     优先级类型
     * @param strategy 优先级策略
     * @param attrCode 属性代码
     * @return Rule对象
     */
    private Rule createTestRule(String ruleCode, PriorityType type, PriorityStrategy strategy, String attrCode) {
        Rule rule = new Rule();
        rule.setCode(ruleCode);
        rule.setRuleSchemaTypeFullName(RuleTypeConstants.PRIORITY_RULE_FULL_NAME);

        PriorityRuleSchema schema = new PriorityRuleSchema();
        schema.setPriorityType(type);
        schema.setPriorityStrategy(strategy);
        schema.setAttrCode(attrCode);
        schema.setVersion("1.0");

        rule.setRawCode(schema);
        return rule;
    }

    /**
     * 测试PartTerm内部类
     */
    @Test
    public void testPartTermCreation() {
        PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();

        term.setIndex(0);
        term.setPartCode("sd1");
        term.setTermValue(1);

        assertEquals(0, term.getIndex());
        assertEquals("sd1", term.getPartCode());
        assertEquals(1, term.getTermValue());
    }

    /**
     * 测试calcToString方法 - 基本计算
     */
    @Test
    public void testCalcToStringBasic() {
        PriorityConstraint constraint = new PriorityConstraint();
        constraint.setExprTemplate("%d*110 + %d*120 + %d*13");
        constraint.setExprTemplateStr("sd1.S_%d*110 + sd2.S_%d*120 + md1.S_%d*13");

        List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

        PriorityConstraint.PartTerm term1 = new PriorityConstraint.PartTerm();
        term1.setIndex(0);
        term1.setPartCode("sd1");
        term1.setTermValue(1);
        exprVariables.add(term1);

        PriorityConstraint.PartTerm term2 = new PriorityConstraint.PartTerm();
        term2.setIndex(1);
        term2.setPartCode("sd2");
        term2.setTermValue(2);
        exprVariables.add(term2);

        PriorityConstraint.PartTerm term3 = new PriorityConstraint.PartTerm();
        term3.setIndex(2);
        term3.setPartCode("md1");
        term3.setTermValue(3);
        exprVariables.add(term3);

        String result = PriorityConstraint.calcToString(constraint, exprVariables);

        // 验证结果格式：printStr = calcResult
        assertNotNull(result);
        assertTrue(result.contains("sd1.S_1*110"));
        assertTrue(result.contains("sd2.S_2*120"));
        assertTrue(result.contains("md1.S_3*13"));
        assertTrue(result.contains("="));

        // 验证计算结果：1*110 + 2*120 + 3*13 = 110 + 240 + 39 = 389
        assertTrue(result.contains("389"));
    }

    /**
     * 测试calcToString方法 - 包含null值的情况
     */
    @Test
    public void testCalcToStringWithNullValues() {
        PriorityConstraint constraint = new PriorityConstraint();
        constraint.setExprTemplate("%d*110 + %d*120");
        constraint.setExprTemplateStr("sd1.S_%d*110 + sd2.S_%d*120");

        List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

        PriorityConstraint.PartTerm term1 = new PriorityConstraint.PartTerm();
        term1.setIndex(0);
        term1.setPartCode("sd1");
        term1.setTermValue(1);
        exprVariables.add(term1);

        PriorityConstraint.PartTerm term2 = new PriorityConstraint.PartTerm();
        term2.setIndex(1);
        term2.setPartCode("sd2");
        term2.setTermValue(null); // null值应该被替换为0
        exprVariables.add(term2);

        String result = PriorityConstraint.calcToString(constraint, exprVariables);

        assertNotNull(result);
        assertTrue(result.contains("sd1.S_1*110"));
        assertTrue(result.contains("sd2.S_0*120"));
        assertTrue(result.contains("="));

        // 验证计算结果：1*110 + 0*120 = 110
        assertTrue(result.contains("110"));
    }

    /**
     * 测试calcToString方法 - 复杂表达式
     */
    @Test
    public void testCalcToStringComplexExpression() {
        PriorityConstraint constraint = new PriorityConstraint();
        constraint.setExprTemplate("%d*100 + %d*200 + %d*300 + %d*50");
        constraint.setExprTemplateStr("p1.S_%d*100 + p2.S_%d*200 + p3.S_%d*300 + p4.S_%d*50");

        List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();
            term.setIndex(i);
            term.setPartCode("p" + (i + 1));
            term.setTermValue(i + 1);
            exprVariables.add(term);
        }

        String result = PriorityConstraint.calcToString(constraint, exprVariables);

        assertNotNull(result);
        assertTrue(result.contains("p1.S_1*100"));
        assertTrue(result.contains("p2.S_2*200"));
        assertTrue(result.contains("p3.S_3*300"));
        assertTrue(result.contains("p4.S_4*50"));
        assertTrue(result.contains("="));

        // 验证计算结果：1*100 + 2*200 + 3*300 + 4*50 = 100 + 400 + 900 + 200 = 1600
        assertTrue(result.contains("1600"));
    }

    /**
     * 测试calcToString方法 - 单个项
     */
    @Test
    public void testCalcToStringSingleTerm() {
        PriorityConstraint constraint = new PriorityConstraint();
        constraint.setExprTemplate("%d*500");
        constraint.setExprTemplateStr("part1.S_%d*500");

        List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

        PriorityConstraint.PartTerm term = new PriorityConstraint.PartTerm();
        term.setIndex(0);
        term.setPartCode("part1");
        term.setTermValue(5);
        exprVariables.add(term);

        String result = PriorityConstraint.calcToString(constraint, exprVariables);

        assertNotNull(result);
        assertTrue(result.contains("part1.S_5*500"));
        assertTrue(result.contains("="));
        assertTrue(result.contains("2500")); // 5*500 = 2500
    }

    /**
     * 测试calcToString方法 - 空列表
     */
    @Test
    public void testCalcToStringEmptyList() {
        PriorityConstraint constraint = new PriorityConstraint();
        constraint.setExprTemplate("");
        constraint.setExprTemplateStr("");

        List<PriorityConstraint.PartTerm> exprVariables = new ArrayList<>();

        String result = PriorityConstraint.calcToString(constraint, exprVariables);

        assertNotNull(result);
        assertTrue(result.contains("="));
    }

    /**
     * 测试exprVariables列表管理
     */
    @Test
    public void testExprVariablesList() {
        PriorityConstraint constraint = new PriorityConstraint();
        assertNotNull(constraint.getExprVariables());
        assertTrue(constraint.getExprVariables().isEmpty());

        PriorityConstraint.PartTerm term1 = new PriorityConstraint.PartTerm();
        term1.setIndex(0);
        term1.setPartCode("part1");
        term1.setTermValue(10);

        PriorityConstraint.PartTerm term2 = new PriorityConstraint.PartTerm();
        term2.setIndex(1);
        term2.setPartCode("part2");
        term2.setTermValue(20);

        constraint.getExprVariables().add(term1);
        constraint.getExprVariables().add(term2);

        assertEquals(2, constraint.getExprVariables().size());
        assertEquals("part1", constraint.getExprVariables().get(0).getPartCode());
        assertEquals("part2", constraint.getExprVariables().get(1).getPartCode());
    }

    /**
     * 测试不同PriorityType和PriorityStrategy组合
     */
    @Test
    public void testPriorityTypeAndStrategy() {
        // 测试SELECT + MAX
        Rule rule1 = createTestRule("rule1", PriorityType.SELECT, PriorityStrategy.MAX, "attr1");
        PriorityConstraint constraint1 = new PriorityConstraint();
        constraint1.setRule(rule1);
        PriorityRuleSchema schema1 = (PriorityRuleSchema) constraint1.getRule().getRawCode();
        assertEquals(PriorityType.SELECT, schema1.getPriorityType());
        assertEquals(PriorityStrategy.MAX, schema1.getPriorityStrategy());

        // 测试SELECT + MIN
        Rule rule2 = createTestRule("rule2", PriorityType.SELECT, PriorityStrategy.MIN, "attr2");
        PriorityConstraint constraint2 = new PriorityConstraint();
        constraint2.setRule(rule2);
        PriorityRuleSchema schema2 = (PriorityRuleSchema) constraint2.getRule().getRawCode();
        assertEquals(PriorityType.SELECT, schema2.getPriorityType());
        assertEquals(PriorityStrategy.MIN, schema2.getPriorityStrategy());

        // 测试SUMARIZE + MAX
        Rule rule3 = createTestRule("rule3", PriorityType.SUMARIZE, PriorityStrategy.MAX, "attr3");
        PriorityConstraint constraint3 = new PriorityConstraint();
        constraint3.setRule(rule3);
        PriorityRuleSchema schema3 = (PriorityRuleSchema) constraint3.getRule().getRawCode();
        assertEquals(PriorityType.SUMARIZE, schema3.getPriorityType());
        assertEquals(PriorityStrategy.MAX, schema3.getPriorityStrategy());

        // 测试SUMARIZE + MIN
        Rule rule4 = createTestRule("rule4", PriorityType.SUMARIZE, PriorityStrategy.MIN, "attr4");
        PriorityConstraint constraint4 = new PriorityConstraint();
        constraint4.setRule(rule4);
        PriorityRuleSchema schema4 = (PriorityRuleSchema) constraint4.getRule().getRawCode();
        assertEquals(PriorityType.SUMARIZE, schema4.getPriorityType());
        assertEquals(PriorityStrategy.MIN, schema4.getPriorityStrategy());
    }
}
