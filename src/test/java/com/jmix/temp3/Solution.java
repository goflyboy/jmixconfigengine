package com.jmix.temp3;

import java.util.List;

/**
 * 解决方案类
 */
class Solution {
    List<PartResult> parts;
    double objectValue;

    public Solution(List<PartResult> parts, double objectValue) {
        this.parts = parts;
        this.objectValue = objectValue;
    }

    public List<PartResult> getParts() {
        return parts;
    }

    public double getObjectValue() {
        return objectValue;
    }
}
