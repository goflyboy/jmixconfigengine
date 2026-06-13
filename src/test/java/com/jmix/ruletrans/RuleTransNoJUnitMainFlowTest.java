package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RuleTransNoJUnitMainFlowTest {

    @Test
    public void testRuleTransMainSourcesDoNotUseJUnitLauncher() throws Exception {
        Path root = Path.of("src/main/java/com/jmix/ruletrans");
        List<String> forbidden = List.of(
                "LauncherFactory",
                "DiscoverySelectors",
                "SummaryGeneratingListener",
                "org.junit.platform");
        try (var paths = Files.walk(root)) {
            List<Path> offenders = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path, forbidden))
                    .toList();
            assertTrue(offenders.isEmpty(), offenders.toString());
        }
    }

    private boolean containsAny(Path path, List<String> forbidden) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            return forbidden.stream().anyMatch(source::contains);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
