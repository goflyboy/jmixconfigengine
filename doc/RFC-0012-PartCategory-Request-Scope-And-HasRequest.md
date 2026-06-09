# RFC-0012: PartCategory request scope and southbound hasRequest4Category

> 状态：草案（Draft）
> 日期：2026-06-09
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0002-PartCategory-Filter-Empty-Handling.md`, `doc/RFC-0009-Optional-PartCategory-Whitelist-Guard.md`, `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`, `doc/RFC-0011-RuleTrans-Module.md`

---

## 设计决策摘要

| 主题 | 决策 |
| --- | --- |
| 问题定位 | 当前请求模型只有 `attrWhereCondition`，没有标记这条 where 是“所有实例候选过滤”还是“单个实例选配要求”。 |
| 默认兼容语义 | 不带汇总的 where-only 输入默认保持“所有实例候选过滤”；带 `Sum`/`SumSum` 的输入默认是实例/汇总要求。 |
| scope 语义 | `PartCategoryRequestScope` 只描述该请求作用于哪个实例范围，不决定它是否算“用户有要求”。 |
| `AUTO` 语义 | `AUTO` 表示 Driver 没有显式指定范围，由执行器按请求形态、同分类兄弟请求和分类策略推导 effective scope。 |
| 一体机场景 | `allInOne: where Memory>=512` 在 `AUTO` 下可作为所有一体机候选过滤，同时仍触发 `hasRequest4Category("allInOne")`。 |
| 普通过滤场景 | `drive: where Speed=5400` 默认是 drive 下所有实例的全局候选过滤，同时记录为用户对 drive 有要求。 |
| 南向接口 | P0 只公开 `hasRequest4Category(String partCategoryCode)`；不把单实例/多实例/所有实例的细分判断暴露给规则作者。 |
| 实现边界 | 先在输入标准化和执行器预处理层拆分全局过滤与实例要求；不改变 `sum4Quantity` / `sum4Selected` 的表达式求和语义。 |
| 命名 | 采用正确拼写 `hasRequest4Category`；如需兼容已讨论样例，可短期保留废弃别名 `hasReqest4Category`。 |

---

## 1. 摘要

当前配置输入中，同一条 `where` 可能有两种业务语义：

```text
drive: where Speed=5400
```

在散件/多实例场景中，它通常表示“drive 分类下所有实例候选都必须先满足 Speed=5400”，也就是全局候选过滤。

```text
allInOne: where Memory>=512
```

在一体机场景中，它可能表示“用户明确提出了一体机相关要求”。此时即使没有显式 `Sum_Quantity >=1`，规则也需要能识别出“一体机被请求过”，进而加上数量下限或优先使用一体机。

现有代码只保存 `attrWhereCondition`，无法区分 where 的作用范围，也无法稳定判断某分类是否被用户请求。因此本 RFC 提议给 PartCategory 输入增加请求作用域标记，并在南向规则接口中新增 `hasRequest4Category`。

---

## 2. 当前代码检查

结论：当前代码确实存在本需求描述的问题。

### 2.1 请求模型缺少作用域字段

`PartCategoryConstraintReqBase` 当前只有汇总属性、比较符、where 和策略字段：

- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReqBase.java:13`
- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReqBase.java:23`
- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReqBase.java:38`

这些字段能表达“过滤条件是什么”，但不能表达“这个过滤条件作用于所有实例，还是代表一个实例级选配要求”。

### 2.2 字符串解析会把 where-only 解析成无汇总请求

`ModuleScenarioTestBase.parseAttrExpr(...)` 中，where-only 输入只设置 `attrWhereCondition`，随后直接返回：

- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java:840`
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java:862`
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java:867`

因此 `drive: where Speed=5400` 和 `allInOne: where Memory>=512` 在请求对象层面没有语义差别。

### 2.3 执行器按同分类请求数量决定单实例/多实例

`ModuleConstraintExecutorImpl.filterClone(...)` 当前按同一 PartCategory 的请求条数分支：

- 只有一条请求时，直接 `filterClone(req)` 并构造单个 `PartCategoryInput`。
- 多条请求时，构造 `MultiInstPartCategoryInput`，每条非 `SumSum` 请求被当成一个实例输入。

相关位置：

- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java:198`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java:220`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java:250`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java:258`

这个分支没有判断 where-only 是全局过滤还是实例需求，所以多条请求混合时会出现语义歧义。

### 2.4 where-only 不会自动增加数量约束

`ModuleBaseAlgImpl.setPartCategoryInput(...)` 只有在 `comparator` 非空时才调用 `sumFunConstraint(...)`：

- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java:153`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java:158`

因此 where-only 输入目前只影响候选过滤，不会自然表达“至少选一个实例”。一体机场景需要额外规则，而规则又缺少“用户是否请求过该分类”的南向查询能力。

### 2.5 南向接口没有请求意图查询

当前 `ModuleAlgBase` 和 `ModuleCPModel` 主要暴露模型变量、求和表达式和实例 view：

- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java:21`
- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java:74`
- `src/main/java/com/jmix/executor/southinf/PartCategoryCPModel.java:12`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundLatestBridge.java:35`

没有接口可以判断“原始输入中是否存在针对某 PartCategory 的实例级要求”。

---

## 3. 动机

### 3.1 多实例散件语义

现有散件输入希望保留以下默认行为：

```text
drive: where Speed=5400
drive:Sum_Quantity ==1 where Speed=7200
```

第一行是全局候选过滤。先把 drive 候选限制为 5400 后，第二行再要求 7200，预期无解。

多条带汇总输入仍然表示多个独立实例要求：

```text
drive:Sum_Quantity ==1 where Speed=7200
drive:Sum_Quantity ==2 where Speed=9000
```

预期可以分别生成满足 7200 和 9000 的实例。

### 3.2 一体机业务语义

一体机同时具备主机、CPU 等属性。用户输入：

```text
allInOne: where Memory>=512
```

在业务上可能不是“把所有一体机候选过滤成 Memory>=512，但不一定选”，而是“我要求配置一台满足 Memory>=512 的一体机”。规则需要能写成：

```java
@CodeRuleAnno(fatherCode = "allInOne", normalNaturalCode = "如果有一体机要求，则至少选一个一体机")
private void qtyConstraint4AllInOne() {
    if (hasRequest4Category("allInOne")) {
        model().addGreaterOrEqual(model().sum4Quantity("fatherCode=allInOne"), 1);
    }
}
```

这要求 Driver 能把 where-only 输入标记为实例级要求，而不是沿用默认的全局过滤语义。

---

## 4. 术语与边界

### 4.1 全局范围要求

全局范围要求指对某 PartCategory 的基础候选集做预过滤，或对该分类的所有实例提出汇总要求。它影响后续所有实例要求可选择的部件集合。

全局范围要求仍然是用户要求。因此它应被 `hasRequest4Category` 识别。它是否自动导致“至少选择一个实例”，不由 scope 自身决定，而由产品规则决定，例如一体机规则可以在 `hasRequest4Category("allInOne")` 为 true 时增加数量下限。

示例：

```text
drive: where Speed=5400
```

### 4.2 实例级要求

实例级要求指用户对某个逻辑实例提出了数量、容量或属性过滤要求。它应该进入单实例或多实例求解流程，并被请求摘要记录。

示例：

```text
drive:Sum_Quantity ==1 where Speed=7200
allInOne: where Memory>=512 [scope=INSTANCE]
```

### 4.3 汇总要求

带 `Sum_*` 的请求默认是实例级要求；带 `SumSum_*` 的请求默认是同一逻辑分类所有实例的汇总要求。它们不需要额外 scope 标记。

如果同一分类出现多条 `Sum_*` 请求，则每条请求代表一个独立实例要求。这个“多实例”不需要新增 `MULTI_INSTANCES` scope；它是多个 `INSTANCE` 请求项组合出来的。

### 4.4 跨分类同时要求

跨分类同时要求指一条输入同时涉及多个 PartCategory，常见形式是跨分类总量或多个分类共享过滤条件：

```text
disk,mainBoard:Sum_Capacity >=100 where PortRate=10G
```

这类请求应在 `PartCategoryRequestSummary` 中记录它涉及的所有分类。对于每个参与分类，`hasRequest4Category(categoryCode)` 都应返回 true，因为用户确实提出了和该分类有关的要求。但跨分类 `where` 仍按 RFC-0010 语义只过滤总量表达式项，不反向覆盖单分类候选集。

### 4.5 非目标

