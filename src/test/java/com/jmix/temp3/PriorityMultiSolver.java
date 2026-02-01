package com.jmix.temp3;

import com.jmix.temp3.basic.PriorityBasicSolver;
import com.jmix.temp3.core.CpModelTracker;
import com.jmix.temp3.core.ExpressionCalculator;
import com.jmix.temp3.core.FilterExpressionExecutor;
import com.jmix.temp3.core.Part;
import com.jmix.temp3.core.PartConstraintReq;
import com.jmix.temp3.core.PartResult;
import com.jmix.temp3.core.PartVar;
import com.jmix.temp3.core.ProductResult;
import com.jmix.temp3.core.Solution;
import com.jmix.temp3.core.TrackedLinearExpr;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 主求解器类
 */
@Slf4j
public class PriorityMultiSolver {

    private List<Part> allParts;
    private NormalizationProcessor normalizationProcessor;
    private ObjectiveExpressionBuilder.ObjectiveConfig[] commonObjectives;

    public PriorityMultiSolver() {
        initParts();
        initNormalization();
        initObjectives();
    }

    // 初始化零件数据
    private void initParts() {
        allParts = new ArrayList<>();
        // 固态硬盘
        allParts.add(createPart("sd1", true, 5400, 3, 100.0, 2.0, 30.0));
        allParts.add(createPart("sd2", true, 7200, 6, 200.0, 3.0, 60.0));
        allParts.add(createPart("sd3", true, 9000, 9, 300.0, 4.0, 90.0));

        // 机械硬盘
        allParts.add(createPart("md1", false, 5400, 1, 50.0, 1.0, 10.0));
        allParts.add(createPart("md2", false, 7200, 2, 80.0, 1.5, 20.0));
        allParts.add(createPart("md3", false, 9000, 3, 120.0, 2.0, 30.0));
    }

    // 创建零件辅助方法
    private Part createPart(String code, boolean isSolidState, int speed, int capacity,
            double listPrice, double deliveryTime, double profit) {
        Part part = new Part(code, isSolidState, speed, capacity);
        part.setAttr(Part.ATTR_LISTPRICE, listPrice);
        part.setAttr(Part.ATTR_DELIVERYTIME, deliveryTime);
        part.setAttr(Part.ATTR_PROFIT, profit);
        return part;
    }

    // 初始化归一化处理器
    private void initNormalization() {
        normalizationProcessor = new NormalizationProcessor();

        // 创建归一化配置
        List<NormalizationProcessor.NormalizationConfig> configs = new ArrayList<>();

        // 配置各种属性的归一化方法
        NormalizationProcessor.NormalizationConfig capacityConfig = new NormalizationProcessor.NormalizationConfig(
                Part.ATTR_CAPACITY);
        // capacityConfig.setMethod(NormalizationProcessor.NormalizationMethod.LINEAR);
        capacityConfig.setMethod(NormalizationProcessor.NormalizationMethod.EQUAL);
        configs.add(capacityConfig);

        NormalizationProcessor.NormalizationConfig priceConfig = new NormalizationProcessor.NormalizationConfig(
                Part.ATTR_LISTPRICE);
        priceConfig.setMethod(NormalizationProcessor.NormalizationMethod.LINEAR);
        configs.add(priceConfig);

        NormalizationProcessor.NormalizationConfig deliveryConfig = new NormalizationProcessor.NormalizationConfig(
                Part.ATTR_DELIVERYTIME);
        deliveryConfig.setMethod(NormalizationProcessor.NormalizationMethod.LINEAR);
        configs.add(deliveryConfig);

        NormalizationProcessor.NormalizationConfig profitConfig = new NormalizationProcessor.NormalizationConfig(
                Part.ATTR_PROFIT);
        profitConfig.setMethod(NormalizationProcessor.NormalizationMethod.LINEAR);
        configs.add(profitConfig);

        NormalizationProcessor.NormalizationConfig weightConfig = new NormalizationProcessor.NormalizationConfig(
                Part.ATTR_WEIGHT);
        weightConfig.setMethod(NormalizationProcessor.NormalizationMethod.LINEAR);
        configs.add(weightConfig);

        // 执行批量归一化
        normalizationProcessor.normalizeBatch(allParts, configs);
    }

