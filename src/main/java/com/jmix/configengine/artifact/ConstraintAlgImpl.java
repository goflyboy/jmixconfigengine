package com.jmix.configengine.artifact;

/**
 * 约束算法实现基类
 */
public abstract class ConstraintAlgImpl {
    /**
     * 初始化变量
     */
    public abstract void initVariables();
    
    /**
     * 初始化约束
     */
    public abstract void initConstraint();
} 