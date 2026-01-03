package com.jmix.temp;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//https://chat.deepseek.com/a/chat/s/b5d179a6-48d7-4b11-97cc-81794df8bd9a
public class ComputerConfigurationModelV2 {
    static {
        Loader.loadNativeLibraries();
    }

    static class Part {
        String code;

        String fatherCode;

        PartType partType;

        Map<String, String> dynAttr = new HashMap<>();

        List<DynamicAttribute> dynAttrSchemas = new ArrayList<>();

        Integer maxQuantity = Integer.MAX_VALUE;

        Long price = 0L;

        List<Map<String, String>> instanceGroups = new ArrayList<>();
    }

    enum PartType {
        ATOMIC, CATEGORY, BUNDLE, GROUP
    }

    static class DynamicAttribute {
        String code;

        String name;

        DynamicAttributeType dynAttrType;

        String value;
    }

    enum DynamicAttributeType {
        STRING, INTEGER, DOUBLE, ENUM
    }

    private CpModel model;

    private CpSolver solver;

    private Map<String, IntVar> partVars = new HashMap<>();

    private Map<String, IntVar> ssdInstanceGroupVars = new HashMap<>();

    private Map<String, IntVar> hddInstanceGroupVars = new HashMap<>();

    private List<Part> allParts = new ArrayList<>();

    private Map<String, Part> partMap = new HashMap<>();

    // 更新后的用户需求
    private int requiredSsd5400Count = 2;

    private int required5400CapacityTB = 11; // 更新为11T

    private boolean require8Core128GCPU = true;

    public ComputerConfigurationModelV2() {
        model = new CpModel();
        solver = new CpSolver();
        initializeParts();
    }

    private void initializeParts() {
        // 固态硬盘
        Part ssd1 = createSSD("SSD1", Arrays.asList(
                createInstanceGroup("1", "5400", "2T"),
                createInstanceGroup("2", "7200/5400", "1T")));

        Part ssd2 = createSSD("SSD2", Arrays.asList(
                createInstanceGroup("1", "7200/5400", "4T")));

        Part ssd3 = createSSD("SSD3", Arrays.asList(
                createInstanceGroup("1", "9000", "4T")));

        // 机械硬盘
        Part hdd1 = createHDD("HDD1", Arrays.asList(
                createInstanceGroup("1", "5400", "2T")), 4);

        Part hdd2 = createHDD("HDD2", Arrays.asList(
                createInstanceGroup("4", "7200", "2T")), 4);

        Part hdd3 = createHDD("HDD3", Arrays.asList(
                createInstanceGroup("1", "9000", "2T")), 4);

        // 新增更高容量的硬盘以满足11T需求
        Part hdd4 = createHDD("HDD4", Arrays.asList(
                createInstanceGroup("5", "5400", "4T")), 4);

        Part hdd5 = createHDD("HDD5", Arrays.asList(
                createInstanceGroup("6", "5400", "8T")), 2); // 8T硬盘最多2块

        Collections.addAll(allParts, ssd1, ssd2, ssd3, hdd1, hdd2, hdd3, hdd4, hdd5);
        for (Part part : allParts) {
            partMap.put(part.code, part);
        }
    }

    private Part createSSD(String code, List<Map<String, String>> groups) {
        Part ssd = new Part();
        ssd.code = code;
        ssd.fatherCode = "SolidStateDrive";
        ssd.partType = PartType.ATOMIC;
        ssd.dynAttr.put("接口带宽", "8GB/S");
        ssd.maxQuantity = 2;
        ssd.instanceGroups = groups;
        return ssd;
    }

    private Part createHDD(String code, List<Map<String, String>> groups, int maxQty) {
        Part hdd = new Part();
        hdd.code = code;
        hdd.fatherCode = "MechanicalDrive";
        hdd.partType = PartType.ATOMIC;
        hdd.maxQuantity = maxQty;
        hdd.instanceGroups = groups;
        return hdd;
    }

    private Map<String, String> createInstanceGroup(String groupId, String speed, String capacity) {
        Map<String, String> group = new HashMap<>();
        group.put("实例分组", groupId);
        group.put("转速", speed);
        group.put("容量", capacity);
        return group;
    }

    public void buildModel() {
        createDecisionVariables();
        addConstraints();
        setOptimizationObjective();
    }

