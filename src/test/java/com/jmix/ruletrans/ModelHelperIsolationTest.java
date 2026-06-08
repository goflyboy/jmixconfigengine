package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModelHelperIsolationTest {

    @Test
    public void testModelHelperRemainsUnchanged() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/jmix/tool/ModelHelper.java"));

        assertFalse(source.contains("ruletrans"));
        assertFalse(source.contains("RuleTransEngine"));
    }
}
