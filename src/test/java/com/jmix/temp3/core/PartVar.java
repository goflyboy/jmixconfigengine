package com.jmix.temp3.core;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;

/**
 * 产品变量类（用于CP-SAT求解）
 * 支持日志跟踪
 */

@Data
public class PartVar {

    // 变量后缀常量
    private static final String SELECTED_SUFFIX = "_S";
    private static final String QUANTITY_SUFFIX = "_Q";

    public Part part;

    public String code;

    public BoolVar isSelected;

    public IntVar qty;

    CpModelTracker model;

    public String getQtyVarName() {
        return code + QUANTITY_SUFFIX;
    }

    public String getSelectVarName() {
        return code + SELECTED_SUFFIX;
    }

    public PartVar(CpModelTracker model, Part part) {
        this.model = model;
        this.part = part;
        this.code = part.code;

        // 创建布尔变量表示是否选中
        this.isSelected = model.newBoolVar(getSelectVarName());
        // 创建数量变量，假设最大数量为10
        this.qty = model.newIntVar(0, 10, getQtyVarName());

        System.out.println("PartVar: Created variables for part " + code
                + " - isSelected: " + isSelected.getName()
                + ", qty: " + qty.getName()
                + " (speed: " + part.speed + ", capacity: " + part.capacity
                + ", isSolidState: " + part.isSolidState + ")");

        // 关联关系：如果数量>0，则必须选中
        model.addGreaterOrEqual(qty, 1).onlyEnforceIf(isSelected);
        model.addLessOrEqual(qty, 0).onlyEnforceIf(isSelected.not());

        System.out.println("PartVar: Added selection constraints for part " + code);
    }

    public int getSpeed() {
        return part.speed;
    }

    public long getCapacity() {
        if (part.hasNormalizedAttr(Part.ATTR_CAPACITY)) {
            double dr = part.getNormalizedAttr(Part.ATTR_CAPACITY);
            return (long) dr;
        }
        return part.getCapacity();
    }

    public boolean isSolidState() {
        return part.isSolidState;
    }
}