    // 初始化目标配置
    private void initObjectives() {
        commonObjectives = new ObjectiveExpressionBuilder.ObjectiveConfig[] {
                new ObjectiveExpressionBuilder.ObjectiveConfig("totalCost",
                        ObjectiveExpressionBuilder.ObjectiveType.MINIMIZE, 1.0, Part.ATTR_LISTPRICE),
                new ObjectiveExpressionBuilder.ObjectiveConfig("totalCapacity",
                        ObjectiveExpressionBuilder.ObjectiveType.MAXIMIZE, 1.5, Part.ATTR_CAPACITY),
                new ObjectiveExpressionBuilder.ObjectiveConfig("totalProfit",
                        ObjectiveExpressionBuilder.ObjectiveType.MAXIMIZE, 1.2, Part.ATTR_PROFIT),
                new ObjectiveExpressionBuilder.ObjectiveConfig("maxDelivery",
                        ObjectiveExpressionBuilder.ObjectiveType.MINIMIZE, 0.7, Part.ATTR_DELIVERYTIME)
        };

        // 设置聚合类型
        for (ObjectiveExpressionBuilder.ObjectiveConfig config : commonObjectives) {
            if ("maxDelivery".equals(config.getName())) {
                config.setQuantityRelated(false);
                config.setAggregateType(ObjectiveExpressionBuilder.AggregateType.MAX);
            }
        }
    }

    // 解析需求字符串
    private PartConstraintReq parseRequirement(String strReq) {
        PartConstraintReq req = new PartConstraintReq();

        try {
            // 查找 "where" 的位置（不区分大小写）
            int whereIndex = strReq.toLowerCase().indexOf("where");

            String constraint;
            String whereCondition = null;

            if (whereIndex >= 0) {
                // 分割为约束部分和 where 条件部分，保留原始格式
                constraint = strReq.substring(0, whereIndex).trim();
                whereCondition = strReq.substring(whereIndex + 5).trim(); // "where" 长度为 5
            } else {
                constraint = strReq;
            }

            if (whereCondition != null) {
                req.setAttrWhereCondition(whereCondition);
            }

            // 使用正则表达式匹配比较符，支持 >=, <=, >, <, =
            // 优先匹配长的比较符（>=, <=），然后再匹配单个字符（>, <, =）
            Pattern pattern = Pattern.compile("(.*?)(>=|<=|>|<|=)(.*)");
            Matcher matcher = pattern.matcher(constraint);

            if (matcher.matches()) {
                // attrCode: 去掉前面的空格，保留后面的空格
                String attrCode = matcher.group(1);
                // 使用正则表达式去掉前导空格，但保留尾部空格
                attrCode = attrCode.replaceFirst("^\\s+", "");
                req.setAttrCode(attrCode.trim());

                req.setAttrComparator(matcher.group(2).trim()); // 比较符

                // attrValue: 去掉前后空格
                req.setAttrValue(matcher.group(3).trim());
            }
        } catch (Exception e) {
            System.err.println("Failed to parse requirement string: " + strReq);
        }

        return req;
    }

    // 主求解方法
    public ProductResult solve(String strReq) {
        // 第一次求解
        ProductResult result = solveReq(strReq, null);
        if (result.getSolutions().size() <= 0) {
            return result;
        }

        final int maxExecTimes = 5; // 最大执行次数
        final int availableSolutionNum = 5;
        final int minSolutionThreshold = 3; // 最少解数量阈值
        int execTimes = 0;
        double baseAdjustCoeff = 0.5; // 基础调整系数
        double objectValue = result.getSolutions().get(0).getObjectValue();
        boolean needIncreased = true; // 是否需要增加objectvalue

        do {
            execTimes++;
            ProductResult lastResult = result;

            // 根据上一次求解结果动态调整策略
            if (execTimes > 1) {
                if (lastResult.isHasSearchMax()) {
                    // objectValue设置太大，最优解都没有处理，系数需要往下调
                    baseAdjustCoeff *= 0.7; // 降低调整幅度
                    needIncreased = false;
                    log.info("Iteration {}: objective value too high, reducing adjustment coefficient to {}",
                            execTimes, baseAdjustCoeff);
                } else if (lastResult.getSolutions().size() < minSolutionThreshold) {
                    // 解数量太少，objectValue设置太小，系数需要往上调
                    baseAdjustCoeff *= 1.5; // 增加调整幅度
                    needIncreased = true;
                    log.info("Iteration {}: too few solutions found, increasing adjustment coefficient to {}",
                            execTimes, baseAdjustCoeff);
                } else {
                    log.info("xxxx");
                    break;
                }
            }
            // 上次增加，还需要增加 ->增加
            // 上次增加，需要减少-> 需要减少

            // 上次减少，还需要增加 ->增加
            // 上次减少，需要减少-> 需要减少

            // 根据上一次调整方向决定本次调整
            if (!needIncreased) {
                objectValue = getAdjustObjectValue(objectValue, -baseAdjustCoeff);
            } else {
                objectValue = getAdjustObjectValue(objectValue, baseAdjustCoeff);
            }

            log.info("Iteration {}: adjusting objective value to {}", execTimes, objectValue);
            result = solveReq(strReq, objectValue);

        } while (execTimes < maxExecTimes
                && result.getSolutions().size() <= availableSolutionNum);
        result.setIterationTimes(execTimes);
        return result;
    }

