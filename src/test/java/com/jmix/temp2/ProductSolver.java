package com.jmix.temp2;

import com.jmix.executor.imodel.Extensible;
import com.jmix.executor.impl.util.FilterExpressionExecutor;
import com.jmix.executor.omodel.PartConstraintReq;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverSolutionCallback;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 产品求解器
 * 
 * @since 2025-01-14
 */
@Slf4j
public class ProductSolver {
    static {
        Loader.loadNativeLibraries();
    }

    /**
     * 所有部件列表
     */
    private List<Part> allParts;

    /**
     * 部件变量映射
     */
    private Map<String, PartVar> partVarMap = new HashMap<>();

    /**
     * 构造函数，初始化部件数据
     */
    public ProductSolver() {
        initializeParts();
    }

    /**
     * 初始化部件数据
     */
    private void initializeParts() {
        allParts = new ArrayList<>();

        // 固态硬盘
        Part sd1 = new Part();
        sd1.setCode("sd1");
        sd1.setSpeed("5400");
        sd1.setCapacity(3);
        sd1.setType("sd");
        allParts.add(sd1);

        Part sd2 = new Part();
        sd2.setCode("sd2");
        sd2.setSpeed("7200");
        sd2.setCapacity(6);
        sd2.setType("sd");
        allParts.add(sd2);

        Part sd3 = new Part();
        sd3.setCode("sd3");
        sd3.setSpeed("9000");
        sd3.setCapacity(9);
        sd3.setType("sd");
        allParts.add(sd3);

        // 机械硬盘
        Part md1 = new Part();
        md1.setCode("md1");
        md1.setSpeed("5400");
        md1.setCapacity(1);
        md1.setType("md");
        allParts.add(md1);

        Part md2 = new Part();
        md2.setCode("md2");
        md2.setSpeed("7200");
        md2.setCapacity(2);
        md2.setType("md");
        allParts.add(md2);

        Part md3 = new Part();
        md3.setCode("md3");
        md3.setSpeed("9000");
        md3.setCapacity(3);
        md3.setType("md");
        allParts.add(md3);
    }

    /**
     * 求解产品配置
     * 
     * @param strReq 字符串请求，例如："Capacity >=5 where Speed = 5400"
     * @return 求解结果
     */
    public ProductResult solve(String strReq) {
        try {
            log.info("Starting product solver with request: {}", strReq);

            // 1. 将strReq转换为PartConstraintReq
            PartConstraintReq req = parseStringRequest(strReq);

            // 2. 加载所有的parts（已在构造函数中初始化）

            // 3. 调用FilterExpressionExecutor.doSelect进行过滤
            List<Part> filteredParts = filterParts(req);

            if (filteredParts.isEmpty()) {
                log.warn("No parts found matching the filter condition");
                return ProductResult.failed("No parts found matching the filter condition");
            }

            log.info("Filtered parts count: {}", filteredParts.size());

            // 4. 根据filteredParts构建约束变量PartVar
            CpModel model = new CpModel();
            List<PartVar> partVars = buildPartVars(model, filteredParts);

            // 5. 根据req和partVars来构造约束
            addRequestConstraint(model, req, partVars);

            // 6. 根据proRule2/proRule1和partVars构建约束
            addProductRules(model, partVars);

            // 7. 进行求解
            return solveModel(model, partVars);

        } catch (Exception e) {
            log.error("Error during product solving", e);
            return ProductResult.failed("Error: " + e.getMessage());
        }
    }

    /**
     * 解析字符串请求
     */
    private PartConstraintReq parseStringRequest(String strReq) {
        PartConstraintReq req = new PartConstraintReq();

        // 解析格式：attrCode comparator value where condition
        // 例如："Capacity >=6 where Speed = 5400"
        String[] parts = strReq.split(" where ");
        String attrExpr = parts[0].trim();

        if (parts.length > 1) {
            req.setAttrWhereCondition(parts[1].trim());
        }

        // 解析属性表达式：attrCode comparator value
        // 支持 ==, !=, <, >, <=, >=
        Pattern pattern = Pattern.compile("(.+?)\\s*(==|!=|<=|>=|<|>)\\s*(.+)");
        Matcher matcher = pattern.matcher(attrExpr);

        if (matcher.matches()) {
            req.setAttrCode(matcher.group(1).trim());
            req.setAttrComparator(matcher.group(2).trim());
            req.setAttrValue(matcher.group(3).trim());
        } else {
            throw new IllegalArgumentException("Invalid attribute expression format: " + attrExpr);
        }

        return req;
    }

