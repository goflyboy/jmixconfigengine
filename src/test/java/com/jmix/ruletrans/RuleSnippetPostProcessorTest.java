package com.jmix.ruletrans;

import static com.jmix.ruletrans.RuleTransTestFixtures.cpuAtMostOneSnippet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.ruletrans.generator.RuleSnippetPostProcessor;
import com.jmix.ruletrans.sdk.SdkProfile;

import org.junit.jupiter.api.Test;

public class RuleSnippetPostProcessorTest {

    @Test
    public void testRejectsWholeClass() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.process("package x; public class Bad { " + cpuAtMostOneSnippet() + " }"));
    }

    @Test
    public void testAcceptsPlainMethodBody() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processMethodBody("""
                model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                """);

        assertEquals("model().addLessOrEqual(model().sum4Selected(\"cpu\", \"\", \"\"), 1);", methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"));
    }

    @Test
    public void testRejectsAnnotationAndMethodDeclarationOnNewPath() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class, () -> processor.processMethodBody(cpuAtMostOneSnippet()));
    }

    @Test
    public void testExtractsLegacySnippetToMethodBody() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        String methodBody = processor.processLegacySnippetToMethodBody(cpuAtMostOneSnippet(), SdkProfile.CONSTRAINT);

        assertTrue(methodBody.contains("model().addLessOrEqual"), methodBody);
        assertFalse(methodBody.contains("@CodeRuleAnno"), methodBody);
        assertFalse(methodBody.contains("public void"), methodBody);
    }

    @Test
    public void testRejectsConstraintMethodBodyUsingPostApi() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        parameter("p1").setValue("1");
                        """, SdkProfile.CONSTRAINT));
    }

    @Test
    public void testRejectsPostMethodBodyUsingConstraintApi() {
        RuleSnippetPostProcessor processor = new RuleSnippetPostProcessor();

        assertThrows(RuleTransException.class,
                () -> processor.processMethodBody("""
                        model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
                        """, SdkProfile.POST));
    }
}
