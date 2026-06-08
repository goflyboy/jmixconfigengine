package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.cpuAtMostOneMethodBody;
import static com.jmix.ruletrans.RuleTransTestFixtures.sampleModule;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleSnippetAssembler;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.context.PartCategoryRuleContext;
import com.jmix.ruletrans.context.RuleContextFactory;
import com.jmix.ruletrans.postprocessor.CompilationProcessor;
import com.jmix.ruletrans.postprocessor.CompilationResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

public class RuleSnippetAssemblerTest {

    @Test
    public void testGeneratedMethodBodyCompiles() {
        PartCategoryRuleContext context = RuleContextFactory.partCategory(sampleModule(), "cpu");
        RuleTransTempFileManager tempFileManager = new RuleTransTempFileManager(Path.of("target/ruletrans-test"));
        RuleSnippetAssembler assembler = new RuleSnippetAssembler(tempFileManager);
        CompilationProcessor processor = new CompilationProcessor(tempFileManager);

        AssembledRuleClass assembled = assembler.assembleCompileUnit(cpuAtMostOneMethodBody(), context,
                "RuleTransCompileOk");
        CompilationResult result = processor.compile(assembled);

        assertTrue(assembled.sourceCode().contains("@CodeRuleAnno"), assembled.sourceCode());
        assertTrue(assembled.sourceCode().contains("public void"), assembled.sourceCode());
        assertTrue(result.success(), String.join("\n", result.errors()));
    }
}