    /**
     * 过滤部件
     * 根据 ProductRule2：固态硬盘优先匹配高速率容量
     * - 对于固态硬盘（sd）：不受 Speed 过滤条件限制，所有固态硬盘都参与求解
     * - 对于机械硬盘（md）：需要满足 Speed 过滤条件
     */
    private List<Part> filterParts(PartConstraintReq req) {
        if (req.getAttrWhereCondition() == null || req.getAttrWhereCondition().trim().isEmpty()) {
            return new ArrayList<>(allParts);
        }

        // 将Part转换为Extensible以便使用FilterExpressionExecutor
        List<ExtensiblePart> extensibleParts = new ArrayList<>();
        for (Part part : allParts) {
            extensibleParts.add(new ExtensiblePart(part));
        }

        // 根据 ProductRule2，固态硬盘优先匹配高速率容量
        // 所有固态硬盘都参与求解（不受 Speed 过滤限制）
        // 机械硬盘需要满足 Speed 过滤条件
        List<Part> filteredParts = new ArrayList<>();

        // 先添加所有固态硬盘
        for (Part part : allParts) {
            if ("sd".equals(part.getType())) {
                filteredParts.add(part);
            }
        }

        // 对机械硬盘应用过滤条件
        List<ExtensiblePart> mdParts = new ArrayList<>();
        for (ExtensiblePart extensiblePart : extensibleParts) {
            if ("md".equals(extensiblePart.getPart().getType())) {
                mdParts.add(extensiblePart);
            }
        }

        List<ExtensiblePart> filteredMdParts = FilterExpressionExecutor.doSelect(mdParts,
                req.getAttrWhereCondition());

        // 添加满足条件的机械硬盘
        for (ExtensiblePart extensiblePart : filteredMdParts) {
            filteredParts.add(extensiblePart.getPart());
        }

        log.info("Filtered parts: {} SD parts (all) + {} MD parts (filtered by: {})",
                allParts.stream().filter(p -> "sd".equals(p.getType())).count(),
                filteredMdParts.size(), req.getAttrWhereCondition());

        return filteredParts;
    }

    /**
     * 构建PartVar列表
     */
    private List<PartVar> buildPartVars(CpModel model, List<Part> parts) {
        List<PartVar> partVars = new ArrayList<>();
        partVarMap.clear();

        for (Part part : parts) {
            PartVar partVar = new PartVar();
            partVar.setPart(part);
            partVar.setCode(part.getCode());

            // 创建BoolVar isSelected
            BoolVar isSelected = model.newBoolVar("isSelected_" + part.getCode());
            partVar.setIsSelected(isSelected);

            // 创建IntVar qty，范围是0到20
            IntVar qty = model.newIntVar(0, 20, "qty_" + part.getCode());
            partVar.setQty(qty);

            // 添加约束：如果qty > 0，则isSelected = true
            // qty > 0 => isSelected = 1
            model.addGreaterOrEqual(qty, 1).onlyEnforceIf(isSelected);
            model.addEquality(qty, 0).onlyEnforceIf(isSelected.not());

            partVars.add(partVar);
            partVarMap.put(part.getCode(), partVar);
        }

        return partVars;
    }

    /**
     * 添加请求约束
     */
    private void addRequestConstraint(CpModel model, PartConstraintReq req, List<PartVar> partVars) {
        String attrCode = req.getAttrCode();
        String comparator = req.getAttrComparator();
        String attrValue = req.getAttrValue();

        if ("Capacity".equals(attrCode) || "capacity".equalsIgnoreCase(attrCode)) {
            // 容量约束：sum(partVars[i].qty * partVars[i].capacity) comparator value
            LinearExprBuilder capacitySum = LinearExpr.newBuilder();
            for (PartVar partVar : partVars) {
                capacitySum.addTerm(partVar.getQty(), partVar.getCapacity());
            }

            int value = Integer.parseInt(attrValue);
            if (">=".equals(comparator)) {
                model.addGreaterOrEqual(capacitySum.build(), value);
            } else if ("<=".equals(comparator)) {
                model.addLessOrEqual(capacitySum.build(), value);
            } else if ("==".equals(comparator) || "=".equals(comparator)) {
                model.addEquality(capacitySum.build(), value);
            } else if (">".equals(comparator)) {
                model.addGreaterThan(capacitySum.build(), value);
            } else if ("<".equals(comparator)) {
                model.addLessThan(capacitySum.build(), value);
            }
        } else if ("Qty".equals(attrCode) || "Quantity".equalsIgnoreCase(attrCode)
                || "qty".equalsIgnoreCase(attrCode)) {
            // 数量约束：sum(partVars[i].qty) comparator value
            LinearExprBuilder qtySum = LinearExpr.newBuilder();
            for (PartVar partVar : partVars) {
                qtySum.add(partVar.getQty());
            }

            int value = Integer.parseInt(attrValue);
            if (">=".equals(comparator)) {
                model.addGreaterOrEqual(qtySum.build(), value);
            } else if ("<=".equals(comparator)) {
                model.addLessOrEqual(qtySum.build(), value);
            } else if ("==".equals(comparator) || "=".equals(comparator)) {
                model.addEquality(qtySum.build(), value);
            } else if (">".equals(comparator)) {
                model.addGreaterThan(qtySum.build(), value);
            } else if ("<".equals(comparator)) {
                model.addLessThan(qtySum.build(), value);
            }
        }
    }

