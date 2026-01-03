package com.jmix.executor.imodel;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 参数定义
 * 表示模块中的参数，支持多种类型（枚举、整数、字符串等）
 *
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Para extends DynamicAttribute {

    /**
     * 参数默认最小值
     */
    public static final String DEFAULT_MIN_VALUE = "-1000";

    /**
     * 参数默认最大值
     */
    public static final String DEFAULT_MAX_VALUE = "1000";

    /**
     * 参数短编码前缀
     */
    public static final String SHORT_CODE_PREFIX = "P";

    private ParaType paraType = ParaType.ENUM;

    /**
     * Range类型的最小值
     */
    private String minValue = DEFAULT_MIN_VALUE;

    /**
     * Range类型的最大值
     */
    private String maxValue = DEFAULT_MAX_VALUE;

    // 引用的规格Code
    private String refSpecCode;

    /**
     * <codeId, ParaOption> Map
     */
    @JsonIgnore
    private Map<Integer, DynamicAttributerOption> optionCodeIdMap = new HashMap<>();

    /**
     * <code, ParaOption> Map
     */
    @JsonIgnore
    private Map<String, DynamicAttributerOption> optionCodeMap = new HashMap<>();

    /**
     * 根据选项编码获取参数选项
     *
     * @param optionCode 选项编码
     * @return 参数选项对象，如果不存在则返回Optional.empty()
     */
    @JsonIgnore
    public Optional<DynamicAttributerOption> getOption(String optionCode) {
        if (optionCodeMap.size() != super.getOptions().size()) {
            optionCodeMap.clear();
            super.getOptions().forEach(option -> optionCodeMap.put(option.getCode(), option));
        }
        DynamicAttributerOption option = optionCodeMap.get(optionCode);
        return option == null ? Optional.empty() : Optional.of(option);
    }

    /**
     * 根据选项编码id获取参数选项
     *
     * @param optionCodeId 选项编码Id
     * @return 参数选项对象，如果不存在则返回Optional.empty()
     */
    @JsonIgnore
    public Optional<DynamicAttributerOption> getOption(Integer optionCodeId) {
        if (optionCodeIdMap.size() != super.getOptions().size()) {
            optionCodeIdMap.clear();
            super.getOptions().forEach(option -> optionCodeIdMap.put(option.getCodeId(), option));
        }
        DynamicAttributerOption option = optionCodeIdMap.get(optionCodeId);
        return option == null ? Optional.empty() : Optional.of(option);
    }

    /**
     * 获取所有选项的ID数组
     *
     * @return 选项ID数组
     */
    @JsonIgnore
    public long[] getOptionIds() {
        List<DynamicAttributerOption> options = super.getOptions();
        if (options == null || options.isEmpty()) {
            return new long[0];
        }
        long[] ids = new long[options.size()];
        for (int i = 0; i < options.size(); i++) {
            ids[i] = options.get(i).getCodeId();
        }
        return ids;
    }

    /**
     * 获取所有选项的编码数组
     *
     * @return 选项编码数组
     */
    @JsonIgnore
    public String[] getOptionCodes() {
        List<DynamicAttributerOption> options = super.getOptions();
        if (options == null || options.isEmpty()) {
            return new String[0];
        }
        String[] codes = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            codes[i] = options.get(i).getCode();
        }
        return codes;
    }
}