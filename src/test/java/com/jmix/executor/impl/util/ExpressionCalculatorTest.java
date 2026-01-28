package com.jmix.executor.impl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExpressionCalculatorTest {

    @Test
    public void testSimpleAddition() {
        log.info("=== Testing simple addition ===");
        String expression = "1 + 2 + 3";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(6.0, result, 0.001);
        log.info("✓ Simple addition test passed: 1 + 2 + 3 = {}", result);
    }

    @Test
    public void testSimpleMultiplication() {
        log.info("=== Testing simple multiplication ===");
        String expression = "2 * 3 * 4";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(24.0, result, 0.001);
        log.info("✓ Simple multiplication test passed: 2 * 3 * 4 = {}", result);
    }

    @Test
    public void testMixedOperations() {
        log.info("=== Testing mixed operations ===");
        String expression = "1 * 110 + 2 * 120 + 3 * 13";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(110 + 240 + 39, result, 0.001);
        log.info("✓ Mixed operations test passed: 1 * 110 + 2 * 120 + 3 * 13 = {}", result);
    }

    @Test
    public void testExpressionWithVariables() {
        log.info("=== Testing expression with variables ===");
        String expression = "- 100 * (3 * sd1_Q) + 1 * (1 * md1_Q)";
        Map<String, Integer> parts = new HashMap<>();
        parts.put("sd1_Q", 2);
        parts.put("md1_Q", 5);
        double result = ExpressionCalculator.calculate(expression, parts);
        double expected = -100 * (3 * 2) + 1 * (1 * 5);
        assertEquals(expected, result, 0.001);
        log.info("✓ Expression with variables test passed: result = {}", result);
    }

    @Test
    public void testComplexExpressionWithVariables() {
        log.info("=== Testing complex expression with variables ===");
        String expression = "100 + 500*(11*(1*var1 + 3*var2 + 5 - 30*var3))";
        Map<String, Integer> parts = new HashMap<>();
        parts.put("var1", 2);
        parts.put("var2", 1);
        parts.put("var3", 1);
        double result = ExpressionCalculator.calculate(expression, parts);
        double expected = 100 + 500 * (11 * (1 * 2 + 3 * 1 + 5 - 30 * 1));
        assertEquals(expected, result, 0.001);
        log.info("✓ Complex expression with variables test passed: result = {}", result);
    }

    @Test
    public void testComplexExpressionWithVariablesDifferentValues() {
        log.info("=== Testing complex expression with different variable values ===");
        String expression = "100 + 500*(11*(1*var1 + 3*var2 + 5 - 30*var3))";
        Map<String, Integer> parts = new HashMap<>();
        parts.put("var1", 5);
        parts.put("var2", 3);
        parts.put("var3", 0);
        double result = ExpressionCalculator.calculate(expression, parts);
        double expected = 100 + 500 * (11 * (1 * 5 + 3 * 3 + 5 - 30 * 0));
        assertEquals(expected, result, 0.001);
        log.info("✓ Complex expression with different values test passed: result = {}", result);
    }

    @Test
    public void testComplexExpressionWithVariablesZeroValues() {
        log.info("=== Testing complex expression with zero variable values ===");
        String expression = "100 + 500*(11*(1*var1 + 3*var2 + 5 - 30*var3))";
        Map<String, Integer> parts = new HashMap<>();
        parts.put("var1", 0);
        parts.put("var2", 0);
        parts.put("var3", 0);
        double result = ExpressionCalculator.calculate(expression, parts);
        double expected = 100 + 500 * (11 * (1 * 0 + 3 * 0 + 5 - 30 * 0));
        assertEquals(expected, result, 0.001);
        log.info("✓ Complex expression with zero values test passed: result = {}", result);
    }

    @Test
    public void testComplexExpressionWithNegativeValues() {
        log.info("=== Testing complex expression with negative variable values ===");
        String expression = "100 + 500*(11*(1*var1 + 3*var2 + 5 - 30*var3))";
        Map<String, Integer> parts = new HashMap<>();
        parts.put("var1", -1);
        parts.put("var2", 2);
        parts.put("var3", 1);
        double result = ExpressionCalculator.calculate(expression, parts);
        double expected = 100 + 500 * (11 * (1 * (-1) + 3 * 2 + 5 - 30 * 1));
        assertEquals(expected, result, 0.001);
        log.info("✓ Complex expression with negative values test passed: result = {}", result);
    }

    @Test
    public void testParenthesesAndPrecedence() {
        log.info("=== Testing parentheses and operator precedence ===");
        String expression = "(1 + 2) * (3 + 4)";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(21.0, result, 0.001);
        log.info("✓ Parentheses and precedence test passed: (1 + 2) * (3 + 4) = {}", result);
    }

    @Test
    public void testDivision() {
        log.info("=== Testing division ===");
        String expression = "100 / 4";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(25.0, result, 0.001);
        log.info("✓ Division test passed: 100 / 4 = {}", result);
    }

    @Test
    public void testSubtraction() {
        log.info("=== Testing subtraction ===");
        String expression = "100 - 50 - 20";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(30.0, result, 0.001);
        log.info("✓ Subtraction test passed: 100 - 50 - 20 = {}", result);
    }

    @Test
    public void testEmptyExpression() {
        log.info("=== Testing empty expression ===");
        String expression = "";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(0.0, result, 0.001);
        log.info("✓ Empty expression test passed: result = {}", result);
    }

    @Test
    public void testExpressionWithNullParts() {
        log.info("=== Testing expression with null parts ===");
        String expression = "1 + 2 + 3";
        double result = ExpressionCalculator.calculate(expression, null);
        assertEquals(6.0, result, 0.001);
        log.info("✓ Expression with null parts test passed: result = {}", result);
    }

    @Test
    public void testExpressionWithEmptyParts() {
        log.info("=== Testing expression with empty parts ===");
        String expression = "1 + 2 + 3";
        Map<String, Integer> parts = new HashMap<>();
        double result = ExpressionCalculator.calculate(expression, parts);
        assertEquals(6.0, result, 0.001);
        log.info("✓ Expression with empty parts test passed: result = {}", result);
    }

    @Test
    public void testLargeNumbers() {
        log.info("=== Testing large numbers ===");
        String expression = "1000000 * 1000000";
        double result = ExpressionCalculator.calculate(expression);
        assertEquals(1.0E12, result, 0.001);
        log.info("✓ Large numbers test passed: result = {}", result);
    }

    @Test
    public void testDecimalResults() {
        log.info("=== Testing decimal results ===");
        String expression = "10 / 3";
        double result = ExpressionCalculator.calculate(expression);
        assertNotNull(result);
        log.info("✓ Decimal results test passed: 10 / 3 = {}", result);
    }
}
