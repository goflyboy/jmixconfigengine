package com.jmix.temp3;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

/**
 * 产品变量类（用于CP-SAT求解）
 * 支持日志跟踪
 */
class PartVar {

    // 变量后缀常量
    private static final String SELECTED_SUFFIX = "_S";
    private static final String QUANTITY_SUFFIX = "_Q";

    Part part;

    String code;

    BoolVar isSelected;

    IntVar qty;

    CpModelTracker model;

    public PartVar(CpModelTracker model, Part part) {
        this.model = model;
        this.part = part;
        this.code = part.code;

        // 创建布尔变量表示是否选中
        this.isSelected = model.newBoolVar(code + SELECTED_SUFFIX);
        // 创建数量变量，假设最大数量为10
        this.qty = model.newIntVar(0, 10, code + QUANTITY_SUFFIX);

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

    public int getCapacity() {
        return part.capacity;
    }

    public boolean isSolidState() {
        return part.isSolidState;
    }
}
