package com.jmix.scenario.ruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.version.SouthApiSince;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
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

    @Test
    public void testSouthinfMustNotReferenceImplOrOrTools() throws IOException {
        List<String> sourceLines = scanJavaSources(Path.of("src/main/java/com/jmix/executor/southinf"));

        assertFalse(sourceLines.stream().anyMatch(line -> line.contains("com.google.ortools")));
        assertFalse(sourceLines.stream().anyMatch(line -> line.contains("com.jmix.executor.impl")));
    }

    @Test
    public void testRemovedSouthboundTypesMustStayRemoved() {
        List<String> removedTypes = List.of(
                "src/main/java/com/jmix/executor/southinf/PartCategoryAlg.java",
                "src/main/java/com/jmix/executor/southinf/ModuleAlg.java",
                "src/main/java/com/jmix/executor/southinf/IModuleAlg.java",
                "src/main/java/com/jmix/executor/southinf/IConstraintFunction.java",
                "src/main/java/com/jmix/executor/southinf/ConstraintFunction.java",
                "src/main/java/com/jmix/executor/southinf/ConstraintContext.java",
                "src/main/java/com/jmix/executor/southinf/ConstraintAlgorithm.java",
                "src/main/java/com/jmix/executor/southinf/ConstraintVarRegistry.java",
                "src/main/java/com/jmix/executor/southinf/ConstraintModel.java",
                "src/main/java/com/jmix/executor/southinf/AlgorithmApiVersion.java",
                "src/main/java/com/jmix/executor/impl/ModuleInstAccessor.java",
                "src/main/java/com/jmix/executor/impl/ModuleInstAccessorImpl.java");

        assertTrue(removedTypes.stream().noneMatch(type -> Files.exists(Path.of(type))));
    }

    @Test
    public void testSouthboundApiMethodsMustHaveSinceVersion() throws Exception {
        for (Class<?> apiType : southboundInterfaces()) {
            assertTrue(apiType.isAnnotationPresent(SouthApiSince.class),
                    "Missing @SouthApiSince on " + apiType.getName());
            for (Method method : apiType.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                assertTrue(method.isAnnotationPresent(SouthApiSince.class),
                        "Missing @SouthApiSince on " + apiType.getName() + "#" + method.getName());
            }
        }
    }

    private List<String> scanJavaSources(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (IOException ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();
        }
    }

    private List<Class<?>> southboundInterfaces() throws Exception {
        String packagePath = "com/jmix/executor/southinf";
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packagePath);
        List<Class<?>> interfaces = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }
            Path root = Path.of(URI.create(resource.toString()));
            try (var paths = Files.walk(root)) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                    String relative = root.relativize(path).toString()
                            .replace('\\', '.')
                            .replace('/', '.')
                            .replace(".class", "");
                    if (relative.contains("$")) {
                        continue;
                    }
                    Class<?> type = Class.forName("com.jmix.executor.southinf." + relative);
                    if (type.isInterface() && !type.isAnnotation()) {
                        interfaces.add(type);
                    }
                }
            }
        }
        return interfaces;
    }

    @ModuleAnno(id = 5006L)
    public static class OldConstraint extends ModuleAlgBase {

        @PartAnno
        private PartVar part1;

        @CodeRuleAnno
        private void rule1() {
            model().addGreaterOrEqual(part1.quantityVar(), 1);
        }
    }
}
