package com.jmix.configengine.artifact;
 
import com.jmix.configengine.artifact.*; 
import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import com.google.ortools.Loader;
/**
 * 约束算法实现基类
 */
public abstract class ConstraintAlgImpl {
    static {
        Loader.loadNativeLibraries();
    }
    /**
     * 初始化变量
     */
    public abstract void initVariables();
    
    /**
     * 初始化约束
     */
    public abstract void initConstraint();

    public abstract void initModel(CpModel model);

    public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
        return model.newIntVarFromDomain(Domain.fromValues(values), name);
    }
} 