    private double getAdjustObjectValue(double objectValue, double adjustCoeff) {
        int cResult = Double.compare(objectValue, 0.0);
        if (cResult < 0) {
            // 对于负值，根据调整系数的正负来决定调整方向
            if (adjustCoeff >= 0) {
                // 正系数：减少绝对值（更接近0）
                objectValue = objectValue * (1 - adjustCoeff);
            } else {
                // 负系数：增加绝对值（更远离0）
                objectValue = objectValue * (1 + Math.abs(adjustCoeff));
            }
        } else if (cResult == 0) {
            // 对于0值，根据调整系数设置起始值
            objectValue = adjustCoeff;
        } else {
            // 对于正值，根据调整系数的正负来决定调整方向
            if (adjustCoeff >= 0) {
                // 正系数：增加值
                objectValue = objectValue * (1 + adjustCoeff);
            } else {
                // 负系数：减少值
                objectValue = objectValue * (1 - Math.abs(adjustCoeff));
            }
        }
        return objectValue;
    }

    public ProductResult solveReq(String strReq, Double objectValue) {
        // 1. 解析需求
        PartConstraintReq req = parseRequirement(strReq);

        // 2. 筛选零件
        List<Part> filteredParts = FilterExpressionExecutor.doSelect(allParts, req.getAttrWhereCondition());

        // 3. 创建模型
        Loader.loadNativeLibraries();
        CpModelTracker model = new CpModelTracker();

        // 4. 创建零件变量
        List<PartVar> partVars = new ArrayList<>();
        for (Part part : filteredParts) {
            partVars.add(new PartVar(model, part));
        }

        // 5. 构建主要约束（容量或数量约束）
        buildMainConstraint(model, partVars, req);

        // 6. 构建产品规则约束
        // ProRule2: 固态硬盘必须配置同一种，并且最多配置2块
        applySolidStateRule(model, partVars);

        // ProRule1: 优先类规则（通过目标函数实现，这里只做约束限制）
        if (objectValue == null) {
            applyPriorityRule(model, partVars, req);
        } else {
            applyPriorityRule(model, partVars, req, objectValue.longValue());
        }

        // 8. 求解
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10);
        solver.getParameters().setEnumerateAllSolutions(true);
        solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解

