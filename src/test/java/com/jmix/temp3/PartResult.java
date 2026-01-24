package com.jmix.temp3;

/**
 * 部件结果类
 */
class PartResult {
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
}
