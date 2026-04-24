# RFC-0001: 基于部件分类的决策策略（搜索策略）

> 状态：草案（Draft）
> 日期：2026-04-24

---

## 1. 摘要

本RFC提议在模块配置输入中新增**决策策略**（Decision Strategy）参数，允许用户按部件分类指定变量分支的优先级和排序方式。该功能可确保在多解枚举时，优先返回符合业务偏好的可行解（如价格最低解）。

## 2. 动机

### 2.1 问题背景

当前约束求解器在枚举多解时，变量的分支顺序由求解器内部算法决定，输出顺序不可控。例如：

**场景**：电脑配置
- CPU 选项：CPU1（¥10）、CPU2（¥5）、CPU3（¥100）、CPU4（¥50）、CPU5（¥200）
- Disk 选项：Disk1、Disk2、Disk3...

当用户要求枚举前20个可行解时，由于分支顺序不确定，最优解（CPU2，价格最低）可能排在第20位之后，导致用户无法获取期望的结果。

### 2.2 用户诉求

用户希望能按部件分类指定搜索策略，例如：
- 指定 CPU 分类按 **价格升序** 搜索
- 指定 Disk 分类按 **价格降序** 搜索

这样可以确保在有限的解空间内，优先返回价格最优的组合。

### 2.3 业界参考

Google OR-Tools CP-SAT 提供了 `addDecisionStrategy()` API，支持按指定顺序和策略处理变量组：

```java
// 先处理 CPU 变量
model.addDecisionStrategy(cpuVars,
    DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
    DecisionStrategyProto.DomainReductionStrategy.SELECT_MIN_VALUE);

// 再处理 Disk 变量
model.addDecisionStrategy(diskVars,
    DecisionStrategyProto.VariableSelectionStrategy.CHOOSE_FIRST,
    DecisionStrategyProto.DomainReductionStrategy.SELECT_MAX_VALUE);
```

## 3. 设计方案

### 3.1 核心概念

#### 3.1.1 决策策略（DecisionStrategy）

决策策略包含以下维度：

| 维度 | 选项 | 说明 |
|------|------|------|
| **策略类型** | `UNSPECIFIED` | 不指定，使用默认行为 |
| | `ASCENDING` | 升序排列（SELECT_MIN_VALUE） |
| | `DESCENDING` | 降序排列（SELECT_MAX_VALUE） |
| **排序属性** | 属性代码 | 基于哪个属性进行排序 |

> **支持排序的属性类型**：
> - Part 的基础整数属性（如 `price`、`quantity`）
> - Part 的 `dynAttrs` 动态属性（需为整数类型）

#### 3.1.2 决策策略配置（StrategyConfig）

```java
public class StrategyConfig {
    /** 策略类型：ASCENDING / DESCENDING / UNSPECIFIED */
    private StrategyType strategyType;

    /** 排序属性代码（如 "price"、"capacity"、"speed"） */
    private String sortAttributeCode;
}

public enum StrategyType {
    UNSPECIFIED,  // 不指定，使用默认行为
    ASCENDING,     // 升序
    DESCENDING     // 降序
}
```

**设计说明**：
- `StrategyConfig` 不需要 `partCategoryCode`，因为它是通过 `PartConstraintReq.decisionStrategies` 关联的
- 每个 `PartConstraintReq` 可以包含多个 `StrategyConfig`，用于指定该部件分类的变量排序方式

### 3.2 接口设计

#### 3.2.1 扩展 PartConstraintReq

决策策略直接添加到 `PartConstraintReq` 中，每个部件约束请求可以单独指定策略。

```java
public class PartConstraintReq {
    // ... 现有字段 ...

    /**
     * 决策策略列表
     * 用于指定该部件分类的变量分支顺序
     */
    private List<StrategyConfig> decisionStrategies;
}
```

**设计说明**：
- 策略是针对每个部件约束请求的，而非全局配置
- 多实例场景下，可以为不同的实例请求设置不同的策略
- 如果未指定策略，则使用默认行为

#### 3.2.2 策略配置示例

```java
// 单个部件约束请求指定策略
PartConstraintReq req = new PartConstraintReq();
req.setPartCategoryCode("drive");
req.setAttrCode("Sum_Capacity");
req.setAttrComparator(">=");
req.setAttrValue("5");
req.setAttrWhereCondition("Speed=5400");

// 设置决策策略
List<StrategyConfig> strategies = new ArrayList<>();
strategies.add(new StrategyConfig(StrategyType.ASCENDING, "price"));
req.setDecisionStrategies(strategies);
```

