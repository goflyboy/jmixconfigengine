package com.jmix.tool.bbuilder;

import com.jmix.executor.bmodel.AttrPara;
import com.jmix.executor.bmodel.AttrParaType;
import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.ModuleAlgArtifact;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.PartType;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValue;
import com.jmix.executor.bmodel.attr.InstanceDynAttrValueItem;
import com.jmix.executor.bmodel.base.Pair;
import com.jmix.executor.bmodel.logic.CodeRuleSchema;
import com.jmix.executor.bmodel.logic.CompatiableRuleSchema;
import com.jmix.executor.bmodel.logic.ExprSchema;
import com.jmix.executor.bmodel.logic.PriorityRuleSchema;
import com.jmix.executor.bmodel.logic.RefProgObjSchema;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.logic.RuleTypeConstants;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.algmodel.ParaVarImpl;
import com.jmix.executor.impl.algmodel.PartCategoryVarImpl;
import com.jmix.executor.impl.algmodel.PartVarImpl;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.CompatiableRuleAnno;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno11;
import com.jmix.tool.bbuilder.anno.DAttrAnno12;
import com.jmix.tool.bbuilder.anno.DAttrAnno13;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrAnno4;
import com.jmix.tool.bbuilder.anno.DAttrAnno5;
import com.jmix.tool.bbuilder.anno.DAttrInherit;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.bbuilder.anno.PriorityRuleAnno;
import com.jmix.tool.impl.ProgObject;

import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于注解的模块生成器
 * 通过反射读取注解信息，生成Module对象
 * 
 * @since 2025-09-22
 */
@Slf4j
public final class ModuleGenneratorByAnno {
    /**
     * 实例属性键名
     */
    public static final String INSTANCE_ATTRS = "instanceAttrs";

    /**
     * 私有构造器，防止工具类被实例化
     */
    private ModuleGenneratorByAnno() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 根据约束算法类构建Module对象
     * 
     * @param moduleAlgClazz 约束算法类
     * @return 构建的Module对象
     */
    public static Module buildModule(Class<?> moduleAlgClazz) {
        Module module = new Module();

        // 1. 根据ModuleAnno信息创建Module
        ModuleAnno moduleAnno = moduleAlgClazz.getAnnotation(ModuleAnno.class);
        if (moduleAnno == null) {
            log.error("ModuleAnno not found for class: {}", moduleAlgClazz.getName());
            throw new AlgLoaderException("ModuleAnno not found for class: " + moduleAlgClazz.getName());
        }
        // 1. 设置模块基础属性
        buildModuleBase(module, moduleAnno, moduleAlgClazz);

        // 生成module.alg
        ModuleAlgArtifact alg = buildAlg(module, moduleAlgClazz);

        module.setAlg(alg);

        // 处理扩展属性
        buildExtAttr(moduleAnno, module);

        // 2. 遍历成员变量，创建Para和Part，根据fatherCode添加到对应的PartCategory
        Pair<List<Para>, List<IPart>> paraParts = buildParameterParts(moduleAlgClazz);
        module.setParas(paraParts.getFirst());
        module.addParts(paraParts.getSecond());
        // 处理多实例分类，根据PartAnno的instCodes属性进行展开
        MultiInstCategoryUtils.processInstCategory(module);
        module.init(); // 保证后续能使用getPara等函数
        // 4. 生成规则
        List<Rule> rules = createRulesFromMethods(moduleAlgClazz, module);
        module.setRules(rules);

        return module;
    }

    /**
     * 根据约束算法类和资源路径构建Module对象
     * 
     * @param moduleAlgClazz 约束算法类
     * @param resourcePath   资源路径
     * @return 构建的Module对象
     */
    public static Module build(Class<?> moduleAlgClazz, String resourcePath) {
        Module module = buildModule(moduleAlgClazz);

        // 保存Module到文件
        saveToFile(module, resourcePath);
        return module;
    }

    private static Optional<Para> createParaFromField(Field field) {
        ParaAnno paraAnno = field.getAnnotation(ParaAnno.class);
        if (paraAnno == null) {
            return Optional.empty();
        }

        Para para = new Para();

        // 生成Para.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        para.setCode(code);

        // 设置其他属性
        para.setFatherCode(paraAnno.fatherCode());
        para.setDefaultValue(paraAnno.defaultValue());
        para.setSortNo(paraAnno.sortNo());
        // 设置参数类型，直接使用注解中的类型
        para.setParaType(paraAnno.type());
        // 设置assignType（从注解）
        para.setAssignType(paraAnno.assignType());
        para.setExtSchema(paraAnno.extSchema());
        para.setMinValue(paraAnno.minValue());
        para.setMaxValue(paraAnno.maxValue());

        // 处理扩展属性
        if (paraAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = new HashMap<>();
            for (String attr : paraAnno.extAttrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    extAttrs.put(parts[0], parts[1]);
                }
            }
            para.setExtAttrs(extAttrs);
        }

        // 处理枚举选项
        if (paraAnno.options().length > 0) {
            List<DynamicAttributerOption> options = new ArrayList<>();
            for (int i = 0; i < paraAnno.options().length; i++) {
                String raw = paraAnno.options()[i];
                DynamicAttributerOption option = createDynamicAttributeOption(raw, i);
                options.add(option);
            }
            para.setOptions(options);
        }

