package com.jmix.temp2;

import lombok.Data;

/**
 * 简化的部件类
 * 用于产品求解器
 * 
 * @since 2025-01-14
 */
@Data
public class Part {
    /**
     * 部件代码
     */
    private String code;

    /**
     * 转速
     */
    private String speed;

    /**
     * 容量（T）
     */
    private int capacity;

    /**
     * 部件类型：sd（固态硬盘）或 md（机械硬盘）
     */
    private String type;
}