**JSON 格式示例**：
```json
{
  "partConstraintReqs": [
    {
      "partCategoryCode": "drive",
      "attrCode": "Sum_Capacity",
      "attrComparator": ">=",
      "attrValue": "5",
      "attrWhereCondition": "Speed=5400",
      "decisionStrategies": [
        {
          "strategyType": "ASCENDING",
          "sortAttributeCode": "price"
        }
      ]
    }
  ]
}
```

#### 3.2.3 测试框架调用方式

参考现有测试框架 `ModuleScenarioTestBase.inferRecommendModule()` 的设计，策略配置可以通过字符串语法扩展：

**现有调用方式**：
```java
// 现有约束请求语法
inferRecommendModule(
    "drive:Sum_Capacity >=5 where Speed=5400",
    "cpu:Sum_Memory >=512 where CoreNum=4"
);
```

**扩展后的调用方式**：
```java
// 带策略配置的约束请求语法
inferRecommendModule(
    "drive:Sum_Capacity >=5 where Speed=5400 [strategy=ASCENDING:price]",
    "cpu:Sum_Memory >=512 where CoreNum=4"
);
```

**策略语法扩展**：
```
[strategy=<类型>:<属性>]
  - 类型: ASCENDING | DESCENDING | UNSPECIFIED
  - 属性: price | <dynAttr代码>
```

**示例**：
```java
// 示例1：单个分类指定策略
inferRecommendModule(
    "drive:Sum_Capacity >=5 where Speed=5400 [strategy=ASCENDING:price]"
);

// 示例2：多个分类指定策略
inferRecommendModule(
    "drive:Sum_Capacity >=5 where Speed=5400 [strategy=ASCENDING:price]",
    "cpu:Sum_Memory >=512 where CoreNum=4 [strategy=DESCENDING:price]"
);

// 示例3：多实例指定策略
inferRecommendModule(
    "driveI0:Sum_Capacity >=5 where Speed=5400 [strategy=ASCENDING:price]",
    "driveI1:Sum_Capacity >=5 where Speed=7200 [strategy=DESCENDING:price]"
);
```

### 3.3 实现方案

#### 3.3.1 架构层级

按照项目的对称设计模式，决策策略功能涉及以下层级：

```
┌─────────────────────────────────────────────────────────────────────┐
│                      决策策略实现层级                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  InferParasReq                    ← 输入层：携带决策策略配置          │
│       │                                                          │
│       ▼                                                          │
│  ModuleConstraintExecutorImpl     ← 执行器层：传递策略到算法模块      │
│       │                                                          │
│       ▼                                                          │
│  ModuleAlgImpl                     ← 算法层：应用策略到 CP-SAT       │
│       │                                                          │
│       ▼                                                          │
│  AlgCPModel                        ← 模型层：封装 addDecisionStrategy │
│       │                                                          │
│       ▼                                                          │
│  CpModel (OR-Tools)                ← 求解器层：执行决策策略          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 3.3.2 关键实现

**AlgCPModel 新增方法**：

```java
public class AlgCPModel {
    // 现有方法...

    /**
     * 添加决策策略
     *
     * @param variables 目标变量数组
     * @param selectionStrategy 变量选择策略
     * @param reductionStrategy 域缩减策略（SELECT_MIN_VALUE / SELECT_MAX_VALUE）
     */
    public void addDecisionStrategy(IntVar[] variables,
            VariableSelectionStrategy selectionStrategy,
            DomainReductionStrategy reductionStrategy) {
        // 封装 OR-Tools 的 addDecisionStrategy
    }
}
```

**ModuleAlgImpl 应用策略**：

```java
public class ModuleAlgImpl {
    private void applyDecisionStrategies(PartConstraintReq req, List<StrategyConfig> strategies) {
        // 1. 获取目标部件分类代码（从 PartConstraintReq 获取）
        String partCategoryCode = req.getPartCategoryCode();

        // 2. 查找目标部件分类的算法模块
        PartCategoryAlgImpl pcAlg = findPartCategoryAlg(partCategoryCode);
        if (pcAlg == null) {
            // 分类不存在，忽略该策略
            return;
        }

        for (StrategyConfig config : strategies) {
            // 3. 获取目标变量
            IntVar[] targetVars = pcAlg.getVariables();

            // 4. 确定域缩减策略
            DomainReductionStrategy reductionStrategy =
                config.getStrategyType() == StrategyType.ASCENDING
                    ? DomainReductionStrategy.SELECT_MIN_VALUE
                    : DomainReductionStrategy.SELECT_MAX_VALUE;

            // 5. 添加决策策略
            model.addDecisionStrategy(
                targetVars,
                VariableSelectionStrategy.CHOOSE_FIRST,
                reductionStrategy
            );
        }
    }
}
```

**调用流程**：

```java
// 在遍历 PartConstraintReqs 时调用
for (PartConstraintReq req : partConstraintReqs) {
    List<StrategyConfig> strategies = req.getDecisionStrategies();
    if (strategies != null && !strategies.isEmpty()) {
        applyDecisionStrategies(req, strategies);
    }
}
```

#### 3.3.3 多实例场景处理

对于 `supportMultiInst=true` 的部件分类，决策策略**统一应用于该分类的所有实例**：

```
PartCategory "disk" (supportMultiInst=true)
       │
       ├── Instance 1: DiskVar[]
       ├── Instance 2: DiskVar[]
       └── Instance 3: DiskVar[]
              │
              ▼
       所有实例变量都应用相同的排序策略
