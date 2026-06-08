package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;
import static com.jmix.ruletrans.RuleTransRealLlmSupport.realLlmInvoker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.prompt.PromptBuilder;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Focused tests for product-level stage1 category identification.
 */
public class CategoryIdentifierTest {

    @Test
    public void testIdentifiesAndValidatesCategories() {
        CategoryIdentifier identifier = new CategoryIdentifier(realLlmInvoker(), new PromptBuilder());

        List<String> result = identifier.identify("cpu 中属性 CoreNum 为 4 的部件不能和 drive 中属性 Speed 为 5400 的部件同时选择", sampleModule());

        assertTrue(result.contains("cpu"));
        assertTrue(result.contains("drive"));
    }

    @Test
    public void testRejectsMissingCategory() {
        CategoryIdentifier identifier = new CategoryIdentifier(realLlmInvoker(), new PromptBuilder());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> identifier.identify("质保期必须至少为 12 个月", sampleModule()));
        assertTrue(ex.getMessage().contains("PartCategory"));
    }
}
