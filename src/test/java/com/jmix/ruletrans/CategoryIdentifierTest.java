package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Focused tests for product-level stage1 category identification.
 */
public class CategoryIdentifierTest {

    @Test
    public void testIdentifiesAndValidatesCategories() {
        CategoryIdentifier identifier = new CategoryIdentifier(invoker("[\"cpu\", \"drive\"]"), new PromptBuilder());

        List<String> result = identifier.identify("4-core CPU cannot use 5400 drive", sampleModule());

        assertTrue(result.contains("cpu"));
        assertTrue(result.contains("drive"));
    }

    @Test
    public void testRejectsMissingCategory() {
        CategoryIdentifier identifier = new CategoryIdentifier(invoker("[\"missing\"]"), new PromptBuilder());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> identifier.identify("Warranty period must be at least 12 months", sampleModule()));
        assertTrue(ex.getMessage().contains("PartCategory"));
        assertTrue(ex.getMessage().contains("missing"));
    }

    private LLMInvoker invoker(String response) {
        return new LLMInvoker() {
            @Override
            public String generate(String systemMessage, String userMessage) {
                return response;
            }

            @Override
            public String getConfigInfo() {
                return "scripted";
            }
        };
    }
}
