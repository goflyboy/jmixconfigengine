package com.jmix.temp3.core;

import lombok.Data;

/**
 * 产品零件类
 */
@Data
public class Part {
    String code;
    boolean isSolidState; // 是否为固态硬盘
    int speed;
    int capacity;

    public Part(String code, boolean isSolidState, int speed, int capacity) {
        this.code = code;
        this.isSolidState = isSolidState;
        this.speed = speed;
        this.capacity = capacity;
    }
}
