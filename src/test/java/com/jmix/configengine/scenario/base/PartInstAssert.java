package com.jmix.configengine.scenario.base;

import java.util.Objects;

import com.jmix.executor.imodel.Module;
import com.jmix.executor.imodel.Part;
import com.jmix.executor.omodel.ModuleInst;
import com.jmix.executor.omodel.PartInst;

/**
 * 部件实例断言类
 */
public class PartInstAssert extends ProgammableInstAssert {
    private PartInst actual;
    private Part part;

    public PartInstAssert(ModuleInst actualModuleInst, Module module, PartInst actual, Part part) {
        super(actualModuleInst, module);
        this.actual = actual;
        this.part = part;
    }

    public PartInstAssert quantityEqual(Integer expectQuantity) {
        if (!Objects.equals(actual.getQuantity(), expectQuantity)) {
            throw new AssertionError(String.format(
                    "部件%s数量不匹配，期望: %s，实际: %s", part.getCode(), expectQuantity, actual.getQuantity()));
        }
        return this;
    }

    public PartInstAssert quantityGreaterThan(Integer minQuantity) {
        if (actual.getQuantity() == null || actual.getQuantity() <= minQuantity) {
            throw new AssertionError(String.format(
                    "部件数量应大于: %s，实际: %s", minQuantity, actual.getQuantity()));
        }
        return this;
    }

    public PartInstAssert hiddenEqual(boolean expectHidden) {
        if (actual.isHidden() != expectHidden) {
            throw new AssertionError(String.format(
                    "部件隐藏状态不匹配，期望: %s，实际: %s", expectHidden, actual.isHidden()));
        }
        return this;
    }
}