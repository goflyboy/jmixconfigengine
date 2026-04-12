package com.jmix.tool.bbuilder;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PartCategory;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.common.base.Strings;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多实例分类工具类
 * 根据PartAnno的instCodes属性，对PartCategory进行多实例构造
 *
 * @since 2025-04-05
 */
@Slf4j
public final class MultiInstCategoryUtils {

    /**
     * 私有构造器，防止工具类被实例化
     */
    private MultiInstCategoryUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 实例编码扩展属性键名
     */
    public static final String EX_INST_CODES = "EX_INST_CODES";

    /**
     * 支持多实例扩展属性键名
     */
    public static final String EX_SUPPORT_MULTI_INST = "EX_SUPPORT_MULTI_INST";

    /**
     * 实例的名称
     */
    public static final char INST_PREFIX_CHAR = 'I';
    /**
     * 实例的名称的长度
     */
    public static final int INST_NAME_LENGTH = 2;

    /**
     * 最大实例数量限制
     */
    private static final int MAX_INST_COUNT = 10;

    /**
     * 实例编码正则表达式：字母开头，后跟实例名称(I0, I1, driveI0等)
     */
    private static final Pattern INST_CODE_PATTERN = Pattern.compile("^([A-Za-z]+)(I\\d+)$");

    public static boolean isMultiInstCategory(PartCategory partCategory) {
        return isMultiInstCategory(partCategory.getCode());
    }

    public static int getInstId(String code) {
        if (isMultiInstCategory(code)) {
            return Integer.parseInt(code.substring(code.length() - 1));
        }
        return ModuleInst.DEFAULT_INSTANCE_ID;
    }

    public static boolean isMultiInstCategory(String code) {
        if (Strings.isNullOrEmpty(code)) {
            return false;
        }
        if (code.length() <= INST_NAME_LENGTH) {
            return false;
        }
        if (code.charAt(code.length() - 2) == INST_PREFIX_CHAR && Character.isDigit(code.charAt(code.length() - 1))) {
            return true;
        }
        return false;
    }

    /**
     * 处理模块中的多实例分类
     * 遍历所有PartCategory，如果有instCodes属性，则按实例进行展开
     *
     * @param module 模块对象
     */
    public static void processInstCategory(Module module) {
        List<PartCategory> srcPartCategorys = module.getPartCategorys();
        if (srcPartCategorys == null || srcPartCategorys.isEmpty()) {
            log.info("No PartCategorys found in module");
            return;
        }

        List<PartCategory> dstPartCategorys = new ArrayList<>();
        for (PartCategory partCategory : srcPartCategorys) {
            processPartCategory(partCategory, dstPartCategorys);
        }

        module.setPartCategorys(dstPartCategorys);
        log.info("Multi-instance processing completed, {} PartCategorys generated", dstPartCategorys.size());
    }

    /**
     * 处理单个PartCategory
     *
     * @param partCategory     源PartCategory
     * @param dstPartCategorys 目标PartCategory列表
     */
    private static void processPartCategory(PartCategory partCategory, List<PartCategory> dstPartCategorys) {
        String instCodeStr = partCategory.getExtAttr(EX_INST_CODES);

        if (Strings.isNullOrEmpty(instCodeStr)) {
            // 没有多实例，直接添加
            dstPartCategorys.add(partCategory);
            log.debug("PartCategory '{}' has no instCodes, added directly", partCategory.getCode());
            return;
        }

        // 有多实例，按instCode进行展开
        List<InstCode> instCodes = parseInstCode(instCodeStr, partCategory.getCode());

        if (instCodes.isEmpty()) {
            dstPartCategorys.add(partCategory);
            return;
        }

        // 验证实例ID的连续性
        validateInstIds(instCodes, partCategory.getCode());

        // 验证实例数量不超过限制
        if (instCodes.size() > MAX_INST_COUNT) {
            throw new IllegalArgumentException(
                    String.format("PartCategory '%s': instCodes count (%d) exceeds maximum limit (%d)",
                            partCategory.getCode(), instCodes.size(), MAX_INST_COUNT));
        }

        log.info("PartCategory '{}' has {} instances: {}",
                partCategory.getCode(), instCodes.size(), instCodeStr);

        // 为每个实例创建一个深拷贝
        for (InstCode instCode : instCodes) {
            PartCategory cloned = deepClone(partCategory, instCode.getInstCode());
            dstPartCategorys.add(cloned);
            log.debug("Cloned PartCategory '{}' -> '{}'", partCategory.getCode(), instCode.getInstCode());
        }
    }