```

**实现要点**：
- `PartCategoryAlgImpl` 在多实例模式下会为每个实例创建独立的变量组
- 应用策略时，需要收集所有实例的变量，统一添加决策策略
- OR-Tools 的 `addDecisionStrategy` 支持一次性传入多个变量

### 3.4 处理流程

```
inferParas(req)
        │
        ▼
┌───────────────────────────────────────┐
│ 1. 解析 PartConstraintReqs            │
│    - 遍历每个部件约束请求              │
│    - 提取其中的 decisionStrategies    │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│ 2. 为每个策略应用 DecisionStrategy     │
│    - 查找策略指定的部件分类算法模块     │
│    - 获取该分类所有实例的变量          │
│    - 根据排序属性对变量进行排序         │
│    - 添加 DecisionStrategy            │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│ 3. CP-SAT 求解（按策略顺序分支）         │
└───────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────┐
│ 4. 返回有序的可行解                     │
└───────────────────────────────────────┘
```

**数据流向**：
```
InferParasReq
    └── List<PartConstraintReq>
            ├── PartConstraintReq #1
            │       └── List<StrategyConfig>  →  应用到对应的 PartCategoryAlgImpl
            ├── PartConstraintReq #2
            │       └── List<StrategyConfig>  →  应用到对应的 PartCategoryAlgImpl
            └── ...
```

## 4. 验收准则

### 4.1 功能验收用例

#### SEARCH-001：按部件分类指定升序策略

**目的**：验证指定 CPU 分类按价格升序搜索

**测试数据（极简）**：
```java
@PartAnno(code = "cpu")
private PartCategoryVar cpu;

@PartAnno(fatherCode = "cpu", attrs = {"10"}, price = 10)  // CPU1 价格10
private PartVar cpu1;

@PartAnno(fatherCode = "cpu", attrs = {"5"}, price = 5)   // CPU2 价格5
private PartVar cpu2;

@PartAnno(fatherCode = "cpu", attrs = {"100"}, price = 100) // CPU3 价格100
private PartVar cpu3;

@PartAnno(code = "disk")
private PartCategoryVar disk;

@PartAnno(fatherCode = "disk", attrs = {"50"})
private PartVar disk1;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-001-1 | CPU: ASCENDING, price | 前几个解应包含 CPU2（价格5） |
| SEARCH-001-2 | CPU: DESCENDING, price | 前几个解应包含 CPU3（价格100） |
| SEARCH-001-3 | 无策略配置 | 解顺序不确定（回归测试） |

#### SEARCH-002：多分类策略组合

**目的**：验证多个部件分类的策略组合

**测试数据（极简）**：
```java
@PartAnno(code = "cpu")
private PartCategoryVar cpu;
// cpu1(¥10), cpu2(¥5), cpu3(¥100)

@PartAnno(code = "disk")
private PartCategoryVar disk;
// disk1(¥50), disk2(¥30)
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-002-1 | CPU:ASCENDING + Disk:ASCENDING | 优先返回 (CPU2, Disk2) |
| SEARCH-002-2 | CPU:DESCENDING + Disk:DESCENDING | 优先返回 (CPU3, Disk1) |
| SEARCH-002-3 | CPU:ASCENDING + Disk:DESCENDING | 优先返回 (CPU2, Disk1) |

#### SEARCH-003：多实例分类的决策策略

**目的**：验证 `supportMultiInst=true` 的分类，策略**统一应用于所有实例**。每个实例都按设置的策略构建变量。

**测试数据（极简）**：

```java
@PartAnno(code = "gpu", supportMultiInst = true)
private PartCategoryVar gpu;

