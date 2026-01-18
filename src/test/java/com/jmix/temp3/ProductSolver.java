package com.jmix.temp3;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            String[] mainParts = strReq.split("where");
            String constraint = mainParts[0].trim();

            if (mainParts.length > 1) {
                req.setAttrWhereCondition(mainParts[1].trim());
            }

            // 解析约束部分，如 "Capacity >=5" 或 "Qty >=2"
            String[] constraintParts = constraint.split("\\s+");
            if (constraintParts.length >= 3) {
                req.setAttrCode(constraintParts[0]);
                req.setAttrComparator(constraintParts[1]);
                req.setAttrValue(constraintParts[2]);
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
        CpModel model = new CpModel();

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
        solver.getParameters().setNumSearchWorkers(8);

        // 收集多个解
        List<Map<String, Integer>> allSolutions = new ArrayList<>();
        SolutionCollector cb = new SolutionCollector(partVars, allSolutions);
        solver.searchAllSolutions(model, cb);

        // 9. 排序解
        sortSolutions(allSolutions);

        return new ProductResult(allSolutions);
    }

    // 构建主要约束
    private void buildMainConstraint(CpModel model, List<PartVar> partVars, PartConstraintReq req) {
        if (req.getAttrCode() == null || req.getAttrComparator() == null || req.getAttrValue() == null) {
            return;
        }

        // 创建线性表达式
        LinearExprBuilder expr = LinearExpr.newBuilder();

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

    // 构建产品规则约束
    private void buildProductRules(CpModel model, List<PartVar> partVars, PartConstraintReq req) {
        // ProRule2: 固态硬盘必须配置同一种，并且最多配置2块
        applySolidStateRule(model, partVars);

        // ProRule1: 优先类规则（通过目标函数实现，这里只做约束限制）
        applyPriorityRule(model, partVars, req);
    }

    // 应用固态硬盘规则
    private void applySolidStateRule(CpModel model, List<PartVar> partVars) {
        List<PartVar> solidStateParts = partVars.stream()
                .filter(PartVar::isSolidState)
                .collect(Collectors.toList());

        // 如果选择了固态硬盘，只能选择一种
        if (!solidStateParts.isEmpty()) {
            // 创建一个变量表示是否选择了固态硬盘
            BoolVar hasSolidState = model.newBoolVar("has_solid_state");

            // 如果有固态硬盘被选中，hasSolidState为true
            for (PartVar pv : solidStateParts) {
                model.addImplication(pv.isSelected, hasSolidState);
            }

            // 最多只能有一种固态硬盘被选中
            LinearExprBuilder sumSelected = LinearExpr.newBuilder();
            for (PartVar pv : solidStateParts) {
                sumSelected.addTerm(pv.isSelected, 1);
            }
            model.addLessOrEqual(sumSelected, 1);

            // 每种固态硬盘最多2块
            for (PartVar pv : solidStateParts) {
                model.addLessOrEqual(pv.qty, 2);
            }
        }

        // 机械硬盘数量限制（可选）
        List<PartVar> mechanicalParts = partVars.stream()
                .filter(pv -> !pv.isSolidState())
                .collect(Collectors.toList());

        for (PartVar pv : mechanicalParts) {
            model.addLessOrEqual(pv.qty, 3); // 假设最多3块
        }
    }

    // 应用优先规则（通过约束实现）
    private void applyPriorityRule(CpModel model, List<PartVar> partVars, PartConstraintReq req) {
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

        // 计算固态硬盘提供的总容量
        LinearExprBuilder ssCapacity = LinearExpr.newBuilder();
        for (PartVar pv : solidStateParts) {
            ssCapacity.addTerm(pv.qty, pv.getCapacity());
        }

        // 计算机械硬盘提供的总容量
        LinearExprBuilder mechCapacity = LinearExpr.newBuilder();
        for (PartVar pv : mechanicalParts) {
            mechCapacity.addTerm(pv.qty, pv.getCapacity());
        }

        // 规则：如果固态硬盘容量已足够，则限制机械硬盘使用
        // 这里通过约束来限制机械硬盘的使用
        // 注意：这是一个简化实现，完整实现需要更复杂的逻辑
    }

    // 设置目标函数（优化目标）
    private void setObjectiveFunction(CpModel model, List<PartVar> partVars) {
        // 目标：最小化成本（这里假设固态硬盘成本高，机械硬盘成本低）
        // 优先使用高速固态硬盘，然后是低速固态硬盘，最后是机械硬盘

        LinearExprBuilder objective = LinearExpr.newBuilder();

        for (PartVar pv : partVars) {
            int weight = 0;
            if (pv.isSolidState()) {
                // 固态硬盘：速度越高，权重越小（越优先）
                weight = 1000 - pv.getSpeed(); // 高速固态硬盘权重小
            } else {
                // 机械硬盘：在固态硬盘之后考虑
                weight = 2000 - pv.getSpeed(); // 机械硬盘权重大
            }

            // 同时考虑数量，数量越少越好
            objective.addTerm(pv.qty, weight);
        }

        model.minimize(objective);
    }

    // 排序解决方案
    private void sortSolutions(List<Map<String, Integer>> solutions) {
        // 排序规则：
        // 1. 优先固态硬盘数量多的
        // 2. 固态硬盘数量相同时，总零件数量少的优先

        solutions.sort((sol1, sol2) -> {
            // 计算固态硬盘总数量
            int ssCount1 = getSolidStateCount(sol1);
            int ssCount2 = getSolidStateCount(sol2);

            if (ssCount1 != ssCount2) {
                return Integer.compare(ssCount2, ssCount1); // 降序
            }

            // 计算总零件数量
            int totalCount1 = getTotalCount(sol1);
            int totalCount2 = getTotalCount(sol2);

            return Integer.compare(totalCount1, totalCount2); // 升序
        });
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
