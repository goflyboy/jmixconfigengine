package com.jmix.temp3;

import java.util.List;

/**
 * 解决方案类
 */
class Solution {
    List<PartResult> parts;
    double objectValue;// 目标函数的值
    int searchStep;// 搜索到这个解的步骤序号

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("Solution{");
        if (parts != null && !parts.isEmpty()) {
            sb.append("parts=[");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(parts.get(i).toString());
            }
            sb.append("]");
        }
        sb.append(", OV=").append(objectValue);
        sb.append(", SS=").append(searchStep);
        // sb.append("}");
        return sb.toString();
    }

    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        // sb.append("Solution{");
        if (parts != null && !parts.isEmpty()) {
            sb.append("parts=[");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(parts.get(i).toShortString());
            }
            sb.append("]");
        }
        sb.append(", OV=").append(objectValue);
        sb.append(", SS=").append(searchStep);
        // sb.append("}");
        return sb.toString();
    }

}
