package com.jmix.executor.impl.algmodel;

import com.jmix.executor.omodel.AlgLoaderException;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpModelProto;
import com.google.ortools.sat.DoubleLinearExpr;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 对CpModel接口的封装，实现变量注册等封装
 * 实现对CpModel接口封装，类似测试代码调用model的代码都不需要修改（如：model.addBoolAnd)
 * 
 * @since 2025-09-22
 */
@Slf4j
@Data
public class AlgCPModel {
    // 底层的CpModel实例
    private CpModel cpModel;

    // other variables map
    private Map<String, OtherVar> otherVarMap = new HashMap<>();

    // other variable index counter
    private int otherVarIndex = 0;

    /**
     * 默认构造函数，创建新的CpModel实例
     */
    public AlgCPModel() {
        this(new CpModel());
    }

    /**
     * 构造函数，使用指定的CpModel实例
     * 
     * @param cpModel 要封装的CpModel实例
     */
    public AlgCPModel(final CpModel cpModel) {
        this.cpModel = cpModel;
    }

    /**
     * 获取底层的CpModel实例
     * 
     * @return 封装的CpModel实例
     */
    public CpModel getCpModel() {
        return cpModel;
    }

    /**
     * 创建整数变量，封装CpModel的newIntVar方法
     * 
     * @param left  变量的最小值
     * @param right 变量的最大值
     * @param name  变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVar(final long left, final long right, final String name) {
        IntVar tv = this.cpModel.newIntVar(left, right, name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 从单个值创建整数变量，封装CpModel的newIntVarFromDomain方法
     * 
     * @param value 变量的固定值
     * @param name  变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVarFromDomain(final long value, final String name) {
        IntVar tv = this.cpModel.newIntVarFromDomain(Domain.fromValues(new long[] { value }), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 从多个值创建整数变量，封装CpModel的newIntVarFromDomain方法
     * 
     * @param values 变量的可能值数组
     * @param name   变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVarFromDomain(final long[] values, final String name) {
        IntVar tv = this.cpModel.newIntVarFromDomain(Domain.fromValues(values), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 从区间创建整数变量，封装CpModel的newIntVarFromDomain方法
     * 
     * @param intervals 变量的区间范围数组
     * @param name      变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVarFromDomain(final long[][] intervals, final String name) {
        IntVar tv = this.cpModel.newIntVarFromDomain(Domain.fromIntervals(intervals), name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 创建具有完整域的整数变量，封装CpModel的newIntVarFromDomain方法
     * 
     * @param name 变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVarFromDomain(final String name) {
        IntVar tv = this.cpModel.newIntVarFromDomain(Domain.fromValues(new long[] { Long.MIN_VALUE, Long.MAX_VALUE }),
                name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 创建布尔变量，封装CpModel的newBoolVar方法
     * 
     * @param name 变量名称
     * @return 创建的布尔变量
     */
    public BoolVar newBoolVar(final String name) {
        BoolVar tv = this.cpModel.newBoolVar(name);
        registerVariables(tv, name);
        return tv;
    }

    /**
     * 注册其他变量
     * 
     * @param tv   变量对象
     * @param name 变量名称
     */
    private void registerVariables(final IntVar tv, final String name) {
        // 判断是否已经存在，如果已经存在，则抛异常
        if (otherVarMap.containsKey(name)) {
            throw new AlgLoaderException("Variable already exists: " + name);
        }

        // name不是以ParaVar.VAR_PATTEN_PREFIX 且
        // PartVar.VAR_PATTEN_PREFIX开头，则添加到otherVarMap
        if (!name.startsWith(ParaVar.VAR_PATTEN_PREFIX) && !name.startsWith(PartVar.VAR_PATTEN_PREFIX)) {
            OtherVar otherVar = new OtherVar();
            otherVar.setCode(name);
            otherVar.setVar(tv);
            otherVar.setShortCode("o" + (++otherVarIndex));
            otherVarMap.put(name, otherVar);
        }
    }

    /**
     * 注册其他变量 - BoolVar版本
     * 
     * @param tv   变量对象
     * @param name 变量名称
     */
    private void registerVariables(final BoolVar tv, final String name) {
        registerVariables((IntVar) tv, name);
    }

    // 以下方法直接委托给底层的CpModel，保持接口兼容性
    // Boolean Constraints