- 不把所有 where-only 输入默认改成实例要求。
- 不改变跨分类总量 `where` 的语义，RFC-0010 中的“总量 where 只影响表达式项集合”保持不变。
- 不在规则中直接读取 `PartConstraintReq` DTO；规则只使用南向稳定接口。
- 不在 P0 暴露 `hasInstanceRequest4Category`、`hasAllInstancesRequest4Category` 等细分接口，避免规则作者必须理解 Driver 的输入拆分细节。

---

## 5. 设计方案

### 5.1 新增请求作用域枚举

新增枚举：

```java
package com.jmix.executor.model;

public enum PartCategoryRequestScope {
    AUTO,
    ALL_INSTANCES,
    INSTANCE
}
```

枚举含义：

| 枚举值 | 含义 |
| --- | --- |
| `AUTO` | Driver 不声明作用范围，由执行器推导。它不是第三种业务语义，而是“未显式指定”的兼容入口。 |
| `ALL_INSTANCES` | 请求作用于该分类的所有逻辑实例，常用于 where-only 全局候选过滤或 `SumSum` 汇总。 |
| `INSTANCE` | 请求作用于一个逻辑实例，常用于 `Sum_*` 数量/容量要求。多实例由多条 `INSTANCE` 请求组成。 |

建议在 `PartCategoryConstraintReq` 增加字段：

```java
private PartCategoryRequestScope requestScope = PartCategoryRequestScope.AUTO;
```

不建议放在 `PartCategoryConstraintReqBase`，因为 `CrossCategoryPartCategoryConstraintReq` 继承同一个 base，但它的 `where` 属于总量表达式项过滤，不参与本 RFC 的 all instances / instance 语义。

### 5.2 `AUTO` 推导规则

如果 Driver 未显式设置 `requestScope`，执行器按以下规则推导：

| 输入形态 | 默认 effective scope | 说明 |
| --- | --- | --- |
| `where ...` 且无 `Sum`/`SumSum` | `ALL_INSTANCES` | 保持当前散件全局过滤习惯 |
| `Sum_* ...`，无论是否带 where | `INSTANCE` | 单个实例要求 |
| `SumSum_* ...` | `ALL_INSTANCES` 汇总要求 | 汇总约束不创建单个实例，但参与多实例总量 |
| 空输入 `drive:` | `ALL_INSTANCES` | 表示提到该分类但无过滤和汇总；主要用于搜索策略/候选枚举兼容 |
| Driver 显式 `INSTANCE` | `INSTANCE` | 用于强制表达单个逻辑实例要求 |
| Driver 显式 `ALL_INSTANCES` | `ALL_INSTANCES` | 用于强制声明全局过滤 |

`AUTO` 还允许后续引入产品侧默认策略。例如如果产品建模声明 `allInOne` 的 where-only 默认应按选配请求处理，执行器可以把它记录为 `ALL_INSTANCES` 范围过滤，同时通过产品规则中的 `hasRequest4Category("allInOne")` 加数量下限。这样 Driver 不必在每个请求上显式传 `INSTANCE`。

### 5.3 字符串测试 DSL 扩展

为了测试和调试，`ModuleScenarioTestBase` 支持可选 scope 标记：

```text
drive: where Speed=5400 [scope=ALL_INSTANCES]
allInOne: where Memory>=512 [scope=INSTANCE]
drive:Sum_Quantity ==1 where Speed=7200
```

规则：

- `[scope=ALL_INSTANCES]` 和 `[scope=INSTANCE]` 仅用于单分类请求。
- 如果请求是跨分类输入，例如 `disk,mainBoard:Sum_Capacity >=100 where PortRate=10G`，scope 标记非法。
- 不写 `[scope=...]` 时等价于 `AUTO`。
- 原有 `[strategy=ASCENDING:price]` 继续支持；解析器应允许多个方括号选项共存。

### 5.4 执行器预处理流程

在 `ModuleConstraintExecutorImpl.filterClone(...)` 内部或其前置标准化阶段，将同一分类请求拆分为三类：

```text
globalFilterReqs: effectiveScope == ALL_INSTANCES 且无 SumSum 汇总比较
instanceReqs:     effectiveScope == INSTANCE
sumSumReqs:       attrType == SumSum
```

处理顺序：

1. 从原始 PartCategory 克隆得到 `baseCategory`。
2. 先按 `globalFilterReqs` 顺序执行 `filterClone`，得到全局过滤后的 `globalFilteredCategory`。
3. 如果 `instanceReqs` 为空：
   - 非可选分类：把 `globalFilteredCategory` 加入模块，作为该分类候选集。
   - 可选分类：保留 absent/present 分支；present 分支使用 `globalFilteredCategory`。
