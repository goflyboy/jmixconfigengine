package com.jmix.coretest;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.Para;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.PriorityAttrValue;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;

/**
 * 可编程实例断言基类
 * 
 * @since 2025-09-23
 */
public class ProgammableInstAssert {
    private ModuleInst actualModuleInst;

    private Module module;

    /**
     * 构造函数
     * 
     * @param actualModuleInst 实际的模块实例
     * @param module           模块定义
     */
    public ProgammableInstAssert(ModuleInst actualModuleInst, Module module) {
        this.actualModuleInst = actualModuleInst;
        this.module = module;
    }

    /**
     * 断言参数
     * 
     * @param code 参数编码
     * @return 参数实例断言对象
     */
    public ParaInstAssert assertPara(String code) {
        ParaInst paraInst = findParaByCode(code);
        if (paraInst == null) {
            throw new AssertionError("Parameter not found in moduleInst: " + code);
        }
        java.util.Optional<Para> paraOpt = module.getPara(code);
        if (!paraOpt.isPresent()) {
            throw new AssertionError("Parameter not found in module: " + code);
        }
        Para para = paraOpt.get();
        return new ParaInstAssert(actualModuleInst, module, paraInst, para);
    }

    /**
     * 断言部件
     * 
     * @param code 部件编码
     * @return 部件实例断言对象
     */
    public PartInstAssert assertPart(String code) {
        PartInst partInst = findPartByCode(code);
        if (partInst == null) {
            throw new AssertionError("Part not found: " + code);
        }
        java.util.Optional<Part> partOpt = module.getPart(code);
        if (!partOpt.isPresent()) {
            throw new AssertionError("Part not found in module: " + code);
        }
        Part part = partOpt.get();
        return new PartInstAssert(actualModuleInst, module, partInst, part);
    }

    private ParaInst findParaByCode(String code) {
        if (actualModuleInst.getParas() == null) {
            return null;
        }
        return actualModuleInst.getParas().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElse(null);
    }

    private PartInst findPartByCode(String code) {
        if (actualModuleInst.getParts() == null) {
            return null;
        }
        return actualModuleInst.getParts().stream()
                .filter(p -> code.equals(p.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 断言优先级属性值
     * 根据短编码（attrCode的前两位大写）查找对应的优先级属性值
     * 
     * @param shortCode 属性短编码，例如 "CA" 对应 "capacityWeight"
     * @return 优先级属性值断言对象
     */
    public PriorityAttrValueAssert assertPA(String shortCode) {
        PriorityAttrValue priorityAttrValue = findPriorityAttrValueByShortCode(shortCode);
        if (priorityAttrValue == null) {
            throw new AssertionError("Priority attribute not found with shortCode: " + shortCode);
        }
        return new PriorityAttrValueAssert(actualModuleInst, module, priorityAttrValue);
    }

    /**
     * 断言优先级综合值（Priority Overall Value）
     * 
     * @param expectedValue 期望的优先级综合值
     * @return 可编程实例断言对象
     */
    public ProgammableInstAssert assertPOEqual(double expectedValue) {
        double actualValue = actualModuleInst.getPriorityOverallValue();
        if (Math.abs(actualValue - expectedValue) > 1e-9) {
            throw new AssertionError(String.format(
                    "Priority overall value mismatch, expected: %s, actual: %s",
                    expectedValue, actualValue));
        }
        return this;
    }

    /**
     * 断言优先级排序号（Priority Sort Number）
     * 
     * @param expectedValue 期望的优先级排序号
     * @return 可编程实例断言对象
     */
    public ProgammableInstAssert assertPSEqual(int expectedValue) {
        int actualValue = actualModuleInst.getPrioritySortNo();
        if (actualValue != expectedValue) {
            throw new AssertionError(String.format(
                    "Priority sort number mismatch, expected: %s, actual: %s",
                    expectedValue, actualValue));
        }
        return this;
    }

    /**
     * 根据短编码查找优先级属性值
     * 短编码是 attrCode 的前两位大写字符
     * 
     * @param shortCode 短编码，例如 "CA"
     * @return 匹配的优先级属性值，如果未找到则返回null
     */
    private PriorityAttrValue findPriorityAttrValueByShortCode(String shortCode) {
        if (actualModuleInst.getPriorityAttrValues() == null || shortCode == null) {
            return null;
        }
        String upperShortCode = shortCode.toUpperCase();
        return actualModuleInst.getPriorityAttrValues().stream()
                .filter(pav -> {
                    String attrCode = pav.getAttrCode();
                    if (attrCode == null || attrCode.length() < 2) {
                        return false;
                    }
                    String attrShortCode = attrCode.substring(0, 2).toUpperCase();
                    return upperShortCode.equals(attrShortCode);
                })
                .findFirst()
                .orElse(null);
    }
}