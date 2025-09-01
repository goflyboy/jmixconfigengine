package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor.*;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.constant.VisibilityModeConstants;
import java.util.Objects;

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
    
    public PartInstAssert visibilityModeEqual(int expectVisibilityMode) {
        if (actual.visibilityMode != expectVisibilityMode) {
            throw new AssertionError(String.format(
                "部件可见性模式不匹配，期望: %s(%s)，实际: %s(%s)", 
                expectVisibilityMode, VisibilityModeConstants.getDescription(expectVisibilityMode),
                actual.visibilityMode, VisibilityModeConstants.getDescription(actual.visibilityMode)));
        }
        return this;
    }
    
    /**
     * @deprecated 使用visibilityModeEqual替代
     */
    @Deprecated
    public PartInstAssert hiddenEqual(boolean expectHidden) {
        int expectedMode = expectHidden ? VisibilityModeConstants.HIDDEN_READONLY : VisibilityModeConstants.VISIBLE_EDITABLE;
        return visibilityModeEqual(expectedMode);
    }
} 