    /**
     * 添加产品规则约束
     */
    private void addProductRules(CpModel model, List<PartVar> partVars) {
        // proRule1: 固态硬盘必须配置同一种，并且最多配置2块
        List<PartVar> sdParts = new ArrayList<>();
        for (PartVar partVar : partVars) {
            if ("sd".equals(partVar.getPart().getType())) {
                sdParts.add(partVar);
            }
        }

        if (!sdParts.isEmpty()) {
            // 固态硬盘最多只能选中一种
            LinearExprBuilder sdSelectedSum = LinearExpr.newBuilder();
            for (PartVar partVar : sdParts) {
                sdSelectedSum.add(partVar.getIsSelected());
            }
            model.addLessOrEqual(sdSelectedSum.build(), 1);

            // 固态硬盘总数量不超过2块
            LinearExprBuilder sdQtySum = LinearExpr.newBuilder();
            for (PartVar partVar : sdParts) {
                sdQtySum.add(partVar.getQty());
            }
            model.addLessOrEqual(sdQtySum.build(), 2);
        }

        // proRule2: 固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量
        // 解读1：如果指定了容量诉求，优先使用固态品牌满足，如果固态硬盘容量已足够，限制机械硬盘使用
        // 解读2：如果没有指定容量诉求，尽量用固态硬盘满足，限制机械硬盘使用
        // 这里我们通过优化目标来实现优先级规则
        // 在约束求解中，我们可以通过添加约束来限制：如果固态硬盘容量足够，则限制机械硬盘使用
        // 但这个规则更适合通过优化目标来实现，在当前的约束求解框架中，我们先不实现这个规则
        // 因为这是一个优先级规则，需要在优化阶段处理
    }

