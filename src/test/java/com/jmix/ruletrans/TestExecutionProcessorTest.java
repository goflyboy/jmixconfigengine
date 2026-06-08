package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.postprocessor.TestExecutionProcessor;

import org.junit.jupiter.api.Test;

public class TestExecutionProcessorTest {

    @Test
    public void testRunsJunitClass() {
        TestExecutionProcessor processor = new TestExecutionProcessor();

        assertDoesNotThrow(() -> assertTrue(
                processor.execute(RuleTransTestFixtures.PassingJUnitCase.class).success()));
    }
}
