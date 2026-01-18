package com.jmix.temp3;

/**
 * 产品零件类
 */
class Part {
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

