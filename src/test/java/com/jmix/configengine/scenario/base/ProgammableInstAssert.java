package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.ModuleInst;
import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import com.jmix.configengine.ModuleConstraintExecutor.PartInst;

import java.util.List;
import java.util.Objects;

/**
 * 可编程实例断言基类
 */
public class ProgammableInstAssert {
    protected ModuleInst actualModuleInst;
    
    public ProgammableInstAssert(ModuleInst actualModuleInst) {
        this.actualModuleInst = actualModuleInst;
    }
    
    public ParaInstAssert assertPara(String code) {
        ParaInst paraInst = findParaByCode(code);
        if (paraInst == null) {
            throw new AssertionError("未找到参数: " + code);
        }
        return new ParaInstAssert(paraInst);
    }
    
    public PartInstAssert assertPart(String code) {
        PartInst partInst = findPartByCode(code);
        if (partInst == null) {
            throw new AssertionError("未找到部件: " + code);
        }
        return new PartInstAssert(partInst);
    }
    
    private ParaInst findParaByCode(String code) {
        if (actualModuleInst.paras == null) return null;
        return actualModuleInst.paras.stream()
            .filter(p -> code.equals(p.code))
            .findFirst()
            .orElse(null);
    }
    
    private PartInst findPartByCode(String code) {
        if (actualModuleInst.parts == null) return null;
        return actualModuleInst.parts.stream()
            .filter(p -> code.equals(p.code))
            .findFirst()
            .orElse(null);
    }
} 