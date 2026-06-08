package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.crossCategorySnippet;
import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.ir.RuleTransIrAssessment;
import com.jmix.ruletrans.ir.RuleTransIrEvaluator;

import org.junit.jupiter.api.Test;

import java.util.List;

public class RuleTransIrEvaluatorTest {

    @Test
    public void testP2IrAssessmentReportsModuleAlgBaseBoundPath() {
        RuleTransIrAssessment assessment = new RuleTransIrEvaluator().assess(
                RuleContextFactory.product(sampleModule(), List.of("cpu", "drive")),
                crossCategorySnippet());

        assertEquals(RuleTransIrAssessment.Status.MODULE_ALG_BASE_BOUND, assessment.status());
        assertFalse(assessment.schemaEmissionReady());
        assertTrue(assessment.targetCategories().contains("cpu"));
        assertFalse(assessment.blockers().isEmpty());
    }
}