    /**
     * 添加布尔或约束，至少有一个字面量为真
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addBoolOr(final Literal[] literals) {
        return cpModel.addBoolOr(literals);
    }

    /**
     * 添加布尔或约束，至少有一个字面量为真
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addBoolOr(final Iterable<Literal> literals) {
        return cpModel.addBoolOr(literals);
    }

    /**
     * 添加至少一个约束，等价于addBoolOr
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addAtLeastOne(final Literal[] literals) {
        return cpModel.addAtLeastOne(literals);
    }

    /**
     * 添加至少一个约束，等价于addBoolOr
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addAtLeastOne(final Iterable<Literal> literals) {
        return cpModel.addAtLeastOne(literals);
    }

    /**
     * 添加最多一个约束，最多有一个字面量为真
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addAtMostOne(final Literal[] literals) {
        return cpModel.addAtMostOne(literals);
    }

    /**
     * 添加最多一个约束，最多有一个字面量为真
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addAtMostOne(final Iterable<Literal> literals) {
        return cpModel.addAtMostOne(literals);
    }

    /**
     * 添加恰好一个约束，恰好有一个字面量为真
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addExactlyOne(final Literal[] literals) {
        return cpModel.addExactlyOne(literals);
    }

    /**
     * 添加恰好一个约束，恰好有一个字面量为真
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addExactlyOne(final Iterable<Literal> literals) {
        return cpModel.addExactlyOne(literals);
    }

    /**
     * 添加布尔与约束，所有字面量都为真
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addBoolAnd(final Literal[] literals) {
        return cpModel.addBoolAnd(literals);
    }

    /**
     * 添加布尔与约束，所有字面量都为真
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addBoolAnd(final Iterable<Literal> literals) {
        return cpModel.addBoolAnd(literals);
    }

    /**
     * 添加布尔异或约束，奇数个字面量为真
     * 
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public Constraint addBoolXor(final Literal[] literals) {
        return cpModel.addBoolXor(literals);
    }

    /**
     * 添加布尔异或约束，奇数个字面量为真
     * 
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public Constraint addBoolXor(final Iterable<Literal> literals) {
        return cpModel.addBoolXor(literals);
    }

    /**
     * 添加蕴含约束，a蕴含b
     * 
     * @param a 前提字面量
     * @param b 结论字面量
     * @return 添加的约束
     */
    public Constraint addImplication(final Literal a, final Literal b) {
        return cpModel.addImplication(a, b);
    }

    // Linear constraints

    /**
     * 添加等式约束，表达式等于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addEquality(final LinearArgument expr, final long value) {
        return cpModel.addEquality(expr, value);
    }

    /**
     * 添加等式约束，两个表达式相等
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addEquality(final LinearArgument left, final LinearArgument right) {
        return cpModel.addEquality(left, right);
    }

    /**
     * 添加小于等于约束，表达式小于等于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addLessOrEqual(final LinearArgument expr, final long value) {
        return cpModel.addLessOrEqual(expr, value);
    }

    /**
     * 添加小于等于约束，左侧表达式小于等于右侧表达式
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addLessOrEqual(final LinearArgument left, final LinearArgument right) {
        return cpModel.addLessOrEqual(left, right);
    }

    /**
     * 添加小于约束，表达式小于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addLessThan(final LinearArgument expr, final long value) {
        return cpModel.addLessThan(expr, value);
    }

    /**
     * 添加小于约束，左侧表达式小于右侧表达式
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addLessThan(final LinearArgument left, final LinearArgument right) {
        return cpModel.addLessThan(left, right);
    }

    /**
     * 添加大于等于约束，表达式大于等于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addGreaterOrEqual(final LinearArgument expr, final long value) {
        return cpModel.addGreaterOrEqual(expr, value);
    }

    /**
     * 添加大于等于约束，左侧表达式大于等于右侧表达式
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addGreaterOrEqual(final LinearArgument left, final LinearArgument right) {
        return cpModel.addGreaterOrEqual(left, right);
    }

    /**
     * 添加大于约束，表达式大于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addGreaterThan(final LinearArgument expr, final long value) {
        return cpModel.addGreaterThan(expr, value);
    }

    /**
     * 添加大于约束，左侧表达式大于右侧表达式
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addGreaterThan(final LinearArgument left, final LinearArgument right) {
        return cpModel.addGreaterThan(left, right);
    }

    /**
     * 添加不等于约束，表达式不等于指定值
     * 
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public Constraint addDifferent(final LinearArgument expr, final long value) {
        return cpModel.addDifferent(expr, value);
    }

    /**
     * 添加不等于约束，两个表达式不相等
     * 
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public Constraint addDifferent(final LinearArgument left, final LinearArgument right) {
        return cpModel.addDifferent(left, right);
    }

    // Integer constraints

    /**
     * 添加全不同约束，所有表达式值都不相同
     * 
     * @param expressions 表达式数组
     * @return 添加的约束
     */
    public Constraint addAllDifferent(final LinearArgument[] expressions) {
        return cpModel.addAllDifferent(expressions);
    }

