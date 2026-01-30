package com.jmix.executor.impl.algmodel;

import com.jmix.executor.model.AlgLoaderException;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpModelProto;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    static {
        Loader.loadNativeLibraries();
    }
    /**
     * 底层的CpModel实例
     */
    private CpModel cpModel;

    /**
     * 其他变量映射表
     */
    private Map<String, OtherVar> otherVarMap = new HashMap<>();

    /**
     * 其他变量索引计数器
     */
    private int otherVarIndex = 0;

    /**
     * 是否启用松弛变量调试模式
     */
    private boolean isAttachRelax = false;

    /**
     * 松弛变量映射表，键为规则名称，值为松弛变量
     */
    private Map<String, RelaxVar> relaxationVarMap = new HashMap<>();

    /**
     * 冲突松弛变量映射
     */
    private Map<String, RelaxVar> confictedRelaxVarMap = new HashMap<>();

    /**
     * 当前正在使用的松弛变量
     */
    private BoolVar currentRelaxVar = null;

    /**
     * 当前松弛变量的名称
     */
    private String currentRelaxVarName = "";

    /**
     * 变量映射表
     */
    private Map<String, IntVar> variables = new HashMap<>();

    /**
     * 变量日志列表
     */
    private List<String> variableLogs = new ArrayList<>();

    /**
     * 约束列表
     */
    private List<AlgCPConstraint> constraints = new ArrayList<>();

    /**
     * 目标表达式
     */
    private AlgCPLinearExpr objectExpr;

    /**
     * 创建跟踪的线性表达式
     *
     * @param name 表达式名称
     * @return AlgCPLinearExpr 实例
     */
    public AlgCPLinearExpr newLinearExpr(String name) {
        return new AlgCPLinearExpr(name);
    }

    /**
     * 创建跟踪的线性表达式
     *
     * @param name 表达式名称
     * @return AlgCPLinearExpr 实例
     */
    public PartAlgCPLinearExpr newPartLinearExpr(String name) {
        return new PartAlgCPLinearExpr(name);
    }

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
        this.variables = new HashMap<>();
        this.variableLogs = new ArrayList<>();
        this.constraints = new ArrayList<>();
    }

    private void setCurrentRelaxVarName(String ruleCode, int weight) {
        if (!isAttachRelax) {
            log.info("relax:{} relax is not attached,setCurrentRelaxVarName", ruleCode);
            return;
        }
        log.info("relax:{} relax is attached,setCurrentRelaxVarName", ruleCode);
        this.currentRelaxVarName = "relax_" + ruleCode;
        this.currentRelaxVar = newBoolVar(this.currentRelaxVarName);

        int tempWeight = weight;
        if (confictedRelaxVarMap.containsKey(this.currentRelaxVarName)) {
            tempWeight = tempWeight + RelaxVar.WEIGHT_ADDER;
        }

        RelaxVar relaxVar = new RelaxVar(this.currentRelaxVarName, ruleCode, this.currentRelaxVar, tempWeight);
        if (relaxationVarMap.containsKey(this.currentRelaxVarName)) {
            throw new AlgLoaderException("Relaxation variable already exists: " + ruleCode);
        }
        relaxationVarMap.put(this.currentRelaxVarName, relaxVar);
    }

    /**
     * 为系统规则设置松弛变量
     * 
     * @param ruleCode 规则代码
     */
    public void setRelax4SysRule(String ruleCode) {
        setCurrentRelaxVarName(ruleCode, RelaxVar.WEIGHT_SMALL);
    }

    /**
     * 为自定义规则设置松弛变量
     * 
     * @param ruleCode 规则代码
     */
    public void setRelax4CustomRule(String ruleCode) {
        setCurrentRelaxVarName(ruleCode, RelaxVar.WEIGHT_MEDIUM);
    }

    /**
     * 获取松弛变量映射
     * 
     * @return 松弛变量映射
     */
    public Map<String, RelaxVar> getRelaxVarMap() {
        return relaxationVarMap;
    }

    /**
     * 设置冲突松弛变量
     * 
     * @param confictedRelaxs 冲突松弛变量列表
     */
    public void setConfictedRelaxVars(List<RelaxVar> confictedRelaxs) {
        confictedRelaxVarMap.clear();
        for (RelaxVar relaxVar : confictedRelaxs) {
            confictedRelaxVarMap.put(relaxVar.getName(), relaxVar);
        }
    }

    /**
     * 设置是否附加松弛变量
     * 
     * @param isAttachRelax 是否附加松弛变量
     */
    public void setIsAttachRelax(boolean isAttachRelax) {
        this.isAttachRelax = isAttachRelax;
    }

    /**
     * 获取是否附加松弛变量
     * 
     * @return 是否附加松弛变量
     */
    public boolean isIsAttachRelax() {
        return isAttachRelax;
    }

    /**
     * 附加松弛变量到约束
     *
     * @param ct      约束
     * @param funName 函数名称
     * @return 带松弛变量的约束包装
     */
    private AlgCPConstraint attachRelax(Constraint ct, String funName) {
        log.info("relax:{} -----{}", currentRelaxVarName, funName);
        AlgCPConstraint constraint = new AlgCPConstraint(ct, this.currentRelaxVar, this.currentRelaxVarName);
        constraint.setName(funName);
        // 注意：约束添加到列表的操作现在在 createAndTrackConstraint 方法中进行
        return constraint;
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
        trackVariable(tv, left, right, name);
        log.info("Variable created: {} in [{}, {}]", name, left, right);
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
        trackVariable(tv, value, value, name);
        log.info("Variable created: {} = {}", name, value);
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
        trackVariable(tv, values[0], values[values.length - 1], name);
        log.info("Variable created: {} in domain {}", name, java.util.Arrays.toString(values));
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
        trackVariable(tv, intervals[0][0], intervals[intervals.length - 1][1], name);
        log.info("Variable created: {} in intervals {}", name, java.util.Arrays.deepToString(intervals));
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
        trackVariable(tv, Long.MIN_VALUE, Long.MAX_VALUE, name);
        log.info("Variable created: {} in full domain", name);
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
        registerVariablesBool(tv, name);
        trackVariable(tv, 0, 1, name);
        log.info("BoolVar created: {} in {0, 1}", name);
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
            log.error("Variable already exists: {}", name);
            throw new AlgLoaderException("Variable already exists: " + name);
        }

        // name不是以ParaVar.VAR_PATTERN_PREFIX 且
        // PartVar.VAR_PATTERN_PREFIX开头，则添加到otherVarMap
        if (!name.startsWith(ParaVar.VAR_PATTERN_PREFIX) && !name.startsWith(PartVar.VAR_PATTERN_PREFIX)) {
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
    private void registerVariablesBool(final BoolVar tv, final String name) {
        registerVariables(tv, name);
    }

    /**
     * 跟踪变量创建信息
     *
     * @param var  变量对象
     * @param lb   下界
     * @param ub   上界
     * @param name 变量名称
     */
    private void trackVariable(IntVar var, long lb, long ub, String name) {
        variables.put(name, var);
        variableLogs.add(String.format("%s in [%d, %d]", name, lb, ub));
    }

    /**
     * 创建并记录约束的私有辅助方法（LinearArgument约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackConstraint(Constraint constraint, LinearArgument leftExpr, long rightValue,
            String constraintName, String operator) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        algCt.setLeft(toNameString(leftExpr));
        algCt.setOperator(operator);
        algCt.setRight(String.valueOf(rightValue));
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 创建并记录约束的私有辅助方法（AlgCPLinearExpr约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightValue     右侧值
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackConstraint(Constraint constraint, AlgCPLinearExpr leftExpr, long rightValue,
            String constraintName, String operator) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        algCt.setLeft(leftExpr.toString());
        algCt.setLeftName(leftExpr.getName());
        algCt.setOperator(operator);
        algCt.setRight(String.valueOf(rightValue));
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 创建并记录约束的私有辅助方法（两个LinearArgument约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightExpr      右侧表达式
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackConstraint(Constraint constraint, LinearArgument leftExpr,
            LinearArgument rightExpr,
            String constraintName, String operator) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        algCt.setLeft(toNameString(leftExpr));
        algCt.setOperator(operator);
        algCt.setRight(toNameString(rightExpr));
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 创建并记录约束的私有辅助方法（LinearArgument和AlgCPLinearExpr约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightExpr      右侧表达式
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackConstraint(Constraint constraint, LinearArgument leftExpr,
            AlgCPLinearExpr rightExpr,
            String constraintName, String operator) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        algCt.setLeft(toNameString(leftExpr));
        algCt.setOperator(operator);
        algCt.setRight(rightExpr.toString());
        algCt.setRightName(rightExpr.getName());
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 创建并记录约束的私有辅助方法（AlgCPLinearExpr和LinearArgument约束）
     *
     * @param constraint     原始约束对象
     * @param leftExpr       左侧表达式
     * @param rightExpr      右侧表达式
     * @param constraintName 约束名称
     * @param operator       操作符
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackConstraint(Constraint constraint, AlgCPLinearExpr leftExpr,
            LinearArgument rightExpr,
            String constraintName, String operator) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        algCt.setLeft(leftExpr.toString());
        algCt.setLeftName(leftExpr.getName());
        algCt.setOperator(operator);
        algCt.setRight(toNameString(rightExpr));
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 创建并记录布尔约束的辅助方法
     *
     * @param constraint     原始约束对象
     * @param constraintName 约束名称
     * @return 包装的约束对象
     */
    private AlgCPConstraint createAndTrackBoolConstraint(Constraint constraint, String constraintName) {
        AlgCPConstraint algCt = attachRelax(constraint, constraintName);
        constraints.add(algCt);
        return algCt;
    }

    /**
     * 将LinearArgument转换为名称字符串
     *
     * @param expr LinearArgument表达式
     * @return 名称字符串
     */
    public static String toNameString(LinearArgument expr) {
        if (expr instanceof IntVar) {
            return ((IntVar) expr).getName();
        } else if (expr instanceof BoolVar) {
            return ((BoolVar) expr).getName();
        } else {
            throw new IllegalArgumentException("Unsupported linear argument type: " + expr.getClass().getName());
        }
    }

    // 以下方法直接委托给底层的CpModel，保持接口兼容性
    // Boolean Constraints

    /**
     * 添加布尔或约束，至少有一个字面量为真
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolOr(final Literal[] literals) {
        Constraint ct = cpModel.addBoolOr(literals);
        return createAndTrackBoolConstraint(ct, "addBoolOr");
    }

    /**
     * 添加布尔或约束，至少有一个字面量为真
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolOr(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addBoolOr(literals);
        return createAndTrackBoolConstraint(ct, "addBoolOr");
    }

    /**
     * 添加至少一个约束，等价于addBoolOr
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addAtLeastOne(final Literal[] literals) {
        Constraint ct = cpModel.addAtLeastOne(literals);
        return createAndTrackBoolConstraint(ct, "addAtLeastOne");
    }

    /**
     * 添加至少一个约束，等价于addBoolOr
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addAtLeastOne(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addAtLeastOne(literals);
        return createAndTrackBoolConstraint(ct, "addAtLeastOne");
    }

    /**
     * 添加最多一个约束，最多有一个字面量为真
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addAtMostOne(final Literal[] literals) {
        Constraint ct = cpModel.addAtMostOne(literals);
        return createAndTrackBoolConstraint(ct, "addAtMostOne");
    }

    /**
     * 添加最多一个约束，最多有一个字面量为真
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addAtMostOne(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addAtMostOne(literals);
        return createAndTrackBoolConstraint(ct, "addAtMostOne");
    }

    /**
     * 添加恰好一个约束，恰好有一个字面量为真
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addExactlyOne(final Literal[] literals) {
        Constraint ct = cpModel.addExactlyOne(literals);
        return createAndTrackBoolConstraint(ct, "addExactlyOne");
    }

    /**
     * 添加恰好一个约束，恰好有一个字面量为真
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addExactlyOne(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addExactlyOne(literals);
        return createAndTrackBoolConstraint(ct, "addExactlyOne");
    }

    /**
     * 添加布尔与约束，所有字面量都为真
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolAnd(final Literal[] literals) {
        Constraint ct = cpModel.addBoolAnd(literals);
        return createAndTrackBoolConstraint(ct, "addBoolAnd");
    }

    /**
     * 添加布尔与约束，所有字面量都为真
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolAnd(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addBoolAnd(literals);
        return createAndTrackBoolConstraint(ct, "addBoolAnd");
    }

    /**
     * 添加布尔异或约束，奇数个字面量为真
     *
     * @param literals 字面量数组
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolXor(final Literal[] literals) {
        Constraint ct = cpModel.addBoolXor(literals);
        return createAndTrackBoolConstraint(ct, "addBoolXor");
    }

    /**
     * 添加布尔异或约束，奇数个字面量为真
     *
     * @param literals 字面量集合
     * @return 添加的约束
     */
    public AlgCPConstraint addBoolXor(final Iterable<Literal> literals) {
        Constraint ct = cpModel.addBoolXor(literals);
        return createAndTrackBoolConstraint(ct, "addBoolXor");
    }

    /**
     * 添加蕴含约束，a蕴含b
     *
     * @param a 前提字面量
     * @param b 结论字面量
     * @return 添加的约束
     */
    public AlgCPConstraint addImplication(final Literal a, final Literal b) {
        Constraint ct = cpModel.addImplication(a, b);
        return createAndTrackBoolConstraint(ct, "addImplication");
    }

    /**
     * 添加等式约束，表达式等于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addEquality(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addEquality(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addEquality", "==");
    }

    /**
     * 添加等式约束，两个表达式相等
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addEquality(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addEquality(left, right);
        return createAndTrackConstraint(ct, left, right, "addEquality", "==");
    }

    /**
     * 添加小于等于约束，表达式小于等于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addLessOrEqual(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addLessOrEqual(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addLessOrEqual", "<=");
    }

    /**
     * 添加小于等于约束，左侧表达式小于等于右侧表达式
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addLessOrEqual(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addLessOrEqual(left, right);
        return createAndTrackConstraint(ct, left, right, "addLessOrEqual", "<=");
    }

    /**
     * 添加小于约束，表达式小于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addLessThan(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addLessThan(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addLessThan", "<");
    }

    /**
     * 添加小于约束，左侧表达式小于右侧表达式
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addLessThan(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addLessThan(left, right);
        return createAndTrackConstraint(ct, left, right, "addLessThan", "<");
    }

    /**
     * 添加大于等于约束，表达式大于等于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterOrEqual(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addGreaterOrEqual(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addGreaterOrEqual", ">=");
    }

    /**
     * 添加大于等于约束，左侧表达式大于等于右侧表达式
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterOrEqual(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addGreaterOrEqual(left, right);
        return createAndTrackConstraint(ct, left, right, "addGreaterOrEqual", ">=");
    }

    /**
     * 添加大于约束，表达式大于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterThan(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addGreaterThan(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addGreaterThan", ">");
    }

    /**
     * 添加大于约束，左侧表达式大于右侧表达式
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterThan(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addGreaterThan(left, right);
        return createAndTrackConstraint(ct, left, right, "addGreaterThan", ">");
    }

    /**
     * 添加不等于约束，表达式不等于指定值
     *
     * @param expr  线性表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addDifferent(final LinearArgument expr, final long value) {
        Constraint ct = cpModel.addDifferent(expr, value);
        return createAndTrackConstraint(ct, expr, value, "addDifferent", "!=");
    }

    /**
     * 添加不等于约束，两个表达式不相等
     *
     * @param left  左侧表达式
     * @param right 右侧表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addDifferent(final LinearArgument left, final LinearArgument right) {
        Constraint ct = cpModel.addDifferent(left, right);
        return createAndTrackConstraint(ct, left, right, "addDifferent", "!=");
    }

    // Integer constraints

    /**
     * 添加全不同约束，所有表达式值都不相同
     *
     * @param expressions 表达式数组
     * @return 添加的约束
     */
    public AlgCPConstraint addAllDifferent(final LinearArgument[] expressions) {
        Constraint ct = cpModel.addAllDifferent(expressions);
        return createAndTrackBoolConstraint(ct, "addAllDifferent");
    }

    /**
     * 添加全不同约束，所有表达式值都不相同
     *
     * @param expressions 表达式集合
     * @return 添加的约束
     */
    public AlgCPConstraint addAllDifferent(final Iterable<? extends LinearArgument> expressions) {
        Constraint ct = cpModel.addAllDifferent(expressions);
        return createAndTrackBoolConstraint(ct, "addAllDifferent");
    }

    /**
     * 添加等式约束，AlgCPLinearExpr等于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addEquality(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addEquality(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addEquality", "==");
    }

    /**
     * 添加小于等于约束，AlgCPLinearExpr小于等于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addLessOrEqual(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addLessOrEqual(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addLessOrEqual", "<=");
    }

    /**
     * 添加小于约束，AlgCPLinearExpr小于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addLessThan(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addLessThan(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addLessThan", "<");
    }

    /**
     * 添加大于等于约束，AlgCPLinearExpr大于等于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterOrEqual(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addGreaterOrEqual(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addGreaterOrEqual", ">=");
    }

    /**
     * 添加大于约束，AlgCPLinearExpr大于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addGreaterThan(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addGreaterThan(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addGreaterThan", ">");
    }

    /**
     * 添加不等于约束，AlgCPLinearExpr不等于指定值
     *
     * @param expr  AlgCPLinearExpr表达式
     * @param value 目标值
     * @return 添加的约束
     */
    public AlgCPConstraint addDifferent(final AlgCPLinearExpr expr, final long value) {
        Constraint ct = cpModel.addDifferent(expr.build(), value);
        return createAndTrackConstraint(ct, expr, value, "addDifferent", "!=");
    }

    /**
     * 添加等式约束，LinearArgument等于AlgCPLinearExpr
     *
     * @param left  左侧LinearArgument表达式
     * @param right 右侧AlgCPLinearExpr表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addEquality(final LinearArgument left, final AlgCPLinearExpr right) {
        Constraint ct = cpModel.addEquality(left, right.build());
        return createAndTrackConstraint(ct, left, right, "addEquality", "==");
    }

    /**
     * 添加等式约束，AlgCPLinearExpr等于LinearArgument
     *
     * @param left  左侧AlgCPLinearExpr表达式
     * @param right 右侧LinearArgument表达式
     * @return 添加的约束
     */
    public AlgCPConstraint addEquality(final AlgCPLinearExpr left, final LinearArgument right) {
        Constraint ct = cpModel.addEquality(left.build(), right);
        return createAndTrackConstraint(ct, left, right, "addEquality", "==");
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
     * 添加双精度线性表达式的最小化目标
     *
     * @param expr 双精度线性表达式
     */
    public void minimize(AlgCPLinearExpr expr) {
        cpModel.minimize(expr.build());
        addVirutalAlgCPConstraint("minimize", expr);
    }

    /**
     * 添加双精度线性表达式的最大化目标
     *
     * @param expr 双精度线性表达式
     */
    public void maximize(AlgCPLinearExpr expr) {
        cpModel.minimize(expr.build());
        addVirutalAlgCPConstraint("maximize", expr);
    }

    private AlgCPConstraint addVirutalAlgCPConstraint(String name, AlgCPLinearExpr expr) {
        AlgCPConstraint ct = new AlgCPConstraint(null, this.currentRelaxVar, this.currentRelaxVarName);
        ct.setName(name);
        ct.setLeft(expr.toString());
        ct.setLeftName(expr.getName());
        this.objectExpr = expr;
        constraints.add(ct);
        return ct;
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

    /**
     * 打印模型摘要
     */
    public void printModelSummary() {
        log.info("\n" + "=".repeat(80));
        log.info("CP-SAT MODEL SUMMARY");
        log.info("=".repeat(80));

        log.info("\nVARIABLES ({}):", variables.size());
        log.info("-".repeat(80));
        for (int i = 0; i < variableLogs.size(); i++) {
            log.info(String.format("%3d. %s", i + 1, variableLogs.get(i)));
        }

        log.info("\nCONSTRAINTS ({}):", constraints.size());
        log.info("-".repeat(80));
        for (int i = 0; i < constraints.size(); i++) {
            log.info(String.format("%3d. %s", i + 1, constraints.get(i).toString()));
        }
        log.info("\n" + "=".repeat(80));

        log.info("\nObjectFuntionDetail: ");
        log.info(objectExpr.toDetailString());
        log.info("\n" + "=".repeat(80));

    }

    public void printRunValue(CpSolver cpSolver, CpSolverStatus status) {
        log.info("\nCpSolverStatus:{} ", status);
        if (status == CpSolverStatus.INFEASIBLE) {
            return;
        }
        log.info("\nVARIABLE VALUES ({}):", variables.size());
        log.info("-".repeat(80));
        for (IntVar var : variables.values()) {
            long value = cpSolver.value(var);
            log.info(String.format("  %-20s = %d", var.getName(), value));
        }
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            log.info("\nObject Value ={}", cpSolver.objectiveValue());
        } else {
            log.info("\nObject Value ={}", "not");
        }

        log.info("-".repeat(80));
    }
}
