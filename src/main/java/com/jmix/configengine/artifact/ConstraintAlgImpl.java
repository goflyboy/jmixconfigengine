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
    // CP模型
    protected CpModel model; 

    public void initModel(CpModel model){
        this.model = model;
        initModelAfter(model);
        initVariables();
        initConstraint();
    }
    protected abstract void initModelAfter(CpModel model);
    protected abstract void initVariables();
    protected abstract void initConstraint();

    public static IntVar newIntVarFromDomain(CpModel model, long[] values, String name) {
        return model.newIntVarFromDomain(Domain.fromValues(values), name);
    }
} 