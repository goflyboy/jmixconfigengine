package com.jmix.temp2;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;

import lombok.Data;

/**
 * 部件变量
 * 包含求解器变量和部件信息
 * 
 * @since 2025-01-14
 */
@Data
public class PartVar {
    /**
     * 部件对象
     */
    private Part part;

    /**
     * 部件代码
     */
    private String code;

    /**
     * 是否选中
     */
    private BoolVar isSelected;

    /**
     * 配置数量
     */
    private IntVar qty;

    /**
     * 获取转速
     * 
     * @return 转速字符串
     */
    public String getSpeed() {
        if (part != null) {
            return part.getSpeed();
        }
        return null;
    }

    /**
     * 获取容量
     * 
     * @return 容量（T）
     */
    public int getCapacity() {
        if (part != null) {
            return part.getCapacity();
        }
        return 0;
    }
}

