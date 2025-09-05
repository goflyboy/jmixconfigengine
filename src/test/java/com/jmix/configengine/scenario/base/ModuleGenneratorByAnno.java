package com.jmix.configengine.scenario.base;

import com.jmix.configengine.artifact.ConstraintAlg;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.ModuleAlgArtifact;
import com.jmix.configengine.model.Rule;
import com.jmix.configengine.schema.CompatiableRuleSchema;
import com.jmix.configengine.schema.CodeRuleSchema;
import com.jmix.configengine.schema.ExprSchema;
import com.jmix.configengine.schema.RefProgObjSchema;
import com.jmix.configengine.util.ModuleUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 通过注解生成Module的工具类
 */
@Slf4j
public class ModuleGenneratorByAnno {
    
    public static Module build(Class<? extends ConstraintAlg> moduleAlgClazz, String resourcePath) {
        Module module = new Module();
        
        // 1. 根据ModuleAnno信息创建Module
        ModuleAnno moduleAnno = moduleAlgClazz.getAnnotation(ModuleAnno.class);
        if (moduleAnno != null) {
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
            module.setPackageName(moduleAnno.packageName().isEmpty() ? 
                moduleAlgClazz.getPackage().getName() : moduleAnno.packageName());
            module.setVersion(moduleAnno.version());
            module.setDescription(moduleAnno.description());
            module.setSortNo(moduleAnno.sortNo());
            module.setExtSchema(moduleAnno.extSchema());

            //生成module.alg
            ModuleAlgArtifact alg = new ModuleAlgArtifact();
            alg.setId(module.getId());
            alg.setFileName(moduleAlgClazz.getName());
            alg.setPackageName(moduleAlgClazz.getPackage().getName());
            
            // 检查是否为内部类，设置parentClassName
            Class<?> enclosingClass = moduleAlgClazz.getEnclosingClass();
            if (enclosingClass != null) {
                // 是内部类，设置父类名称
                alg.setParentClassName(enclosingClass.getSimpleName());
                log.info("检测到内部类: {}，父类: {}", moduleAlgClazz.getName(), enclosingClass.getName());
            } else {
                // 不是内部类，保持默认空字符串
                alg.setParentClassName("");
            }
            
            module.setAlg(alg);
            
            // 处理扩展属性
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
        
        // 2. 遍历成员变量，创建Para和Part
        List<Para> paras = new ArrayList<>();
        List<Part> parts = new ArrayList<>();
        
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (field.getType().getSimpleName().equals("ParaVar")) {
                Para para = createParaFromField(field);
                if (para != null) {
                    paras.add(para);
                }
            } else if (field.getType().getSimpleName().equals("PartVar")) {
                Part part = createPartFromField(field);
                if (part != null) {
                    parts.add(part);
                }
            }
        }
        
        module.setParas(paras);
        module.setParts(parts);
        module.init();//保证后续能使用getPara等函数
        // 4. 生成规则
        List<Rule> rules = createRulesFromMethods(moduleAlgClazz, module);
        module.setRules(rules);
        
        // 3. 保存Module到文件
        String fileName = module.getCode() + ".base.json";
        String filePath = resourcePath + "/" + fileName;
        
        try {
            ModuleUtils.toJsonFile(module, filePath);
            log.info("Module已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("保存Module失败", e);
        }
        
        return module;
    }
    
    private static Para createParaFromField(Field field) {
        ParaAnno paraAnno = field.getAnnotation(ParaAnno.class);
        if (paraAnno == null) return null;
        
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
                option.setCodeId(explicitId != null ? explicitId : (i + 1) * 10);
                option.setDefaultValue(label);
                option.setDescription("");
                option.setSortNo(i + 1);
                options.add(option);
            }
            para.setOptions(options);
        }
        
        return para;
    }
    
    private static Part createPartFromField(Field field) {
        PartAnno partAnno = field.getAnnotation(PartAnno.class);
        if (partAnno == null) return null;
        
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
            Map<String, String> attrs = new HashMap<>();
            for (String attr : partAnno.attrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    attrs.put(parts[0], parts[1]);
                }
            }
            part.setAttrs(attrs);
        }
        
        // 处理扩展属性
        if (partAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = new HashMap<>();
            for (String attr : partAnno.extAttrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    extAttrs.put(parts[0], parts[1]);
                }
            }
            part.setExtAttrs(extAttrs);
        }
        
        return part;
    }
    
    /**
     * 从方法中创建规则
     */
    private static List<Rule> createRulesFromMethods(Class<?> moduleAlgClazz, Module module) {
        List<Rule> rules = new ArrayList<>();
        
        // 遍历所有方法
        for (java.lang.reflect.Method method : moduleAlgClazz.getDeclaredMethods()) {
            // 检查是否有CompatiableRuleAnno注解
            CompatiableRuleAnno compatiableRuleAnno = method.getAnnotation(CompatiableRuleAnno.class);
            if (compatiableRuleAnno != null) {
                Rule rule = createCompatiableRule(method, compatiableRuleAnno, module);
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
     */
    private static Rule createCompatiableRule(java.lang.reflect.Method method, CompatiableRuleAnno anno, Module module) {
        Rule rule = new Rule();
        
        // 设置基本信息
        String methodName = method.getName();
        rule.setCode(methodName);
        rule.setName(methodName);
        rule.setProgObjType("Module");
        rule.setProgObjCode(module.getCode());
        rule.setProgObjField("constraints");
        rule.setNormalNaturalCode(anno.normalNaturalCode());
        rule.setRuleSchemaTypeFullName("CDSL.V5.Struct.CompatiableRule");
        
        // 创建CompatiableRuleSchema
        CompatiableRuleSchema schema = new CompatiableRuleSchema();
        schema.setType("CompatiableRule");
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
        schema.setRefProgObjs(new ArrayList<>()); // 代码规则暂时不解析引用对象
        
        rule.setRawCode(schema);
        return rule;
    }
    
    /**
     * 生成引用编程对象Schema列表
     */
    private static List<RefProgObjSchema> generateRefProgObjSchemas(String code, Module currentModule) {
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        List<ProgObject> progObjects = parseVariableObjects(code);
        
        for (ProgObject progObject : progObjects) {
            RefProgObjSchema refProgObjSchema = new RefProgObjSchema();
            refProgObjSchema.setProgObjCode(progObject.getObjCode());
            refProgObjSchema.setProgObjField(progObject.getObjField());
            
            // 判断是Part还是Para
            if (currentModule.getPara(progObject.getObjCode()) != null) {
                refProgObjSchema.setProgObjType("Para");
            } else if (currentModule.getPart(progObject.getObjCode()) != null) {
                refProgObjSchema.setProgObjType("Part");
            } else {
                throw new RuntimeException("未找到对象: " + progObject.getObjCode());
            }
            
            refProgObjs.add(refProgObjSchema);
        }
        
        return refProgObjs;
    }
    
    /**
     * 解析代码中的变量对象
     */
    private static List<ProgObject> parseVariableObjects(String code) {
        List<ProgObject> progObjects = new ArrayList<>();
        
        // 使用正则表达式匹配 "Object.Field" 模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)");
        java.util.regex.Matcher matcher = pattern.matcher(code);
        
        while (matcher.find()) {
            String objCode = matcher.group(1);
            //如果objCode="ColorVar"，则objCode="Color"
            if (objCode.endsWith("Var")) {
                objCode = objCode.substring(0, objCode.length() - "Var".length());
            }
            String objField = matcher.group(2);
            progObjects.add(new ProgObject(objCode, objField));
        }
        
        return progObjects;
    }
} 