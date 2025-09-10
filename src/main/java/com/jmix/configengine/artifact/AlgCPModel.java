package com.jmix.configengine.artifact;

import com.google.ortools.util.Domain;
import com.google.ortools.sat.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map; 

/**
 * 对CpModel接口的封装，实现变量注册等封装
 * 实现对CpModel接口封装，类似测试代码调用model的代码都不需要修改（如：model.addBoolAnd)
 */
@Slf4j
public class AlgCPModel {
    private CpModel model;
    
    // other variables map
    public Map<String, OtherVar> otherVarMap = new HashMap<>();
    // other variable index counter
    private int otherVarIndex = 0;

    public AlgCPModel() {
        this.model = new CpModel();
    }

    public AlgCPModel(CpModel model) {
        this.model = model;
    }

    /**
     * 获取底层的CpModel实例
     */
    public CpModel getModel() {
        return model;
    }

    /**
     * 封装CpModel的newIntVar方法
     */
    public IntVar newIntVar(long left, long right, String name) {
        IntVar tv = this.model.newIntVar(left, right, name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 单个值
     */
    public IntVar newIntVarFromDomain(long value, String name) {
        IntVar tv = this.model.newIntVarFromDomain(Domain.fromValues(new long[]{value}), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 多个值
     */
    public IntVar newIntVarFromDomain(long[] values, String name) {
        IntVar tv = this.model.newIntVarFromDomain(Domain.fromValues(values), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 区间
     */
    public IntVar newIntVarFromDomain(long[][] intervals, String name) {
        IntVar tv = this.model.newIntVarFromDomain(Domain.fromIntervals(intervals), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 封装CpModel的newIntVarFromDomain方法 - 完整域
     */
    public IntVar newIntVarFromDomain(String name) {
        IntVar tv = this.model.newIntVarFromDomain(Domain.fromValues(new long[]{Long.MIN_VALUE, Long.MAX_VALUE}), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 封装CpModel的newBoolVar方法
     */
    public BoolVar newBoolVar(String name) {
        BoolVar tv = this.model.newBoolVar(name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 注册其他变量
     * @param tv 变量对象
     * @param name 变量名称
     */
    private void registerVariables(IntVar tv, String name) {
        // 判断是否已经存在，如果已经存在，则抛异常
        if (otherVarMap.containsKey(name)) {
            throw new RuntimeException("Variable already exists: " + name);
        }
        
        // name不是以ParaVar.VAR_PATTEN_PREFIX 且 PartVar.VAR_PATTEN_PREFIX开头，则添加到otherVarMap
        if (!name.startsWith(ParaVar.VAR_PATTEN_PREFIX) && !name.startsWith(PartVar.VAR_PATTEN_PREFIX)) {
            OtherVar otherVar = new OtherVar();
            otherVar.code = name;
            otherVar.var = tv;
            otherVar.shortCode = "o" + (++otherVarIndex);
            otherVarMap.put(name, otherVar);
        }
    }

    /**
     * 注册其他变量 - BoolVar版本
     * @param tv 变量对象
     * @param name 变量名称
     */
    private void registerVariables(BoolVar tv, String name) {
        registerVariables((IntVar) tv, name);
    }

    // 以下方法直接委托给底层的CpModel，保持接口兼容性
    
    // Boolean Constraints
    
    /** Adds {@code Or(literals) == true}. */
    public Constraint addBoolOr(Literal[] literals) {
        return model.addBoolOr(literals);
    }

    /** Adds {@code Or(literals) == true}. */
    public Constraint addBoolOr(Iterable<Literal> literals) {
        return model.addBoolOr(literals);
    }

    /** Same as addBoolOr. {@code Sum(literals) >= 1}. */
    public Constraint addAtLeastOne(Literal[] literals) {
        return model.addAtLeastOne(literals);
    }

    /** Same as addBoolOr. {@code Sum(literals) >= 1}. */
    public Constraint addAtLeastOne(Iterable<Literal> literals) {
        return model.addAtLeastOne(literals);
    }

    /** Adds {@code AtMostOne(literals): Sum(literals) <= 1}. */
    public Constraint addAtMostOne(Literal[] literals) {
        return model.addAtMostOne(literals);
    }

    /** Adds {@code AtMostOne(literals): Sum(literals) <= 1}. */
    public Constraint addAtMostOne(Iterable<Literal> literals) {
        return model.addAtMostOne(literals);
    }

    /** Adds {@code ExactlyOne(literals): Sum(literals) == 1}. */
    public Constraint addExactlyOne(Literal[] literals) {
        return model.addExactlyOne(literals);
    }

    /** Adds {@code ExactlyOne(literals): Sum(literals) == 1}. */
    public Constraint addExactlyOne(Iterable<Literal> literals) {
        return model.addExactlyOne(literals);
    }

    /** Adds {@code And(literals) == true}. */
    public Constraint addBoolAnd(Literal[] literals) {
        return model.addBoolAnd(literals);
    }

    /** Adds {@code And(literals) == true}. */
    public Constraint addBoolAnd(Iterable<Literal> literals) {
        return model.addBoolAnd(literals);
    }

    /** Adds {@code XOr(literals) == true}. */
    public Constraint addBoolXor(Literal[] literals) {
        return model.addBoolXor(literals);
    }

    /** Adds {@code XOr(literals) == true}. */
    public Constraint addBoolXor(Iterable<Literal> literals) {
        return model.addBoolXor(literals);
    }

    /** Adds {@code a => b}. */
    public Constraint addImplication(Literal a, Literal b) {
        return model.addImplication(a, b);
    }

    // Linear constraints

    /** Adds {@code expr == value}. */
    public Constraint addEquality(LinearArgument expr, long value) {
        return model.addEquality(expr, value);
    }

    /** Adds {@code left == right}. */
    public Constraint addEquality(LinearArgument left, LinearArgument right) {
        return model.addEquality(left, right);
    }

    /** Adds {@code expr <= value}. */
    public Constraint addLessOrEqual(LinearArgument expr, long value) {
        return model.addLessOrEqual(expr, value);
    }

    /** Adds {@code left <= right}. */
    public Constraint addLessOrEqual(LinearArgument left, LinearArgument right) {
        return model.addLessOrEqual(left, right);
    }

    /** Adds {@code expr < value}. */
    public Constraint addLessThan(LinearArgument expr, long value) {
        return model.addLessThan(expr, value);
    }

    /** Adds {@code left < right}. */
    public Constraint addLessThan(LinearArgument left, LinearArgument right) {
        return model.addLessThan(left, right);
    }

    /** Adds {@code expr >= value}. */
    public Constraint addGreaterOrEqual(LinearArgument expr, long value) {
        return model.addGreaterOrEqual(expr, value);
    }

    /** Adds {@code left >= right}. */
    public Constraint addGreaterOrEqual(LinearArgument left, LinearArgument right) {
        return model.addGreaterOrEqual(left, right);
    }

    /** Adds {@code expr > value}. */
    public Constraint addGreaterThan(LinearArgument expr, long value) {
        return model.addGreaterThan(expr, value);
    }

    /** Adds {@code left > right}. */
    public Constraint addGreaterThan(LinearArgument left, LinearArgument right) {
        return model.addGreaterThan(left, right);
    }

    /** Adds {@code expr != value}. */
    public Constraint addDifferent(LinearArgument expr, long value) {
        return model.addDifferent(expr, value);
    }

    /** Adds {@code left != right}. */
    public Constraint addDifferent(LinearArgument left, LinearArgument right) {
        return model.addDifferent(left, right);
    }

    // Integer constraints

    /** Adds {@code AllDifferent(expressions)}. */
    public Constraint addAllDifferent(LinearArgument[] expressions) {
        return model.addAllDifferent(expressions);
    }

    /** Adds {@code AllDifferent(expressions)}. */
    public Constraint addAllDifferent(Iterable<? extends LinearArgument> expressions) {
        return model.addAllDifferent(expressions);
    }

    // Variable creation methods (delegated without registration)

    /** Creates an integer variable with given domain. */
    public IntVar newIntVarFromDomain(Domain domain, String name) {
        return model.newIntVarFromDomain(domain, name);
    }

    /** Creates a constant variable. */
    public IntVar newConstant(long value) {
        return model.newConstant(value);
    }

    /** Returns the true literal. */
    public Literal trueLiteral() {
        return model.trueLiteral();
    }

    /** Returns the false literal. */
    public Literal falseLiteral() {
        return model.falseLiteral();
    }

    // Utility methods

    /** Returns some statistics on model as a string. */
    public String modelStats() {
        return model.modelStats();
    }

    /** Returns a non empty string explaining the issue if the model is invalid. */
    public String validate() {
        return model.validate();
    }

    /** Write the model as a protocol buffer to 'file'. */
    public Boolean exportToFile(String file) {
        return model.exportToFile(file);
    }

    /** Returns the model builder. */
    public CpModelProto.Builder getBuilder() {
        return model.getBuilder();
    }

    /** Returns the model. */
    public CpModelProto model() {
        return model.model();
    }

    /** Returns a clone of the model. */
    public CpModel getClone() {
        return model.getClone();
    }

    /** Rebuilds a Boolean variable from an index. */
    public BoolVar getBoolVarFromProtoIndex(int index) {
        return model.getBoolVarFromProtoIndex(index);
    }

    /** Rebuilds an integer variable from an index. */
    public IntVar getIntVarFromProtoIndex(int index) {
        return model.getIntVarFromProtoIndex(index);
    }

    // Objective methods

    /** Adds a minimization objective of a linear expression. */
    public void minimize(LinearArgument expr) {
        model.minimize(expr);
    }

    /** Adds a minimization objective of a linear expression. */
    public void minimize(DoubleLinearExpr expr) {
        model.minimize(expr);
    }

    /** Adds a maximization objective of a linear expression. */
    public void maximize(LinearArgument expr) {
        model.maximize(expr);
    }

    /** Adds a maximization objective of a linear expression. */
    public void maximize(DoubleLinearExpr expr) {
        model.maximize(expr);
    }

    /** Clears the objective. */
    public void clearObjective() {
        model.clearObjective();
    }

    /** Checks if the model contains an objective. */
    public boolean hasObjective() {
        return model.hasObjective();
    }

    // Standard Object methods

    @Override
    public String toString() {
        return model.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return model.equals(obj);
    }

    @Override
    public int hashCode() {
        return model.hashCode();
    }
}