        return Optional.of(para);
    }

    private static Optional<IPart> createPartFromField(Field field, Map<String, IPart> partMap) {
        PartAnno partAnno = field.getAnnotation(PartAnno.class);
        if (partAnno == null) {
            return Optional.empty();
        }

        // 根据字段类型决定部件类型
        IPart ipart = null;
        if (isPartCategoryVarField(field)) {
            PartCategory part = new PartCategory();
            ipart = part;
            part.setPartType(PartType.CATEGORY);
            part.setDefaultValue(0); // Part的默认值类型是Integer
            part.setDescription(partAnno.description());
            part.setSortNo(partAnno.sortNo());
            // part.setMaxQuantity(partAnno.maxQuantity());
            // part.setPrice(partAnno.price());
            part.setExtSchema(partAnno.extSchema());
        } else {
            // PartVar 对应 ATOMIC 类型，不应该有 dynAttrSchemas
            Part part = new Part();
            ipart = part;
            part.setPartType(PartType.ATOMIC);
            // 确保 ATOMIC 类型的部件没有 dynAttrSchemas
            part.setDynAttrSchemas(new ArrayList<>());
            part.setDefaultValue(0); // Part的默认值类型是Integer
            part.setDescription(partAnno.description());
            part.setSortNo(partAnno.sortNo());
            part.setMaxQuantity(partAnno.maxQuantity());
            part.setPrice(partAnno.price());
            part.setExtSchema(partAnno.extSchema());
        }

        // 生成Part.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        ipart.setCode(code);

        // 设置其他属性
        ipart.setFatherCode(partAnno.fatherCode());

        // 处理规格属性
        if (partAnno.attrs().length > 0 && ipart.getPartType() == PartType.ATOMIC && ipart.getFatherCode() != null) {
            PartCategory fatherPartCategory = (PartCategory) partMap.get(ipart.getFatherCode());
            Map<String, String> attrs = parseAttributes(partAnno.attrs(),
                    fatherPartCategory.queryDynAttrSchemas4NotInst());
            ipart.setDynAttr(attrs);

            // 处理实例规格属性
            processInstanceAttrs(ipart, partAnno, fatherPartCategory.queryDynAttrSchemas4Inst());
        }

        // 处理扩展属性
        if (partAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = parseAttributes(partAnno.extAttrs(), new ArrayList<>());
            ipart.setExtAttrs(extAttrs);
        }

        // 设置实例编码（用于多实例分类处理）
        MultiInstCategoryUtils.setInstcodes(partAnno, ipart);

        // 处理动态属性注解（只有CATEGORY类型的部件才需要）
        if (ipart.getPartType() == PartType.CATEGORY) {
            processDynamicAttributeAnnotations(field, (PartCategory) ipart);
            // 继承的属性在后面，方便实例属性等处理
            processInheritance(field, ipart, partMap);
        }

        return Optional.of(ipart);
    }

    /**
     * 从方法中创建规则
     * 
     * @param moduleAlgClazz 模块算法类
     * @param module         模块对象
     * @return 创建的规则列表（仅包含添加到Module的规则）
     */
    private static List<Rule> createRulesFromMethods(Class<?> moduleAlgClazz, Module module) {
        List<Rule> moduleRules = new ArrayList<>();

        // 遍历所有方法
        for (Method method : moduleAlgClazz.getDeclaredMethods()) {
            // 检查是否有CompatiableRuleAnno注解
            CompatiableRuleAnno compatiableRuleAnno = method.getAnnotation(CompatiableRuleAnno.class);
            if (compatiableRuleAnno != null) {
                addAttrParaToModuleOrPartCategory(compatiableRuleAnno.fatherCode(),
                        parseAndAddAttrParas(compatiableRuleAnno.attrParaCodes()), module);
                Rule rule = createCompatibleRule(method, compatiableRuleAnno, module);
                if (rule != null) {
                    // CompatiableRuleAnno 没有 fatherCode，默认添加到 Module
                    moduleRules.add(rule);
                }
            }

            // 检查是否有PriorityRuleAnno注解
            PriorityRuleAnno priorityRuleAnno = method.getAnnotation(PriorityRuleAnno.class);
            if (priorityRuleAnno != null) {
                addAttrParaToModuleOrPartCategory(priorityRuleAnno.fatherCode(),
                        parseAndAddAttrParas(priorityRuleAnno.attrParaCodes()), module);
                Rule rule = createPriorityRule(method, priorityRuleAnno, module);
                if (rule != null) {
                    addRuleToModuleOrPartCategory(rule, module, moduleRules);
                }
            }

            // 检查是否有CodeRuleAnno注解
            CodeRuleAnno codeRuleAnno = method.getAnnotation(CodeRuleAnno.class);
            if (codeRuleAnno != null) {
                addAttrParaToModuleOrPartCategory(codeRuleAnno.fatherCode(),
                        parseAndAddAttrParas(codeRuleAnno.attrParaCodes()), module);
                Rule rule = createCodeRule(method, codeRuleAnno, module);
                if (rule != null) {
                    addRuleToModuleOrPartCategory(rule, module, moduleRules);
                }
            }
        }

        return moduleRules;
    }

    private static void addAttrParaToModuleOrPartCategory(String fatherCode, List<AttrPara> attrParas, Module module) {
        if (attrParas.isEmpty()) {
            return;
        }
        if (Strings.isNullOrEmpty(fatherCode)) {
            // fatherCode 为 null 或空字符串，添加到 Module
            module.getAttrParas().addAll(deleteDuplicate(module.getAttrParas(), attrParas));
        } else {
            // fatherCode 不为空，添加到对应的 PartCategory
            IPart part = module.getPart(fatherCode);
            if (part instanceof PartCategory) {
                PartCategory partCategory = (PartCategory) part;
                partCategory.getAttrParas().addAll(deleteDuplicate(partCategory.getAttrParas(), attrParas));
            } else {
                log.error("PartCategory '{}' not found for attrParas '{}'..., adding to Module instead",
                        fatherCode, attrParas.get(0).getAttrCode());
                throw new AlgLoaderException("PartCategory '" + fatherCode + "' not found for attrParas '"
                        + attrParas.get(0).getAttrCode() + "...', adding to Module instead");
            }
        }
    }

    private static List<AttrPara> deleteDuplicate(List<AttrPara> existAttrParas, List<AttrPara> newAttrParas) {
        Set<String> attrCodes = existAttrParas.stream()
                .map(attrPara -> attrPara.getAttrCode() + ":" + attrPara.getType()).collect(Collectors.toSet());
        return newAttrParas.stream()
                .filter(attrPara -> !attrCodes.contains(attrPara.getAttrCode() + ":" + attrPara.getType()))
                .collect(Collectors.toList());
    }

    /**
     * 将规则添加到 Module 或 PartCategory
     * 根据规则的 fatherCode 字段决定添加到哪个位置s
     * 
     * @param rule        规则对象
     * @param module      模块对象
     * @param moduleRules Module 的规则列表
     */
    private static void addRuleToModuleOrPartCategory(Rule rule, Module module, List<Rule> moduleRules) {
        String fatherCode = rule.getFatherCode();
        if (Strings.isNullOrEmpty(fatherCode)) {
            // fatherCode 为 null 或空字符串，添加到 Module
            moduleRules.add(rule);
        } else {
            // fatherCode 不为空，添加到对应的 PartCategory
            IPart part = module.getPart(fatherCode);
            if (part instanceof PartCategory) {
                PartCategory partCategory = (PartCategory) part;
                if (partCategory.getRules() == null) {
                    partCategory.setRules(new ArrayList<>());
                }
                partCategory.getRules().add(rule);
                log.info("Rule '{}' added to PartCategory '{}'", rule.getCode(), fatherCode);
            } else {
                log.error("PartCategory '{}' not found for rule '{}', adding to Module instead",
                        fatherCode, rule.getCode());
                throw new AlgLoaderException("PartCategory '" + fatherCode + "' not found for rule '"
                        + rule.getCode() + "', adding to Module instead");
            }
        }
    }

    /**
     * 创建兼容性规则
     * 
     * @param method 方法对象
     * @param anno   兼容性规则注解
     * @param module 模块对象
     * @return 创建的兼容性规则
     */
    private static Rule createCompatibleRule(java.lang.reflect.Method method, CompatiableRuleAnno anno, Module module) {
        Rule rule = new Rule();

        // 璁剧疆鍩烘湰淇℃伅
        String methodName = method.getName();
        rule.setCode(methodName);
        rule.setFatherCode(anno.fatherCode());
        rule.setName(methodName);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatibleRule");

        // 设置作用范围
        rule.setEffectScope(anno.effectScope());

        // 创建CompatibleRuleSchema
        CompatiableRuleSchema schema = new CompatiableRuleSchema();
        schema.setType("CompatibleRule");
        schema.setVersion("1.0");

        // 创建左表达式
        if (!anno.leftExprCode().isEmpty()) {
            ExprSchema leftExpr = new ExprSchema();
            leftExpr.setRawCode(anno.leftExprCode());
            leftExpr.setRefProgObjs(generateRefProgObjSchemas(anno.leftExprCode(), module));
            schema.setLeftExpr(leftExpr);
        }

        // 设置操作符
        schema.setOperator(anno.operator());

        // 创建右表达式
        if (!anno.rightExprCode().isEmpty()) {
            ExprSchema rightExpr = new ExprSchema();
            rightExpr.setRawCode(anno.rightExprCode());
            rightExpr.setRefProgObjs(generateRefProgObjSchemas(anno.rightExprCode(), module));
            schema.setRightExpr(rightExpr);
        }

        rule.setRawCode(schema);
        return rule;
    }

    /**
     * 创建优先级规则
     * 
     * @param method 方法对象
     * @param anno   优先级规则注解
     * @param module 模块对象
     * @return 创建的优先级规则
     */
    private static Rule createPriorityRule(java.lang.reflect.Method method, PriorityRuleAnno anno, Module module) {
        Rule rule = new Rule();

        // 璁剧疆鍩烘湰淇℃伅
        String methodName = method.getName();
        String ruleCode = anno.code().isEmpty() ? methodName : anno.code();
        rule.setCode(ruleCode);
        rule.setFatherCode(anno.fatherCode());
        rule.setName(ruleCode);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName(RuleTypeConstants.PRIORITY_RULE_FULL_NAME);
        rule.setFatherCode(anno.fatherCode());

        // 设置作用范围
        rule.setEffectScope(anno.effectScope());

        // 创建PriorityRuleSchema
        PriorityRuleSchema schema = new PriorityRuleSchema();
        schema.setVersion("1.0");
        schema.setPriorityStrategy(anno.strategy());
        schema.setLeftRefProgObjs(new ArrayList<>());
        schema.setRightRefProgObjs(new ArrayList<>());

        // 解析leftProObjsStr，设置额外的左侧编程对象
        if (!anno.leftProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalLeftObjs = parseProObjsStr(anno.leftProObjsStr(), module);
            schema.getLeftRefProgObjs().addAll(additionalLeftObjs);
        }

        // 解析rightProObjsStr，设置额外的右侧编程对象
        if (!anno.rightProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalRightObjs = parseProObjsStr(anno.rightProObjsStr(), module);
            schema.getRightRefProgObjs().addAll(additionalRightObjs);
        }
        rule.setRawCode(schema);
        return rule;
    }

    /**
     * 创建代码规则
     * 
     * @param method 方法对象
     * @param anno   代码规则注解
     * @param module 模块对象
     * @return 创建的代码规则
     */
    private static Rule createCodeRule(java.lang.reflect.Method method, CodeRuleAnno anno, Module module) {
        Rule rule = new Rule();

        // 璁剧疆鍩烘湰淇℃伅
        String methodName = method.getName();
        rule.setCode(methodName);
        rule.setFatherCode(anno.fatherCode());
        rule.setName(methodName);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CodeRule");
        rule.setFatherCode(anno.fatherCode());
        rule.setLeftCardinality(anno.leftCardinality());
        rule.setRightCardinality(anno.rightCardinality());
        // 设置计算阶段
        rule.setCalcStage(anno.calcStage());

        // 创建CodeRuleSchema
        CodeRuleSchema schema = new CodeRuleSchema();
        schema.setType("CodeRule");
        schema.setVersion("1.0");
        schema.setRawCode(anno.code());

        // 解析左右两侧的引用编程对象
        List<List<RefProgObjSchema>> leftRightRefProgObjs = parseLeftRightRefProgObjSchemas(anno.code(),
                module);
        schema.setLeftRefProgObjs(leftRightRefProgObjs.get(0));
        schema.setRightRefProgObjs(leftRightRefProgObjs.get(1));

        // 设置作用范围
        rule.setEffectScope(anno.effectScope());

        // 解析leftProObjsStr，设置额外的左侧编程对象
        if (!anno.leftProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalLeftObjs = parseProObjsStr(anno.leftProObjsStr(), module);
            for (RefProgObjSchema refProgObj : additionalLeftObjs) {
                schema.getFromLeftProgObjs().add(refProgObj);
            }
        }

        // 解析rightProObjsStr，设置额外的右侧编程对象
        if (!anno.rightProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalRightObjs = parseProObjsStr(anno.rightProObjsStr(), module);
            for (RefProgObjSchema refProgObj : additionalRightObjs) {
                schema.getToRightProgObjs().add(refProgObj);
            }
        }

        rule.setRawCode(schema);
        return rule;
    }

    /**
     * 生成引用编程对象Schema列表
     * 
     * @param code          代码字符串
     * @param currentModule 当前模块
     * @return 引用编程对象Schema列表
     */
    private static List<RefProgObjSchema> generateRefProgObjSchemas(String code, Module currentModule) {
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        List<ProgObject> progObjects = parseVariableObjects(code);

        for (ProgObject progObject : progObjects) {
            RefProgObjSchema refProgObjSchema = new RefProgObjSchema();
            refProgObjSchema.setProgObjCode(progObject.getObjCode());
            refProgObjSchema.setProgObjField(progObject.getObjField());

            // 判断是Part还是Para
            if (currentModule.getPara(progObject.getObjCode()).isPresent()) {
                refProgObjSchema.setProgObjType(RefProgObjSchema.PROG_OBJ_TYPE_PARA);
            } else if (currentModule.getPart(progObject.getObjCode()) != null) {
                refProgObjSchema.setProgObjType(RefProgObjSchema.PROG_OBJ_TYPE_PART);
            } else {
                log.error("Object not found: {}", progObject.getObjCode());
                throw new AlgLoaderException("Object not found: " + progObject.getObjCode());
            }

            refProgObjs.add(refProgObjSchema);
        }

        return refProgObjs;
    }

    /**
     * 解析代码中的变量对象
     * 
     * @param code 代码字符串
     * @return 解析出的编程对象列表
     */
    private static List<ProgObject> parseVariableObjects(String code) {
        List<ProgObject> progObjects = new ArrayList<>();

        // 使用正则表达式匹配 "Object.Field" 模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)");
        java.util.regex.Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String objCode = matcher.group(1);
            // 如果objCode="colorVar"，则objCode="Color"
            if (objCode.endsWith("Var")) {
                objCode = objCode.substring(0, objCode.length() - "Var".length());
            }
            String objField = matcher.group(2);
            progObjects.add(new ProgObject(objCode, objField));
        }

        return progObjects;
    }

    /**
     * 解析左右两侧的引用编程对象Schema列表
     * 
     * @param normalNaturalCode 规范化自然语言代码
     * @param currentModule     当前模块
     * @return 包含两个列表的列表，第一个是左侧引用对象，第二个是右侧引用对象
     */
    private static List<List<RefProgObjSchema>> parseLeftRightRefProgObjSchemas(String normalNaturalCode,
            Module currentModule) {
        List<RefProgObjSchema> leftRefProgObjs = new ArrayList<>();
        List<RefProgObjSchema> rightRefProgObjs = new ArrayList<>();

        if (normalNaturalCode == null || normalNaturalCode.trim().isEmpty()) {
            return Arrays.asList(leftRefProgObjs, rightRefProgObjs);
        }

        log.info("Parsing rule: {}", normalNaturalCode);

        // 范式1：规则语句，如果有if A then B [else
        // C]，那么A里面的就是leftRefProgObjs,B和C就是rightRefProgObjs，根据then切割
        if (normalNaturalCode.contains("then ")) {
            log.info("Matched pattern 1: if-then");
            // 例如：normalNaturalCode= "if P0.value > 1 then P11.value > P0.value+1",
            // 则：leftRefProgObjs=P0,rightRefProgObjs=(P11,P0)
            String[] parts = normalNaturalCode.split(" then ");
            if (parts.length >= 2) {
                String leftPart = parts[0].replaceFirst(".*if\\s+", ""); // 鍘绘帀"if "鍓嶇紑
                String rightPart = parts[1];

                log.info("Left part: {}, Right part: {}", leftPart, rightPart);

                // 解析左侧部分
                leftRefProgObjs = generateRefProgObjSchemas(leftPart, currentModule);

                // 解析右侧部分（包括else部分）
                rightRefProgObjs = generateRefProgObjSchemas(rightPart, currentModule);

                // 日志内容保持英文，且修正大括号位置以符合代码规范
                log.info("Left refProgObjs: {}, Right refProgObjs: {}", leftRefProgObjs.size(),
                        rightRefProgObjs.size());
            }
        } else if (normalNaturalCode.contains(" = ")) { // 范式2：赋值语句 A=B，通过=识别，是返过来，右边决定左边
            // 例如：normalNaturalCode= "PT1.qty=P11.value ",
            // 则：leftRefProgObjs=P11,rightRefProgObjs=PT1
            String[] parts = normalNaturalCode.split(" = ");
            if (parts.length == 2) {
                String leftPart = parts[0].trim();
                String rightPart = parts[1].trim();

                // 对于赋值语句，右边决定左边，所以右边是leftRefProgObjs，左边是rightRefProgObjs
                leftRefProgObjs = generateRefProgObjSchemas(rightPart, currentModule);
                rightRefProgObjs = generateRefProgObjSchemas(leftPart, currentModule);
            }
        } else { // 范式3：其他语句，直接返回空列表
            log.error("Unsupported rule pattern: {}", normalNaturalCode);
            throw new AlgLoaderException("Unsupported rule pattern: " + normalNaturalCode);
        }

        return Arrays.asList(leftRefProgObjs, rightRefProgObjs);
    }

    /**
     * 解析编程对象描述字符串
     * 格式：progObjCode:progObjField|progObjField
     * 例如："drive:Select|Quantity" -> [RefProgObj(drive, Select), RefProgObj(drive,
     * Quantity)]
     *
     * @param proObjsStr 编程对象描述字符串
     * @param module     当前模块
     * @return 解析后的引用编程对象列表
     */
    private static List<RefProgObjSchema> parseProObjsStr(String proObjsStr, Module module) {
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();

        if (Strings.isNullOrEmpty(proObjsStr)) {
            return refProgObjs;
        }

        // 按逗号分隔多个ProgObj
        String[] objParts = proObjsStr.split(",");
        for (String objPart : objParts) {
            String trimmed = objPart.trim();
            if (Strings.isNullOrEmpty(trimmed)) {
                continue;
            }

            // 按冒号分隔progObjCode和属性列表
            String[] codeAndFields = trimmed.split(":");
            if (codeAndFields.length != 2) {
                throw new AlgLoaderException(
                        "Invalid proObjsStr format: " + trimmed + ", expected 'code:field1|field2'");
            }

            String progObjCode = codeAndFields[0].trim();
            String fieldsStr = codeAndFields[1].trim();

            // 检查是Part还是Para
            String progObjType;
            if (module.getPara(progObjCode).isPresent()) {
                progObjType = RefProgObjSchema.PROG_OBJ_TYPE_PARA;
            } else if (module.getAtomicPart(progObjCode) != null) {
                progObjType = RefProgObjSchema.PROG_OBJ_TYPE_PART;
            } else if (module.getPartCategory(progObjCode) != null) {
                progObjType = RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY;
            } else {
                throw new AlgLoaderException("Object not found: " + progObjCode);
            }

            // 按|分隔多个属性
            String[] fields = fieldsStr.split("\\|");
            for (String field : fields) {
                String fieldTrimmed = field.trim();
                if (Strings.isNullOrEmpty(fieldTrimmed)) {
                    continue;
                }

                RefProgObjSchema refProgObj = new RefProgObjSchema();
                refProgObj.setProgObjType(progObjType);
                refProgObj.setProgObjCode(progObjCode);
                refProgObj.setProgObjField(fieldTrimmed);
                refProgObjs.add(refProgObj);
            }
        }

        return refProgObjs;
    }

    /**
     * 构建扩展属性映射
     * 
     * @param moduleAnno 模块注解
     * @param module     模块对象
     */
    private static void buildExtAttr(ModuleAnno moduleAnno, Module module) {
        if (moduleAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = new HashMap<>();
            for (String attr : moduleAnno.extAttrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    extAttrs.put(parts[0], parts[1]);
                }
            }
            module.setExtAttrs(extAttrs);
        }
    }

    /**
     * 解析attrParaCodes并添加到module的attrParas列表中
     * 格式：attrCode:Type,attrCode:Type
     * 例如："Capacity:SumSum,Quantity:SumSum"
     * 
     * @param attrParaCodes 属性参数编码字符串
     */
    private static List<AttrPara> parseAndAddAttrParas(String attrParaCodes) {
        if (Strings.isNullOrEmpty(attrParaCodes)) {
            return new ArrayList<>();
        }
        List<AttrPara> attrParas = new ArrayList<>();
        // 按逗号分隔多个AttrPara
        String[] attrParaParts = attrParaCodes.split(",");
        for (String attrParaStr : attrParaParts) {
            String trimmed = attrParaStr.trim();
            if (Strings.isNullOrEmpty(trimmed)) {
                continue;
            }

            // 按冒号分隔attrCode和Type
            String[] codeAndType = trimmed.split(":");
            if (codeAndType.length != 2) {
                log.warn("Invalid attrPara format: {}, expected 'attrCode:Type'", trimmed);
                continue;
            }

            String attrCode = codeAndType[0].trim();
            String typeStr = codeAndType[1].trim();

            // 解析类型
            AttrParaType type = AttrParaType.valueOf(typeStr);

            // 创建AttrPara并添加到module
            AttrPara attrPara = new AttrPara();
            attrPara.setAttrCode(attrCode);
            attrPara.setType(type);
            attrParas.add(attrPara);

            log.info("Added AttrPara: attrCode={}, type={}", attrCode, type);
        }
        return attrParas;
    }

    /**
     * 构建Para和Part列表
     *
     * @param moduleAlgClazz 模块算法类
     * @return 包含Para列表和Part列表的Pair
     */
    private static Pair<List<Para>, List<IPart>> buildParaParts(Class<?> moduleAlgClazz) {
        List<Para> paras = new ArrayList<>();

        Map<String, IPart> partMap = new HashMap<>();
        // 首先收集所有字段，用于后续继承处理
        List<Field> partFields = new ArrayList<>();
        Map<String, String> fieldNameToPartCode = new HashMap<>();
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (isParaVarField(field)) {
                Optional<Para> paraOpt = createParaFromField(field);
                if (paraOpt.isPresent()) {
                    paras.add(paraOpt.get());
                }
            } else if (isPartLikeVarField(field)) {
                Optional<IPart> partOpt = createPartFromField(field, partMap);
                if (partOpt.isPresent()) {
                    partMap.put(partOpt.get().getCode(), partOpt.get());
                    fieldNameToPartCode.put(field.getName(), partOpt.get().getCode());
                }
                partFields.add(field);
            } else {
                log.info("ignore field type: " + field.getType().getSimpleName());
            }
        }
        List<IPart> parts = new ArrayList<>(partMap.values());

        return Pair.of(paras, parts);
    }

    /**
     * 构建参数部分，根据fatherCode添加到对应的PartCategory
     *
     * @param moduleAlgClazz 模块算法类
     * @return 包含Para列表和Part列表的Pair
     */
    private static Pair<List<Para>, List<IPart>> buildParameterParts(Class<?> moduleAlgClazz) {
        List<Para> paras = new ArrayList<>();
        Map<String, IPart> partMap = new HashMap<>();

        // 首先收集所有字段，用于后续继承处理
        List<Field> partFields = new ArrayList<>();
        Map<String, String> fieldNameToPartCode = new HashMap<>();

        // 收集Part字段
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (isPartLikeVarField(field)) {
                Optional<IPart> partOpt = createPartFromField(field, partMap);
                if (partOpt.isPresent()) {
                    partMap.put(partOpt.get().getCode(), partOpt.get());
                    fieldNameToPartCode.put(field.getName(), partOpt.get().getCode());
                }
                partFields.add(field);
            }
        }

        // 处理Para字段，根据fatherCode添加到对应的PartCategory
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (isParaVarField(field)) {
                Optional<Para> paraOpt = createParaFromField(field);
                if (paraOpt.isPresent()) {
                    Para para = paraOpt.get();
                    String fatherCode = para.getFatherCode();

                    // 如果有fatherCode，添加到对应的PartCategory
                    if (!Strings.isNullOrEmpty(fatherCode)) {
                        IPart fatherPart = partMap.get(fatherCode);
                        if (fatherPart instanceof PartCategory) {
                            PartCategory partCategory = (PartCategory) fatherPart;
                            if (partCategory.getParas() == null) {
                                partCategory.setParas(new ArrayList<>());
                            }
                            partCategory.getParas().add(para);
                            log.info("Parameter '{}' added to PartCategory '{}'", para.getCode(), fatherCode);
                        } else {
                            log.warn(
                                    "Father part '{}' not found or not a PartCategory for parameter '{}', adding to module level",
                                    fatherCode, para.getCode());
                            paras.add(para);
                        }
                    } else {
                        // 没有fatherCode的Para添加到模块级别
                        paras.add(para);
                    }
                }
            } else if (!isPartLikeVarField(field)) {
                log.info("ignore field type: " + field.getType().getSimpleName());
            }
        }

        List<IPart> parts = new ArrayList<>(partMap.values());
        return Pair.of(paras, parts);
    }

    /**
     * 处理所有部件的继承关系
     *
     * @param parts               部件列表
     * @param fieldNameToPartCode 字段名到部件编码的映射
     * @param partOverrideMap     部件重写映射
     */
    private static boolean isParaVarField(Field field) {
        return ParaVarImpl.class.isAssignableFrom(field.getType())
                || ParaVar.class.isAssignableFrom(field.getType())
                || "ParaVar".equals(field.getType().getSimpleName());
    }

    private static boolean isPartLikeVarField(Field field) {
        return isPartVarField(field) || isPartCategoryVarField(field);
    }

    private static boolean isPartVarField(Field field) {
        return PartVarImpl.class.isAssignableFrom(field.getType())
                || PartVar.class.isAssignableFrom(field.getType())
                || "PartVar".equals(field.getType().getSimpleName());
    }

    private static boolean isPartCategoryVarField(Field field) {
        return PartCategoryVarImpl.class.isAssignableFrom(field.getType())
                || PartCategoryVar.class.isAssignableFrom(field.getType())
                || "PartCategoryVar".equals(field.getType().getSimpleName());
    }

    private static void processInheritance(Field field, IPart part,
            Map<String, IPart> partMap) {
        Map<String, String> overrideMap = new HashMap<>();
        // 处理继承注解，为partOverrideMap赋值
        DAttrInherit inheritAnno = field.getAnnotation(DAttrInherit.class);
        if (inheritAnno != null) {

            // 解析重写属性
            String[] overrideAttrs = inheritAnno.overrideAttrs();
            for (String override : overrideAttrs) {
                String[] partCodes = override.split(":");
                if (partCodes.length == 2) {
                    overrideMap.put(partCodes[0], partCodes[1]);
                }
            }
        }

        // 处理每个部件的继承
        if (part instanceof PartCategory && !Strings.isNullOrEmpty(part.getFatherCode())) {
            PartCategory partCategory = (PartCategory) part;
            IPart parentPart = partMap.get(part.getFatherCode());
            if (null == parentPart) {
                log.error("Parent part not found,please check the order {}", part.getFatherCode());
                throw new AlgLoaderException("Parent part not found:please check the order " + part.getFatherCode());
            }
            inheritFromParent(partCategory, (PartCategory) parentPart, overrideMap);
        }

    }

    /**
     * 从父部件继承属性
     *
     * @param child       子部件
     * @param parent      父部件
     * @param overrideMap 重写属性映射
     */
    private static void inheritFromParent(PartCategory child, PartCategory parent, Map<String, String> overrideMap) {
        // 复制父部件的所有动态属性
        for (DynamicAttribute parentAttr : parent.getDynAttrSchemas()) {
            try {
                // 深拷贝属性
                DynamicAttribute childAttr = copyDynamicAttribute(parentAttr);

                // 检查是否有重写
                String overrideValue = overrideMap.get(parentAttr.getCode());
                if (overrideValue != null) {
                    // 去除所有空格
                    overrideValue = overrideValue.replaceAll("\\s+", "");
                }

                if (overrideValue != null && overrideValue.startsWith("instType=")) {
                    String instTypeStr = overrideValue.substring("instType=".length());
                    try {
                        int instType = Integer.parseInt(instTypeStr);
                        childAttr.setInstType(instType);
                    } catch (NumberFormatException e) {
                        // 保持原有值
                    }
                }

                // 添加到子部件
                child.getDynAttrSchemas().add(childAttr);
            } catch (Exception e) {
                log.error("Failed to inherit attribute: " + parentAttr.getCode(), e);
            }
        }
    }

    /**
     * 深拷贝动态属性
     *
     * @param original 原始属性
     * @return 拷贝的属性
     */
    private static DynamicAttribute copyDynamicAttribute(DynamicAttribute original) {
        DynamicAttribute copy = new DynamicAttribute();
        copy.setCode(original.getCode());
        copy.setName(original.getName());
        copy.setDynAttrType(original.getDynAttrType());
        copy.setValue(original.getValue());
        copy.setOptionExtSchema(original.getOptionExtSchema());
        copy.setInstType(original.getInstType());

        // 深拷贝选项列表
        List<DynamicAttributerOption> copiedOptions = new ArrayList<>();
        for (DynamicAttributerOption option : original.getOptions()) {
            DynamicAttributerOption copiedOption = new DynamicAttributerOption();
            copiedOption.setCode(option.getCode());
            copiedOption.setCodeId(option.getCodeId());
            copiedOption.setCodeValue(option.getCodeValue());
            copiedOption.setDefaultValue(option.getDefaultValue());
            copiedOption.setDescription(option.getDescription());
            copiedOption.setSortNo(option.getSortNo());
            copiedOptions.add(copiedOption);
        }
        copy.setOptions(copiedOptions);

        return copy;
    }

    /**
     * 构建ModuleAlgArtifact对象
     * 
     * @param module         模块对象
     * @param moduleAlgClazz 模块算法类
     * @return 配置好的ModuleAlgArtifact对象
     */
    private static ModuleAlgArtifact buildAlg(Module module, Class<?> moduleAlgClazz) {
        ModuleAlgArtifact alg = new ModuleAlgArtifact();
        alg.setId(module.getId());
        alg.setModuleCode(module.getCode());
        alg.setFileName(moduleAlgClazz.getName());
        alg.setPackageName(moduleAlgClazz.getPackage().getName());
        AlgorithmApiVersion apiVersion = moduleAlgClazz.getAnnotation(AlgorithmApiVersion.class);
        if (apiVersion != null) {
            alg.setSouthApiVersion(apiVersion.southApiVersion());
            alg.setAlgorithmVersion(apiVersion.algorithmVersion());
        }

        // 检查是否为内部类，设置parentClassName
        Class<?> enclosingClass = moduleAlgClazz.getEnclosingClass();
        if (enclosingClass != null) {
            // 是内部类，设置父类名称
            alg.setParentClassName(enclosingClass.getSimpleName());
            log.info("Detected inner class: {}, parent class: {}", moduleAlgClazz.getName(),
                    enclosingClass.getName());
        } else {
            // 不是内部类，保持默认空字符串
            alg.setParentClassName("");
        }

        return alg;
    }

    /**
     * 构建模块基础属性
     * 
     * @param module         模块对象
     * @param moduleAnno     模块注解
     * @param moduleAlgClazz 模块算法类
     */
    private static void buildModuleBase(Module module, ModuleAnno moduleAnno, Class<?> moduleAlgClazz) {
        // 1.1 生成Module.code
        String className = moduleAlgClazz.getSimpleName();
        // 优化代码生成逻辑：只去掉末尾的"Constraint"
        String code = className;
        if (className.endsWith("Constraint")) {
            code = className.substring(0, className.length() - "Constraint".length());
        }
        module.setCode(code);

        // 1.2 设置其他属性
        module.setId(moduleAnno.id());
        module.setPackageName(moduleAnno.packageName().isEmpty() ? moduleAlgClazz.getPackage().getName()
                : moduleAnno.packageName());
        module.setVersion(moduleAnno.version());
        module.setDescription(moduleAnno.description());
        module.setSortNo(moduleAnno.sortNo());
        module.setExtSchema(moduleAnno.extSchema());
    }

    /**
     * 保存模块到文件
     * 
     * @param module       模块对象
     * @param resourcePath 资源路径
     */
    private static void saveToFile(Module module, String resourcePath) {
        String fileName = module.getCode() + ".base.json";
        String filePath = resourcePath + File.separator + fileName;

        // 标准化路径，解决Windows路径问题
        try {
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1, filePath.length());
            }
            Path normalizedPath = Paths.get(filePath).normalize();
            filePath = normalizedPath.toString();
        } catch (Exception e) {
            log.error("Failed to normalize path: {}, using original path", filePath);
        }

        try {
            ModuleUtils.toJsonFile(module, filePath);
            log.info("Module saved to: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to save Module", e);
        }
    }

    /**
     * 创建参数选项
     * 
     * @param raw   原始选项字符串
     * @param index 选项索引
     * @return 创建的ParaOption对象
     */
    private static DynamicAttributerOption createDynamicAttributeOption(String raw, int index) {
        String label = raw;
        Integer explicitId = null;
        int sep = raw.indexOf(':');
        if (sep >= 0) {
            String left = raw.substring(0, sep).trim();
            String right = raw.substring(sep + 1).trim();
            // Preferred: label:id
            if (!right.isEmpty() && right.chars().allMatch(Character::isDigit)) {
                label = left;
                explicitId = Integer.parseInt(right);
            } else if (!left.isEmpty() && left.chars().allMatch(Character::isDigit)) {
                // Backward compatibility: id:label
                explicitId = Integer.parseInt(left);
                label = right;
            } else {
                // Fallback: treat whole as label
                label = raw.trim();
            }
        }
        DynamicAttributerOption option = new DynamicAttributerOption();
        option.setCode(label);
        option.setCodeValue(label);
        option.setCodeId(explicitId != null ? explicitId : (index + 1) * 10);
        option.setDefaultValue(label);
        option.setSortNo(index + 1);
        return option;
    }

    /**
     * 解析属性字符串数组为Map
     *
     * @param attributes 属性字符串数组，格式为 "key:value"
     * @return 解析后的属性Map
     */
    private static Map<String, String> parseAttributes(String[] attributes, List<DynamicAttribute> dynAttrSchemas) {
        Map<String, String> attrs = new HashMap<>();
        // 如果 attributes.length！= dynAttrSchemas.size() 打日志，并抛异常
        if (attributes.length != dynAttrSchemas.size()) {
            log.error(
                    "Attributes array length ({}) does not match dynamic attribute schemas size ({}). Expected: {}, Actual: {}",
                    attributes.length, dynAttrSchemas.size(), dynAttrSchemas.size(), attributes.length);
            throw new AlgLoaderException("Attributes array length mismatch: expected " + dynAttrSchemas.size()
                    + ", but got " + attributes.length);
        }

        for (int i = 0; i < attributes.length && i < dynAttrSchemas.size(); i++) {
            String attribute = attributes[i];
            DynamicAttribute dynAttrSchema = dynAttrSchemas.get(i);

            String attrCode = "";
            String attrValue = "";

            // 判断attributes[i]是否有":"
            if (attribute.contains(":")) {
                // "attrCode:attrValue"模式
                String[] parts = attribute.split(":", 2);
                attrCode = parts[0].trim();
                attrValue = parts[1].trim();

                // 校验 dynAttrSchema.getCode 和 attrCode是否相等
                if (!dynAttrSchema.getCode().equals(attrCode)) {
                    String errorMessage = String.format("Attribute code mismatch: expected %s, got %s",
                            dynAttrSchema.getCode(), attrCode);
                    log.error(errorMessage);
                    throw new AlgLoaderException(errorMessage);
                }

                // 校验 dynAttrSchema.queryOptionByCodeValue(attrValue)是否存在
                if (dynAttrSchema.queryOptionByCodeValue(attrValue) == null) {
                    String errorMessage = String
                            .format("Option not found for codeValue: {%s} in attribute {%s}", attrValue, attrCode);
                    log.error(errorMessage);
                    throw new AlgLoaderException(errorMessage);
                }
            } else {
                // "attrValue"模式
                attrValue = attribute.trim();
                attrCode = dynAttrSchema.getCode();

                // 校验dynAttrSchema.queryOptionByCodeValue(attrValue)是否存在
                if (dynAttrSchema.queryOptionByCodeValue(attrValue) == null) {
                    String errorMessage = String
                            .format("Option not found for codeValue: {%s} in attribute {%s}", attrValue, attrCode);
                    log.error(errorMessage);
                    throw new AlgLoaderException(errorMessage);
                }
            }

            attrs.put(attrCode, attrValue);
        }

        return attrs;
    }

    /**
     * 处理动态属性注解
     *
     * @param field        字段对象
     * @param partCategory 部件分类对象
     */
    private static void processDynamicAttributeAnnotations(Field field, PartCategory partCategory) {
        // 处理DAttrAnno1
        DAttrAnno1 dAttrAnno1 = field.getAnnotation(DAttrAnno1.class);
        if (dAttrAnno1 != null) {
            addDynamicAttribute(partCategory, dAttrAnno1.code(), dAttrAnno1.optionExtSchema(),
                    dAttrAnno1.options(), dAttrAnno1.instType(), dAttrAnno1.dynAttrType());
        }

        // 处理DAttrAnno2
        DAttrAnno2 dAttrAnno2 = field.getAnnotation(DAttrAnno2.class);
        if (dAttrAnno2 != null) {
            addDynamicAttribute(partCategory, dAttrAnno2.code(), dAttrAnno2.optionExtSchema(),
                    dAttrAnno2.options(), dAttrAnno2.instType(), dAttrAnno2.dynAttrType());
        }

        // 处理DAttrAnno3
        DAttrAnno3 dAttrAnno3 = field.getAnnotation(DAttrAnno3.class);
        if (dAttrAnno3 != null) {
            addDynamicAttribute(partCategory, dAttrAnno3.code(), dAttrAnno3.optionExtSchema(),
                    dAttrAnno3.options(), dAttrAnno3.instType(), dAttrAnno3.dynAttrType());
        }

        // 处理DAttrAnno4
        DAttrAnno4 dAttrAnno4 = field.getAnnotation(DAttrAnno4.class);
        if (dAttrAnno4 != null) {
            addDynamicAttribute(partCategory, dAttrAnno4.code(), dAttrAnno4.optionExtSchema(),
                    dAttrAnno4.options(), dAttrAnno4.instType(), dAttrAnno4.dynAttrType());
        }

        // 处理DAttrAnno5
        DAttrAnno5 dAttrAnno5 = field.getAnnotation(DAttrAnno5.class);
        if (dAttrAnno5 != null) {
            addDynamicAttribute(partCategory, dAttrAnno5.code(), dAttrAnno5.optionExtSchema(),
                    dAttrAnno5.options(), dAttrAnno5.instType(), dAttrAnno5.dynAttrType());
        }

        // 处理DAttrAnno11
        DAttrAnno11 dAttrAnno11 = field.getAnnotation(DAttrAnno11.class);
        if (dAttrAnno11 != null) {
            addDynamicAttribute(partCategory, dAttrAnno11.code(), dAttrAnno11.optionExtSchema(),
                    dAttrAnno11.options(), dAttrAnno11.instType(), dAttrAnno11.dynAttrType());
        }

        // 处理DAttrAnno12
        DAttrAnno12 dAttrAnno12 = field.getAnnotation(DAttrAnno12.class);
        if (dAttrAnno12 != null) {
            addDynamicAttribute(partCategory, dAttrAnno12.code(), dAttrAnno12.optionExtSchema(),
                    dAttrAnno12.options(), dAttrAnno12.instType(), dAttrAnno12.dynAttrType());
        }

        // 处理DAttrAnno13
        DAttrAnno13 dAttrAnno13 = field.getAnnotation(DAttrAnno13.class);
        if (dAttrAnno13 != null) {
            addDynamicAttribute(partCategory, dAttrAnno13.code(), dAttrAnno13.optionExtSchema(),
                    dAttrAnno13.options(), dAttrAnno13.instType(), dAttrAnno13.dynAttrType());
        }
    }

    private static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 添加动态属性
     *
     * @param partCategory    部件分类对象
     * @param code            属性编码
     * @param optionExtSchema 扩展模式
     * @param options         可选值列表
     * @param instType        实例类型
     * @param dynAttrType     动态属性类型
     */
    private static void addDynamicAttribute(PartCategory partCategory, String code, String optionExtSchema,
            String[] options, int instType, DynamicAttributeType dynAttrType) {
        DynamicAttribute dynAttr = new DynamicAttribute();
        dynAttr.setCode(code);
        dynAttr.setName(code);
        dynAttr.setOptionExtSchema(optionExtSchema);
        dynAttr.setDynAttrType(dynAttrType);

        // 解析选项
        List<DynamicAttributerOption> dynOptions = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            String optionStr = options[i];
            String[] parts = optionStr.split(":");
            if (parts.length >= 3) {
                DynamicAttributerOption option = new DynamicAttributerOption();
                option.setCode(parts[0]);
                // 如果parts[1]是数字，则设置为codeId
                if (isNumber(parts[1])) {
                    option.setCodeId(Integer.parseInt(parts[1]));
                } else {
                    option.setCodeId(i + 1);
                }
                option.setCodeValue(parts[1]);
                option.setDefaultValue(parts[1]); // 值
                option.setDescription(parts[2]); // 单位或描述
                option.setSortNo(i + 1);
                dynOptions.add(option);
            } else if (parts.length >= 2) {
                DynamicAttributerOption option = new DynamicAttributerOption();
                option.setCode(parts[0]);
                // 如果parts[1]是数字，则设置为codeId
                if (isNumber(parts[1])) {
                    option.setCodeId(Integer.parseInt(parts[1]));
                } else {
                    option.setCodeId(i + 1);
                }
                option.setCodeValue(parts[1]);
                option.setDefaultValue(parts[1]); // 值
                option.setSortNo(i + 1);
                dynOptions.add(option);
            } else {
                log.warn("Invalid option format: {}", optionStr);
            }
        }
        dynAttr.setOptions(dynOptions);
        dynAttr.setInstType(instType);

        partCategory.getDynAttrSchemas().add(dynAttr);
    }

    /**
     * 处理继承注解
     *
     * @param field               字段对象
     * @param partCategory        部件分类对象
     * @param fieldNameToPartCode 字段名到部件编码的映射
     * @return 重写属性映射
     */
    private static Map<String, String> processInheritAnnotation(Field field, PartCategory partCategory,
            Map<String, String> fieldNameToPartCode) {
        Map<String, String> overrideMap = new HashMap<>();

        DAttrInherit inheritAnno = field.getAnnotation(DAttrInherit.class);
        if (inheritAnno != null) {
            // 将字段名映射为部件编码
            String fatherFieldName = inheritAnno.fatherCode();
            String fatherPartCode = fieldNameToPartCode.get(fatherFieldName);
            if (fatherPartCode != null) {
                partCategory.setFatherCode(fatherPartCode);
            } else {
                // 如果找不到映射，直接使用字段名
                partCategory.setFatherCode(fatherFieldName);
            }

            // 解析重写属性
            String[] overrideAttrs = inheritAnno.overrideAttrs();
            for (String override : overrideAttrs) {
                String[] parts = override.split(":");
                if (parts.length == 2) {
                    overrideMap.put(parts[0], parts[1]);
                }
            }
        }

        return overrideMap;
    }

    /**
     * 处理实例规格属性
     *
     * @param part     部件对象
     * @param partAnno 部件注解
     */
    private static void processInstanceAttrs(IPart part, PartAnno partAnno, List<DynamicAttribute> dynAttrSchemas) {
        // 检查是否有实例属性
        if (partAnno.attrsInst1().length > 0 || partAnno.attrsInst2().length > 0 ||
                partAnno.attrsInst3().length > 0 || partAnno.attrsInst4().length > 0) {

            // 创建 InstanceDynAttrValue 容器对象
            InstanceDynAttrValue instanceDynAttrValue = new InstanceDynAttrValue();

            // 处理各个实例
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst1(), 1, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst2(), 2, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst3(), 3, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst4(), 4, dynAttrSchemas);

            // 将序列化后的数据存储到 dynAttr 中
            Map<String, String> dynAttr = part.getDynAttr();
            if (dynAttr == null) {
                dynAttr = new HashMap<>();
                part.setDynAttr(dynAttr);
            }
            dynAttr.put(INSTANCE_ATTRS, InstanceDynAttrValue.toJsonString(instanceDynAttrValue));
        }
    }

    /**
     * 添加实例属性到 InstanceDynAttrValue 容器中
     *
     * @param container InstanceDynAttrValue 容器对象
     * @param attrs     属性数组
     * @param instId    实例ID
     */
    private static void addInstanceAttr(InstanceDynAttrValue container, String[] attrs, int instId,
            List<DynamicAttribute> dynAttrSchemas) {
        if (attrs.length > 0) {
            InstanceDynAttrValueItem instValue = new InstanceDynAttrValueItem();
            instValue.setInstId(instId);
            Map<String, String> instAttr = parseAttributes(attrs, dynAttrSchemas);
            instValue.setInstAttrs(instAttr);
            container.getInstsValues().add(instValue);
        }
    }
}
