package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.PartInst;
import com.jmix.configengine.model.Part;
import java.util.Objects;

/**
 * 部件实例断言类
 */
public class PartInstAssert extends ProgammableInstAssert {
    private PartInst actual;
    private Part part;
    
    public PartInstAssert(PartInst actual) {
        super(null);
        this.actual = actual;
    }
    
    public PartInstAssert(PartInst actual, Part part) {
        super(null);
        this.actual = actual;
        this.part = part;
    }
    
    public PartInstAssert quantityEqual(Integer expectQuantity) {
        if (!Objects.equals(actual.quantity, expectQuantity)) {
            throw new AssertionError(String.format(
                "部件数量不匹配，期望: %s，实际: %s", expectQuantity, actual.quantity));
        }
        return this;
    }
    
    public PartInstAssert quantityGreaterThan(Integer minQuantity) {
        if (actual.quantity == null || actual.quantity <= minQuantity) {
            throw new AssertionError(String.format(
                "部件数量应大于: %s，实际: %s", minQuantity, actual.quantity));
        }
        return this;
    }
    
    public PartInstAssert hiddenEqual(boolean expectHidden) {
        if (actual.isHidden != expectHidden) {
            throw new AssertionError(String.format(
                "部件隐藏状态不匹配，期望: %s，实际: %s", expectHidden, actual.isHidden));
        }
        return this;
    }
} 