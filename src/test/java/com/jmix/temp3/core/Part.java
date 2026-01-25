package com.jmix.temp3.core;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 部件类（数据模型）
 */
@Data
public class Part {
    public String code;
    public boolean isSolidState; // 是否为固态硬盘
    public int speed; // 转速
    public long capacity; // 容量
    private Map<String, Double> attrMap = new HashMap<>(); // 原始属性
    private Map<String, Double> normalizedAttrMap = new HashMap<>(); // 归一化属性

    // 常量定义
    public static final String ATTR_CAPACITY = "CAPACITY";
    public static final String ATTR_LISTPRICE = "LISTPRICE";
    public static final String ATTR_DELIVERYTIME = "DELIVERYTIME";
    public static final String ATTR_PROFIT = "PROFIT";
    public static final String ATTR_WEIGHT = "WEIGHT";
    public static final String ATTR_PRIORITY_SCORE = "PRIORITY_SCORE";

    public Part(String code, boolean isSolidState, int speed, int capacity) {
        this.code = code;
        this.isSolidState = isSolidState;
        this.speed = speed;
        this.capacity = capacity;

        // 初始化基本属性
        setAttr(ATTR_CAPACITY, (double) capacity);
        setAttr(ATTR_WEIGHT, (double) speed / 100.0); // 简化权重计算
    }

    // 设置原始属性
    public void setAttr(String attrName, double value) {
        attrMap.put(attrName, value);
    }

    // 获取原始属性
    public double getAttr(String attrName) {
        return attrMap.getOrDefault(attrName, 0.0);
    }

    // 设置归一化属性
    public void setNormalizedAttr(String attrName, double normalizedValue) {
        normalizedAttrMap.put(attrName, normalizedValue);
    }

    // 获取归一化属性
    public double getNormalizedAttr(String attrName) {
        return normalizedAttrMap.get(attrName); // 默认50
    }

    // 检查是否包含某个属性
    public boolean hasAttr(String attrName) {
        return attrMap.containsKey(attrName);
    } // 检查是否包含某个属性

    public boolean hasNormalizedAttr(String attrName) {
        return normalizedAttrMap.containsKey(attrName);
    }
}
