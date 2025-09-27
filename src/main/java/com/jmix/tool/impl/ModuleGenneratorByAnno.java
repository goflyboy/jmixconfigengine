package com.jmix.tool.impl;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.ModuleAlgArtifact;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaOption;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.imodel.Rule;
import com.jmix.executor.imodel.rule.CodeRuleSchema;
import com.jmix.executor.imodel.rule.CompatiableRuleSchema;
import com.jmix.executor.imodel.rule.ExprSchema;
import com.jmix.executor.imodel.rule.RefProgObjSchema;
import com.jmix.executor.impl.algmodel.ConstraintAlg;
import com.jmix.executor.impl.algmodel.ParaVar;
import com.jmix.executor.impl.algmodel.PartVar;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.impl.util.Pair;
import com.jmix.executor.omodel.AlgLoaderException;
import com.jmix.tool.anno.CodeRuleAnno;
import com.jmix.tool.anno.CompatiableRuleAnno;
import com.jmix.tool.anno.ModuleAnno;
import com.jmix.tool.anno.ParaAnno;
import com.jmix.tool.anno.PartAnno;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于注解的模块生成器
 * 通过反射读取注解信息，生成Module对象
 * 
 * @since 2025-09-22
 */
@Slf4j
public final class ModuleGenneratorByAnno {
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
    public static Module buildModule(Class<? extends ConstraintAlg> moduleAlgClazz) {
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

        // 2. 遍历成员变量，创建Para和Part
        Pair<List<Para>, List<Part>> paraParts = buildParaParts(moduleAlgClazz);
        module.setParas(paraParts.getFirst());
        module.setParts(paraParts.getSecond());
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
    public static Module build(Class<? extends ConstraintAlg> moduleAlgClazz, String resourcePath) {
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
        para.setDescription(paraAnno.description());
        para.setSortNo(paraAnno.sortNo());
        para.setType(paraAnno.type());
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
            List<ParaOption> options = new ArrayList<>();
            for (int i = 0; i < paraAnno.options().length; i++) {
                String raw = paraAnno.options()[i];
                ParaOption option = createParaOption(raw, i);
                options.add(option);
            }
            para.setOptions(options);
        }

        return Optional.of(para);
    }

    private static Optional<Part> createPartFromField(Field field) {
        PartAnno partAnno = field.getAnnotation(PartAnno.class);
        if (partAnno == null) {
            return Optional.empty();
        }

        Part part = new Part();

        // 生成Part.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        part.setCode(code);

        // 设置其他属性
        part.setFatherCode(partAnno.fatherCode());
        part.setDefaultValue(0); // Part的默认值类型是Integer
        part.setDescription(partAnno.description());
        part.setSortNo(partAnno.sortNo());
        part.setMaxQuantity(partAnno.maxQuantity());
        part.setType(partAnno.type());
        part.setPrice(partAnno.price());
        part.setExtSchema(partAnno.extSchema());

        // 处理规格属性
        if (partAnno.attrs().length > 0) {
            Map<String, String> attrs = parseAttributes(partAnno.attrs());
            part.setAttrs(attrs);
        }

        // 处理扩展属性
        if (partAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = parseAttributes(partAnno.extAttrs());
            part.setExtAttrs(extAttrs);
        }

        return Optional.of(part);
    }

