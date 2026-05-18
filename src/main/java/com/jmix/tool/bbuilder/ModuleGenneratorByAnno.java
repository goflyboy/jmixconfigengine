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
 * 鍩轰簬娉ㄨВ鐨勬ā鍧楃敓鎴愬櫒
 * 閫氳繃鍙嶅皠璇诲彇娉ㄨВ淇℃伅锛岀敓鎴怣odule瀵硅薄
 * 
 * @since 2025-09-22
 */
@Slf4j
public final class ModuleGenneratorByAnno {
    /**
     * 瀹炰緥灞炴€ч敭鍚?
     */
    public static final String INSTANCE_ATTRS = "instanceAttrs";

    /**
     * 绉佹湁鏋勯€犲櫒锛岄槻姝㈠伐鍏风被琚疄渚嬪寲
     */
    private ModuleGenneratorByAnno() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 鏍规嵁绾︽潫绠楁硶绫绘瀯寤篗odule瀵硅薄
     * 
     * @param moduleAlgClazz 绾︽潫绠楁硶绫?
     * @return 鏋勫缓鐨凪odule瀵硅薄
     */
    public static Module buildModule(Class<?> moduleAlgClazz) {
        Module module = new Module();

        // 1. 鏍规嵁ModuleAnno淇℃伅鍒涘缓Module
        ModuleAnno moduleAnno = moduleAlgClazz.getAnnotation(ModuleAnno.class);
        if (moduleAnno == null) {
            log.error("ModuleAnno not found for class: {}", moduleAlgClazz.getName());
            throw new AlgLoaderException("ModuleAnno not found for class: " + moduleAlgClazz.getName());
        }
        // 1. 璁剧疆妯″潡鍩虹灞炴€?
        buildModuleBase(module, moduleAnno, moduleAlgClazz);

        // 鐢熸垚module.alg
        ModuleAlgArtifact alg = buildAlg(module, moduleAlgClazz);

        module.setAlg(alg);

        // 澶勭悊鎵╁睍灞炴€?
        buildExtAttr(moduleAnno, module);

        // 2. 閬嶅巻鎴愬憳鍙橀噺锛屽垱寤篜ara鍜孭art锛屾牴鎹甪atherCode娣诲姞鍒板搴旂殑PartCategory
        Pair<List<Para>, List<IPart>> paraParts = buildParameterParts(moduleAlgClazz);
        module.setParas(paraParts.getFirst());
        module.addParts(paraParts.getSecond());
        // 澶勭悊澶氬疄渚嬪垎绫伙紝鏍规嵁PartAnno鐨刬nstCodes灞炴€ц繘琛屽睍寮€
        MultiInstCategoryUtils.processInstCategory(module);
        module.init(); // 淇濊瘉鍚庣画鑳戒娇鐢╣etPara绛夊嚱鏁?
        // 4. 鐢熸垚瑙勫垯
        List<Rule> rules = createRulesFromMethods(moduleAlgClazz, module);
        module.setRules(rules);

        return module;
    }

    /**
     * 鏍规嵁绾︽潫绠楁硶绫诲拰璧勬簮璺緞鏋勫缓Module瀵硅薄
     * 
     * @param moduleAlgClazz 绾︽潫绠楁硶绫?
     * @param resourcePath   璧勬簮璺緞
     * @return 鏋勫缓鐨凪odule瀵硅薄
     */
    public static Module build(Class<?> moduleAlgClazz, String resourcePath) {
        Module module = buildModule(moduleAlgClazz);

        // 淇濆瓨Module鍒版枃浠?
        saveToFile(module, resourcePath);
        return module;
    }