// 每个实例可独立配置部件，这里简化为每个实例只有一个可选部件
// 实例1: GPU1(¥50), 实例2: GPU2(¥80), 实例3: GPU3(¥120)
@PartAnno(fatherCode = "gpu", attrs = {"GPU1"}, price = 50)
private PartVar gpu1;

@PartAnno(fatherCode = "gpu", attrs = {"GPU2"}, price = 80)
private PartVar gpu2;

@PartAnno(fatherCode = "gpu", attrs = {"GPU3"}, price = 120)
private PartVar gpu3;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-003-1 | GPU: ASCENDING, price | 所有3个实例都按价格升序（GPU1→GPU2→GPU3） |
| SEARCH-003-2 | GPU: DESCENDING, price | 所有3个实例都按价格降序（GPU3→GPU2→GPU1） |

**验证逻辑**：
- ASCENDING 时：求解器优先探索价格低的变量（GPU1→GPU2→GPU3）
- DESCENDING 时：求解器优先探索价格高的变量（GPU3→GPU2→GPU1）

---

#### SEARCH-004：多实例 + 多部件组合

**目的**：验证每个实例内有多个可选部件时的排序策略

**测试数据（极简）**：

```java
@PartAnno(code = "gpu", supportMultiInst = true)
private PartCategoryVar gpu;

// 实例1 可选部件：GPU1A(¥50), GPU1B(¥70)
@PartAnno(fatherCode = "gpu", attrs = {"1", "A"}, price = 50)
private PartVar gpu1A;

@PartAnno(fatherCode = "gpu", attrs = {"1", "B"}, price = 70)
private PartVar gpu1B;

// 实例2 可选部件：GPU2A(¥60), GPU2B(¥90)
@PartAnno(fatherCode = "gpu", attrs = {"2", "A"}, price = 60)
private PartVar gpu2A;

@PartAnno(fatherCode = "gpu", attrs = {"2", "B"}, price = 90)
private PartVar gpu2B;

// 实例3 可选部件：GPU3A(¥80), GPU3B(¥110)
@PartAnno(fatherCode = "gpu", attrs = {"3", "A"}, price = 80)
private PartVar gpu3A;

@PartAnno(fatherCode = "gpu", attrs = {"3", "B"}, price = 110)
private PartVar gpu3B;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-004-1 | GPU: ASCENDING, price | 每个实例内都按升序：GPU1A(50)→GPU1B(70)，GPU2A(60)→GPU2B(90)... |
| SEARCH-004-2 | GPU: DESCENDING, price | 每个实例内都按降序：GPU1B(70)→GPU1A(50)，GPU2B(90)→GPU2A(60)... |

**验证逻辑**：
- ASCENDING：每个实例内的变量按价格从低到高排列
- DESCENDING：每个实例内的变量按价格从高到低排列

---

#### SEARCH-005：多实例 + 单实例混合场景

**目的**：验证单实例分类和多实例分类同时存在时的策略应用

**测试数据（极简）**：

```java
// 单实例分类
@PartAnno(code = "cpu")
private PartCategoryVar cpu;
// cpu1(¥10), cpu2(¥5), cpu3(¥100)

// 多实例分类
@PartAnno(code = "gpu", supportMultiInst = true)
private PartCategoryVar gpu;
// 实例1: gpu1(¥50), 实例2: gpu2(¥80), 实例3: gpu3(¥120)
@PartAnno(fatherCode = "gpu", attrs = {"1"}, price = 50)
private PartVar gpu1;
@PartAnno(fatherCode = "gpu", attrs = {"2"}, price = 80)
private PartVar gpu2;
@PartAnno(fatherCode = "gpu", attrs = {"3"}, price = 120)
private PartVar gpu3;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-005-1 | CPU: ASCENDING | 仅 CPU 按价格升序，GPU 不受影响 |
| SEARCH-005-2 | GPU: ASCENDING | 仅 GPU 所有实例按价格升序，CPU 不受影响 |
| SEARCH-005-3 | CPU: ASCENDING + GPU: ASCENDING | 两者都按价格升序 |

**验证逻辑**：
- 各分类的策略相互独立，互不影响
- 未配置策略的分类使用默认行为 |

---

