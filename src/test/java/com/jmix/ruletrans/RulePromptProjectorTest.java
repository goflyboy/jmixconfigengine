package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.prompt.PartCategoryPromptView;
import com.jmix.ruletrans.prompt.RulePromptProjector;

import org.junit.jupiter.api.Test;

public class RulePromptProjectorTest {

    @Test
    public void testBuildPartCategoryRuleContextFromAnnotatedClass() {
        PartCategoryRuleContext context = RuleContextFactory.partCategory(sampleModule(), "cpu");
        PartCategoryPromptView view = new RulePromptProjector().projectPartCategory(context);

        assertTrue(view.dynAttrSchemas().stream().anyMatch(attr -> "CoreNum".equals(attr.code())));
        assertTrue(view.attrParas().stream().anyMatch(attr -> "CoreNum".equals(attr.attrCode())));
        assertFalse(view.atomicParts().isEmpty());
    }
}
