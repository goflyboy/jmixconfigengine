package com.jmix.coretest;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.PriorityAttrValue;

/**
 * 优先级属性值断言类
 * 
 * @since 2025-01-XX
 */
public class PriorityAttrValueAssert extends ProgammableInstAssert {
    private PriorityAttrValue actual;

    /**
     * 构造优先级属性值断言对象
     * 
     * @param actualModuleInst 实际的模块实例
     * @param module           模块定义
     * @param actual           实际的优先级属性值
     */
    public PriorityAttrValueAssert(ModuleInst actualModuleInst, Module module, PriorityAttrValue actual) {
        super(actualModuleInst, module);
        this.actual = actual;
    }

    /**
     * 断言优先级属性值等于指定值
     * 
     * @param expectValue 期望的值
     * @return 优先级属性值断言对象
     */
    public PriorityAttrValueAssert valueEqual(double expectValue) {
        double actualValue = actual.getOptimalValue();
        if (Math.abs(actualValue - expectValue) > 1e-9) {
            throw new AssertionError(String.format(
                    "Priority attribute value mismatch, expected: %s, actual: %s, attrCode: %s",
                    expectValue, actualValue, actual.getAttrCode()));
        }
        return this;
    }
}