    /**
     * 深拷贝PartCategory
     *
     * @param org     原始PartCategory
     * @param newCode 新的编码
     * @return 深拷贝的PartCategory
     */
    private static PartCategory deepClone(PartCategory org, String newCode) {
        PartCategory dst = org.clone();
        dst.setCode(newCode);

        // 克隆atomicParts，将每个part的fatherCode设置为newCode
        List<Part> clonedAtomicParts = new ArrayList<>();
        if (org.getAtomicParts() != null) {
            for (Part atomicPart : org.getAtomicParts()) {
                Part clonedPart = atomicPart.clone();
                clonedPart.setFatherCode(newCode);
                clonedAtomicParts.add(clonedPart);
            }
        }
        dst.setAtomicParts(clonedAtomicParts);

        // 克隆paras（Para没有clone方法，需要手动克隆）
        List<Para> clonedParas = new ArrayList<>();
        if (org.getParas() != null) {
            for (Para para : org.getParas()) {
                Para clonedPara = clonePara(para);
                clonedPara.setFatherCode(newCode);
                clonedParas.add(clonedPara);
            }
        }
        dst.setParas(clonedParas);

        // 克隆dynAttr
        if (org.getDynAttr() != null) {
            dst.setDynAttr(new HashMap<>(org.getDynAttr()));
        }

        // 克隆dynAttrSchemas
        List<DynamicAttribute> clonedDynAttrSchemas = new ArrayList<>();
        if (org.getDynAttrSchemas() != null) {
            for (DynamicAttribute dynAttrSchema : org.getDynAttrSchemas()) {
                clonedDynAttrSchemas.add(cloneDynamicAttribute(dynAttrSchema));
            }
        }
        dst.setDynAttrSchemas(clonedDynAttrSchemas);

        return dst;
    }

    /**
     * 深拷贝Para对象
     *
     * @param original 原始Para
     * @return 拷贝的Para
     */
    private static Para clonePara(Para original) {
        Para copy = new Para();
        copy.setCode(original.getCode());
        copy.setName(original.getName());
        copy.setFatherCode(original.getFatherCode());
        copy.setDefaultValue(original.getDefaultValue());
        copy.setSortNo(original.getSortNo());
        copy.setParaType(original.getParaType());
        copy.setAssignType(original.getAssignType());
        copy.setExtSchema(original.getExtSchema());
        copy.setMinValue(original.getMinValue());
        copy.setMaxValue(original.getMaxValue());
        copy.setDynAttrType(original.getDynAttrType());
        copy.setValue(original.getValue());
        copy.setOptionExtSchema(original.getOptionExtSchema());
        copy.setInstType(original.getInstType());

        // 深拷贝选项列表
        if (original.getOptions() != null) {
            List<DynamicAttributerOption> copiedOptions = new ArrayList<>();
            for (DynamicAttributerOption option : original.getOptions()) {
                copiedOptions.add(cloneDynamicAttributerOption(option));
            }
            copy.setOptions(copiedOptions);
        }

        // 深拷贝扩展属性
        if (original.getExtAttrs() != null) {
            copy.setExtAttrs(new HashMap<>(original.getExtAttrs()));
        }

        return copy;
    }

    /**
     * 深拷贝DynamicAttributerOption对象
     *
     * @param original 原始选项
     * @return 拷贝的选项
     */
    private static DynamicAttributerOption cloneDynamicAttributerOption(DynamicAttributerOption original) {
        DynamicAttributerOption copy = new DynamicAttributerOption();
        copy.setCode(original.getCode());
        copy.setCodeId(original.getCodeId());
        copy.setCodeValue(original.getCodeValue());
        copy.setDefaultValue(original.getDefaultValue());
        copy.setDescription(original.getDescription());
        copy.setSortNo(original.getSortNo());
        return copy;
    }

    /**
     * 深拷贝Rule对象
     *
     * @param original 原始Rule
     * @return 拷贝的Rule
     */
    private static Rule cloneRule(Rule original) {
        Rule copy = new Rule();
        copy.setCode(original.getCode());
        copy.setName(original.getName());
        copy.setFatherCode(original.getFatherCode());
        copy.setProgObjType(original.getProgObjType());
        copy.setProgObjCode(original.getProgObjCode());
        copy.setProgObjField(original.getProgObjField());
        copy.setNormalNaturalCode(original.getNormalNaturalCode());
        copy.setRuleSchemaTypeFullName(original.getRuleSchemaTypeFullName());
        copy.setExtSchema(original.getExtSchema());
        // rawCode是RuleSchema，需要根据具体类型进行克隆
        if (original.getRawCode() != null) {
            copy.setRawCode(original.getRawCode());
        }
        // 深拷贝扩展属性
        if (original.getExtAttrs() != null) {
            copy.setExtAttrs(new HashMap<>(original.getExtAttrs()));
        }
        return copy;
    }

