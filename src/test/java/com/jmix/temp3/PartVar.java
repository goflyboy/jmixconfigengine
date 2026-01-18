package com.jmix.temp3;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;

/**
 * 产品变量类（用于CP-SAT求解）
 */
class PartVar {
    Part part;
    String code;
    BoolVar isSelected;
    IntVar qty;
    CpModel model;
    
    public PartVar(CpModel model, Part part) {
        this.model = model;
        this.part = part;
        this.code = part.code;
        
        // 创建布尔变量表示是否选中
        this.isSelected = model.newBoolVar("selected_" + code);
        // 创建数量变量，假设最大数量为10
        this.qty = model.newIntVar(0, 10, "qty_" + code);
        
        // 关联关系：如果数量>0，则必须选中
        model.addGreaterOrEqual(qty, 1).onlyEnforceIf(isSelected);
        model.addLessOrEqual(qty, 0).onlyEnforceIf(isSelected.not());
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

