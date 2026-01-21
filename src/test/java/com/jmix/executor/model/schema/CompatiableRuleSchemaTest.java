package com.jmix.executor.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.rule.CompatiableRuleSchema;

import org.junit.jupiter.api.Test;

/**
 * CompatiableRuleSchema测试类
 */
public class CompatiableRuleSchemaTest {

    @Test
    public void testOperatorConstants() {
        // 验证常量值
        assertEquals("Incompatible", CompatiableRuleSchema.Operator.INCOMPATIBLE);
        assertEquals("CoDependent", CompatiableRuleSchema.Operator.CO_REFENT);
        assertEquals("Requires", CompatiableRuleSchema.Operator.REQUIRES);

        // 验证常量不为空
        assertNotNull(CompatiableRuleSchema.Operator.INCOMPATIBLE);
        assertNotNull(CompatiableRuleSchema.Operator.CO_REFENT);
        assertNotNull(CompatiableRuleSchema.Operator.REQUIRES);

        // 验证常量不为空字符串
        assertFalse(CompatiableRuleSchema.Operator.INCOMPATIBLE.isEmpty());
        assertFalse(CompatiableRuleSchema.Operator.CO_REFENT.isEmpty());
        assertFalse(CompatiableRuleSchema.Operator.REQUIRES.isEmpty());
    }

    @Test
    public void testCompatiableRuleSchemaCreation() {
        // 创建CompatiableRuleSchema实例
        CompatiableRuleSchema rule = new CompatiableRuleSchema();

        // 设置操作符使用常量
        rule.setOperator(CompatiableRuleSchema.Operator.INCOMPATIBLE);
        assertEquals(CompatiableRuleSchema.Operator.INCOMPATIBLE, rule.getOperator());

        // 测试其他操作符
        rule.setOperator(CompatiableRuleSchema.Operator.CO_REFENT);
        assertEquals(CompatiableRuleSchema.Operator.CO_REFENT, rule.getOperator());

        rule.setOperator(CompatiableRuleSchema.Operator.REQUIRES);
        assertEquals(CompatiableRuleSchema.Operator.REQUIRES, rule.getOperator());
    }

    @Test
    public void testOperatorValidation() {
        // 验证操作符常量是有效的
        String[] validOperators = {
                CompatiableRuleSchema.Operator.INCOMPATIBLE,
                CompatiableRuleSchema.Operator.CO_REFENT,
                CompatiableRuleSchema.Operator.REQUIRES
        };

        for (String operator : validOperators) {
            assertNotNull(operator, "Operator should not be null");
            assertFalse(operator.isEmpty(), "Operator should not be empty");
            assertTrue(operator.length() > 0, "Operator should be a valid string");
        }
    }
}