#### SEARCH-006：基于 dynAttrs 动态属性排序

**目的**：验证使用部件的动态属性进行排序

**测试数据（极简）**：
```java
@PartAnno(code = "cpu")
private PartCategoryVar cpu;

@PartAnno(code = "core", fatherCode = "cpu")
@DAttrAnno(code = "CoreCount", options = {"4:4", "8:8", "16:16"})
private PartCategoryVar core;

@PartAnno(fatherCode = "core", attrs = {"4"})
private PartVar core4;

@PartAnno(fatherCode = "core", attrs = {"8"})
private PartVar core8;

@PartAnno(fatherCode = "core", attrs = {"16"})
private PartVar core16;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-006-1 | CoreCount: ASCENDING | 优先选择 CoreCount=4 的部件 |
| SEARCH-006-2 | CoreCount: DESCENDING | 优先选择 CoreCount=16 的部件 |

---

#### SEARCH-007：多实例 + 动态属性排序

**目的**：验证多实例分类使用 dynAttrs 动态属性进行排序

**测试数据（极简）**：

```java
@PartAnno(code = "disk", supportMultiInst = true)
@DAttrAnno(code = "Speed", options = {"5400:5400", "7200:7200"})
private PartCategoryVar disk;

// 实例1
@PartAnno(fatherCode = "disk", attrs = {"5400"}, price = 100)
private PartVar disk1_5400;

@PartAnno(fatherCode = "disk", attrs = {"7200"}, price = 150)
private PartVar disk1_7200;

// 实例2
@PartAnno(fatherCode = "disk", attrs = {"5400"}, price = 110)
private PartVar disk2_5400;

@PartAnno(fatherCode = "disk", attrs = {"7200"}, price = 160)
private PartVar disk2_7200;
```

**测试用例**：

| ID | 策略配置 | 预期行为 |
|----|----------|----------|
| SEARCH-007-1 | Speed: ASCENDING | 所有实例内优先选择 Speed=5400 的部件 |
| SEARCH-007-2 | Speed: DESCENDING | 所有实例内优先选择 Speed=7200 的部件 |

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
|------|------|----------|
| 策略指定的分类不存在 | `PartConstraintReq.partCategoryCode = "nonexist"` | 忽略，跳过该策略 |
| 排序属性不存在或非数值 | `sortAttributeCode = "nonexist"` | 使用默认排序 |
| 策略类型为 UNSPECIFIED | `strategyType = UNSPECIFIED` | 使用默认行为 |
| 策略列表为空 | `decisionStrategies = []` | 使用默认行为 |

### 4.3 回归测试

确保原有功能不受影响：
- 现有枚举多解功能正常
- 无策略配置时行为不变

---

## 5. 未解决的问题

无。（已确认所有问题）

---

## 6. 实现计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| 1 | 定义 `StrategyConfig` 数据结构和 `StrategyType` 枚举 | P0 |
| 2 | 扩展 `PartConstraintReq` 添加 `decisionStrategies` 字段 | P0 |
| 3 | 在 `AlgCPModel` 中封装 `addDecisionStrategy` | P0 |
| 4 | 在 `ModuleAlgImpl` 中实现策略应用逻辑 | P0 |
| 5 | 处理多实例场景：收集所有实例变量后统一应用策略 | P0 |
| 6 | 编写单元测试 `SearchStrategyTest` | P1 |
| 6.1 | 单实例分类的决策策略测试 | P1 |
| 6.2 | 多实例分类的决策策略测试（**重点**） | P1 |
| 6.3 | 多分类策略组合测试 | P1 |
| 6.4 | 动态属性排序测试 | P1 |
| 6.5 | 边界条件测试 | P1 |
| 7 | 验证现有功能回归 | P1 |

---

## 7. 参考资料

- Google OR-Tools CP-SAT: Decision Strategies
- Rust RFC Format: https://github.com/rust-lang/rfcs

---

## 附录

### A. 与 PriorityRule 的关系

> **说明**：本RFC引入的 `DecisionStrategy` 与现有的 `PriorityRule` 是**独立功能**，不存在交互或冲突。
>
> - `DecisionStrategy`：控制变量分支/搜索顺序，影响多解枚举时的解的顺序
> - `PriorityRule`：定义优化目标（如最小化成本、最大化性能），影响最优解的判定
>
> 两者可独立使用，互不影响。

### B. 性能说明

> 本RFC暂不考虑性能优化，后续可根据实际使用情况进行评估和优化。
