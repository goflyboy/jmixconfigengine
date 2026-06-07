package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.ruletrans.identifier.CategoryIdentifier;
import com.jmix.ruletrans.prompt.PromptBuilder;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.impl.llm.LLMInvoker;
import com.jmix.tool.impl.llm.LLMInvokerImpl;

import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Focused tests for product-level stage1 category identification.
 */
public class CategoryIdentifierTest {

    private static final String TEMP_RESOURCE_PATH = "target/ruletrans-test-resources";

    @Test
    public void testIdentifiesAndValidatesCategories() {
        CategoryIdentifier identifier = new CategoryIdentifier(realLlm(), new PromptBuilder());

        List<String> result = identifier.identify("4-core CPU cannot use 5400 drive", sampleModule());

        assertTrue(result.contains("cpu"));
        assertTrue(result.contains("drive"));
    }

    @Test
    public void testRejectsMissingCategory() {
        CategoryIdentifier identifier = new CategoryIdentifier(realLlm(), new PromptBuilder());

        CategoryNotFoundException ex = assertThrows(CategoryNotFoundException.class,
                () -> identifier.identify("Warranty period must be at least 12 months", sampleModule()));
        assertTrue(ex.getMessage().contains("PartCategory"));
    }

    private LLMInvoker realLlm() {
        return RealLlmHolder.INSTANCE;
    }

    private Module sampleModule() {
        return ModuleGenneratorByAnno.build(
                RuleTransModuleTest.SampleRuleTransConstraint.class,
                TEMP_RESOURCE_PATH);
    }

    private static final class RealLlmHolder {
        private static final LLMInvoker INSTANCE = new LLMInvokerImpl();
    }
}
