package com.jmix.ruletrans.assembler;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.base.AssignType;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.bmodel.logic.EffectScope;
import com.jmix.ruletrans.metadata.RuleMetadata;
import com.jmix.ruletrans.scenario.RuleFamily;
import com.jmix.ruletrans.scenario.RuleScenario;
import com.jmix.ruletrans.scenario.RuleScope;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.context.RuleContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Assembles generated rule method bodies into temporary compile units.
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

    public AssembledRuleClass assembleCompileUnit(String methodBody, RuleContext context, String className) {
        RuleScenario scenario = defaultScenario(context);
        RuleMetadata metadata = RuleMetadata.from("", context, scenario);
        return assembleCompileUnit(methodBody, context, scenario, metadata, className);
    }

    public AssembledRuleClass assembleCompileUnit(
            String methodBody,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            String className) {
        validateBodyAndContext(methodBody, context);
        String safeClassName = normalizeClassName(className);
        String source = sourceFor(safeClassName, completeRuleMethod(methodBody, context, scenario, metadata), context);
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

    private String sourceFor(String className, String ruleMethod, RuleContext context) {
        StringBuilder fields = new StringBuilder();
        appendModuleFields(fields, context.module());
        return """
                package %s;

                import com.jmix.executor.southinf.ModuleAlgBase;
                import com.jmix.executor.southinf.ModuleCPModel;
                import com.jmix.executor.southinf.PartCategoryCPModel;
                import com.jmix.executor.southinf.cp.*;
                import com.jmix.executor.southinf.var.*;
                import com.jmix.executor.bmodel.attr.DynamicAttributeType;
                import com.jmix.executor.bmodel.base.AssignType;
                import com.jmix.executor.bmodel.logic.CalcStage;
                import com.jmix.executor.bmodel.logic.EffectScope;
                import com.jmix.executor.bmodel.logic.PriorityStrategy;
                import com.jmix.executor.bmodel.para.ParaType;
                import com.jmix.tool.bbuilder.anno.*;
                import java.util.*;

                @ModuleAnno(id = %dL)
                public class %s extends ModuleAlgBase {

                %s
                %s
                }
                """.formatted(DEFAULT_PACKAGE, context.module().getId(), className, fields, indent(ruleMethod.trim()));
    }

    private void validateBodyAndContext(String methodBody, RuleContext context) {
        if (methodBody == null || methodBody.trim().isEmpty()) {
            throw new IllegalArgumentException("methodBody must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    private RuleScenario defaultScenario(RuleContext context) {
        RuleScope scope = context != null && context.isModuleLevel() ? RuleScope.PRODUCT : RuleScope.PART_CATEGORY;
        return RuleScenario.constraint(scope, RuleFamily.UNKNOWN);
    }

    private String completeRuleMethod(
            String methodBody,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata) {
        RuleScenario safeScenario = scenario == null ? defaultScenario(context) : scenario;
        RuleMetadata safeMetadata = metadata == null ? RuleMetadata.from("", context, safeScenario) : metadata;
        String methodName = normalizeMethodName(safeMetadata.methodName());
        String ruleCode = notBlank(safeMetadata.ruleCode()) ? safeMetadata.ruleCode() : methodName;
        return annotationFor(safeScenario, safeMetadata)
                + "\n"
                + "public void " + methodName + "() {\n"
                + "    String ruleCode = " + javaString(ruleCode) + ";\n"
                + indent(methodBody.trim(), 1) + "\n"
                + "}";
    }

    private String annotationFor(RuleScenario scenario, RuleMetadata metadata) {
        if (scenario != null && scenario.family() == RuleFamily.PRIORITY) {
            return priorityAnnotationFor(metadata);
        }
        List<String> args = new ArrayList<>();
        if (scenario != null && scenario.isPost()) {
            args.add("calcStage = CalcStage.POST");
        }
        addAnnotationArg(args, "normalNaturalCode", metadata.normalNaturalCode());
        addAnnotationArg(args, "fatherCode", metadata.fatherCode());
        addAnnotationArg(args, "attrParaCodes", metadata.attrParaCodes());
        addAnnotationArg(args, "leftProObjsStr", metadata.leftProObjsStr());
        addAnnotationArg(args, "rightProObjsStr", metadata.rightProObjsStr());
        addEffectScopeArg(args, metadata);
        if (args.isEmpty()) {
            return "@CodeRuleAnno";
        }
        return "@CodeRuleAnno(" + String.join(", ", args) + ")";
    }

    private String priorityAnnotationFor(RuleMetadata metadata) {
        List<String> args = new ArrayList<>();
        args.add("strategy = PriorityStrategy.MIN");
        addAnnotationArg(args, "normalNaturalCode", metadata.normalNaturalCode());
        addAnnotationArg(args, "fatherCode", metadata.fatherCode());
        addAnnotationArg(args, "attrParaCodes", metadata.attrParaCodes());
        addAnnotationArg(args, "leftProObjsStr", metadata.leftProObjsStr());
        addAnnotationArg(args, "rightProObjsStr", metadata.rightProObjsStr());
        addEffectScopeArg(args, metadata);
        return "@PriorityRuleAnno(" + String.join(", ", args) + ")";
    }

    private void addAnnotationArg(List<String> args, String name, String value) {
        if (notBlank(value)) {
            args.add(name + " = " + javaString(value));
        }
    }

    private void addEffectScopeArg(List<String> args, RuleMetadata metadata) {
        if (metadata.effectScope() != EffectScope.SingleInst) {
            args.add("effectScope = EffectScope." + metadata.effectScope().name());
        }
    }

    private void appendModuleFields(StringBuilder builder, Module module) {
        Set<String> emittedCodes = new HashSet<>();
        for (Para para : module.getParas()) {
            appendPara(builder, para, emittedCodes);
        }
        List<Part> rootAtomicParts = new ArrayList<>(module.getAtomicParts());
        rootAtomicParts.sort(Comparator.comparing(Part::getCode));
        for (Part part : rootAtomicParts) {
            appendRootAtomicPart(builder, part, emittedCodes);
        }
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

        for (Para para : category.getParas()) {
            appendPara(builder, para, emittedCodes);
        }

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

    private void appendRootAtomicPart(StringBuilder builder, Part part, Set<String> emittedCodes) {
        validateJavaCode(part.getCode(), "Part");
        if (!emittedCodes.add(part.getCode())) {
            return;
        }
        builder.append("        @PartAnno(")
                .append(rootAtomicPartAnnoArgs(part))
                .append(")\n");
        builder.append("        private PartVar ")
                .append(fieldName(part.getCode()))
                .append(";\n\n");
    }

    private void appendPara(StringBuilder builder, Para para, Set<String> emittedCodes) {
        validateJavaCode(para.getCode(), "Parameter");
        if (!emittedCodes.add("para:" + para.getCode())) {
            return;
        }
        builder.append("        @ParaAnno(")
                .append(paraAnnoArgs(para))
                .append(")\n");
        builder.append("        private ParaVar ")
                .append(fieldName(para.getCode()))
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

    private String rootAtomicPartAnnoArgs(Part part) {
        List<String> args = new ArrayList<>();
        if (notBlank(part.getFatherCode())) {
            args.add("fatherCode = " + javaString(part.getFatherCode()));
        }
        if (part.getMaxQuantity() != null && part.getMaxQuantity() != Part.MAX_QUANTITY) {
            args.add("maxQuantity = " + part.getMaxQuantity());
        }
        if (part.getPrice() != null && part.getPrice() != 0L) {
            args.add("price = " + part.getPrice() + "L");
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

    private String paraAnnoArgs(Para para) {
        List<String> args = new ArrayList<>();
        if (notBlank(para.getCode())) {
            args.add("code = " + javaString(para.getCode()));
        }
        if (notBlank(para.getFatherCode())) {
            args.add("fatherCode = " + javaString(para.getFatherCode()));
        }
        if (para.getParaType() != null && para.getParaType() != ParaType.ENUM) {
            args.add("type = ParaType." + para.getParaType().name());
        }
        if (para.getAssignType() != null && para.getAssignType() != AssignType.CALC) {
            args.add("assignType = AssignType." + para.getAssignType().name());
        }
        if (notBlank(para.getDefaultValue())) {
            args.add("defaultValue = " + javaString(para.getDefaultValue()));
        }
        if (para.getOptions() != null && !para.getOptions().isEmpty()) {
            args.add("options = " + annotationArray(para.getOptions().stream()
                    .map(this::paraOptionString)
                    .toList()));
        }
        if (notBlank(para.getMinValue()) && !Para.DEFAULT_MIN_VALUE.equals(para.getMinValue())) {
            args.add("minValue = " + javaString(para.getMinValue()));
        }
        if (notBlank(para.getMaxValue()) && !Para.DEFAULT_MAX_VALUE.equals(para.getMaxValue())) {
            args.add("maxValue = " + javaString(para.getMaxValue()));
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

    private String paraOptionString(DynamicAttributerOption option) {
        String code = option.getCode();
        if (!notBlank(code)) {
            throw new RuleTransException("Parameter option code must not be blank");
        }
        return code;
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

    private String normalizeMethodName(String methodName) {
        String candidate = methodName == null || methodName.trim().isEmpty()
                ? "ruleGenerated"
                : methodName.trim();
        if (!candidate.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Invalid Java method name: " + methodName);
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

    private String fieldName(String code) {
        return code;
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