    /**
     * 添加全不同约束，所有表达式值都不相同
     * 
     * @param expressions 表达式集合
     * @return 添加的约束
     */
    public Constraint addAllDifferent(final Iterable<? extends LinearArgument> expressions) {
        return cpModel.addAllDifferent(expressions);
    }

    // Variable creation methods (delegated without registration)

    /**
     * 从指定域创建整数变量（不进行变量注册）
     * 
     * @param domain 变量的域
     * @param name   变量名称
     * @return 创建的整数变量
     */
    public IntVar newIntVarFromDomain(final Domain domain, final String name) {
        return cpModel.newIntVarFromDomain(domain, name);
    }

    /**
     * 创建常量变量
     * 
     * @param value 常量值
     * @return 创建的常量变量
     */
    public IntVar newConstant(final long value) {
        return cpModel.newConstant(value);
    }

    /**
     * 返回真字面量
     * 
     * @return 真字面量
     */
    public Literal trueLiteral() {
        return cpModel.trueLiteral();
    }

    /**
     * 返回假字面量
     * 
     * @return 假字面量
     */
    public Literal falseLiteral() {
        return cpModel.falseLiteral();
    }

    // Utility methods

    /**
     * 返回模型的统计信息字符串
     * 
     * @return 模型统计信息
     */
    public String modelStats() {
        return cpModel.modelStats();
    }

    /**
     * 验证模型，如果模型无效则返回错误信息
     * 
     * @return 如果模型有效返回空字符串，否则返回错误信息
     */
    public String validate() {
        return cpModel.validate();
    }

    /**
     * 将模型导出为协议缓冲区文件
     * 
     * @param file 文件路径
     * @return 导出是否成功
     */
    public Boolean exportToFile(final String file) {
        return cpModel.exportToFile(file);
    }

    /**
     * 返回模型构建器
     * 
     * @return CpModelProto构建器
     */
    public CpModelProto.Builder getBuilder() {
        return cpModel.getBuilder();
    }

    /**
     * 返回模型
     * 
     * @return CpModelProto模型
     */
    public CpModelProto model() {
        return cpModel.model();
    }

    /**
     * 返回模型的克隆
     * 
     * @return 模型的克隆
     */
    public CpModel getClone() {
        return cpModel.getClone();
    }

    /**
     * 从协议索引重建布尔变量
     * 
     * @param index 协议索引
     * @return 重建的布尔变量
     */
    public BoolVar getBoolVarFromProtoIndex(final int index) {
        return cpModel.getBoolVarFromProtoIndex(index);
    }

    /**
     * 从协议索引重建整数变量
     * 
     * @param index 协议索引
     * @return 重建的整数变量
     */
    public IntVar getIntVarFromProtoIndex(final int index) {
        return cpModel.getIntVarFromProtoIndex(index);
    }

    // Objective methods

    /**
     * 添加线性表达式的最小化目标
     * 
     * @param expr 线性表达式
     */
    public void minimize(final LinearArgument expr) {
        cpModel.minimize(expr);
    }

    /**
     * 添加双精度线性表达式的最小化目标
     * 
     * @param expr 双精度线性表达式
     */
    public void minimize(final DoubleLinearExpr expr) {
        cpModel.minimize(expr);
    }

    /**
     * 添加线性表达式的最大化目标
     * 
     * @param expr 线性表达式
     */
    public void maximize(final LinearArgument expr) {
        cpModel.maximize(expr);
    }

    /**
     * 添加双精度线性表达式的最大化目标
     * 
     * @param expr 双精度线性表达式
     */
    public void maximize(final DoubleLinearExpr expr) {
        cpModel.maximize(expr);
    }

    /**
     * 清除目标函数
     */
    public void clearObjective() {
        cpModel.clearObjective();
    }

    /**
     * 检查模型是否包含目标函数
     * 
     * @return 如果包含目标函数返回true，否则返回false
     */
    public boolean hasObjective() {
        return cpModel.hasObjective();
    }

    // Standard Object methods

    @Override
    public final String toString() {
        return cpModel.toString();
    }

    @Override
    public final boolean equals(final Object obj) {
        return cpModel.equals(obj);
    }

    @Override
    public final int hashCode() {
        return cpModel.hashCode();
    }
}