    /**
     * 从方法中创建规则
     * 
     * @param moduleAlgClazz 模块算法类
     * @param module         模块对象
     * @return 创建的规则列表
     */
    private static List<Rule> createRulesFromMethods(Class<?> moduleAlgClazz, Module module) {
        List<Rule> rules = new ArrayList<>();

        // 遍历所有方法
        for (java.lang.reflect.Method method : moduleAlgClazz.getDeclaredMethods()) {
            // 检查是否有CompatiableRuleAnno注解
            CompatiableRuleAnno compatiableRuleAnno = method.getAnnotation(CompatiableRuleAnno.class);
            if (compatiableRuleAnno != null) {
                Rule rule = createCompatibleRule(method, compatiableRuleAnno, module);
                if (rule != null) {
                    rules.add(rule);
                }
            }

            // 检查是否有CodeRuleAnno注解
            CodeRuleAnno codeRuleAnno = method.getAnnotation(CodeRuleAnno.class);
            if (codeRuleAnno != null) {
                Rule rule = createCodeRule(method, codeRuleAnno, module);
                if (rule != null) {
                    rules.add(rule);
                }
            }
        }

        return rules;
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

        // 设置基本信息
        String methodName = method.getName();
        rule.setCode(methodName);
        rule.setName(methodName);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatibleRule");

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
     * 创建代码规则
     * 
     * @param method 方法对象
     * @param anno   代码规则注解
     * @param module 模块对象
     * @return 创建的代码规则
     */
    private static Rule createCodeRule(java.lang.reflect.Method method, CodeRuleAnno anno, Module module) {
        Rule rule = new Rule();

        // 设置基本信息
        String methodName = method.getName();
        rule.setCode(methodName);
        rule.setName(methodName);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CodeRule");

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
                refProgObjSchema.setProgObjType("Para");
            } else if (currentModule.getPart(progObject.getObjCode()).isPresent()) {
                refProgObjSchema.setProgObjType("Part");
            } else {
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

        log.debug("Parsing rule: {}", normalNaturalCode);

        // 范式1：规则语句，如果有if A then B [else
        // C]，那么A里面的就是leftRefProgObjs,B和C就是rightRefProgObjs，根据then切割
        if (normalNaturalCode.contains("then ")) {
            log.debug("Matched pattern 1: if-then");
            // 例如：normalNaturalCode= "if P0.value > 1 then P11.value > P0.value+1",
            // 则：leftRefProgObjs=P0,rightRefProgObjs=(P11,P0)
            String[] parts = normalNaturalCode.split(" then ");
            if (parts.length >= 2) {
                String leftPart = parts[0].replaceFirst(".*if\\s+", ""); // 去掉"if "前缀
                String rightPart = parts[1];

                log.debug("Left part: {}, Right part: {}", leftPart, rightPart);

                // 解析左侧部分
                leftRefProgObjs = generateRefProgObjSchemas(leftPart, currentModule);

                // 解析右侧部分（包括else部分）
                rightRefProgObjs = generateRefProgObjSchemas(rightPart, currentModule);

                // 日志内容保持英文，且修正大括号位置以符合代码规范
                log.debug("Left refProgObjs: {}, Right refProgObjs: {}", leftRefProgObjs.size(),
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
     * 构建Para和Part列表
     * 
     * @param moduleAlgClazz 模块算法类
     * @return 包含Para列表和Part列表的Pair
     */
    private static Pair<List<Para>, List<Part>> buildParaParts(Class<?> moduleAlgClazz) {
        List<Para> paras = new ArrayList<>();
        List<Part> parts = new ArrayList<>();

        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (field.getType().getSimpleName().equals(ParaVar.class.getSimpleName())) {
                Optional<Para> paraOpt = createParaFromField(field);
                if (paraOpt.isPresent()) {
                    paras.add(paraOpt.get());
                }
            } else if (field.getType().getSimpleName().equals(PartVar.class.getSimpleName())) {
                Optional<Part> partOpt = createPartFromField(field);
                if (partOpt.isPresent()) {
                    parts.add(partOpt.get());
                }
            } else {
                log.info("ignore field type: " + field.getType().getSimpleName());
            }
        }

        return Pair.of(paras, parts);
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
    private static ParaOption createParaOption(String raw, int index) {
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
        ParaOption option = new ParaOption();
        option.setCode(label);
        option.setCodeId(explicitId != null ? explicitId : (index + 1) * 10);
        option.setDefaultValue(label);
        option.setDescription("");
        option.setSortNo(index + 1);
        return option;
    }

    /**
     * 解析属性字符串数组为Map
     * 
     * @param attributes 属性字符串数组，格式为 "key:value"
     * @return 解析后的属性Map
     */
    private static Map<String, String> parseAttributes(String[] attributes) {
        Map<String, String> attrs = new HashMap<>();
        for (String attr : attributes) {
            String[] parts = attr.split(":");
            if (parts.length == 2) {
                attrs.put(parts[0], parts[1]);
            }
        }
        return attrs;
    }
}