package com.jmix.coretest;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.attr.DynamicAttributerOption;
import com.jmix.executor.bmodel.para.Para;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.cmodel.ParaInst;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 参数实例断言类
 * 
 * @since 2025-09-23
 */
public class ParaInstAssert extends ProgammableInstAssert {
    private ParaInst actual;
    private Para para;

    /**
     * 构造参数实例断言对象
     * 
     * @param actualModuleInst 实际的模块实例
     * @param module           模块定义
     * @param actual           实际的参数实例
     * @param para             参数定义
     */
    public ParaInstAssert(ModuleInst actualModuleInst, Module module, ParaInst actual, Para para) {
        super(actualModuleInst, module);
        this.actual = actual;
        this.para = para;
    }

    /**
     * 断言参数值等于指定整数值
     * 
     * @param expectValue 期望的整数值
     * @return 参数实例断言对象
     */
    public ParaInstAssert valueEqual(Integer expectValue) {
        return valueEqual(expectValue.toString());
    }

    /**
     * 断言参数值等于指定字符串值
     * 
     * @param expectValue 期望的字符串值
     * @return 参数实例断言对象
     */
    public ParaInstAssert valueEqual(String expectValue) {
        // expectValue的值是code值，转化为codeId
        try {
            String expectCodeIdValue = ParaTypeHandler.getCodeIdValue(para, expectValue);
            if (!Objects.equals(actual.getValue(), expectCodeIdValue)) {
                throw new AssertionError(String.format(
                        "参数值不匹配，期望: %s，实际: %s", expectValue, actual.getValue()));
            }
        } catch (RuntimeException e) {
            throw new AssertionError(e.getMessage());
        }
        return this;
    }

    /**
     * 断言参数值ID等于指定值
     * 
     * @param expectValueId 期望的值ID
     * @return 参数实例断言对象
     */
    public ParaInstAssert valueIdEqual(String expectValueId) {
        if (!Objects.equals(actual.getValue(), expectValueId)) {
            throw new AssertionError(String.format(
                    "参数值不匹配，期望: %s，实际: %s", expectValueId, actual.getValue()));
        }
        return this;
    }

    /**
     * 断言参数值不等于指定值
     * 
     * @param expectValue 期望的值
     * @return 参数实例断言对象
     */
    public ParaInstAssert valueNotEqual(String expectValue) {
        if (Objects.equals(actual.getValue(), expectValue)) {
            throw new AssertionError(String.format(
                    "参数值不应等于: %s", expectValue));
        }
        return this;
    }

    /**
     * 断言参数选项等于指定选项
     * 
     * @param expectOptions 期望的选项数组
     * @return 参数实例断言对象
     */
    public ParaInstAssert optionsEqual(String... expectOptions) {
        if (actual.getOptions() == null) {
            throw new AssertionError("Parameter option is null");
        }

        Set<String> actualOptions = new HashSet<>(actual.getOptions());
        Set<String> expectedOptions = new HashSet<>(Arrays.asList(expectOptions));

        if (!actualOptions.containsAll(expectedOptions)) {
            throw new AssertionError(String.format(
                    "参数选项不匹配，期望包含: %s，实际: %s",
                    Arrays.toString(expectOptions), actual.getOptions()));
        }
        return this;
    }

    /**
     * 断言参数隐藏状态等于指定值
     * 
     * @param expectHidden 期望的隐藏状态
     * @return 参数实例断言对象
     */
    public ParaInstAssert hiddenEqual(boolean expectHidden) {
        if (actual.isHidden() != expectHidden) {
            throw new AssertionError(String.format(
                    "参数隐藏状态不匹配，期望: %s，实际: %s", expectHidden, actual.isHidden()));
        }
        return this;
    }

    /**
     * 断言参数隐藏状态等于指定值（布尔值）
     * 
     * @param expectHidden 期望的隐藏状态
     * @return 参数实例断言对象
     */
    public ParaInstAssert isHiddenEqual(boolean expectHidden) {
        return hiddenEqual(expectHidden);
    }

    /**
     * 断言参数隐藏状态等于指定值（字符串）
     * 
     * @param expectHidden 期望的隐藏状态（"0"或"1"）
     * @return 参数实例断言对象
     */
    public ParaInstAssert isHiddenEqual(String expectHidden) {
        // expectHidden="0" or "1"
        boolean expectHiddenValue = expectHidden.equals("0") ? false : true;
        return hiddenEqual(expectHiddenValue);
    }

    /**
     * 断言参数值在指定值列表中
     * 
     * @param expectValues 期望的值列表
     * @return 参数实例断言对象
     */
    public ParaInstAssert valueIn(String... expectValues) {
        boolean found = false;
        for (String expectValue : expectValues) {
            DynamicAttributerOption option = para.getOption(expectValue).orElse(null);
            if (option != null && Objects.equals(actual.getValue(), String.valueOf(option.getCodeId()))) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError(String.format(
                    "参数值不在期望范围内，期望: %s，实际: %s",
                    Arrays.toString(expectValues), actual.getValue()));
        }
        return this;
    }
}