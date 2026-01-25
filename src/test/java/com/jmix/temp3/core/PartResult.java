package com.jmix.temp3.core;

import lombok.Data;

/**
 * 部件结果类
 */
@Data
public class PartResult {
    String code;
    boolean isSelected;
    int qty;

    public PartResult(String code, boolean isSelected, int qty) {
        this.code = code;
        this.isSelected = isSelected;
        this.qty = qty;
    }

    public String getCode() {
        return code;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public int getQty() {
        return qty;
    }

    @Override
    public String toString() {
        return code + " S:" + (isSelected ? 1 : 0) + " Q:" + qty;
    }

    public String toShortString() {
        return code + " Q:" + qty;
    }
}
