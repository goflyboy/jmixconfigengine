package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.*;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.constant.VisibilityModeConstants;
import java.util.Arrays;
import java.util.HashSet;
import com.jmix.configengine.util.ParaTypeHandler;
import java.util.Objects;
import java.util.Set;

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
    
    public ParaInstAssert valueEqual(int expectValue) {
        return valueEqual(String.valueOf(expectValue));
    }
    public ParaInstAssert valueEqual(String expectValue) {
        //expectValue的值是code值，转化为codeId
        try {
            String expectCodeIdValue = ParaTypeHandler.getCodeIdValue(para, expectValue);
            if (!Objects.equals(actual.value, expectCodeIdValue)) {
                throw new AssertionError(String.format(
                    "参数值不匹配，期望: %s，实际: %s", expectValue, actual.value));
            }
        } catch (RuntimeException e) {
            throw new AssertionError(e.getMessage());
        }
        return this;
    }
    public ParaInstAssert valueIdEqual(String expectValueId) {
        if (!Objects.equals(actual.value, expectValueId)) {
            throw new AssertionError(String.format(
                "参数值不匹配，期望: %s，实际: %s", expectValueId, actual.value));
        }
        return this;
    }
    public ParaInstAssert valueNotEqual(String expectValue) {
        if (Objects.equals(actual.value, expectValue)) {
            throw new AssertionError(String.format(
                "参数值不应等于: %s", expectValue));
        }
        return this;
    }
    
    public ParaInstAssert optionsEqual(String... expectOptions) {
        if (actual.options == null) {
            throw new AssertionError("参数选项为空");
        }
        
        Set<String> actualOptions = new HashSet<>(actual.options);
        Set<String> expectedOptions = new HashSet<>(Arrays.asList(expectOptions));
        
        if (!actualOptions.containsAll(expectedOptions)) {
            throw new AssertionError(String.format(
                "参数选项不匹配，期望包含: %s，实际: %s", 
                Arrays.toString(expectOptions), actual.options));
        }
        return this;
    }
    
    public ParaInstAssert visibilityModeEqual(int expectVisibilityMode) {
        if (actual.visibilityMode != expectVisibilityMode) {
            throw new AssertionError(String.format(
                "参数可见性模式不匹配，期望: %s(%s)，实际: %s(%s)", 
                expectVisibilityMode, VisibilityModeConstants.getDescription(expectVisibilityMode),
                actual.visibilityMode, VisibilityModeConstants.getDescription(actual.visibilityMode)));
        }
        return this;
    }
    
    public ParaInstAssert visibilityModeNotEqual(int expectVisibilityMode) {
        if (actual.visibilityMode == expectVisibilityMode) {
            throw new AssertionError(String.format(
                "参数可见性模式不应等于: %s(%s)，实际: %s(%s)", 
                expectVisibilityMode, VisibilityModeConstants.getDescription(expectVisibilityMode),
                actual.visibilityMode, VisibilityModeConstants.getDescription(actual.visibilityMode)));
        }
        return this;
    }
    
    /**
     * @deprecated 使用visibilityModeEqual替代
     */
    @Deprecated
    public ParaInstAssert hiddenEqual(boolean expectHidden) {
        int expectedMode = expectHidden ? VisibilityModeConstants.HIDDEN_READONLY : VisibilityModeConstants.VISIBLE_EDITABLE;
        return visibilityModeEqual(expectedMode);
    }
    
    public ParaInstAssert valueIn(String... expectValues) {
        boolean found = false;
        for (String expectValue : expectValues) {
            ParaOption option = para.getOption(expectValue);
            if (option != null && Objects.equals(actual.value, String.valueOf(option.getCodeId()))) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError(String.format(
                "参数值不在期望范围内，期望: %s，实际: %s", 
                Arrays.toString(expectValues), actual.value));
        }
        return this;
    }
} 