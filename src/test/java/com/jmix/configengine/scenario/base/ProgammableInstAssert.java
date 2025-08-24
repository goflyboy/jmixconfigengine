package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.ModuleInst;
import com.jmix.configengine.ModuleConstraintExecutor.ParaInst;
import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.Part;


/**
 * 可编程实例断言基类
 */
public class ProgammableInstAssert {
    private ModuleInst actualModuleInst;
    private Module module;
    public ProgammableInstAssert(ModuleInst actualModuleInst, Module module) {
        this.actualModuleInst = actualModuleInst;
        this.module = module;
    }
    
    public ParaInstAssert assertPara(String code) {
        ParaInst paraInst = findParaByCode(code);
        if (paraInst == null) {
            throw new AssertionError("未找到参数 in moduleInst: " + code);
        }
        Para para = module.getPara(code);
        if (para == null) {
            throw new AssertionError("未找到参数 in module: " + code);
        }
        return new ParaInstAssert(actualModuleInst, module, paraInst, para);
    }
    
    public PartInstAssert assertPart(String code) {
        PartInst partInst = findPartByCode(code);
        if (partInst == null) {
            throw new AssertionError("未找到部件: " + code);
        }
        Part part = module.getPart(code);
        if (part == null) {
            throw new AssertionError("未找到部件 in module: " + code);
        }
        return new PartInstAssert(actualModuleInst, module, partInst, part);
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