4. 如果 `instanceReqs` 只有一条：
   - 在 `globalFilteredCategory` 基础上继续套用该实例请求的 where。
   - 构造单个 `PartCategoryInput`。
5. 如果 `instanceReqs` 有多条：
   - 构造 `MultiInstPartCategoryInput`。
   - 每个实例都从 `globalFilteredCategory` 出发，再套用自己的实例 where。
   - `sumSumReqs` 设置到 `MultiInstPartCategoryInput` 上。

### 5.5 过滤为空语义

全局过滤为空沿用 RFC-0002/RFC-0009 的过滤空诊断。

实例过滤为空按“请求项部分满足”处理：

- 如果一个分类只有一个实例请求，且该请求在全局过滤后候选为空，则该分类无可满足实例，整体无解或返回过滤空诊断。
- 如果同一分类有多个实例请求，部分实例请求为空时，记录该请求项失败；其余可满足请求仍参与求解。
- 如果所有实例请求均失败，则整体无解或返回该分类全部实例请求失败诊断。

这个规则解释组 1 与组 3：

```text
组 1:
drive: where Speed=5400
drive:Sum_Quantity ==1 where Speed=7200
```

全局过滤后，唯一实例请求为空，所以无解。

```text
组 3:
drive: where Speed=5400
drive:Sum_Quantity ==1
drive:Sum_Quantity ==1 where Speed=7200
drive:Sum_Quantity ==2 where Speed=9000
```

全局过滤后，第一个实例请求可满足，后两个实例请求失败，因此返回部分满足结果，例如 `sd1.Q=1`，并在诊断中记录第二、第三个实例请求无解。

### 5.6 南向接口

新增南向方法：

```java
protected final boolean hasRequest4Category(String partCategoryCode) {
    return runtime().hasRequest4Category(partCategoryCode);
}
```

同时扩展 `ModuleAlgBase.RuntimeSupport`：

```java
boolean hasRequest4Category(String partCategoryCode);
```

`SouthboundModuleAlgAdapter.Runtime` 转调底层 `ModuleAlgImpl`：

```java
public boolean hasRequest4Category(String partCategoryCode) {
    return adapter.hasRequest4Category(partCategoryCode);
}
```

`ModuleAlgImpl` 根据 `ModuleInput` 中保留的请求摘要判断：

```java
public boolean hasRequest4Category(String partCategoryCode) {
    return moduleInput.hasInstanceRequestForCategory(partCategoryCode);
}
```

短期可提供拼写兼容别名：

```java
@Deprecated
protected final boolean hasReqest4Category(String partCategoryCode) {
    return hasRequest4Category(partCategoryCode);
}
```

### 5.7 请求摘要

为了避免规则侧直接依赖 DTO，建议在执行器标准化后构造轻量请求摘要：

```java
public class PartCategoryRequestSummary {
    private String partCategoryCode;
    private PartCategoryRequestScope effectiveScope;
    private boolean hasWhereCondition;
    private boolean hasAggregateConstraint;
    private AttrParaType attrType;
}
```

`ModuleInput` 增加：

```java
private List<PartCategoryRequestSummary> partCategoryRequestSummaries;
```

`hasRequest4Category` 只看该摘要，不回读原始请求对象：

```text
exists summary where
  summary.partCategoryCode == input code
  and summary.effectiveScope == INSTANCE
```

### 5.8 可选分类关系

当前可选分类 where-only 输入会通过 `isMentionOnlyOptionalReq(...)` 触发 present 分支。引入 scope 后应调整为：

| 输入 | 可选分类行为 |
| --- | --- |
| `optional: where X=1 [scope=INSTANCE]` | 视为用户请求，强制 present，且 `hasRequest4Category` 为 true |
| `optional: where X=1 [scope=ALL_INSTANCES]` | 只过滤 present 分支候选，不强制 present |
| `optional:Sum_Quantity ==1 where X=1` | 强制 present，且 `hasRequest4Category` 为 true |
| 无请求 | 沿用 RFC-0009 absent/present 分支 |

---

## 6. 正反语义矩阵