    private static Optional<Para> createParaFromField(Field field) {
        ParaAnno paraAnno = field.getAnnotation(ParaAnno.class);
        if (paraAnno == null) {
            return Optional.empty();
        }

        Para para = new Para();

        // 鐢熸垚Para.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        para.setCode(code);

        // 璁剧疆鍏朵粬灞炴€?
        para.setFatherCode(paraAnno.fatherCode());
        para.setDefaultValue(paraAnno.defaultValue());
        para.setSortNo(paraAnno.sortNo());
        // 璁剧疆鍙傛暟绫诲瀷锛岀洿鎺ヤ娇鐢ㄦ敞瑙ｄ腑鐨勭被鍨?
        para.setParaType(paraAnno.type());
        // 璁剧疆assignType锛堜粠娉ㄨВ锛?
        para.setAssignType(paraAnno.assignType());
        para.setExtSchema(paraAnno.extSchema());
        para.setMinValue(paraAnno.minValue());
        para.setMaxValue(paraAnno.maxValue());

        // 澶勭悊鎵╁睍灞炴€?
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

        // 澶勭悊鏋氫妇閫夐」
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

        // 鏍规嵁瀛楁绫诲瀷鍐冲畾閮ㄤ欢绫诲瀷
        IPart ipart = null;
        if (isPartCategoryVarField(field)) {
            PartCategory part = new PartCategory();
            ipart = part;
            part.setPartType(PartType.CATEGORY);
            part.setDefaultValue(0); // Part鐨勯粯璁ゅ€肩被鍨嬫槸Integer
            part.setDescription(partAnno.description());
            part.setSortNo(partAnno.sortNo());
            // part.setMaxQuantity(partAnno.maxQuantity());
            // part.setPrice(partAnno.price());
            part.setExtSchema(partAnno.extSchema());
        } else {
            // PartVar 瀵瑰簲 ATOMIC 绫诲瀷锛屼笉搴旇鏈?dynAttrSchemas
            Part part = new Part();
            ipart = part;
            part.setPartType(PartType.ATOMIC);
            // 纭繚 ATOMIC 绫诲瀷鐨勯儴浠舵病鏈?dynAttrSchemas
            part.setDynAttrSchemas(new ArrayList<>());
            part.setDefaultValue(0); // Part鐨勯粯璁ゅ€肩被鍨嬫槸Integer
            part.setDescription(partAnno.description());
            part.setSortNo(partAnno.sortNo());
            part.setMaxQuantity(partAnno.maxQuantity());
            part.setPrice(partAnno.price());
            part.setExtSchema(partAnno.extSchema());
        }

        // 鐢熸垚Part.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        ipart.setCode(code);

        // 璁剧疆鍏朵粬灞炴€?
        ipart.setFatherCode(partAnno.fatherCode());

        // 澶勭悊瑙勬牸灞炴€?
        if (partAnno.attrs().length > 0 && ipart.getPartType() == PartType.ATOMIC && ipart.getFatherCode() != null) {
            PartCategory fatherPartCategory = (PartCategory) partMap.get(ipart.getFatherCode());
            Map<String, String> attrs = parseAttributes(partAnno.attrs(),
                    fatherPartCategory.queryDynAttrSchemas4NotInst());
            ipart.setDynAttr(attrs);

            // 澶勭悊瀹炰緥瑙勬牸灞炴€?
            processInstanceAttrs(ipart, partAnno, fatherPartCategory.queryDynAttrSchemas4Inst());
        }

        // 澶勭悊鎵╁睍灞炴€?
        if (partAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = parseAttributes(partAnno.extAttrs(), new ArrayList<>());
            ipart.setExtAttrs(extAttrs);
        }

        // 璁剧疆瀹炰緥缂栫爜锛堢敤浜庡瀹炰緥鍒嗙被澶勭悊锛?
        MultiInstCategoryUtils.setInstcodes(partAnno, ipart);

        // 澶勭悊鍔ㄦ€佸睘鎬ф敞瑙ｏ紙鍙湁CATEGORY绫诲瀷鐨勯儴浠舵墠闇€瑕侊級
        if (ipart.getPartType() == PartType.CATEGORY) {
            processDynamicAttributeAnnotations(field, (PartCategory) ipart);
            // 缁ф壙鐨勫睘鎬у湪鍚庨潰锛屾柟渚垮疄渚嬪睘鎬х瓑澶勭悊
            processInheritance(field, ipart, partMap);
        }

        return Optional.of(ipart);
    }

