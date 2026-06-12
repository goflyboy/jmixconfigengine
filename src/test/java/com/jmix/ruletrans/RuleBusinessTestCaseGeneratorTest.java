package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.ruletrans.testgen.RuleTestCaseGenerator;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

public class RuleBusinessTestCaseGeneratorTest {

    @Test
    public void testGenerateBusinessCasesNormalizesServiceMethod() {
        LLMInvoker invoker = new StubInvoker("""
                {
                  "cases": [
                    {
                      "id": "ASSIGN-001",
                      "given": {"parameters": [{"code": "color", "value": "red"}]},
                      "expect": {"parts": [{"code": "tshirt", "quantity": 2}]}
                    }
                  ]
                }
                """);
        RuleTestCaseGenerator generator = new RuleTestCaseGenerator(invoker, new PromptBuilder());

        BusinessRuleTestCaseSet caseSet = generator.generateBusinessCases(
                "红色 T 恤数量为 2",
                RuleContextFactory.module(sampleModule()),
                RuleScenario.constraint(RuleScope.PRODUCT, RuleFamily.CALCULATE),
                "model().addEquality(tshirt.quantityVar(), 2);");

        assertEquals(BusinessRuleFamily.ASSIGNMENT, caseSet.cases().get(0).businessFamily());
        assertEquals(TestEnvironment.CONSTRAINT, caseSet.cases().get(0).environment());
        assertEquals("testAssignment", caseSet.cases().get(0).serviceMethod());
        assertFalse(caseSet.toJson().contains("selectedParts"));
    }

    private record StubInvoker(String response) implements LLMInvoker {

        @Override
        public String generate(String systemMessage, String userMessage) {
            return response;
        }

        @Override
        public String getConfigInfo() {
            return "stub";
        }
    }
}
