package com.jmix.ruletrans.assembler;

import com.jmix.ruletrans.context.RuleContext;

/**
 * Assembles generated rule method snippets into temporary compile units.
 */
public final class RuleSnippetAssembler {

    private static final String DEFAULT_PACKAGE = "com.jmix.ruletrans.generated";

    private final RuleTransTempFileManager tempFileManager;

    public RuleSnippetAssembler() {
        this(new RuleTransTempFileManager());
    }

    public RuleSnippetAssembler(RuleTransTempFileManager tempFileManager) {
        this.tempFileManager = tempFileManager;
    }

    public AssembledRuleClass assembleCompileUnit(String snippet, RuleContext context, String className) {
        if (snippet == null || snippet.trim().isEmpty()) {
            throw new IllegalArgumentException("snippet must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        String safeClassName = normalizeClassName(className);
        String source = sourceFor(safeClassName, snippet);
        return new AssembledRuleClass(
                DEFAULT_PACKAGE,
                safeClassName,
                DEFAULT_PACKAGE + "." + safeClassName,
                source,
                tempFileManager.writeSource(DEFAULT_PACKAGE, safeClassName, source));
    }

    public RuleTransTempFileManager tempFileManager() {
        return tempFileManager;
    }

    private String sourceFor(String className, String snippet) {
        return """
                package %s;

                import com.jmix.executor.southinf.ModuleAlgBase;
                import com.jmix.executor.southinf.ModuleCPModel;
                import com.jmix.executor.southinf.PartCategoryCPModel;
                import com.jmix.executor.southinf.cp.*;
                import com.jmix.executor.southinf.var.*;
                import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
                import java.util.*;

                public class %s extends ModuleAlgBase {

                %s
                }
                """.formatted(DEFAULT_PACKAGE, className, indent(snippet.trim()));
    }

    private String normalizeClassName(String className) {
        String candidate = className == null || className.trim().isEmpty()
                ? "RuleTransCandidate"
                : className.trim();
        if (!candidate.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Invalid Java class name: " + className);
        }
        return candidate;
    }

    private String indent(String snippet) {
        return "    " + snippet.replace("\n", "\n    ");
    }
}