    /**
     * 求解模型
     */
    private ProductResult solveModel(CpModel model, List<PartVar> partVars) {
        CpSolver solver = new CpSolver();
        solver.getParameters().setEnumerateAllSolutions(true);
        solver.getParameters().setNumSearchWorkers(1);

        List<List<ProductResult.PartVarSolution>> solutions = new ArrayList<>();

        SolutionCallback callback = new SolutionCallback(partVars, solutions);
        CpSolverStatus status = solver.solve(model, callback);

        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            log.info("Solver found {} solutions", solutions.size());
            // 对解按优先级规则排序
            sortSolutionsByPriority(solutions, partVars);
            return ProductResult.success(solutions);
        } else if (status == CpSolverStatus.INFEASIBLE) {
            log.warn("No feasible solution found");
            return ProductResult.failed("No feasible solution found");
        } else {
            log.error("Solver failed with status: {}", status);
            return ProductResult.failed("Solver failed with status: " + status);
        }
    }

    /**
     * 对解按优先级规则排序
     * 根据 ProductRule2：固态硬盘优先匹配高速率容量
     * 排序规则：
     * 1. 优先使用固态硬盘（sd）而非机械硬盘（md）
     * 2. 优先使用高速率、高容量的固态硬盘
     * 3. 尽量少使用机械硬盘
     */
    private void sortSolutionsByPriority(List<List<ProductResult.PartVarSolution>> solutions,
            List<PartVar> partVars) {
        // 创建 partVar 映射，方便查找
        Map<String, PartVar> partVarMap = new HashMap<>();
        for (PartVar partVar : partVars) {
            partVarMap.put(partVar.getCode(), partVar);
        }

        solutions.sort((s1, s2) -> {
            // 计算解的优先级分数
            int score1 = calculateSolutionScore(s1, partVarMap);
            int score2 = calculateSolutionScore(s2, partVarMap);
            // 降序排列，分数高的在前
            return Integer.compare(score2, score1);
        });
    }

    /**
     * 计算解的优先级分数
     * 分数越高，优先级越高
     * 
     * @param solution   解
     * @param partVarMap 部件变量映射
     * @return 优先级分数
     */
    private int calculateSolutionScore(List<ProductResult.PartVarSolution> solution,
            Map<String, PartVar> partVarMap) {
        int score = 0;
        int totalSdCapacity = 0;

        int totalSdSpeed = 0;

        int totalMdQty = 0;

        int totalSdQty = 0;

        for (ProductResult.PartVarSolution partSolution : solution) {
            PartVar partVar = partVarMap.get(partSolution.getCode());
            if (partVar == null) {
                continue;
            }

            int qty = partSolution.getQty();
            if (qty <= 0) {
                continue;
            }

            String type = partVar.getPart().getType();
            if ("sd".equals(type)) {
                // 固态硬盘：容量和速率越高，分数越高
                totalSdCapacity += partVar.getCapacity() * qty;
                totalSdSpeed += Integer.parseInt(partVar.getSpeed()) * qty;
                totalSdQty += qty;
            } else if ("md".equals(type)) {
                // 机械硬盘：使用越少越好，所以扣分
                totalMdQty += qty;
            }
        }

        // 计算分数：
        // 根据 ProductRule2：固态硬盘优先匹配高速率容量
        // 排序规则：
        // 1. 优先使用固态硬盘而非机械硬盘（如果使用了机械硬盘，大幅扣分）
        // 2. 固态硬盘的速率越高越好（优先匹配高速率）
        // 3. 固态硬盘的容量越高越好（优先匹配高容量）
        // 4. 固态硬盘数量越少越好（在满足容量要求的前提下，用更少的块数更好）

        if (totalMdQty > 0) {
            // 如果使用了机械硬盘，大幅扣分
            score = -1000000 + totalSdSpeed * 100 + totalSdCapacity * 10 - totalMdQty * 1000;
        } else {
            // 只使用固态硬盘，按速率和容量排序
            // 速率权重更高，因为优先匹配高速率
            score = totalSdSpeed * 1000 + totalSdCapacity * 100 - totalSdQty;
        }

        log.debug("Solution score: {} (sdCapacity: {}, sdSpeed: {}, sdQty: {}, mdQty: {})", score,
                totalSdCapacity, totalSdSpeed, totalSdQty, totalMdQty);

        return score;
    }

    /**
     * 解决方案回调
     */
    private static class SolutionCallback extends CpSolverSolutionCallback {
        private final List<PartVar> partVars;
        private final List<List<ProductResult.PartVarSolution>> solutions;
        private int solutionCount = 0;
        private static final int MAX_SOLUTIONS = 100;

        public SolutionCallback(List<PartVar> partVars, List<List<ProductResult.PartVarSolution>> solutions) {
            this.partVars = partVars;
            this.solutions = solutions;
        }

        @Override
        public void onSolutionCallback() {
            if (solutionCount >= MAX_SOLUTIONS) {
                stopSearch();
                return;
            }

            List<ProductResult.PartVarSolution> solution = new ArrayList<>();
            for (PartVar partVar : partVars) {
                long qtyValue = value(partVar.getQty());
                long isSelectedValue = value(partVar.getIsSelected());

                if (qtyValue > 0 || isSelectedValue > 0) {
                    ProductResult.PartVarSolution partSolution = new ProductResult.PartVarSolution();
                    partSolution.setCode(partVar.getCode());
                    partSolution.setQty((int) qtyValue);
                    partSolution.setSelected(isSelectedValue > 0);
                    solution.add(partSolution);
                }
            }

            solutions.add(solution);
            solutionCount++;
        }
    }

    /**
     * 将Part包装为Extensible以便使用FilterExpressionExecutor
     */
    private static class ExtensiblePart extends Extensible {
        private final Part part;

        public ExtensiblePart(Part part) {
            this.part = part;
            // 设置扩展属性
            setExtAttr("Speed", part.getSpeed());
            setExtAttr("Capacity", String.valueOf(part.getCapacity()));
            setExtAttr("Type", part.getType());
        }

        public Part getPart() {
            return part;
        }

    }
}
