package com.jmix.temp3;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

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
public class ProductSolver {

    private List<Part> allParts;

    public ProductSolver() {
        initParts();
    }

    // 初始化零件数据
    private void initParts() {
        allParts = new ArrayList<>();
        // 固态硬盘
        allParts.add(new Part("sd1", true, 5400, 3));
        allParts.add(new Part("sd2", true, 7200, 6));
        allParts.add(new Part("sd3", true, 9000, 9));
        // 机械硬盘
        allParts.add(new Part("md1", false, 5400, 1));
        allParts.add(new Part("md2", false, 7200, 2));
        allParts.add(new Part("md3", false, 9000, 3));
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
        buildProductRules(model, partVars, req);

        // 7. 设置目标函数（优化目标）
        setObjectiveFunction(model, partVars);

        // 8. 求解
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10);
        solver.getParameters().setEnumerateAllSolutions(true);
        solver.getParameters().setNumSearchWorkers(1); // 单线程搜索，防止有重复解

        // 收集多个解
        List<Map<String, Integer>> allSolutions = new ArrayList<>();
        SolutionCollector cb = new SolutionCollector(partVars, allSolutions);
        CpSolverStatus status = solver.solve(model.getModel(), cb);

        // 检查求解状态
        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE
                && status != CpSolverStatus.INFEASIBLE) {
            // 求解失败，返回空解
            return new ProductResult(new ArrayList<>());
        }

        // 9. 排序解
        sortSolutions(allSolutions);

        return new ProductResult(allSolutions);
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
                expr.addTerm(pv.qty, pv.getCapacity(), pv.code + " capacity");
            } else if (req.getAttrCode().equals("Qty")) {
                // 总数量约束
                expr.addTerm(pv.qty, 1, pv.code + " quantity");
            }
        }

        int value = Integer.parseInt(req.getAttrValue());

        switch (req.getAttrComparator()) {
            case ">=":
                model.addGreaterOrEqual(expr, value);
                break;
            case "=":
                model.addEquality(expr.build(), value);
                break;
            case "<=":
                model.addLessOrEqual(expr.build(), value);
                break;
        }
    }

    // 构建产品规则约束
    private void buildProductRules(CpModelTracker model, List<PartVar> partVars, PartConstraintReq req) {
        // ProRule2: 固态硬盘必须配置同一种，并且最多配置2块
        applySolidStateRule(model, partVars);

        // ProRule1: 优先类规则（通过目标函数实现，这里只做约束限制）
        applyPriorityRule(model, partVars, req);
    }

    // 应用固态硬盘规则
    private void applySolidStateRule(CpModelTracker model, List<PartVar> partVars) {
        List<PartVar> solidStateParts = partVars.stream()
                .filter(PartVar::isSolidState)
                .collect(Collectors.toList());

        // 如果选择了固态硬盘，只能选择一种
        if (!solidStateParts.isEmpty()) {
            // 创建一个变量表示是否选择了固态硬盘
            BoolVar hasSolidState = (BoolVar) model.newBoolVar(
                    "Indicates if any solid-state drive is selected");

            // 如果有固态硬盘被选中，hasSolidState为true
            for (PartVar pv : solidStateParts) {
                TrackerConstraint implication = model.addImplication(pv.isSelected, hasSolidState);
                implication.onlyEnforceIf(pv.isSelected);
            }

            // 最多只能有一种固态硬盘被选中
            TrackedLinearExpr sumSelected = model.newTrackedExpr("SolidState_Count");
            for (PartVar pv : solidStateParts) {
                sumSelected.addTerm(pv.isSelected, 1, pv.code + " selected");
            }
            model.addLessOrEqual(sumSelected.build(), 1);

            // 每种固态硬盘最多2块
            for (PartVar pv : solidStateParts) {
                LinearExprBuilder qtyExpr = LinearExpr.newBuilder();
                qtyExpr.addTerm(pv.qty, 1);
                model.addLessOrEqual(qtyExpr.build(), 2);
            }
        }

        // // 机械硬盘数量限制（可选）
        // List<PartVar> mechanicalParts = partVars.stream()
        // .filter(pv -> !pv.isSolidState())
        // .collect(Collectors.toList());

        // for (PartVar pv : mechanicalParts) {
        // model.addLessOrEqual(pv.qty, 3); // 假设最多3块
        // }
    }

    // 规则1: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
    private static void applyPriorityRule(CpModelTracker model, List<PartVar> partVars,
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
            ssTotalCapacity.addTerm(pv.qty, pv.getCapacity(), pv.code + " capacity");
        }

        // 创建机械硬盘总容量表达式
        TrackedLinearExpr mechTotalCapacity = model.newTrackedExpr("Mech_Total_Capacity");
        for (PartVar pv : mechanicalParts) {
            mechTotalCapacity.addTerm(pv.qty, pv.getCapacity(), pv.code + " capacity");
        }

        // 如果是容量需求
        if ("Capacity".equals(req.getAttrCode())) {
            int requiredCapacity = Integer.parseInt(req.getAttrValue());

            // 创建固态硬盘是否足够的布尔变量
            BoolVar ssSufficient = (BoolVar) model.newBoolVar(
                    "Indicates if SSD capacity is sufficient");

            // 定义：如果固态硬盘容量 >= 需求容量，则 ssSufficient = true
            TrackerConstraint geConstraint = model.addGreaterOrEqual(ssTotalCapacity, requiredCapacity);
            geConstraint.onlyEnforceIf(ssSufficient);

            TrackerConstraint ltConstraint = model.addLessThan(ssTotalCapacity.build(),
                    LinearExpr.constant(requiredCapacity));
            ltConstraint.onlyEnforceIf(ssSufficient.not());

            // 规则1.1: 如果固态硬盘足够，则禁止使用机械硬盘
            for (PartVar pv : mechanicalParts) {
                LinearExprBuilder qtyExpr = LinearExpr.newBuilder();
                qtyExpr.addTerm(pv.qty, 1);
                TrackerConstraint equality = model.addEquality(qtyExpr.build(), 0);
                equality.onlyEnforceIf(ssSufficient);
            }
            // 步骤5: 核心约束 - 如果SSD足够，限制HDD使用
            // 使用大惩罚权重在目标函数中实现
            LinearExprBuilder objective = LinearExpr.newBuilder();

            // 基础目标: 最大化SSD使用（负权重）
            objective.addTerm(ssTotalCapacity.build(), -10000); // 最大化SSD容量

            // 关键: 动态惩罚HDD使用 - 当SSD足够时惩罚更大
            IntVar hddPenaltyFactor = model.newIntVar(0, 1000000, "Penalty factor for HDD usage");

            // 计算惩罚系数: 如果SSD足够，惩罚系数很大；否则惩罚系数小
            // 惩罚系数 = 1000000 * ssdSufficient + 1000 * (1 - ssdSufficient)
            TrackedLinearExpr penaltyExpr = model.newTrackedExpr("Penalty_Calculation");
            penaltyExpr.addTerm((IntVar) ssSufficient, 1000000 - 1000, "High penalty when SSD sufficient");
            penaltyExpr.addConstant(1000, "Base penalty");

            LinearExprBuilder penaltyFactorExpr = LinearExpr.newBuilder();
            penaltyFactorExpr.addTerm(hddPenaltyFactor, 1);
            // 这里需要更复杂的处理来设置惩罚系数计算
            // 暂时简化：直接设置惩罚系数为固定值
            LinearExprBuilder factorExpr = LinearExpr.newBuilder();
            factorExpr.addTerm(hddPenaltyFactor, 1);
            model.addEquality(factorExpr.build(), 1000);

            // 创建目标函数
            TrackedLinearExpr objectiveExpr = model.newTrackedExpr("Objective");

            // 基础目标: 最大化SSD使用（负权重）- 这里需要特殊处理，因为我们想要整个表达式的值
            // 暂时简化处理
            TrackedLinearExpr ssdPenalty = model.newTrackedExpr("SSD_Penalty");
            // 这里应该处理SSD容量表达式，但暂时简化

            // HDD惩罚 = HDD容量 * 惩罚系数
            TrackedLinearExpr hddPenalty = model.newTrackedExpr("HDD_Penalty");
            // 这里也需要简化处理，因为我们想要表达式的值

            // 暂时使用简单的方式设置目标函数
            model.setObjective(ssTotalCapacity, true, "Maximize SSD capacity, minimize HDD usage");

            // 设置目标函数
            model.setObjective(objectiveExpr, false, "Storage capacity optimization with priority rules");
        }
    }

    // // 应用优先规则（通过约束实现）
    // private void applyPriorityRule(CpModel model, List<PartVar> partVars,
    // PartConstraintReq req) {
    // // 分离固态硬盘和机械硬盘
    // List<PartVar> solidStateParts = partVars.stream()
    // .filter(PartVar::isSolidState)
    // .collect(Collectors.toList());

    // List<PartVar> mechanicalParts = partVars.stream()
    // .filter(pv -> !pv.isSolidState())
    // .collect(Collectors.toList());

    // if (solidStateParts.isEmpty() || mechanicalParts.isEmpty()) {
    // return;
    // }

    // // 计算固态硬盘提供的总容量
    // LinearExprBuilder ssCapacity = LinearExpr.newBuilder();
    // for (PartVar pv : solidStateParts) {
    // ssCapacity.addTerm(pv.qty, pv.getCapacity());
    // }

    // // 计算机械硬盘提供的总容量
    // LinearExprBuilder mechCapacity = LinearExpr.newBuilder();
    // for (PartVar pv : mechanicalParts) {
    // mechCapacity.addTerm(pv.qty, pv.getCapacity());
    // }

    // // 规则：如果固态硬盘容量已足够，则限制机械硬盘使用
    // // 这里通过约束来限制机械硬盘的使用
    // // 注意：这是一个简化实现，完整实现需要更复杂的逻辑
    // }

    // 设置目标函数（优化目标）
    private void setObjectiveFunction(CpModelTracker model, List<PartVar> partVars) {
        // 注意：目标函数现在在applyPriorityRule中设置，这里仅用于记录日志
        // 如果需要简单的目标函数，可以在这里设置

        // 记录目标函数设置信息
        System.out.println("Setting objective function for " + partVars.size() + " parts");
    }

    // 排序解决方案
    private void sortSolutions(List<Map<String, Integer>> solutions) {
        // 排序规则：
        // 1. 优先高速率固态硬盘（speed 高的优先）
        // 2. 然后低速率固态硬盘
        // 3. 最后机械硬盘
        // 4. 在相同类型内，数量少的优先

        solutions.sort((sol1, sol2) -> {
            // 计算最高速固态硬盘速度
            int maxSpeed1 = getMaxSolidStateSpeed(sol1);
            int maxSpeed2 = getMaxSolidStateSpeed(sol2);

            if (maxSpeed1 != maxSpeed2) {
                return Integer.compare(maxSpeed2, maxSpeed1); // 降序，高速优先
            }

            // 计算是否有机械硬盘
            boolean hasMech1 = hasMechanicalDrive(sol1);
            boolean hasMech2 = hasMechanicalDrive(sol2);

            if (hasMech1 != hasMech2) {
                return Boolean.compare(hasMech1, hasMech2); // 有机械硬盘的排在后面
            }

            // 计算总零件数量
            int totalCount1 = getTotalCount(sol1);
            int totalCount2 = getTotalCount(sol2);

            return Integer.compare(totalCount1, totalCount2); // 升序，数量少的优先
        });
    }

    private int getMaxSolidStateSpeed(Map<String, Integer> solution) {
        int maxSpeed = 0;
        for (Map.Entry<String, Integer> entry : solution.entrySet()) {
            if (entry.getKey().startsWith("sd") && entry.getValue() > 0) {
                // 查找对应的 Part 来获取 speed
                for (Part part : allParts) {
                    if (part.code.equals(entry.getKey())) {
                        maxSpeed = Math.max(maxSpeed, part.speed);
                        break;
                    }
                }
            }
        }
        return maxSpeed;
    }

    private boolean hasMechanicalDrive(Map<String, Integer> solution) {
        for (Map.Entry<String, Integer> entry : solution.entrySet()) {
            if (entry.getKey().startsWith("md") && entry.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private int getSolidStateCount(Map<String, Integer> solution) {
        int count = 0;
        for (Map.Entry<String, Integer> entry : solution.entrySet()) {
            if (entry.getKey().startsWith("sd")) {
                count += entry.getValue();
            }
        }
        return count;
    }

    private int getTotalCount(Map<String, Integer> solution) {
        int total = 0;
        for (int count : solution.values()) {
            total += count;
        }
        return total;
    }

    // 解决方案收集器

    class SolutionCollector extends CpSolverSolutionCallback {

        private final List<PartVar> partVars;

        private final List<Map<String, Integer>> allSolutions;

        public SolutionCollector(List<PartVar> partVars, List<Map<String, Integer>> allSolutions) {
            this.partVars = partVars;
            this.allSolutions = allSolutions;
        }

        @Override
        public void onSolutionCallback() {
            Map<String, Integer> solution = new HashMap<>();
            for (PartVar pv : partVars) {
                int qty = (int) value(pv.qty);
                if (qty > 0) {
                    solution.put(pv.code, qty);
                }
            }
            allSolutions.add(solution);

            // 限制最多收集100个解
            if (allSolutions.size() >= 100) {
                stopSearch();
            }
        }
    }

    // 测试方法
    public static void main(String[] args) {
        ProductSolver solver = new ProductSolver();

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
