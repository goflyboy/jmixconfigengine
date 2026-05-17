package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.southinf.ConstraintAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RFC-0005 southbound API acceptance tests.
 */
public class SouthboundApiDecouplingTest extends ModuleScenarioTestBase {

    public SouthboundApiDecouplingTest() {
        super(MigratedSouthboundConstraint.class);
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Test
    public void testMigratedSouthboundAlgorithmCanRun() {
        inferParas("part1", 1);
        resultAssert().assertSuccess().assertSolutionSizeEqual(1);
    }

    @Test
    public void testOldAlgorithmWithoutApiVersionMustBeRejected() {
        String tempResourcePath = CommHelper.createTempPath(OldConstraint.class);
        Module oldModule = ModuleGenneratorByAnno.build(OldConstraint.class, tempResourcePath);
        oldModule.getAlg().setSouthApiVersion(null);

        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(false);
        ModuleConstraintExecutor.INST.init(config);
        try {
            Result<Void> result = ModuleConstraintExecutor.INST.addModule(oldModule.getId(), oldModule);

            assertEquals(Result.FAILED, result.getCode());
            assertTrue(result.getMessage().contains("southApiVersion"));
            assertTrue(result.getMessage().contains("regenerate"));
        } finally {
            ModuleConstraintExecutor.INST.fini();
        }
    }

    @Test
    public void testUnsupportedFutureApiVersionMustBeRejected() {
        String tempResourcePath = CommHelper.createTempPath(MigratedSouthboundConstraint.class);
        Module module = ModuleGenneratorByAnno.build(MigratedSouthboundConstraint.class, tempResourcePath);
        module.getAlg().setSouthApiVersion("99.0");

        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        ModuleConstraintExecutor.INST.init(config);
        try {
            Result<Void> result = ModuleConstraintExecutor.INST.addModule(module.getId(), module);

            assertEquals(Result.FAILED, result.getCode());
            assertTrue(result.getMessage().contains("Unsupported southApiVersion"));
            assertTrue(result.getMessage().contains("99.0"));
        } finally {
            ModuleConstraintExecutor.INST.fini();
        }
    }

    @Test
    public void testNewAlgorithmMustNotImportImplPackages() throws IOException {
        Path source = Path.of("src/test/java/com/jmix/scenario/ruletest/MigratedSouthboundConstraint.java");
        List<String> imports = Files.readAllLines(source).stream()
                .map(String::trim)
                .filter(line -> line.startsWith("import "))
                .toList();

        assertFalse(imports.stream().anyMatch(line -> line.contains("com.jmix.executor.impl.algmodel")));
        assertFalse(imports.stream().anyMatch(line -> line.contains("com.jmix.coretest.ConstraintAlgImplTestBase")));
    }

    @ModuleAnno(id = 5006L)
    public static class OldConstraint extends ConstraintAlgBase {

        @PartAnno
        private PartVar part1;

        @CodeRuleAnno
        private void rule1() {
            model.addGreaterOrEqual(part1.quantityVar(), 1);
        }
    }
}