    /**
     * 浠庢柟娉曚腑鍒涘缓瑙勫垯
     * 
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     * @param module         妯″潡瀵硅薄
     * @return 鍒涘缓鐨勮鍒欏垪琛紙浠呭寘鍚坊鍔犲埌Module鐨勮鍒欙級
     */
    private static List<Rule> createRulesFromMethods(Class<?> moduleAlgClazz, Module module) {
        List<Rule> moduleRules = new ArrayList<>();

        // 閬嶅巻鎵€鏈夋柟娉?
        for (Method method : moduleAlgClazz.getDeclaredMethods()) {
            // 妫€鏌ユ槸鍚︽湁CompatiableRuleAnno娉ㄨВ
            CompatiableRuleAnno compatiableRuleAnno = method.getAnnotation(CompatiableRuleAnno.class);
            if (compatiableRuleAnno != null) {
                addAttrParaToModuleOrPartCategory(compatiableRuleAnno.fatherCode(),
                        parseAndAddAttrParas(compatiableRuleAnno.attrParaCodes()), module);
                Rule rule = createCompatibleRule(method, compatiableRuleAnno, module);
                if (rule != null) {
                    // CompatiableRuleAnno 娌℃湁 fatherCode锛岄粯璁ゆ坊鍔犲埌 Module
                    moduleRules.add(rule);
                }
            }

            // 妫€鏌ユ槸鍚︽湁PriorityRuleAnno娉ㄨВ
            PriorityRuleAnno priorityRuleAnno = method.getAnnotation(PriorityRuleAnno.class);
            if (priorityRuleAnno != null) {
                addAttrParaToModuleOrPartCategory(priorityRuleAnno.fatherCode(),
                        parseAndAddAttrParas(priorityRuleAnno.attrParaCodes()), module);
                Rule rule = createPriorityRule(method, priorityRuleAnno, module);
                if (rule != null) {
                    addRuleToModuleOrPartCategory(rule, module, moduleRules);
                }
            }

            // 妫€鏌ユ槸鍚︽湁CodeRuleAnno娉ㄨВ
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
            // fatherCode 涓?null 鎴栫┖瀛楃涓诧紝娣诲姞鍒?Module
            module.getAttrParas().addAll(deleteDuplicate(module.getAttrParas(), attrParas));
        } else {
            // fatherCode 涓嶄负绌猴紝娣诲姞鍒板搴旂殑 PartCategory
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
     * 灏嗚鍒欐坊鍔犲埌 Module 鎴?PartCategory
     * 鏍规嵁瑙勫垯鐨?fatherCode 瀛楁鍐冲畾娣诲姞鍒板摢涓綅缃畇
     * 
     * @param rule        瑙勫垯瀵硅薄
     * @param module      妯″潡瀵硅薄
     * @param moduleRules Module 鐨勮鍒欏垪琛?
     */
    private static void addRuleToModuleOrPartCategory(Rule rule, Module module, List<Rule> moduleRules) {
        String fatherCode = rule.getFatherCode();
        if (Strings.isNullOrEmpty(fatherCode)) {
            // fatherCode 涓?null 鎴栫┖瀛楃涓诧紝娣诲姞鍒?Module
            moduleRules.add(rule);
        } else {
            // fatherCode 涓嶄负绌猴紝娣诲姞鍒板搴旂殑 PartCategory
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
     * 鍒涘缓鍏煎鎬ц鍒?
     * 
     * @param method 鏂规硶瀵硅薄
     * @param anno   鍏煎鎬ц鍒欐敞瑙?
     * @param module 妯″潡瀵硅薄
     * @return 鍒涘缓鐨勫吋瀹规€ц鍒?
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

        // 璁剧疆浣滅敤鑼冨洿
        rule.setEffectScope(anno.effectScope());

        // 鍒涘缓CompatibleRuleSchema
        CompatiableRuleSchema schema = new CompatiableRuleSchema();
        schema.setType("CompatibleRule");
        schema.setVersion("1.0");

        // 鍒涘缓宸﹁〃杈惧紡
        if (!anno.leftExprCode().isEmpty()) {
            ExprSchema leftExpr = new ExprSchema();
            leftExpr.setRawCode(anno.leftExprCode());
            leftExpr.setRefProgObjs(generateRefProgObjSchemas(anno.leftExprCode(), module));
            schema.setLeftExpr(leftExpr);
        }

        // 璁剧疆鎿嶄綔绗?
        schema.setOperator(anno.operator());

        // 鍒涘缓鍙宠〃杈惧紡
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
     * 鍒涘缓浼樺厛绾ц鍒?
     * 
     * @param method 鏂规硶瀵硅薄
     * @param anno   浼樺厛绾ц鍒欐敞瑙?
     * @param module 妯″潡瀵硅薄
     * @return 鍒涘缓鐨勪紭鍏堢骇瑙勫垯
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

        // 璁剧疆浣滅敤鑼冨洿
        rule.setEffectScope(anno.effectScope());

        // 鍒涘缓PriorityRuleSchema
        PriorityRuleSchema schema = new PriorityRuleSchema();
        schema.setVersion("1.0");
        schema.setPriorityStrategy(anno.strategy());
        schema.setLeftRefProgObjs(new ArrayList<>());
        schema.setRightRefProgObjs(new ArrayList<>());

        // 瑙ｆ瀽leftProObjsStr锛岃缃澶栫殑宸︿晶缂栫▼瀵硅薄
        if (!anno.leftProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalLeftObjs = parseProObjsStr(anno.leftProObjsStr(), module);
            schema.getLeftRefProgObjs().addAll(additionalLeftObjs);
        }

        // 瑙ｆ瀽rightProObjsStr锛岃缃澶栫殑鍙充晶缂栫▼瀵硅薄
        if (!anno.rightProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalRightObjs = parseProObjsStr(anno.rightProObjsStr(), module);
            schema.getRightRefProgObjs().addAll(additionalRightObjs);
        }
        rule.setRawCode(schema);
        return rule;
    }

    /**
     * 鍒涘缓浠ｇ爜瑙勫垯
     * 
     * @param method 鏂规硶瀵硅薄
     * @param anno   浠ｇ爜瑙勫垯娉ㄨВ
     * @param module 妯″潡瀵硅薄
     * @return 鍒涘缓鐨勪唬鐮佽鍒?
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
        // 璁剧疆璁＄畻闃舵
        rule.setCalcStage(anno.calcStage());

        // 鍒涘缓CodeRuleSchema
        CodeRuleSchema schema = new CodeRuleSchema();
        schema.setType("CodeRule");
        schema.setVersion("1.0");
        schema.setRawCode(anno.code());

        // 瑙ｆ瀽宸﹀彸涓や晶鐨勫紩鐢ㄧ紪绋嬪璞?
        List<List<RefProgObjSchema>> leftRightRefProgObjs = parseLeftRightRefProgObjSchemas(anno.code(),
                module);
        schema.setLeftRefProgObjs(leftRightRefProgObjs.get(0));
        schema.setRightRefProgObjs(leftRightRefProgObjs.get(1));

        // 璁剧疆浣滅敤鑼冨洿
        rule.setEffectScope(anno.effectScope());

        // 瑙ｆ瀽leftProObjsStr锛岃缃澶栫殑宸︿晶缂栫▼瀵硅薄
        if (!anno.leftProObjsStr().isEmpty()) {
            List<RefProgObjSchema> additionalLeftObjs = parseProObjsStr(anno.leftProObjsStr(), module);
            for (RefProgObjSchema refProgObj : additionalLeftObjs) {
                schema.getFromLeftProgObjs().add(refProgObj);
            }
        }

        // 瑙ｆ瀽rightProObjsStr锛岃缃澶栫殑鍙充晶缂栫▼瀵硅薄
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
     * 鐢熸垚寮曠敤缂栫▼瀵硅薄Schema鍒楄〃
     * 
     * @param code          浠ｇ爜瀛楃涓?
     * @param currentModule 褰撳墠妯″潡
     * @return 寮曠敤缂栫▼瀵硅薄Schema鍒楄〃
     */
    private static List<RefProgObjSchema> generateRefProgObjSchemas(String code, Module currentModule) {
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();
        List<ProgObject> progObjects = parseVariableObjects(code);

        for (ProgObject progObject : progObjects) {
            RefProgObjSchema refProgObjSchema = new RefProgObjSchema();
            refProgObjSchema.setProgObjCode(progObject.getObjCode());
            refProgObjSchema.setProgObjField(progObject.getObjField());

            // 鍒ゆ柇鏄疨art杩樻槸Para
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
     * 瑙ｆ瀽浠ｇ爜涓殑鍙橀噺瀵硅薄
     * 
     * @param code 浠ｇ爜瀛楃涓?
     * @return 瑙ｆ瀽鍑虹殑缂栫▼瀵硅薄鍒楄〃
     */
    private static List<ProgObject> parseVariableObjects(String code) {
        List<ProgObject> progObjects = new ArrayList<>();

        // 浣跨敤姝ｅ垯琛ㄨ揪寮忓尮閰?"Object.Field" 妯″紡
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("([A-Za-z_][A-Za-z0-9_]*)\\.([A-Za-z_][A-Za-z0-9_]*)");
        java.util.regex.Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            String objCode = matcher.group(1);
            // 濡傛灉objCode="colorVar"锛屽垯objCode="Color"
            if (objCode.endsWith("Var")) {
                objCode = objCode.substring(0, objCode.length() - "Var".length());
            }
            String objField = matcher.group(2);
            progObjects.add(new ProgObject(objCode, objField));
        }

        return progObjects;
    }

    /**
     * 瑙ｆ瀽宸﹀彸涓や晶鐨勫紩鐢ㄧ紪绋嬪璞chema鍒楄〃
     * 
     * @param normalNaturalCode 瑙勮寖鍖栬嚜鐒惰瑷€浠ｇ爜
     * @param currentModule     褰撳墠妯″潡
     * @return 鍖呭惈涓や釜鍒楄〃鐨勫垪琛紝绗竴涓槸宸︿晶寮曠敤瀵硅薄锛岀浜屼釜鏄彸渚у紩鐢ㄥ璞?
     */
    private static List<List<RefProgObjSchema>> parseLeftRightRefProgObjSchemas(String normalNaturalCode,
            Module currentModule) {
        List<RefProgObjSchema> leftRefProgObjs = new ArrayList<>();
        List<RefProgObjSchema> rightRefProgObjs = new ArrayList<>();

        if (normalNaturalCode == null || normalNaturalCode.trim().isEmpty()) {
            return Arrays.asList(leftRefProgObjs, rightRefProgObjs);
        }

        log.info("Parsing rule: {}", normalNaturalCode);

        // 鑼冨紡1锛氳鍒欒鍙ワ紝濡傛灉鏈塱f A then B [else
        // C]锛岄偅涔圓閲岄潰鐨勫氨鏄痩eftRefProgObjs,B鍜孋灏辨槸rightRefProgObjs锛屾牴鎹畉hen鍒囧壊
        if (normalNaturalCode.contains("then ")) {
            log.info("Matched pattern 1: if-then");
            // 渚嬪锛歯ormalNaturalCode= "if P0.value > 1 then P11.value > P0.value+1",
            // 鍒欙細leftRefProgObjs=P0,rightRefProgObjs=(P11,P0)
            String[] parts = normalNaturalCode.split(" then ");
            if (parts.length >= 2) {
                String leftPart = parts[0].replaceFirst(".*if\\s+", ""); // 鍘绘帀"if "鍓嶇紑
                String rightPart = parts[1];

                log.info("Left part: {}, Right part: {}", leftPart, rightPart);

                // 瑙ｆ瀽宸︿晶閮ㄥ垎
                leftRefProgObjs = generateRefProgObjSchemas(leftPart, currentModule);

                // 瑙ｆ瀽鍙充晶閮ㄥ垎锛堝寘鎷琫lse閮ㄥ垎锛?
                rightRefProgObjs = generateRefProgObjSchemas(rightPart, currentModule);

                // 鏃ュ織鍐呭淇濇寔鑻辨枃锛屼笖淇澶ф嫭鍙蜂綅缃互绗﹀悎浠ｇ爜瑙勮寖
                log.info("Left refProgObjs: {}, Right refProgObjs: {}", leftRefProgObjs.size(),
                        rightRefProgObjs.size());
            }
        } else if (normalNaturalCode.contains(" = ")) { // 鑼冨紡2锛氳祴鍊艰鍙?A=B锛岄€氳繃=璇嗗埆锛屾槸杩旇繃鏉ワ紝鍙宠竟鍐冲畾宸﹁竟
            // 渚嬪锛歯ormalNaturalCode= "PT1.qty=P11.value ",
            // 鍒欙細leftRefProgObjs=P11,rightRefProgObjs=PT1
            String[] parts = normalNaturalCode.split(" = ");
            if (parts.length == 2) {
                String leftPart = parts[0].trim();
                String rightPart = parts[1].trim();

                // 瀵逛簬璧嬪€艰鍙ワ紝鍙宠竟鍐冲畾宸﹁竟锛屾墍浠ュ彸杈规槸leftRefProgObjs锛屽乏杈规槸rightRefProgObjs
                leftRefProgObjs = generateRefProgObjSchemas(rightPart, currentModule);
                rightRefProgObjs = generateRefProgObjSchemas(leftPart, currentModule);
            }
        } else { // 鑼冨紡3锛氬叾浠栬鍙ワ紝鐩存帴杩斿洖绌哄垪琛?
            log.error("Unsupported rule pattern: {}", normalNaturalCode);
            throw new AlgLoaderException("Unsupported rule pattern: " + normalNaturalCode);
        }

        return Arrays.asList(leftRefProgObjs, rightRefProgObjs);
    }

    /**
     * 瑙ｆ瀽缂栫▼瀵硅薄鎻忚堪瀛楃涓?
     * 鏍煎紡锛歱rogObjCode:progObjField|progObjField
     * 渚嬪锛?drive:Select|Quantity" -> [RefProgObj(drive, Select), RefProgObj(drive,
     * Quantity)]
     *
     * @param proObjsStr 缂栫▼瀵硅薄鎻忚堪瀛楃涓?
     * @param module     褰撳墠妯″潡
     * @return 瑙ｆ瀽鍚庣殑寮曠敤缂栫▼瀵硅薄鍒楄〃
     */
    private static List<RefProgObjSchema> parseProObjsStr(String proObjsStr, Module module) {
        List<RefProgObjSchema> refProgObjs = new ArrayList<>();

        if (Strings.isNullOrEmpty(proObjsStr)) {
            return refProgObjs;
        }

        // 鎸夐€楀彿鍒嗛殧澶氫釜ProgObj
        String[] objParts = proObjsStr.split(",");
        for (String objPart : objParts) {
            String trimmed = objPart.trim();
            if (Strings.isNullOrEmpty(trimmed)) {
                continue;
            }

            // 鎸夊啋鍙峰垎闅攑rogObjCode鍜屽睘鎬у垪琛?
            String[] codeAndFields = trimmed.split(":");
            if (codeAndFields.length != 2) {
                throw new AlgLoaderException(
                        "Invalid proObjsStr format: " + trimmed + ", expected 'code:field1|field2'");
            }

            String progObjCode = codeAndFields[0].trim();
            String fieldsStr = codeAndFields[1].trim();

            // 妫€鏌ユ槸Part杩樻槸Para
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

            // 鎸墊鍒嗛殧澶氫釜灞炴€?
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
     * 鏋勫缓鎵╁睍灞炴€ф槧灏?
     * 
     * @param moduleAnno 妯″潡娉ㄨВ
     * @param module     妯″潡瀵硅薄
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
     * 瑙ｆ瀽attrParaCodes骞舵坊鍔犲埌module鐨刟ttrParas鍒楄〃涓?
     * 鏍煎紡锛歛ttrCode:Type,attrCode:Type
     * 渚嬪锛?Capacity:SumSum,Quantity:SumSum"
     * 
     * @param attrParaCodes 灞炴€у弬鏁扮紪鐮佸瓧绗︿覆
     */
    private static List<AttrPara> parseAndAddAttrParas(String attrParaCodes) {
        if (Strings.isNullOrEmpty(attrParaCodes)) {
            return new ArrayList<>();
        }
        List<AttrPara> attrParas = new ArrayList<>();
        // 鎸夐€楀彿鍒嗛殧澶氫釜AttrPara
        String[] attrParaParts = attrParaCodes.split(",");
        for (String attrParaStr : attrParaParts) {
            String trimmed = attrParaStr.trim();
            if (Strings.isNullOrEmpty(trimmed)) {
                continue;
            }

            // 鎸夊啋鍙峰垎闅攁ttrCode鍜孴ype
            String[] codeAndType = trimmed.split(":");
            if (codeAndType.length != 2) {
                log.warn("Invalid attrPara format: {}, expected 'attrCode:Type'", trimmed);
                continue;
            }

            String attrCode = codeAndType[0].trim();
            String typeStr = codeAndType[1].trim();

            // 瑙ｆ瀽绫诲瀷
            AttrParaType type = AttrParaType.valueOf(typeStr);

            // 鍒涘缓AttrPara骞舵坊鍔犲埌module
            AttrPara attrPara = new AttrPara();
            attrPara.setAttrCode(attrCode);
            attrPara.setType(type);
            attrParas.add(attrPara);

            log.info("Added AttrPara: attrCode={}, type={}", attrCode, type);
        }
        return attrParas;
    }

    /**
     * 鏋勫缓Para鍜孭art鍒楄〃
     *
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     * @return 鍖呭惈Para鍒楄〃鍜孭art鍒楄〃鐨凱air
     */
    private static Pair<List<Para>, List<IPart>> buildParaParts(Class<?> moduleAlgClazz) {
        List<Para> paras = new ArrayList<>();

        Map<String, IPart> partMap = new HashMap<>();
        // 棣栧厛鏀堕泦鎵€鏈夊瓧娈碉紝鐢ㄤ簬鍚庣画缁ф壙澶勭悊
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
     * 鏋勫缓鍙傛暟閮ㄥ垎锛屾牴鎹甪atherCode娣诲姞鍒板搴旂殑PartCategory
     *
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     * @return 鍖呭惈Para鍒楄〃鍜孭art鍒楄〃鐨凱air
     */
    private static Pair<List<Para>, List<IPart>> buildParameterParts(Class<?> moduleAlgClazz) {
        List<Para> paras = new ArrayList<>();
        Map<String, IPart> partMap = new HashMap<>();

        // 棣栧厛鏀堕泦鎵€鏈夊瓧娈碉紝鐢ㄤ簬鍚庣画缁ф壙澶勭悊
        List<Field> partFields = new ArrayList<>();
        Map<String, String> fieldNameToPartCode = new HashMap<>();

        // 鏀堕泦Part瀛楁
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

        // 澶勭悊Para瀛楁锛屾牴鎹甪atherCode娣诲姞鍒板搴旂殑PartCategory
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (isParaVarField(field)) {
                Optional<Para> paraOpt = createParaFromField(field);
                if (paraOpt.isPresent()) {
                    Para para = paraOpt.get();
                    String fatherCode = para.getFatherCode();

                    // 濡傛灉鏈塮atherCode锛屾坊鍔犲埌瀵瑰簲鐨凱artCategory
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
                        // 娌℃湁fatherCode鐨凱ara娣诲姞鍒版ā鍧楃骇鍒?
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
     * 澶勭悊鎵€鏈夐儴浠剁殑缁ф壙鍏崇郴
     *
     * @param parts               閮ㄤ欢鍒楄〃
     * @param fieldNameToPartCode 瀛楁鍚嶅埌閮ㄤ欢缂栫爜鐨勬槧灏?
     * @param partOverrideMap     閮ㄤ欢閲嶅啓鏄犲皠
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
        // 澶勭悊缁ф壙娉ㄨВ锛屼负partOverrideMap璧嬪€?
        DAttrInherit inheritAnno = field.getAnnotation(DAttrInherit.class);
        if (inheritAnno != null) {

            // 瑙ｆ瀽閲嶅啓灞炴€?
            String[] overrideAttrs = inheritAnno.overrideAttrs();
            for (String override : overrideAttrs) {
                String[] partCodes = override.split(":");
                if (partCodes.length == 2) {
                    overrideMap.put(partCodes[0], partCodes[1]);
                }
            }
        }

        // 澶勭悊姣忎釜閮ㄤ欢鐨勭户鎵?
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
     * 浠庣埗閮ㄤ欢缁ф壙灞炴€?
     *
     * @param child       瀛愰儴浠?
     * @param parent      鐖堕儴浠?
     * @param overrideMap 閲嶅啓灞炴€ф槧灏?
     */
    private static void inheritFromParent(PartCategory child, PartCategory parent, Map<String, String> overrideMap) {
        // 澶嶅埗鐖堕儴浠剁殑鎵€鏈夊姩鎬佸睘鎬?
        for (DynamicAttribute parentAttr : parent.getDynAttrSchemas()) {
            try {
                // 娣辨嫹璐濆睘鎬?
                DynamicAttribute childAttr = copyDynamicAttribute(parentAttr);

                // 妫€鏌ユ槸鍚︽湁閲嶅啓
                String overrideValue = overrideMap.get(parentAttr.getCode());
                if (overrideValue != null) {
                    // 鍘婚櫎鎵€鏈夌┖鏍?
                    overrideValue = overrideValue.replaceAll("\\s+", "");
                }

                if (overrideValue != null && overrideValue.startsWith("instType=")) {
                    String instTypeStr = overrideValue.substring("instType=".length());
                    try {
                        int instType = Integer.parseInt(instTypeStr);
                        childAttr.setInstType(instType);
                    } catch (NumberFormatException e) {
                        // 淇濇寔鍘熸湁鍊?
                    }
                }

                // 娣诲姞鍒板瓙閮ㄤ欢
                child.getDynAttrSchemas().add(childAttr);
            } catch (Exception e) {
                log.error("Failed to inherit attribute: " + parentAttr.getCode(), e);
            }
        }
    }

    /**
     * 娣辨嫹璐濆姩鎬佸睘鎬?
     *
     * @param original 鍘熷灞炴€?
     * @return 鎷疯礉鐨勫睘鎬?
     */
    private static DynamicAttribute copyDynamicAttribute(DynamicAttribute original) {
        DynamicAttribute copy = new DynamicAttribute();
        copy.setCode(original.getCode());
        copy.setName(original.getName());
        copy.setDynAttrType(original.getDynAttrType());
        copy.setValue(original.getValue());
        copy.setOptionExtSchema(original.getOptionExtSchema());
        copy.setInstType(original.getInstType());

        // 娣辨嫹璐濋€夐」鍒楄〃
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
     * 鏋勫缓ModuleAlgArtifact瀵硅薄
     * 
     * @param module         妯″潡瀵硅薄
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     * @return 閰嶇疆濂界殑ModuleAlgArtifact瀵硅薄
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

        // 妫€鏌ユ槸鍚︿负鍐呴儴绫伙紝璁剧疆parentClassName
        Class<?> enclosingClass = moduleAlgClazz.getEnclosingClass();
        if (enclosingClass != null) {
            // 鏄唴閮ㄧ被锛岃缃埗绫诲悕绉?
            alg.setParentClassName(enclosingClass.getSimpleName());
            log.info("Detected inner class: {}, parent class: {}", moduleAlgClazz.getName(),
                    enclosingClass.getName());
        } else {
            // 涓嶆槸鍐呴儴绫伙紝淇濇寔榛樿绌哄瓧绗︿覆
            alg.setParentClassName("");
        }

        return alg;
    }

    /**
     * 鏋勫缓妯″潡鍩虹灞炴€?
     * 
     * @param module         妯″潡瀵硅薄
     * @param moduleAnno     妯″潡娉ㄨВ
     * @param moduleAlgClazz 妯″潡绠楁硶绫?
     */
    private static void buildModuleBase(Module module, ModuleAnno moduleAnno, Class<?> moduleAlgClazz) {
        // 1.1 鐢熸垚Module.code
        String className = moduleAlgClazz.getSimpleName();
        // 浼樺寲浠ｇ爜鐢熸垚閫昏緫锛氬彧鍘绘帀鏈熬鐨?Constraint"
        String code = className;
        if (className.endsWith("Constraint")) {
            code = className.substring(0, className.length() - "Constraint".length());
        }
        module.setCode(code);

        // 1.2 璁剧疆鍏朵粬灞炴€?
        module.setId(moduleAnno.id());
        module.setPackageName(moduleAnno.packageName().isEmpty() ? moduleAlgClazz.getPackage().getName()
                : moduleAnno.packageName());
        module.setVersion(moduleAnno.version());
        module.setDescription(moduleAnno.description());
        module.setSortNo(moduleAnno.sortNo());
        module.setExtSchema(moduleAnno.extSchema());
    }

    /**
     * 淇濆瓨妯″潡鍒版枃浠?
     * 
     * @param module       妯″潡瀵硅薄
     * @param resourcePath 璧勬簮璺緞
     */
    private static void saveToFile(Module module, String resourcePath) {
        String fileName = module.getCode() + ".base.json";
        String filePath = resourcePath + File.separator + fileName;

        // 鏍囧噯鍖栬矾寰勶紝瑙ｅ喅Windows璺緞闂
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
     * 鍒涘缓鍙傛暟閫夐」
     * 
     * @param raw   鍘熷閫夐」瀛楃涓?
     * @param index 閫夐」绱㈠紩
     * @return 鍒涘缓鐨凱araOption瀵硅薄
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
     * 瑙ｆ瀽灞炴€у瓧绗︿覆鏁扮粍涓篗ap
     *
     * @param attributes 灞炴€у瓧绗︿覆鏁扮粍锛屾牸寮忎负 "key:value"
     * @return 瑙ｆ瀽鍚庣殑灞炴€ap
     */
    private static Map<String, String> parseAttributes(String[] attributes, List<DynamicAttribute> dynAttrSchemas) {
        Map<String, String> attrs = new HashMap<>();
        // 濡傛灉 attributes.length锛? dynAttrSchemas.size() 鎵撴棩蹇楋紝骞舵姏寮傚父
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

            // 鍒ゆ柇attributes[i]鏄惁鏈?:"
            if (attribute.contains(":")) {
                // "attrCode:attrValue"妯″紡
                String[] parts = attribute.split(":", 2);
                attrCode = parts[0].trim();
                attrValue = parts[1].trim();

                // 鏍￠獙 dynAttrSchema.getCode 鍜?attrCode鏄惁鐩哥瓑
                if (!dynAttrSchema.getCode().equals(attrCode)) {
                    String errorMessage = String.format("Attribute code mismatch: expected %s, got %s",
                            dynAttrSchema.getCode(), attrCode);
                    log.error(errorMessage);
                    throw new AlgLoaderException(errorMessage);
                }

                // 鏍￠獙 dynAttrSchema.queryOptionByCodeValue(attrValue)鏄惁瀛樺湪
                if (dynAttrSchema.queryOptionByCodeValue(attrValue) == null) {
                    String errorMessage = String
                            .format("Option not found for codeValue: {%s} in attribute {%s}", attrValue, attrCode);
                    log.error(errorMessage);
                    throw new AlgLoaderException(errorMessage);
                }
            } else {
                // "attrValue"妯″紡
                attrValue = attribute.trim();
                attrCode = dynAttrSchema.getCode();

                // 鏍￠獙dynAttrSchema.queryOptionByCodeValue(attrValue)鏄惁瀛樺湪
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
     * 澶勭悊鍔ㄦ€佸睘鎬ф敞瑙?
     *
     * @param field        瀛楁瀵硅薄
     * @param partCategory 閮ㄤ欢鍒嗙被瀵硅薄
     */
    private static void processDynamicAttributeAnnotations(Field field, PartCategory partCategory) {
        // 澶勭悊DAttrAnno1
        DAttrAnno1 dAttrAnno1 = field.getAnnotation(DAttrAnno1.class);
        if (dAttrAnno1 != null) {
            addDynamicAttribute(partCategory, dAttrAnno1.code(), dAttrAnno1.optionExtSchema(),
                    dAttrAnno1.options(), dAttrAnno1.instType(), dAttrAnno1.dynAttrType());
        }

        // 澶勭悊DAttrAnno2
        DAttrAnno2 dAttrAnno2 = field.getAnnotation(DAttrAnno2.class);
        if (dAttrAnno2 != null) {
            addDynamicAttribute(partCategory, dAttrAnno2.code(), dAttrAnno2.optionExtSchema(),
                    dAttrAnno2.options(), dAttrAnno2.instType(), dAttrAnno2.dynAttrType());
        }

        // 澶勭悊DAttrAnno3
        DAttrAnno3 dAttrAnno3 = field.getAnnotation(DAttrAnno3.class);
        if (dAttrAnno3 != null) {
            addDynamicAttribute(partCategory, dAttrAnno3.code(), dAttrAnno3.optionExtSchema(),
                    dAttrAnno3.options(), dAttrAnno3.instType(), dAttrAnno3.dynAttrType());
        }

        // 澶勭悊DAttrAnno4
        DAttrAnno4 dAttrAnno4 = field.getAnnotation(DAttrAnno4.class);
        if (dAttrAnno4 != null) {
            addDynamicAttribute(partCategory, dAttrAnno4.code(), dAttrAnno4.optionExtSchema(),
                    dAttrAnno4.options(), dAttrAnno4.instType(), dAttrAnno4.dynAttrType());
        }

        // 澶勭悊DAttrAnno5
        DAttrAnno5 dAttrAnno5 = field.getAnnotation(DAttrAnno5.class);
        if (dAttrAnno5 != null) {
            addDynamicAttribute(partCategory, dAttrAnno5.code(), dAttrAnno5.optionExtSchema(),
                    dAttrAnno5.options(), dAttrAnno5.instType(), dAttrAnno5.dynAttrType());
        }

        // 澶勭悊DAttrAnno11
        DAttrAnno11 dAttrAnno11 = field.getAnnotation(DAttrAnno11.class);
        if (dAttrAnno11 != null) {
            addDynamicAttribute(partCategory, dAttrAnno11.code(), dAttrAnno11.optionExtSchema(),
                    dAttrAnno11.options(), dAttrAnno11.instType(), dAttrAnno11.dynAttrType());
        }

        // 澶勭悊DAttrAnno12
        DAttrAnno12 dAttrAnno12 = field.getAnnotation(DAttrAnno12.class);
        if (dAttrAnno12 != null) {
            addDynamicAttribute(partCategory, dAttrAnno12.code(), dAttrAnno12.optionExtSchema(),
                    dAttrAnno12.options(), dAttrAnno12.instType(), dAttrAnno12.dynAttrType());
        }

        // 澶勭悊DAttrAnno13
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
     * 娣诲姞鍔ㄦ€佸睘鎬?
     *
     * @param partCategory    閮ㄤ欢鍒嗙被瀵硅薄
     * @param code            灞炴€х紪鐮?
     * @param optionExtSchema 鎵╁睍妯″紡
     * @param options         鍙€夊€煎垪琛?
     * @param instType        瀹炰緥绫诲瀷
     * @param dynAttrType     鍔ㄦ€佸睘鎬х被鍨?
     */
    private static void addDynamicAttribute(PartCategory partCategory, String code, String optionExtSchema,
            String[] options, int instType, DynamicAttributeType dynAttrType) {
        DynamicAttribute dynAttr = new DynamicAttribute();
        dynAttr.setCode(code);
        dynAttr.setName(code);
        dynAttr.setOptionExtSchema(optionExtSchema);
        dynAttr.setDynAttrType(dynAttrType);

        // 瑙ｆ瀽閫夐」
        List<DynamicAttributerOption> dynOptions = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            String optionStr = options[i];
            String[] parts = optionStr.split(":");
            if (parts.length >= 3) {
                DynamicAttributerOption option = new DynamicAttributerOption();
                option.setCode(parts[0]);
                // 濡傛灉parts[1]鏄暟瀛楋紝鍒欒缃负codeId
                if (isNumber(parts[1])) {
                    option.setCodeId(Integer.parseInt(parts[1]));
                } else {
                    option.setCodeId(i + 1);
                }
                option.setCodeValue(parts[1]);
                option.setDefaultValue(parts[1]); // 鍊?
                option.setDescription(parts[2]); // 鍗曚綅鎴栨弿杩?
                option.setSortNo(i + 1);
                dynOptions.add(option);
            } else if (parts.length >= 2) {
                DynamicAttributerOption option = new DynamicAttributerOption();
                option.setCode(parts[0]);
                // 濡傛灉parts[1]鏄暟瀛楋紝鍒欒缃负codeId
                if (isNumber(parts[1])) {
                    option.setCodeId(Integer.parseInt(parts[1]));
                } else {
                    option.setCodeId(i + 1);
                }
                option.setCodeValue(parts[1]);
                option.setDefaultValue(parts[1]); // 鍊?
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
     * 澶勭悊缁ф壙娉ㄨВ
     *
     * @param field               瀛楁瀵硅薄
     * @param partCategory        閮ㄤ欢鍒嗙被瀵硅薄
     * @param fieldNameToPartCode 瀛楁鍚嶅埌閮ㄤ欢缂栫爜鐨勬槧灏?
     * @return 閲嶅啓灞炴€ф槧灏?
     */
    private static Map<String, String> processInheritAnnotation(Field field, PartCategory partCategory,
            Map<String, String> fieldNameToPartCode) {
        Map<String, String> overrideMap = new HashMap<>();

        DAttrInherit inheritAnno = field.getAnnotation(DAttrInherit.class);
        if (inheritAnno != null) {
            // 灏嗗瓧娈靛悕鏄犲皠涓洪儴浠剁紪鐮?
            String fatherFieldName = inheritAnno.fatherCode();
            String fatherPartCode = fieldNameToPartCode.get(fatherFieldName);
            if (fatherPartCode != null) {
                partCategory.setFatherCode(fatherPartCode);
            } else {
                // 濡傛灉鎵句笉鍒版槧灏勶紝鐩存帴浣跨敤瀛楁鍚?
                partCategory.setFatherCode(fatherFieldName);
            }

            // 瑙ｆ瀽閲嶅啓灞炴€?
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
     * 澶勭悊瀹炰緥瑙勬牸灞炴€?
     *
     * @param part     閮ㄤ欢瀵硅薄
     * @param partAnno 閮ㄤ欢娉ㄨВ
     */
    private static void processInstanceAttrs(IPart part, PartAnno partAnno, List<DynamicAttribute> dynAttrSchemas) {
        // 妫€鏌ユ槸鍚︽湁瀹炰緥灞炴€?
        if (partAnno.attrsInst1().length > 0 || partAnno.attrsInst2().length > 0 ||
                partAnno.attrsInst3().length > 0 || partAnno.attrsInst4().length > 0) {

            // 鍒涘缓 InstanceDynAttrValue 瀹瑰櫒瀵硅薄
            InstanceDynAttrValue instanceDynAttrValue = new InstanceDynAttrValue();

            // 澶勭悊鍚勪釜瀹炰緥
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst1(), 1, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst2(), 2, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst3(), 3, dynAttrSchemas);
            addInstanceAttr(instanceDynAttrValue, partAnno.attrsInst4(), 4, dynAttrSchemas);

            // 灏嗗簭鍒楀寲鍚庣殑鏁版嵁瀛樺偍鍒?dynAttr 涓?
            Map<String, String> dynAttr = part.getDynAttr();
            if (dynAttr == null) {
                dynAttr = new HashMap<>();
                part.setDynAttr(dynAttr);
            }
            dynAttr.put(INSTANCE_ATTRS, InstanceDynAttrValue.toJsonString(instanceDynAttrValue));
        }
    }

    /**
     * 娣诲姞瀹炰緥灞炴€у埌 InstanceDynAttrValue 瀹瑰櫒涓?
     *
     * @param container InstanceDynAttrValue 瀹瑰櫒瀵硅薄
     * @param attrs     灞炴€ф暟缁?
     * @param instId    瀹炰緥ID
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