    /**
     * 深拷贝DynamicAttribute
     *
     * @param original 原始DynamicAttribute
     * @return 拷贝的DynamicAttribute
     */
    private static DynamicAttribute cloneDynamicAttribute(DynamicAttribute original) {
        DynamicAttribute copy = new DynamicAttribute();
        copy.setCode(original.getCode());
        copy.setName(original.getName());
        copy.setDynAttrType(original.getDynAttrType());
        copy.setValue(original.getValue());
        copy.setOptionExtSchema(original.getOptionExtSchema());
        copy.setInstType(original.getInstType());

        // 深拷贝选项列表
        if (original.getOptions() != null) {
            List<DynamicAttributerOption> copiedOptions = new ArrayList<>();
            for (DynamicAttributerOption option : original.getOptions()) {
                copiedOptions.add(cloneDynamicAttributerOption(option));
            }
            copy.setOptions(copiedOptions);
        }

        return copy;
    }

    /**
     * 解析实例编码字符串
     * 例如：instCodeStr="driveI0,driveI1" 返回 [InstCode{instCode=driveI0, code=drive,
     * instName=I0, instId=0}, ...]
     *
     * @param instCodeStr 实例编码字符串，格式：codeI0,codeI1,...
     * @param partCode    PartCategory编码，用于错误信息
     * @return InstCode列表
     * @throws IllegalArgumentException 如果解析失败或实例ID不连续
     */
    static List<InstCode> parseInstCode(String instCodeStr, String partCode) {
        List<InstCode> instCodes = new ArrayList<>();

        if (Strings.isNullOrEmpty(instCodeStr)) {
            return instCodes;
        }

        String[] parts = instCodeStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (Strings.isNullOrEmpty(trimmed)) {
                continue;
            }

            InstCode instCode = parseSingleInstCode(trimmed, partCode);
            instCodes.add(instCode);
        }

        return instCodes;
    }

    /**
     * 解析单个实例编码
     *
     * @param instCode 单个实例编码，如 "driveI0"
     * @param partCode PartCategory编码，用于错误信息
     * @return InstCode对象
     * @throws IllegalArgumentException 如果解析失败
     */
    private static InstCode parseSingleInstCode(String instCode, String partCode) {
        Matcher matcher = INST_CODE_PATTERN.matcher(instCode);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    String.format(
                            "PartCategory '%s': invalid instCode format '%s'. Expected format: codeI0,codeI1 (e.g., driveI0,driveI1)",
                            partCode, instCode));
        }

        String code = matcher.group(1);
        String instName = matcher.group(2);
        String instId = instName.substring(1); // Remove 'I' prefix

        InstCode result = new InstCode();
        result.setInstCode(instCode);
        result.setCode(code);
        result.setInstName(instName);
        result.setInstId(instId);

        return result;
    }

    /**
     * 验证实例ID是否从0开始连续
     *
     * @param instCodes InstCode列表
     * @param partCode  PartCategory编码，用于错误信息
     * @throws IllegalArgumentException 如果验证失败
     */
    private static void validateInstIds(List<InstCode> instCodes, String partCode) {
        for (int i = 0; i < instCodes.size(); i++) {
            InstCode instCode = instCodes.get(i);
            try {
                int expectedId = i;
                int actualId = Integer.parseInt(instCode.getInstId());

                if (actualId != expectedId) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "PartCategory '%s': instCode '%s' has non-consecutive instId. Expected: %d, Actual: %d. InstId must be consecutive starting from 0.",
                                    partCode, instCode.getInstCode(), expectedId, actualId));
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format(
                                "PartCategory '%s': instCode '%s' has invalid instId format '%s'. Expected numeric value.",
                                partCode, instCode.getInstCode(), instCode.getInstId()));
            }
        }
    }

    /**
     * 设置实例编码和是否支持多实例到Part
     *
     * @param partAnno PartAnno注解
     * @param part     Part对象
     */
    public static void setInstcodes(PartAnno partAnno, IPart part) {
        if (partAnno == null || part == null) {
            return;
        }

        String instCodes = partAnno.instCodes();
        if (!Strings.isNullOrEmpty(instCodes)) {
            part.setExtAttr(EX_INST_CODES, instCodes);
            log.debug("Set instCodes '{}' for part '{}'", instCodes, part.getCode());
        }

        // 设置是否支持多实例
        if (part instanceof PartCategory) {
            PartCategory partCategory = (PartCategory) part;
            partCategory.setSupportMultiInst(partAnno.supportMultiInst());
        }
    }

    /**
     * 实例编码内部类
     */
    @Data
    @NoArgsConstructor
    public static class InstCode {
        /**
         * 完整实例编码，如：driveI0
         */
        private String instCode;

        /**
         * 实例基础编码，如：drive
         */
        private String code;

        /**
         * 实例名称，如：I0
         */
        private String instName;

        /**
         * 实例ID，如：0
         */
        private String instId;
    }

}