| 场景 | 输入 | `hasRequest4Category` | 预期 |
| --- | --- | --- | --- |
| 全局过滤 | `drive: where Speed=5400` | false | drive 候选先过滤到 5400 |
| 单实例数量 | `drive:Sum_Quantity ==1` | true | 一个 drive 实例数量为 1 |
| 单实例数量带过滤 | `drive:Sum_Quantity ==1 where Speed=7200` | true | 一个 drive 实例从 7200 候选中求解 |
| 一体机 where-only 选配 | `allInOne: where Memory>=512 [scope=INSTANCE]` | true | 规则可据此约束一体机数量 >= 1 |
| 一体机全局过滤 | `allInOne: where Memory>=512 [scope=ALL_INSTANCES]` | false | 只限制候选，不代表必须选一体机 |
| 跨分类总量 | `disk,board:Sum_Capacity >=100 where PortRate=10G` | 不适用 | 仍按 RFC-0010，只过滤总量表达式项 |

---

## 7. 接口矩阵

| 入口 | 变更 | 验收 |
| --- | --- | --- |
| 字符串测试 DSL | 支持 `[scope=INSTANCE]` / `[scope=ALL_INSTANCES]` | `ModuleScenarioTestBase` 覆盖解析结果 |
| 结构化请求 | `PartCategoryConstraintReq.requestScope` | 单元测试直接构造 req 验证 effective scope |
| `inferParas` 推荐路径 | 按 scope 拆分全局过滤与实例要求 | 场景测试覆盖组 1、组 2、一体机 |
| 可选分类分支 | where-only 是否强制 present 取决于 scope | 补充 RFC-0009 风格测试 |
| 南向规则 | 新增 `hasRequest4Category` | 规则中调用并影响求解结果 |
| 校验路径 | 暂不新增行为，保留为后续任务 | RFC 标记为 P1 或实现后补验收 |
| 序列化 | 新字段默认 `AUTO`，缺省兼容旧 JSON | JSON 反序列化旧请求不报错 |
| RuleTrans Prompt | 增加新南向 API 说明 | RFC-0011 后续更新或补充测试 |

---

## 8. 复用优先清单

- 优先复用：
  - `ModuleScenarioTestBase.inferRecommendModule(...)`
  - `ModuleConstraintExecutorImpl.normalizePartConstraint(...)`
  - `PartCategory.filterClone(...)`
  - `PartCategoryInput` / `MultiInstPartCategoryInput`
  - `ModuleInput`
  - `SouthboundModuleAlgAdapter.Runtime`
  - `SouthboundLatestBridge`
- 不新增：
  - 不新增另一套 PartCategory 请求 DTO。
  - 不让规则直接依赖 `PartConstraintReq`。
  - 不新增测试专用的复杂请求 builder，除非现有 helper 无法表达 scope。
- 可由上下文推导：
  - `hasAggregateConstraint` 可由 `attrCode` / `attrComparator` / `attrValue` 推导。
  - `hasWhereCondition` 可由 `attrWhereCondition` 推导。
  - `effectiveScope` 可由显式 `requestScope` 和请求形态推导。
- 相似测试来源：
  - `src/test/java/com/jmix/scenario/ruletest/PartCategoryFilterEmptyTest.java`
  - `src/test/java/com/jmix/scenario/ruletest/OptionalPartCategoryWhitelistGuardTest.java`
  - `src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java`
  - `src/test/java/com/jmix/opti/base/BaseOptiTest.java`

---

## 9. 验收准则

### AC-001: where-only 默认是全局过滤

```java
@Test
public void testWhereOnlyDefaultsToAllInstancesFilter() {
    inferRecommendModule(
            "drive: where Speed=5400",
            "drive:Sum_Quantity ==1 where Speed=7200");
    printSimpleSolutions();

    assertNoSolution();
}
```

### AC-002: 多条汇总输入仍表示多个实例要求

```java
@Test
public void testAggregateInputsBecomeIndependentInstances() {
    inferRecommendModule(
            "drive:Sum_Quantity ==1 where Speed=7200",
            "drive:Sum_Quantity ==2 where Speed=9000");
    printSimpleSolutions();

    assertSoluContain("sd2(Q:1,H:0,S:1)");
    assertSoluContain("sd3(Q:2,H:0,S:1)");
}
```

### AC-003: 显式实例级 where-only 可触发一体机数量规则

