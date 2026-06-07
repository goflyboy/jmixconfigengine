package com.jmix.ruletrans.assembler;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;
import com.jmix.ruletrans.testgen.RuleTransTestCase;
import com.jmix.ruletrans.testgen.RuleTransTestCaseSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles generated rule method snippets into temporary compile units.
 */
public final class RuleSnippetAssembler {

    private static final String DEFAULT_PACKAGE = "com.jmix.ruletrans.generated";
    private static final int[] DATTR_ANNOTATIONS = {1, 2, 3, 4, 5, 11, 12, 13};

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

    public AssembledRuleClass assembleExecutableTest(
            String snippet,
            RuleContext context,
            RuleTransTestCaseSet testCaseSet,
            String className) {
        if (snippet == null || snippet.trim().isEmpty()) {
            throw new IllegalArgumentException("snippet must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (testCaseSet == null || testCaseSet.isEmpty()) {
            throw new IllegalArgumentException("testCaseSet must not be empty");
        }
        String safeClassName = normalizeClassName(className);
        String source = executableTestSource(safeClassName, snippet, context, testCaseSet);
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

    private String executableTestSource(
            String className,
            String snippet,
            RuleContext context,
            RuleTransTestCaseSet testCaseSet) {
        String constraintClassName = className + "Constraint";
        StringBuilder testMethods = new StringBuilder();
        int index = 1;
        for (RuleTransTestCase testCase : testCaseSet.cases()) {
            appendTestMethod(testMethods, testCase, index++);
        }

        StringBuilder fields = new StringBuilder();
        appendModuleFields(fields, context.module());

        return """
                package %s;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import com.jmix.coretest.ModuleScenarioTestBase;
                import com.jmix.executor.bmodel.attr.DynamicAttributeType;
                import com.jmix.executor.model.ModuleValidateResp;
                import com.jmix.executor.southinf.ModuleAlgBase;
                import com.jmix.executor.southinf.ModuleCPModel;
                import com.jmix.executor.southinf.PartCategoryCPModel;
                import com.jmix.executor.southinf.cp.*;
                import com.jmix.executor.southinf.var.*;
                import com.jmix.tool.bbuilder.anno.*;

                import org.junit.jupiter.api.Test;

                import java.util.*;

                public class %s extends ModuleScenarioTestBase {

                    public %s() {
                        super(%s.class);
                    }

                %s
                    @ModuleAnno(id = %dL)
                    public static class %s extends ModuleAlgBase {

                %s
                %s
                    }
                }
                """.formatted(
                DEFAULT_PACKAGE,
                className,
                className,
                constraintClassName,
                indent(testMethods.toString().trim(), 1),
                context.module().getId(),
                constraintClassName,
                fields,
                indent(snippet.trim(), 2));
    }

    private void appendTestMethod(StringBuilder builder, RuleTransTestCase testCase, int index) {
        if (testCase.isValidateCase()) {
            appendValidateTestMethod(builder, testCase, index);
            return;
        }
        if (testCase.isRecommendCase()) {
            appendRecommendTestMethod(builder, testCase, index);
            return;
        }
        throw new RuleTransException("Unsupported RuleTrans test case type: " + testCase.type());
    }

    private void appendValidateTestMethod(StringBuilder builder, RuleTransTestCase testCase, int index) {
        if (testCase.expectedValid() == null) {
            throw new RuleTransException("Validate test case expectedValid must not be null: " + testCase.id());
        }
        String methodName = testMethodName(testCase, index);
        String parts = stringArrayArgs(testCase.selectedPartsOrEmpty());
        builder.append("    @Test\n");
        builder.append("    public void ").append(methodName).append("() {\n");
        builder.append("        ModuleValidateResp resp = validateData(").append(parts).append(");\n");
        builder.append("        assertEquals(").append(testCase.expectedValid()).append(", resp.isValid(), ")
                .append(javaString("RuleTrans validate case " + caseId(testCase, index)
                        + " selectedParts=" + testCase.selectedPartsOrEmpty()))
                .append(" + \", violated=\" + resp.getViolatedRuleCodes());\n");
        for (String ruleCode : testCase.expectedViolatedRuleCodesOrEmpty()) {
            builder.append("        assertTrue(resp.getViolatedRuleCodes().contains(")
                    .append(javaString(ruleCode))
                    .append("), ")
                    .append(javaString("Expected violated rule code " + ruleCode + " for case "
                            + caseId(testCase, index)))
                    .append(" + \", actual=\" + resp.getViolatedRuleCodes());\n");
        }
        builder.append("    }\n\n");
    }

    private void appendRecommendTestMethod(StringBuilder builder, RuleTransTestCase testCase, int index) {
        String expected = testCase.expectedResult() == null ? "" : testCase.expectedResult().trim().toUpperCase();
        if (expected.isEmpty()) {
            throw new RuleTransException("Recommend test case expectedResult must not be blank: " + testCase.id());
        }
        String methodName = testMethodName(testCase, index);
        builder.append("    @Test\n");
        builder.append("    public void ").append(methodName).append("() {\n");
        builder.append("        inferRecommendModule(")
                .append(stringArrayArgs(testCase.requestsOrEmpty()))
                .append(");\n");
        builder.append("        printSimpleSolutions();\n");
        if ("NO_SOLUTION".equals(expected)) {
            builder.append("        resultAssert().assertNoSolution();\n");
        } else if ("HAS_SOLUTION".equals(expected) || "SUCCESS".equals(expected)) {
            builder.append("        resultAssert().assertSuccess();\n");
        } else {
            throw new RuleTransException("Unsupported recommend expectedResult: " + testCase.expectedResult());
        }
        builder.append("    }\n\n");
    }

    private void appendModuleFields(StringBuilder builder, Module module) {
        Set<String> emittedCodes = new HashSet<>();
        for (PartCategory category : module.getPartCategorys()) {
            appendCategory(builder, category, emittedCodes);
        }
    }

    private void appendCategory(StringBuilder builder, PartCategory category, Set<String> emittedCodes) {
        validateJavaCode(category.getCode(), "PartCategory");
        if (!emittedCodes.add(category.getCode())) {
            return;
        }
        builder.append("        @PartAnno(")
                .append(categoryPartAnnoArgs(category))
                .append(")\n");
        appendDynamicAttrAnnotations(builder, category);
        builder.append("        private PartCategoryVar ")
                .append(fieldName(category.getCode()))
                .append(";\n\n");

        List<PartCategory> childCategories = new ArrayList<>(category.getPartCategorys());
        childCategories.sort(Comparator.comparing(PartCategory::getCode));
        for (PartCategory child : childCategories) {
            appendCategory(builder, child, emittedCodes);
        }
        List<Part> atomicParts = new ArrayList<>(category.getAtomicParts());
        atomicParts.sort(Comparator.comparing(Part::getCode));
        for (Part part : atomicParts) {
            appendAtomicPart(builder, part, category, emittedCodes);
        }
    }

    private void appendAtomicPart(
            StringBuilder builder,
            Part part,
            PartCategory category,
            Set<String> emittedCodes) {
        validateJavaCode(part.getCode(), "Part");
        if (!emittedCodes.add(part.getCode())) {
            return;
        }
        builder.append("        @PartAnno(")
                .append(atomicPartAnnoArgs(part, category))
                .append(")\n");
        builder.append("        private PartVar ")
                .append(fieldName(part.getCode()))
                .append(";\n\n");
    }

    private String categoryPartAnnoArgs(PartCategory category) {
        List<String> args = new ArrayList<>();
        if (notBlank(category.getFatherCode())) {
            args.add("fatherCode = " + javaString(category.getFatherCode()));
        }
        if (category.isSupportMultiInst()) {
            args.add("supportMultiInst = true");
        }
        if (!category.isRequiredSelection()) {
            args.add("required = false");
        }
        return String.join(", ", args);
    }

    private String atomicPartAnnoArgs(Part part, PartCategory category) {
        List<String> args = new ArrayList<>();
        args.add("fatherCode = " + javaString(category.getCode()));
        List<String> attrs = attrsFor(part, category);
        if (!attrs.isEmpty()) {
            args.add("attrs = " + annotationArray(attrs));
        }
        if (part.getMaxQuantity() != null && part.getMaxQuantity() != Part.MAX_QUANTITY) {
            args.add("maxQuantity = " + part.getMaxQuantity());
        }
        if (part.getPrice() != null && part.getPrice() != 0L) {
            args.add("price = " + part.getPrice() + "L");
        }
        return String.join(", ", args);
    }

    private List<String> attrsFor(Part part, PartCategory category) {
        List<DynamicAttribute> schemas = category.queryDynAttrSchemas4NotInst();
        if (schemas.isEmpty()) {
            return List.of();
        }
        List<String> attrs = new ArrayList<>();
        for (DynamicAttribute schema : schemas) {
            String attrValue = part.getDynAttr().get(schema.getCode());
            if (attrValue == null) {
                throw new RuleTransException("Part " + part.getCode()
                        + " does not contain dynamic attribute " + schema.getCode());
            }
            attrs.add(attrValue);
        }
        return attrs;
    }

    private void appendDynamicAttrAnnotations(StringBuilder builder, PartCategory category) {
        List<DynamicAttribute> schemas = category.getDynAttrSchemas();
        if (schemas.size() > DATTR_ANNOTATIONS.length) {
            throw new RuleTransException("PartCategory " + category.getCode()
                    + " has " + schemas.size()
                    + " dynamic attributes, but the annotation harness supports "
                    + DATTR_ANNOTATIONS.length);
        }
        for (int i = 0; i < schemas.size(); i++) {
            DynamicAttribute attr = schemas.get(i);
            int annoNo = DATTR_ANNOTATIONS[i];
            builder.append("        @DAttrAnno").append(annoNo).append("(");
            builder.append("code = ").append(javaString(attr.getCode()));
            List<String> options = optionsFor(category, attr);
            if (!options.isEmpty()) {
                builder.append(", options = ").append(annotationArray(options));
            }
            if (attr.getInstType() != 0) {
                builder.append(", instType = ").append(attr.getInstType());
            }
            if (attr.getDynAttrType() != null && attr.getDynAttrType() != DynamicAttributeType.E_INT) {
                builder.append(", dynAttrType = DynamicAttributeType.")
                        .append(attr.getDynAttrType().name());
            }
            builder.append(")\n");
        }
    }

    private List<String> optionsFor(PartCategory category, DynamicAttribute attr) {
        if (attr.getOptions() != null && !attr.getOptions().isEmpty()) {
            return attr.getOptions().stream()
                    .map(this::optionString)
                    .toList();
        }
        return category.getAllAtomicParts().stream()
                .map(part -> part.getDynAttr().get(attr.getCode()))
                .filter(this::notBlank)
                .distinct()
                .map(value -> "Opt_" + value + ":" + value)
                .toList();
    }

    private String optionString(DynamicAttributerOption option) {
        String code = notBlank(option.getCode()) ? option.getCode() : "Opt_" + option.getCodeValue();
        String codeValue = option.getCodeValue();
        if (code == null || code.contains(":") || codeValue == null || codeValue.contains(":")) {
            throw new RuleTransException("Dynamic attribute option cannot contain ':' in generated annotations");
        }
        return code + ":" + codeValue;
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

    private String indent(String snippet, int levels) {
        if (snippet.isEmpty()) {
            return "";
        }
        String prefix = "    ".repeat(levels);
        return prefix + snippet.replace("\n", "\n" + prefix);
    }

    private String stringArrayArgs(List<String> values) {
        return values.stream()
                .map(this::javaString)
                .collect(Collectors.joining(", "));
    }

    private String annotationArray(List<String> values) {
        return "{" + values.stream().map(this::javaString).collect(Collectors.joining(", ")) + "}";
    }

    private String javaString(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.append('"').toString();
    }

    private String testMethodName(RuleTransTestCase testCase, int index) {
        return "testRuleTrans_" + index + "_" + toJavaIdentifier(caseId(testCase, index));
    }

    private String caseId(RuleTransTestCase testCase, int index) {
        return notBlank(testCase.id()) ? testCase.id() : "case" + index;
    }

    private String fieldName(String code) {
        return code;
    }

    private String toJavaIdentifier(String value) {
        String candidate = value == null ? "" : value.replaceAll("[^A-Za-z0-9_$]", "_");
        if (candidate.isEmpty() || !Character.isJavaIdentifierStart(candidate.charAt(0))) {
            candidate = "_" + candidate;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidate.length(); i++) {
            char ch = candidate.charAt(i);
            builder.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
        }
        return builder.toString();
    }

    private void validateJavaCode(String code, String type) {
        if (!notBlank(code)) {
            throw new RuleTransException(type + " code must not be blank");
        }
        if (code.contains("Var")) {
            throw new RuleTransException(type + " code contains unsupported token 'Var': " + code);
        }
        if (!Character.isJavaIdentifierStart(code.charAt(0))) {
            throw new RuleTransException(type + " code is not a Java identifier: " + code);
        }
        for (int i = 1; i < code.length(); i++) {
            if (!Character.isJavaIdentifierPart(code.charAt(i))) {
                throw new RuleTransException(type + " code is not a Java identifier: " + code);
            }
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
