package com.jmix.executor.impl.algmodel;

import com.jmix.executor.bmodel.IPart;
import com.jmix.executor.bmodel.Part;
import com.jmix.executor.bmodel.attr.DynamicAttribute;
import com.jmix.executor.bmodel.attr.IDynamicAttributable;
import com.jmix.executor.bmodel.base.IExtensible;
import com.jmix.executor.cmodel.ModuleInst;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.IntVar;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部件变量
 * 表示约束求解中的部件变量，包含数量和隐藏状态
 * 
 * @since 2025-09-22
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class PartVarImpl extends VarImpl<IPart> implements IExtensible, IDynamicAttributable {

    /**
     * 部件数量Var的短名称
     */
    public static final String QTY_SHORT_NAME = "Q";

    /**
     * 显示隐藏Var的短名称
     */
    public static final String HIDDEN_SHORT_NAME = "H";

    /**
     * 选中Var的短名称
     */
    public static final String ISSELECTED_SHORT_NAME = "S";

    /**
     * 部件变量命名模式常量
     */
    public static final String VAR_PATTERN_PREFIX = "##Part.";

    /**
     * 部件数量Var的命名模式
     */
    public static final String QTY_PATTERN = VAR_PATTERN_PREFIX + "%s%s." + QTY_SHORT_NAME;

    /**
     * 显示隐藏Var的命名模式
     */
    public static final String HIDDEN_PATTERN = VAR_PATTERN_PREFIX + "%s%s." + HIDDEN_SHORT_NAME;
    /**
     * 选中隐藏Var的命名模式
     */
    public static final String ISSELECTED_PATTERN = VAR_PATTERN_PREFIX + "%s%s." + ISSELECTED_SHORT_NAME;

    /**
     * 实例ID
     */
    private int instId = ModuleInst.DEFAULT_INSTANCE_ID;

    /**
     * 部件的数量值
     */
    private IntVar qty;

    /**
     * 显示隐藏属性
     */
    private BoolVar isHidden;

    /**
     * 子部件列表
     */
    private List<PartVarImpl> subParts = new ArrayList<>();

    /**
     * 子部件映射表(Part.code -> PartVarImpl)
     */
    private Map<String, PartVarImpl> subPartMap = new HashMap<>();

    /**
     * 是否选中
     */
    private BoolVar isSelected;

    /**
     * 获取变量字符串表示
     * 
     * @param solutionCallback 求解回调
     * @return 变量字符串表示
     */
    @Override
    public String getVarString(CpSolverSolutionCallback solutionCallback) {
        StringBuilder sb = new StringBuilder();
        sb.append("PartVarImpl{code=").append(getCode());
        sb.append(", qty=").append(this.qty == null ? "null" : solutionCallback.value(this.getQty()));
        sb.append(", hidden=").append(this.isHidden == null ? "null" : solutionCallback.value(this.getIsHidden()));
        sb.append(", selected=")
                .append(this.isSelected == null ? "null" : solutionCallback.value(this.getIsSelected()));
        sb.append(", subParts=").append(getSubParts().size());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取短字符串表示
     * 
     * @param solutionCallback 求解回调
     * @return 短字符串表示
     */
    @Override
    public String getShortString(CpSolverSolutionCallback solutionCallback) {
        // P1(Q:1,H:0,S:1)
        return String.format("%s(%s:%s,%s:%s,%s:%s)", getCode(), QTY_SHORT_NAME,
                this.qty == null ? "null" : solutionCallback.value(this.qty),
                HIDDEN_SHORT_NAME,
                this.isHidden == null ? "null" : solutionCallback.value(this.isHidden),
                ISSELECTED_SHORT_NAME,
                this.isSelected == null ? "null" : solutionCallback.value(this.isSelected));
    }

    @Override
    public void setExtAttrs(Map<String, String> extAttrs) {
        getBase().setExtAttrs(extAttrs);
    }

    @Override
    public Map<String, String> getExtAttrs() {
        return getBase().getExtAttrs();
    }

    @Override
    public String getExtAttr(String key) {
        return getBase().getExtAttr(key);
    }

    @Override
    public void setExtAttr(String key, String value) {
        getBase().setExtAttr(key, value);
    }

    @Override
    public String getExtSchema() {
        return getBase().getExtSchema();
    }

    @Override
    public void setExtSchema(String extSchema) {
        getBase().setExtSchema(extSchema);
    }

    @Override
    public Map<String, String> getDynAttr() {
        return getBase().getDynAttr();
    }

    @Override
    public void setDynAttr(Map<String, String> dynAttr) {
        getBase().setDynAttr(dynAttr);
    }

    @Override
    public void setAttr(String key, String value) {
        getBase().setAttr(key, value);
    }

    @Override
    public String getAttr(String key) {
        String result = getBase().getAttr(key);
        if (result == null) {
            Part part = (Part) getBase();
            if (key.equals("fatherCode")) {
                result = part.getFatherCode();
            }
            if (key.equals("code")) {
                result = part.getCode();
            }
        }
        return result;
    }

    public int getAttr4Int(String attrCode) {
        return getAttr4IntDefault(attrCode, null);
    }

    public int getAttr4IntDefault(String attrCode, Integer defaultValue) {
        String attrValue = getBase().getAttr(attrCode);
        if (attrValue == null && defaultValue == null) {
            log.error("code={},attrCode={} is not found and defaultValue is not set", getCode(), attrCode);
            throw new IllegalArgumentException(
                    String.format("code=%s,attrCode=%s is not found and defaultValue is not set", getCode(), attrCode));
        }
        if (attrValue != null) {
            return Integer.parseInt(attrValue);
        }
        return defaultValue;
    }

    /**
     * Legacy accessor kept for older packaged algorithms.
     */
    public Map<String, BoolVar> getSubPartSelectedVars() {
        Map<String, BoolVar> selectedVars = new HashMap<>();
        for (Map.Entry<String, PartVarImpl> entry : subPartMap.entrySet()) {
            selectedVars.put(entry.getKey(), entry.getValue().getIsSelected());
        }
        return selectedVars;
    }

    @Override
    public List<DynamicAttribute> getDynAttrSchemas() {
        return getBase().getDynAttrSchemas();
    }

    @Override
    public void setDynAttrSchemas(List<DynamicAttribute> dynAttrSchemas) {
        getBase().setDynAttrSchemas(dynAttrSchemas);
    }

    @Override
    public DynamicAttribute getDynAttrSchema(String code) {
        return getBase().getDynAttrSchema(code);
    }

    @Override
    public void setDynAttrSchema(String code, DynamicAttribute dynAttrSchema) {
        getBase().setDynAttrSchema(code, dynAttrSchema);
    }
}