    private void createDecisionVariables() {
        // 部件数量变量
        for (Part part : allParts) {
            String key = part.code;
            IntVar var = model.newIntVar(0, part.maxQuantity, key + "_count");
            partVars.put(key, var);
        }

        // SSD实例分组变量
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    String key = part.code + "_group_" + i;
                    IntVar var = model.newIntVar(0, part.maxQuantity, key);
                    ssdInstanceGroupVars.put(key, var);
                }
            }
        }

        // HDD实例分组变量
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    String key = part.code + "_group_" + i;
                    IntVar var = model.newIntVar(0, part.maxQuantity, key);
                    hddInstanceGroupVars.put(key, var);
                }
            }
        }
    }

    private void addConstraints() {
        // 约束1: SSD必须同一种且最多2块
        addSsdSameTypeAndMaxConstraint();

        // 约束2: 5400速率SSD数量要求
        add5400SsdCountConstraint();

        // 约束3: 5400速率总容量≥11T
        add5400CapacityConstraint();

        // 约束4: SSD优先使用
        addSsdPriorityConstraint();

        // 约束5: 排除不包含5400的SSD
        addSsd5400CompatibilityConstraint();

        // 约束6: 实例分组与部件数量一致性
        addInstanceConsistencyConstraints();

        // 约束7: 固态硬盘优先匹配高速率容量
        addSsdHighSpeedPriorityConstraint();

        // 约束8: 机械硬盘补充低速率容量
        addHddLowSpeedConstraint();
    }

    private void addSsdSameTypeAndMaxConstraint() {
        List<IntVar> ssdSelectors = new ArrayList<>();
        int totalSsdSelected = 0;

        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                String key = part.code + "_selected";
                BoolVar selected = model.newBoolVar(key);
                ssdSelectors.add(selected);

                IntVar count = partVars.get(part.code);
                // 如果选中，数量必须>0
                model.addGreaterThan(count, 0).onlyEnforceIf(selected);
                model.addEquality(count, 0).onlyEnforceIf(selected.not());

                totalSsdSelected += 1;
            }
        }

        // 最多选择一种SSD
        model.addLessOrEqual(LinearExpr.sum(ssdSelectors.toArray(new IntVar[0])), 1);

        // SSD总数不超过2
        LinearExprBuilder totalSsd = LinearExpr.newBuilder();
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                totalSsd.add(partVars.get(part.code));
            }

        }
        model.addLessOrEqual(totalSsd.build(), 2);
    }

    private void add5400SsdCountConstraint() {
        LinearExprBuilder ssd5400Count = LinearExpr.newBuilder();

        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    Map<String, String> group = part.instanceGroups.get(i);
                    if (group.get("转速").contains("5400")) {
                        String key = part.code + "_group_" + i;
                        ssd5400Count.add(ssdInstanceGroupVars.get(key));
                    }
                }
            }
        }

        model.addEquality(ssd5400Count.build(), requiredSsd5400Count);
    }

    private void add5400CapacityConstraint() {
        LinearExprBuilder total5400Capacity = LinearExpr.newBuilder();

        // SSD 5400容量
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    Map<String, String> group = part.instanceGroups.get(i);
                    if (group.get("转速").contains("5400")) {
                        int capacityTB = parseCapacityTB(group.get("容量"));
                        String key = part.code + "_group_" + i;
                        total5400Capacity.addTerm(ssdInstanceGroupVars.get(key), capacityTB);
                    }
                }
            }
        }

        // HDD 5400容量
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    Map<String, String> group = part.instanceGroups.get(i);
                    if (group.get("转速").equals("5400")) {
                        int capacityTB = parseCapacityTB(group.get("容量"));
                        String key = part.code + "_group_" + i;
                        total5400Capacity.addTerm(hddInstanceGroupVars.get(key), capacityTB);
                    }
                }
            }
        }

        model.addGreaterOrEqual(total5400Capacity.build(), required5400CapacityTB);
    }

    private void addSsdPriorityConstraint() {
        // 优先使用固态硬盘：如果固态硬盘容量已足够，限制机械硬盘使用
        LinearExprBuilder ssd5400Capacity = LinearExpr.newBuilder();

        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    Map<String, String> group = part.instanceGroups.get(i);
                    if (group.get("转速").contains("5400")) {
                        int capacityTB = parseCapacityTB(group.get("容量"));
                        String key = part.code + "_group_" + i;
                        ssd5400Capacity.addTerm(ssdInstanceGroupVars.get(key), capacityTB);
                    }
                }
            }
        }

        // 如果SSD容量足够，限制HDD数量
        BoolVar ssdCapacityEnough = model.newBoolVar("ssd_capacity_enough");
        model.addGreaterOrEqual(ssd5400Capacity.build(), required5400CapacityTB)
                .onlyEnforceIf(ssdCapacityEnough);

        // 当SSD容量足够时，限制HDD数量为0
        LinearExprBuilder totalHdd = LinearExpr.newBuilder();
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                totalHdd.add(partVars.get(part.code));
            }
        }
        model.addEquality(totalHdd.build(), 0).onlyEnforceIf(ssdCapacityEnough);
    }

    private void addSsd5400CompatibilityConstraint() {
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                boolean has5400 = false;
                for (Map<String, String> group : part.instanceGroups) {
                    if (group.get("转速").contains("5400")) {
                        has5400 = true;
                        break;
                    }
                }

                if (!has5400) {
                    IntVar count = partVars.get(part.code);
                    model.addEquality(count, 0);
                }
            }
        }
    }

    private void addInstanceConsistencyConstraints() {
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                LinearExprBuilder groupSum = LinearExpr.newBuilder();
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    String key = part.code + "_group_" + i;
                    groupSum.add(ssdInstanceGroupVars.get(key));
                }
                model.addEquality(groupSum.build(), partVars.get(part.code));
            } else if ("MechanicalDrive".equals(part.fatherCode)) {
                LinearExprBuilder groupSum = LinearExpr.newBuilder();
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    String key = part.code + "_group_" + i;
                    groupSum.add(hddInstanceGroupVars.get(key));
                }
                model.addEquality(groupSum.build(), partVars.get(part.code));
            }
        }
    }

    private void addSsdHighSpeedPriorityConstraint() {
        // 优先选择高速率容量：对于同时包含高低速率的SSD，优先分配高速率容量
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode) && part.instanceGroups.size() > 1) {
                // 如果有多个实例分组，优先分配高速率（容量更大）的分组
                // 这里假设实例分组按容量排序，容量大的优先级高
                List<Map<String, String>> sortedGroups = new ArrayList<>(part.instanceGroups);
                sortedGroups.sort((g1, g2) -> parseCapacityTB(g2.get("容量")) - parseCapacityTB(g1.get("容量")));

                // 确保容量大的分组优先使用
                for (int i = 0; i < sortedGroups.size() - 1; i++) {
                    // 找到对应分组的变量
                    String highCapKey = findGroupKey(part.code, sortedGroups.get(i));
                    String lowCapKey = findGroupKey(part.code, sortedGroups.get(i + 1));

                    if (highCapKey != null && lowCapKey != null) {
                        IntVar highCapVar = ssdInstanceGroupVars.get(highCapKey);
                        IntVar lowCapVar = ssdInstanceGroupVars.get(lowCapKey);

                        // 先尽量使用高容量分组
                        BoolVar highCapUsed = model.newBoolVar(part.code + "_high_used");
                        model.addGreaterThan(highCapVar, 0).onlyEnforceIf(highCapUsed);
                        model.addEquality(highCapVar, 0).onlyEnforceIf(highCapUsed.not());

                        // 如果高容量分组还有剩余空间，则限制低容量分组的使用
                        IntVar partCount = partVars.get(part.code);
                        IntVar highCapRemaining = model.newIntVar(0, part.maxQuantity,
                                part.code + "_high_remaining");
                        model.addEquality(highCapRemaining,
                                LinearExpr.newBuilder().add(partCount).addTerm(highCapVar, -1).build());

                        // 只有当高容量分组无法满足需求时才使用低容量分组
                        BoolVar highCapRemainingZero = model.newBoolVar(part.code + "_high_remaining_zero");
                        model.addEquality(highCapRemaining, 0).onlyEnforceIf(highCapRemainingZero);
                        model.addGreaterThan(highCapRemaining, 0).onlyEnforceIf(highCapRemainingZero.not());
                        model.addEquality(lowCapVar, 0).onlyEnforceIf(highCapRemainingZero.not());
                    }
                }
            }
        }
    }

    private void addHddLowSpeedConstraint() {
        // 机械硬盘用于增配低速率容量
        // 这里可以添加逻辑确保HDD只用于补充容量，而不是替代SSD
    }

    private String findGroupKey(String partCode, Map<String, String> group) {
        Part part = partMap.get(partCode);
        if (part != null) {
            for (int i = 0; i < part.instanceGroups.size(); i++) {
                if (part.instanceGroups.get(i).equals(group)) {
                    return partCode + "_group_" + i;
                }
            }
        }
        return null;
    }

    private void setOptimizationObjective() {
        // 多目标优化：
        // 1. 最小化总成本（部件数量）
        // 2. 最小化机械硬盘数量（优先使用SSD）
        // 3. 最大化使用高容量部件

        LinearExprBuilder totalCost = LinearExpr.newBuilder();
        LinearExprBuilder hddPenalty = LinearExpr.newBuilder();
        LinearExprBuilder capacityUtilization = LinearExpr.newBuilder();

        // 成本：总部件数量
        for (Part part : allParts) {
            totalCost.addTerm(partVars.get(part.code), 1);
        }

        // 惩罚机械硬盘使用
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                hddPenalty.addTerm(partVars.get(part.code), 10); // 机械硬盘惩罚权重更高
            } else if ("SolidStateDrive".equals(part.fatherCode)) {
                hddPenalty.addTerm(partVars.get(part.code), -1); // 鼓励使用SSD
            }
        }

        // 鼓励使用高容量部件
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (int i = 0; i < part.instanceGroups.size(); i++) {
                    int capacityTB = parseCapacityTB(part.instanceGroups.get(i).get("容量"));
                    String key = part.code + "_group_" + i;
                    capacityUtilization.addTerm(ssdInstanceGroupVars.get(key), -capacityTB); // 负权重，使容量越大越好
                }
            }
        }

        // 加权组合目标
        LinearExprBuilder objective = LinearExpr.newBuilder();
        objective.add(totalCost.build()); // 最小化总部件数
        objective.addTerm(hddPenalty.build(), 1); // 最小化机械硬盘使用
        objective.addTerm(capacityUtilization.build(), 1); // 最大化容量利用率（使用整数权重）

        model.minimize(objective.build());
    }

    private int parseCapacityTB(String capacityStr) {
        if (capacityStr == null) {
            return 0;
        }
        if (capacityStr.endsWith("T")) {
            return Integer.parseInt(capacityStr.substring(0, capacityStr.length() - 1));
        }
        return 0;
    }

    public void solve() {
        try {
            // 配置求解器参数
            solver.getParameters().setMaxTimeInSeconds(30);
            solver.getParameters().setNumSearchWorkers(8);

            CpSolverStatus status = solver.solve(model);

            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
                printSolution();
            } else {
                System.out.println("未找到可行解");
                analyzeInfeasibility();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printSolution() {
        System.out.println("=== 配置方案（5400速率硬盘容量≥11T）===");
        System.out.println("目标值: " + solver.objectiveValue());

        int totalCost = 0;
        int ssd5400Count = 0;
        int total5400Capacity = 0;

        System.out.println("\n固态硬盘配置:");
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                IntVar countVar = partVars.get(part.code);
                long count = solver.value(countVar);
                if (count > 0) {
                    totalCost += count;
                    System.out.println(part.code + ": " + count + "块");

                    for (int i = 0; i < part.instanceGroups.size(); i++) {
                        String key = part.code + "_group_" + i;
                        IntVar groupVar = ssdInstanceGroupVars.get(key);
                        long groupCount = solver.value(groupVar);
                        if (groupCount > 0) {
                            Map<String, String> group = part.instanceGroups.get(i);
                            int capacityTB = parseCapacityTB(group.get("容量"));
                            System.out.println("  实例分组" + group.get("实例分组") +
                                    ": " + groupCount + "块" +
                                    ", 转速: " + group.get("转速") +
                                    ", 容量: " + group.get("容量"));

                            if (group.get("转速").contains("5400")) {
                                ssd5400Count += groupCount;
                                total5400Capacity += groupCount * capacityTB;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("\n机械硬盘配置:");
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                IntVar countVar = partVars.get(part.code);
                long count = solver.value(countVar);
                if (count > 0) {
                    totalCost += count;
                    System.out.println(part.code + ": " + count + "块");

                    for (int i = 0; i < part.instanceGroups.size(); i++) {
                        String key = part.code + "_group_" + i;
                        IntVar groupVar = hddInstanceGroupVars.get(key);
                        long groupCount = solver.value(groupVar);
                        if (groupCount > 0) {
                            Map<String, String> group = part.instanceGroups.get(i);
                            int capacityTB = parseCapacityTB(group.get("容量"));
                            System.out.println("  实例分组" + group.get("实例分组") +
                                    ": " + groupCount + "块" +
                                    ", 转速: " + group.get("转速") +
                                    ", 容量: " + group.get("容量"));

                            if ("5400".equals(group.get("转速"))) {
                                total5400Capacity += groupCount * capacityTB;
                            }
                        }
                    }
                }
            }
        }

        System.out.println("\n=== 需求满足情况 ===");
        System.out.println("5400速率固态硬盘数量: " + ssd5400Count
                + " / 要求: " + requiredSsd5400Count
                + " [" + (ssd5400Count >= requiredSsd5400Count ? "✓" : "✗") + "]");
        System.out.println("5400速率硬盘总容量: " + total5400Capacity + "TB"
                + " / 要求: ≥" + required5400CapacityTB + "TB"
                + " [" + (total5400Capacity >= required5400CapacityTB ? "✓" : "✗") + "]");
        System.out.println("总部件数: " + totalCost);

        System.out.println("\n=== 推荐配置 ===");
        printRecommendedConfiguration();
    }

    private void printRecommendedConfiguration() {
        // 分析最佳配置方案
        Map<String, Integer> ssdConfig = new HashMap<>();
        Map<String, Integer> hddConfig = new HashMap<>();

        // 收集SSD配置
        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                IntVar countVar = partVars.get(part.code);
                long count = solver.value(countVar);
                if (count > 0) {
                    for (int i = 0; i < part.instanceGroups.size(); i++) {
                        String key = part.code + "_group_" + i;
                        long groupCount = solver.value(ssdInstanceGroupVars.get(key));
                        if (groupCount > 0) {
                            Map<String, String> group = part.instanceGroups.get(i);
                            ssdConfig.put(group.get("转速") + " " + group.get("容量"),
                                    (int) groupCount);
                        }
                    }
                }
            }
        }

        // 收集HDD配置
        for (Part part : allParts) {
            if ("MechanicalDrive".equals(part.fatherCode)) {
                IntVar countVar = partVars.get(part.code);
                long count = solver.value(countVar);
                if (count > 0) {
                    for (int i = 0; i < part.instanceGroups.size(); i++) {
                        String key = part.code + "_group_" + i;
                        long groupCount = solver.value(hddInstanceGroupVars.get(key));
                        if (groupCount > 0) {
                            Map<String, String> group = part.instanceGroups.get(i);
                            hddConfig.put(group.get("转速") + " " + group.get("容量"),
                                    (int) groupCount);
                        }
                    }
                }
            }
        }

        System.out.println("固态硬盘:");
        ssdConfig.forEach((spec, count) -> System.out.println("  " + count + "块 " + spec));

        System.out.println("机械硬盘:");
        hddConfig.forEach((spec, count) -> System.out.println("  " + count + "块 " + spec));
    }

    private void analyzeInfeasibility() {
        System.out.println("\n=== 可行性分析 ===");
        System.out.println("容量要求: " + required5400CapacityTB + "TB");

        // 计算最大可能容量
        int maxSsd5400Capacity = 0;
        int maxHdd5400Capacity = 0;

        for (Part part : allParts) {
            if ("SolidStateDrive".equals(part.fatherCode)) {
                for (Map<String, String> group : part.instanceGroups) {
                    if (group.get("转速").contains("5400")) {
                        int capacity = parseCapacityTB(group.get("容量"));
                        maxSsd5400Capacity += Math.min(2, part.maxQuantity) * capacity;
                    }
                }
            } else if ("MechanicalDrive".equals(part.fatherCode)) {
                for (Map<String, String> group : part.instanceGroups) {
                    if ("5400".equals(group.get("转速"))) {
                        int capacity = parseCapacityTB(group.get("容量"));
                        maxHdd5400Capacity += part.maxQuantity * capacity;
                    }
                }
            }
        }

        System.out.println("最大固态硬盘5400容量: " + maxSsd5400Capacity + "TB");
        System.out.println("最大机械硬盘5400容量: " + maxHdd5400Capacity + "TB");
        System.out.println("总最大5400容量: " + (maxSsd5400Capacity + maxHdd5400Capacity) + "TB");

        if (required5400CapacityTB > maxSsd5400Capacity + maxHdd5400Capacity) {
            System.out.println("错误: 要求的容量(" + required5400CapacityTB +
                    "TB)超过系统最大容量(" +
                    (maxSsd5400Capacity + maxHdd5400Capacity) + "TB)");
        }
    }

    public static void main(String[] args) {

        ComputerConfigurationModelV2 model = new ComputerConfigurationModelV2();
        model.buildModel();
        model.solve();
    }
}