```java
@CodeRuleAnno(fatherCode = "allInOne", normalNaturalCode = "如果有一体机要求，则至少选择一个一体机")
private void requireAllInOneWhenRequested() {
    if (hasRequest4Category("allInOne")) {
        model().addGreaterOrEqual(model().sum4Quantity("fatherCode=allInOne"), 1);
    }
}

@Test
public void testInstanceWhereOnlyTriggersHasRequest() {
    inferRecommendModule("allInOne: where Memory>=512 [scope=INSTANCE]");
    printSimpleSolutions();

    assertSoluContain("aio512(Q:1,H:0,S:1)");
}
```

### AC-004: 全局 where-only 不触发一体机数量规则

```java
@Test
public void testAllInstancesWhereOnlyDoesNotTriggerHasRequest() {
    inferRecommendModule("allInOne: where Memory>=512 [scope=ALL_INSTANCES]");
    printSimpleSolutions();

    assertNoForcedPartQuantity("allInOne");
}
```

### AC-005: 结构化请求可显式设置 scope

```java
@Test
public void testStructuredRequestScopeInstance() {
    PartConstraintReq req = new PartConstraintReq();
    req.setPartCategoryCode("allInOne");
    req.setAttrWhereCondition("Memory>=512");
    req.setRequestScope(PartCategoryRequestScope.INSTANCE);

    inferRecommendModuleWithReqs(req);

    assertTrue(lastModuleInput().hasInstanceRequestForCategory("allInOne"));
}
```

### AC-006: 可选分类 where-only 行为由 scope 决定

```java
@Test
public void testOptionalWhereOnlyScopeControlsPresentBranch() {
    inferRecommendModule("mouse: where Level=1 [scope=ALL_INSTANCES]");
    printSimpleSolutions();
    assertFalse(hasRequest4CategoryInLastRun("mouse"));

    inferRecommendModule("mouse: where Level=1 [scope=INSTANCE]");
    printSimpleSolutions();
    assertTrue(hasRequest4CategoryInLastRun("mouse"));
    assertSoluContain("mouse1(Q:1,H:0,S:1)");
}
```

---

## 10. 实现计划

1. 新增 `PartCategoryRequestScope` 枚举。
2. 在 `PartCategoryConstraintReq` 增加 `requestScope` 字段，默认 `AUTO`。
3. 扩展字符串 DSL 解析，支持 `[scope=...]`，并保持 `[strategy=...]` 可共存。
4. 增加 `PartCategoryRequestSummary`，在 `ModuleInput` 中保存标准化后的请求摘要。
5. 在执行器预处理阶段实现 effective scope 推导。
6. 改造 `filterClone(...)`：先应用全局过滤，再处理实例请求和 `SumSum`。
7. 调整可选分类 where-only present 逻辑，使其只对 `INSTANCE` 生效。
8. 在 `ModuleAlgBase.RuntimeSupport` / `SouthboundModuleAlgAdapter.Runtime` / `ModuleAlgBase` 增加 `hasRequest4Category`。
9. 补充场景测试，覆盖 AC-001 到 AC-006。
10. 如 RuleTrans 需要生成该 API，补充 RFC-0011 的 SDK 上下文测试或后续 RFC patch。

---

## 11. 待确认问题

Q1. 组 3 的预期需要确认。按组 1 的语义，如果存在：

```text
drive: where Speed=5400
drive:Sum_Quantity ==1
drive:Sum_Quantity ==1 where Speed=7200
drive:Sum_Quantity ==2 where Speed=9000
```

全局过滤后，7200 和 9000 的实例请求都无法满足，逻辑上应当无解。你给出的预期是 `sd1.Q=1`。这是否表示“全局过滤后实例 where 为空的请求要被忽略”？如果是，这会和组 1 的无解预期冲突，需要单独定义优先级或诊断语义。

Q2. `hasRequest4Category` 是否应该对“带汇总但无 where”的请求返回 true？本 RFC 暂定返回 true，因为带汇总默认是实例级要求。

Q3. 一体机场景中的默认输入是否一定能由 Driver 标记为 `INSTANCE`？如果不能，是否需要产品建模侧声明某些分类的 where-only 默认语义为实例级？

Q4. 方法名是否必须保留用户示例里的拼写 `hasReqest4Category`？本 RFC 建议正式 API 使用 `hasRequest4Category`，短期提供 deprecated 别名。

---

## 12. 参考资料

- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReqBase.java`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java`
- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundModuleAlgAdapter.java`
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java`