        // 收集多个解
        ProductResult pResult = new ProductResult();
        SolutionCollector cb = new SolutionCollector(model, partVars, pResult);
        model.printModelSummary();
        CpSolverStatus status = solver.solve(model.getModel(), cb);
        model.printRunValue(solver, status);
        // 检查求解状态
        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                && status != CpSolverStatus.INFEASIBLE) {
            // 求解失败，返回空解
            pResult.setSolverStatus(status.toString());
            return pResult;
        }

        // 9. 排序解
        sortSolutions(pResult);
        pResult.setSolverStatus(status.toString());
        System.out.println(pResult.toShortString());
        return pResult;
    }

    // 构建主要约束
    private void buildMainConstraint(CpModelTracker model, List<PartVar> partVars, PartConstraintReq req) {
        if (req.getAttrCode() == null || req.getAttrComparator() == null || req.getAttrValue() == null) {
            return;
        }

        // 创建跟踪的线性表达式
        TrackedLinearExpr expr = model.newTrackedExpr("MainConstraint_" + req.getAttrCode());

        for (PartVar pv : partVars) {
            if (req.getAttrCode().equals("Capacity")) {
                // 总容量约束
                expr.addTerm(pv.qty, pv.getCapacity());
            } else if (req.getAttrCode().equals("Qty")) {
                // 总数量约束
                expr.addTerm(pv.qty, 1);
            }
        }

        int value = Integer.parseInt(req.getAttrValue());

        // 对value进行标准化（如果适用）
        if (req.getAttrCode().equals("Capacity")) {
            double normalizedValue = normalizationProcessor.normalizeValue(value, Part.ATTR_CAPACITY);
            value = (int) Math.round(normalizedValue);
            req.setNormalizeAttrValue(value);
        }

        switch (req.getAttrComparator()) {
            case ">=":
                model.addGreaterOrEqual(expr, value);
                break;
            case "=":
                model.addEquality(expr, value);
                break;
            case "<=":
                model.addLessOrEqual(expr, value);
                break;
        }
    }

    // 应用固态硬盘规则
    private void applySolidStateRule(CpModelTracker model, List<PartVar> partVars) {
        List<PartVar> solidStateParts = partVars.stream()
                .filter(PartVar::isSolidState)
                .collect(Collectors.toList());

        // 如果选择了固态硬盘，只能选择一种硬盘
        if (!solidStateParts.isEmpty()) {
            // // 创建一个变量表示是否选择了固态硬盘
            BoolVar hasSolidState = (BoolVar) model.newBoolVar(
                    "hasSolidState");

            // 如果有固态硬盘被选中，hasSolidState为true
            for (PartVar pv : solidStateParts) {
                model.addImplication(pv.isSelected,
                        hasSolidState);
            }

            // 最多只能有一种固态硬盘被选中
            TrackedLinearExpr sumSelected = model.newTrackedExpr("SolidState_Count");
            for (PartVar pv : solidStateParts) {
                sumSelected.addTerm(pv.isSelected, 1);
            }
            model.addLessOrEqual(sumSelected, 1);

            // 每种固态硬盘最多2块
            for (PartVar pv : solidStateParts) {
                model.addLessOrEqual(pv.qty, 2);
            }
        }

    }

    // 规则1: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
    private void applyPriorityRule(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req) {
        TrackedLinearExpr objectiveExpr = buildPriorityRule(model, partVars, req);
        model.minimize(objectiveExpr);
    }

    private void applyPriorityRule(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req, long objectValue) {
        TrackedLinearExpr objectiveExpr = buildPriorityRule(model, partVars, req);
        model.addLessOrEqual(objectiveExpr, objectValue);
    }

    // 规则1: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
    private static TrackedLinearExpr buildPriorityRule(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req) {
        // 分离固态硬盘和机械硬盘
        List<PartVar> solidStateParts = partVars.stream()
                .filter(PartVar::isSolidState)
                .collect(Collectors.toList());

        List<PartVar> mechanicalParts = partVars.stream()
                .filter(pv -> !pv.isSolidState())
                .collect(Collectors.toList());

        if (solidStateParts.isEmpty() || mechanicalParts.isEmpty()) {
            return null;
        }
        // 创建固态硬盘总容量表达式
        TrackedLinearExpr ssTotalCapacity = model.newTrackedExpr("SS_Total_Capacity");
        for (PartVar pv : solidStateParts) {
            ssTotalCapacity.addTerm(pv.qty, pv.getCapacity());
        }

        // 创建机械硬盘总容量表达式
        TrackedLinearExpr mechTotalCapacity = model.newTrackedExpr("Mech_Total_Capacity");
        for (PartVar pv : mechanicalParts) {
            mechTotalCapacity.addTerm(pv.qty, pv.getCapacity());
        }
        // 如果是容量需求
        if ("Capacity".equals(req.getAttrCode())) {

            int requiredCapacity = Integer.parseInt(req.getAttrValue());

            // 创建固态硬盘是否足够的布尔变量
            BoolVar ssSufficient = (BoolVar) model.newBoolVar(
                    "ssSufficient");

            // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
            model.addGreaterOrEqual(ssTotalCapacity, requiredCapacity).onlyEnforceIf(ssSufficient);
            model.addLessThan(ssTotalCapacity, requiredCapacity).onlyEnforceIf(ssSufficient.not());

            // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
            for (PartVar pv : mechanicalParts) {
                model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficient);
            }

            // 创建目标函数
            TrackedLinearExpr objectiveExpr = model.newTrackedExpr("ObjectiveFun");

            // 基础目标: 最大化SSD使用（负权重）--容量越大越好
            objectiveExpr.addExpr(ssTotalCapacity, -100);

            // HDD惩罚 = HDD容量 * 惩罚系数S
            objectiveExpr.addExpr(mechTotalCapacity, 1);

            // 3. 惩罚过度配置（重要！）
            // 创建总容量变量
            TrackedLinearExpr totalCapacityExpr = model.newTrackedExpr("Total_Capacity");
            for (PartVar pv : partVars) {
                totalCapacityExpr.addTerm(pv.qty, pv.getCapacity());
            }

            // 创建过度配置变量
            // 约束：excessCapacity = totalCapacity - requiredCapacity
            TrackedLinearExpr tExpr = model.newTrackedExpr("excessCapacityExpr");
            tExpr.addExpr(totalCapacityExpr, 1);
            tExpr.addConstant(-requiredCapacity);
            // 2. 过度配置惩罚
            objectiveExpr.addExpr(tExpr, 500); // 惩罚过度配置

            // 4. 惩罚使用多个零件（鼓励简洁配置）
            // 总零件数量惩罚
            TrackedLinearExpr totalPartsExpr = model.newTrackedExpr("Total_Parts");
            for (PartVar pv : partVars) {
                totalPartsExpr.addTerm(pv.qty, 1);
            }

            objectiveExpr.addExpr(totalPartsExpr, 500); // 零件数量惩罚

            // model.addLessOrEqual(objectiveExpr, 2000);
            model.setObjectExpr(objectiveExpr);

            // model.minimize(objectiveExpr); // 设置目标函数为最小化（因为SSD有负权重）
            return objectiveExpr;
        } else {// 给的数量qty总数
            // 创建固态硬盘总容量表达式
            TrackedLinearExpr ssTotalQty = model.newTrackedExpr("ssTotalQty");
            for (PartVar pv : solidStateParts) {
                ssTotalQty.addTerm(pv.qty, 1);
            }
            // 创建机械硬盘总容量表达式
            TrackedLinearExpr mechTotalQty = model.newTrackedExpr("mechTotalQty");
            for (PartVar pv : mechanicalParts) {
                mechTotalQty.addTerm(pv.qty, 1);
            }
            int requiredQty = Integer.parseInt(req.getAttrValue());
            // 创建固态硬盘是否足够的布尔变量
            BoolVar ssSufficientQty = (BoolVar) model.newBoolVar(
                    "ssSufficientQty");
            // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
            model.addGreaterOrEqual(ssTotalQty, requiredQty).onlyEnforceIf(ssSufficientQty);
            model.addLessThan(ssTotalQty, requiredQty).onlyEnforceIf(ssSufficientQty.not());
            // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
            for (PartVar pv : mechanicalParts) {
                model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficientQty);
            }

            // 创建目标函数
            TrackedLinearExpr objectiveExpr = model.newTrackedExpr("ObjectiveFunQty");

            // 基础目标: 最大化SSD使用（负权重）--容量越大越好
            objectiveExpr.addExpr(ssTotalCapacity, -100);
            // objectiveExpr.addExpr(ssTotalQty, -100);
            // HDD惩罚 = HDD容量 * 惩罚系数S，容量越小越好
            objectiveExpr.addExpr(mechTotalCapacity, 1);
            // objectiveExpr.addExpr(mechTotalQty, 1);

            // 3. 惩罚过度配置（重要！）
            // 创建总容量变量
            TrackedLinearExpr totalQtyExpr = model.newTrackedExpr("totalQtyExpr");
            for (PartVar pv : partVars) {
                totalQtyExpr.addTerm(pv.qty, 1);
            }
            // 创建过度配置变量
            // 约束：excessCapacity = totalCapacity - requiredCapacity
            TrackedLinearExpr excessQyExpr = model.newTrackedExpr("excessQyExpr");
            excessQyExpr.addExpr(totalQtyExpr, 1);
            excessQyExpr.addConstant(-requiredQty);
            // 2. 过度配置惩罚
            objectiveExpr.addExpr(excessQyExpr, 500); // 惩罚过度配置

            // model.addLessOrEqual(objectiveExpr, 1000);
            model.setObjectExpr(objectiveExpr);
            // model.minimize(objectiveExpr); // 设置目标函数为最小化（因为SSD有负权重）
            return objectiveExpr;
        }
    }

    private void setCompositeObjective(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req) {
        // 使用新的目标构建器
        ObjectiveExpressionBuilder builder = new ObjectiveExpressionBuilder(model, partVars);

        // 创建目标配置列表
        List<ObjectiveExpressionBuilder.ObjectiveConfig> objectives = new ArrayList<>();
        for (ObjectiveExpressionBuilder.ObjectiveConfig config : commonObjectives) {
            if (config.getAttrName().equals(Part.ATTR_CAPACITY)) {
                objectives.add(config);
            }
        }
        // objectives.addAll(Arrays.asList(commonObjectives));

        // 根据需求类型调整权重
        if ("Capacity".equals(req.getAttrCode())) {
            // 容量需求时，更重视容量和成本
            for (ObjectiveExpressionBuilder.ObjectiveConfig config : objectives) {
                if ("totalCapacity".equals(config.getName())) {
                    config.setWeight(2.0); // 提高容量权重
                } else if ("totalCost".equals(config.getName())) {
                    config.setWeight(1.5); // 提高成本权重
                }
            }
        }

        // 构建综合目标表达式
        TrackedLinearExpr objectiveExpr = builder.buildCompositeObjective(objectives);

        model.setObjectExpr(objectiveExpr);
        // setMin/max等
    }

    // 添加固态硬盘优先约束
    private void addSolidStatePriorityConstraints(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req) {
        // 分离固态硬盘和机械硬盘
        List<PartVar> solidStateParts = partVars.stream()
                .filter(PartVar::isSolidState)
                .collect(Collectors.toList());

        List<PartVar> mechanicalParts = partVars.stream()
                .filter(pv -> !pv.isSolidState())
                .collect(Collectors.toList());

        if (solidStateParts.isEmpty() || mechanicalParts.isEmpty()) {
            return;
        }

        // 创建固态硬盘总容量表达式
        TrackedLinearExpr ssTotalCapacity = model.newTrackedExpr("SS_Total_Capacity");
        for (PartVar pv : solidStateParts) {
            ssTotalCapacity.addTerm(pv.qty, pv.getCapacity());
        }

        if ("Capacity".equals(req.getAttrCode())) {
            // int requiredCapacity = Integer.parseInt(req.getAttgetrValue());
            int requiredCapacity = (Integer) (req.getNormalizeAttrValue());

            // 创建固态硬盘是否足够的布尔变量
            BoolVar ssSufficient = (BoolVar) model.newBoolVar("ssSufficient");

            // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
            model.addGreaterOrEqual(ssTotalCapacity, requiredCapacity).onlyEnforceIf(ssSufficient);
            model.addLessThan(ssTotalCapacity, requiredCapacity).onlyEnforceIf(ssSufficient.not());

            // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
            for (PartVar pv : mechanicalParts) {
                model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficient);
            }
        } else {
            // 数量约束的逻辑
            TrackedLinearExpr ssTotalQty = model.newTrackedExpr("ssTotalQty");
            for (PartVar pv : solidStateParts) {
                ssTotalQty.addTerm(pv.qty, 1);
            }

            int requiredQty = Integer.parseInt(req.getAttrValue());
            BoolVar ssSufficientQty = (BoolVar) model.newBoolVar("ssSufficientQty");

            model.addGreaterOrEqual(ssTotalQty, requiredQty).onlyEnforceIf(ssSufficientQty);
            model.addLessThan(ssTotalQty, requiredQty).onlyEnforceIf(ssSufficientQty.not());

            // 如果固态硬盘足够，则禁止使用机械硬盘
            for (PartVar pv : mechanicalParts) {
                model.addEquality(pv.qty, 0).onlyEnforceIf(ssSufficientQty);
            }
        }
    }

    // 添加过度配置惩罚
    private void addExcessConfigurationPenalty(CpModelTracker model, List<PartVar> partVars,
            PartConstraintReq req, TrackedLinearExpr objectiveExpr) {
        if ("Capacity".equals(req.getAttrCode())) {
            int requiredValue = (Integer) (req.getNormalizeAttrValue());

            // 创建总容量表达式
            TrackedLinearExpr totalCapacityExpr = model.newTrackedExpr("Total_Capacity");
            for (PartVar pv : partVars) {
                // totalCapacityExpr.addTerm(pv.qty, pv.getCapacity());
                totalCapacityExpr.addTerm(pv.qty, pv.getCapacity());
            }

            // 创建过度配置表达式：totalCapacity - requiredCapacity
            TrackedLinearExpr excessExpr = model.newTrackedExpr("excessCapacityExpr");
            excessExpr.addExpr(totalCapacityExpr, 1);
            excessExpr.addConstant(-requiredValue);

            objectiveExpr.addExpr(excessExpr, 500); // 惩罚过度配置
        } else {
            int requiredValue = Integer.parseInt(req.getAttrValue());

            // 创建总数量表达式
            TrackedLinearExpr totalQtyExpr = model.newTrackedExpr("totalQtyExpr");
            for (PartVar pv : partVars) {
                totalQtyExpr.addTerm(pv.qty, 1);
            }

            // 创建过度配置表达式：totalQty - requiredQty
            TrackedLinearExpr excessExpr = model.newTrackedExpr("excessQtyExpr");
            excessExpr.addExpr(totalQtyExpr, 1);
            excessExpr.addConstant(-requiredValue);

            objectiveExpr.addExpr(excessExpr, 500); // 惩罚过度配置
        }
    }

    // 添加零件数量惩罚
    private void addPartCountPenalty(CpModelTracker model, List<PartVar> partVars,
            TrackedLinearExpr objectiveExpr) {
        // 总零件数量惩罚（鼓励简洁配置）
        TrackedLinearExpr totalPartsExpr = model.newTrackedExpr("Total_Parts");
        for (PartVar pv : partVars) {
            totalPartsExpr.addTerm(pv.qty, 1);
        }

        objectiveExpr.addExpr(totalPartsExpr, 500); // 零件数量惩罚
    }

    // 简化版本：假设容量需求总是被满足
    public static IntVar createSimpleExcessCapacityVar(CpModelTracker model,
            TrackedLinearExpr totalCapacityExpr,
            int requiredCapacity) {

        // 首先确保容量约束
        model.addGreaterOrEqual(totalCapacityExpr, requiredCapacity);

        // 创建过度容量变量
        IntVar excessCapacity = model.newIntVar(0, 1000, "excess_capacity_simple");

        TrackedLinearExpr tExpr = model.newTrackedExpr("excessCapacityExpr");
        tExpr.addExpr(totalCapacityExpr, 1);
        tExpr.addConstant(-requiredCapacity);
        // 定义 excessCapacity = totalCapacity - requiredCapacity
        model.addEquality(excessCapacity, tExpr);

        return excessCapacity;
    }
    // // 规则：如果固态硬盘容量已足够，则限制机械硬盘使用
    // // 这里通过约束来限制机械硬盘的使用
    // // 注意：这是一个简化实现，完整实现需要更复杂的逻辑
    // }

    // 排序解决方案
    private void sortSolutions(ProductResult pResult) {
        sortSolutions(pResult.getSolutions());
    }

    private void sortSolutions(List<Solution> solutions) {
        // 排序规则：
        // 1. 按目标值从小到大排序（目标值越小越好）
        // 2. 如果目标值相同，优先高速率固态硬盘（speed 高的优先）
        // 3. 如果速度相同，无机械硬盘的优先
        // 4. 如果都相同，总零件数量少的优先
        solutions.sort((sol1, sol2) -> {
            // 1. 按solution.objectValue从小到大排序（目标值越小越好）
            int objCompare = Double.compare(sol1.getObjectValue(), sol2.getObjectValue());
            if (objCompare != 0) {
                return objCompare;
            }

            // 2. 如果目标值相同，按最高速固态硬盘速度降序（高速优先）
            int maxSpeed1 = getMaxSolidStateSpeed(sol1);
            int maxSpeed2 = getMaxSolidStateSpeed(sol2);

            if (maxSpeed1 != maxSpeed2) {
                return Integer.compare(maxSpeed2, maxSpeed1);
            }

            // 3. 如果速度相同，无机械硬盘的优先
            boolean hasMech1 = hasMechanicalDrive(sol1);
            boolean hasMech2 = hasMechanicalDrive(sol2);

            if (hasMech1 != hasMech2) {
                return Boolean.compare(hasMech1, hasMech2);
            }

            // 4. 如果都相同，总零件数量少的优先
            int totalCount1 = getTotalCount(sol1);
            int totalCount2 = getTotalCount(sol2);

            return Integer.compare(totalCount1, totalCount2);
        });
    }

    private int getMaxSolidStateSpeed(Solution solution) {
        int maxSpeed = 0;
        for (PartResult pr : solution.getParts()) {
            if (pr.getCode().startsWith("sd") && pr.isSelected()) {
                // 查找对应的 Part 来获取 speed
                for (Part part : allParts) {
                    if (part.getCode().equals(pr.getCode())) {
                        maxSpeed = Math.max(maxSpeed, part.getSpeed());
                        break;
                    }
                }
            }
        }
        return maxSpeed;
    }

    private boolean hasMechanicalDrive(Solution solution) {
        for (PartResult pr : solution.getParts()) {
            if (pr.getCode().startsWith("md") && pr.isSelected()) {
                return true;
            }
        }
        return false;
    }

    private int getTotalCount(Solution solution) {
        int total = 0;
        for (PartResult pr : solution.getParts()) {
            total += pr.getQty();
        }
        return total;
    }

    // 解决方案收集器

    class SolutionCollector extends CpSolverSolutionCallback {
        private CpModelTracker model;
        private final List<PartVar> partVars;

        private final ProductResult productResult;
        final int SEARCH_MAX = 100;// 搜索的最大解

        public SolutionCollector(CpModelTracker model, List<PartVar> partVars, ProductResult productResult) {
            this.model = model;
            this.partVars = partVars;
            this.productResult = productResult;
            this.productResult.setSearchMax(SEARCH_MAX);
        }

        @Override
        public void onSolutionCallback() {
            List<PartResult> partResults = new ArrayList<>();
            Map<String, Integer> partQtyMap = new HashMap<>();
            for (PartVar pv : partVars) {
                int qty = (int) value(pv.qty);
                partQtyMap.put(pv.getQtyVarName(), qty);
                boolean isSelected = qty > 0;
                partResults.add(new PartResult(pv.code, isSelected, qty));
            }

            double objectValue = calcObjectValue(model.getObjectExpr(), partQtyMap);
            Solution solution = new Solution(partResults, objectValue);
            solution.setSearchStep(productResult.getSolutions().size() + 1);
            productResult.getSolutions().add(solution);
            log.info("Current Solution: " + productResult.getSolutions().size() + " OV=" + objectValue);
            // 限制最多收集100个解

            if (productResult.getSolutions().size() >= SEARCH_MAX) {
                log.warn("Has reach the max:" + SEARCH_MAX + " will stop!");
                productResult.setHasSearchMax(true);
                stopSearch();
            }
        }

        private double calcObjectValue(TrackedLinearExpr objectExp, Map<String, Integer> partQtyMap) {
            if (objectExp == null) {
                return 0.0;
            }
            return ExpressionCalculator.calculate(objectExp.toString(), partQtyMap);
        }
    }

    // 测试方法
    public static void main(String[] args) {
        PriorityBasicSolver solver = new PriorityBasicSolver();

        System.out.println("=== 测试用例0 ===");
        System.out.println("需求: Capacity >=6 where Speed = 5400");
        ProductResult result0 = solver.solve("Capacity >=6 where Speed = 5400");
        System.out.println(result0);

        System.out.println("\n=== 测试用例1 ===");
        System.out.println("需求: Capacity >=5 where Speed = 5400");
        ProductResult result1 = solver.solve("Capacity >=5 where Speed = 5400");
        System.out.println(result1);

        System.out.println("\n=== 测试用例2 ===");
        System.out.println("需求: Capacity >=7 where Speed = 5400");
        ProductResult result2 = solver.solve("Capacity >=7 where Speed = 5400");
        System.out.println(result2);

        System.out.println("\n=== 测试用例3 ===");
        System.out.println("需求: Qty >=2 where Speed = 5400");
        ProductResult result3 = solver.solve("Qty >=2 where Speed = 5400");
        System.out.println(result3);

        System.out.println("\n=== 测试用例4 ===");
        System.out.println("需求: Qty >=3 where Speed = 5400");
        ProductResult result4 = solver.solve("Qty >=3 where Speed = 5400");
        System.out.println(result4);

        System.out.println("\n=== 测试用例5 ===");
        System.out.println("需求: Capacity >=5");
        ProductResult result5 = solver.solve("Capacity >=5");
        System.out.println(result5);
    }
}
