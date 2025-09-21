package com.jmix.configengine.scenario.base;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.ParaOption;
import com.jmix.executor.impl.util.ParaTypeHandler;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;

/**
 * 参数实例断言类
 */
public class ParaInstAssert extends ProgammableInstAssert {
    private ParaInst actual;
    private Para para;

    public ParaInstAssert(ModuleInst actualModuleInst, Module module, ParaInst actual, Para para) {
        super(actualModuleInst, module);
        this.actual = actual;
        this.para = para;
    }

    public ParaInstAssert valueEqual(Integer expectValue) {
        return valueEqual(expectValue.toString());
    }

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

    public ParaInstAssert valueIdEqual(String expectValueId) {
        if (!Objects.equals(actual.getValue(), expectValueId)) {
            throw new AssertionError(String.format(
                    "参数值不匹配，期望: %s，实际: %s", expectValueId, actual.getValue()));
        }
        return this;
    }

    public ParaInstAssert valueNotEqual(String expectValue) {
        if (Objects.equals(actual.getValue(), expectValue)) {
            throw new AssertionError(String.format(
                    "参数值不应等于: %s", expectValue));
        }
        return this;
    }

    public ParaInstAssert optionsEqual(String... expectOptions) {
        if (actual.getOptions() == null) {
            throw new AssertionError("参数选项为空");
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

    public ParaInstAssert hiddenEqual(boolean expectHidden) {
        if (actual.isHidden() != expectHidden) {
            throw new AssertionError(String.format(
                    "参数隐藏状态不匹配，期望: %s，实际: %s", expectHidden, actual.isHidden()));
        }
        return this;
    }

    public ParaInstAssert isHiddenEqual(boolean expectHidden) {
        return hiddenEqual(expectHidden);
    }

    public ParaInstAssert isHiddenEqual(String expectHidden) {
        // expectHidden="0" or "1"
        boolean expectHiddenValue = expectHidden.equals("0") ? false : true;
        return hiddenEqual(expectHiddenValue);
    }

    public ParaInstAssert valueIn(String... expectValues) {
        boolean found = false;
        for (String expectValue : expectValues) {
            ParaOption option = para.getOption(expectValue);
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