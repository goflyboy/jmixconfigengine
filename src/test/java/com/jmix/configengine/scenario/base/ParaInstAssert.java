package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 参数实例断言类
 */
public class ParaInstAssert extends ProgammableInstAssert {
    private ParaInst actual;
    
    public ParaInstAssert(ParaInst actual) {
        super(null);
        this.actual = actual;
    }
    
    public ParaInstAssert valueEqual(String expectValue) {
        if (!Objects.equals(actual.value, expectValue)) {
            throw new AssertionError(String.format(
                "参数值不匹配，期望: %s，实际: %s", expectValue, actual.value));
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
    
    public ParaInstAssert hiddenEqual(boolean expectHidden) {
        if (actual.isHidden != expectHidden) {
            throw new AssertionError(String.format(
                "参数隐藏状态不匹配，期望: %s，实际: %s", expectHidden, actual.isHidden));
        }
        return this;
    }
} 