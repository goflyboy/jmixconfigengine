package com.jmix.configengine.scenario.base;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Para;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.ParaInst;
import com.jmix.executor.omodel.PartInst;

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
        java.util.Optional<Para> paraOpt = module.getPara(code);
        if (!paraOpt.isPresent()) {
            throw new AssertionError("未找到参数 in module: " + code);
        }
        Para para = paraOpt.get();
        return new ParaInstAssert(actualModuleInst, module, paraInst, para);
    }

    public PartInstAssert assertPart(String code) {
        PartInst partInst = findPartByCode(code);
        if (partInst == null) {
            throw new AssertionError("未找到部件: " + code);
        }
        java.util.Optional<Part> partOpt = module.getPart(code);
        if (!partOpt.isPresent()) {
            throw new AssertionError("未找到部件 in module: " + code);
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
}