# RFC-0002: 部件分类过滤为空时的实例级错误输出处理

> 状态：修订草案（Revised Draft）
> 日期：2026-04-30
> 原文档：`doc/RFC-0002-PartCategory-Filter-Empty-Handling.md`

---

## 1. 摘要

当某个 `PartCategory` 或某个多实例分类实例根据输入过滤条件找不到匹配部件时，系统不应直接抛出异常导致整体失败，而应将该问题作为实例级诊断信息写入输出端 `PartCategoryInst`。

本 RFC 建议：

- 北向输入不变：不修改 `InferParasReq`、`InferPartCategoryReq`、`PartConstraintReq`
- 输出模型增强：`PartCategoryInst` 增加 `errorCode`、`errorMessage`
- 结果码增强：新增 `Result.PARTIAL_SUCCESS`
- 求解流程增强：过滤为空的实例跳过变量和规则构建，其他正常实例继续求解
- 诊断输出增强：即使某个实例没有创建求解变量，最终结果中仍应能看到该实例的错误信息

---

## 2. 动机

### 2.1 当前问题

当前流程先根据 `PartConstraintReq.attrWhereCondition` 过滤部件分类，再基于过滤后的部件构建约束表达式。

当过滤结果为空时，`ModuleConstraintExecutorImpl.filterClone()` 当前会抛出 `AlgExecutorException`，导致整体返回 `Result.failed`。这会带来两个业务问题：

- 其他分类明明可以正常求解，却无法返回给用户
- 用户无法知道是哪个分类、哪个实例、哪个过滤条件导致没有结果

### 2.2 典型场景

输入：

```text
drive: Sum_Capacity >= 5 where Speed=5400
cpu:   Sum_Memory >= 512 where CoreNum=8
```

如果 `drive` 能找到部件，而 `cpu` 根据 `CoreNum=8` 找不到部件，期望返回：

- `drive` 正常输出可行解
- `cpu` 对应 `PartCategoryInst` 输出 `FILTER_EMPTY`
- 顶层结果为 `PARTIAL_SUCCESS`

### 2.3 多实例场景

输入：

```text
driveI0: where Speed=5400
driveI1: where Speed=3000
```

如果 `driveI0` 正常、`driveI1` 过滤为空，则只在 `driveI1` 对应的 `PartCategoryInst` 上输出错误，不能把整个 `drive` 分类都标记失败。

---

## 3. 设计目标

### 3.1 目标

- 保留其他可求解分类或实例的正常输出
- 将过滤为空的问题定位到 `PartCategoryInst` 级别
- 错误信息包含足够上下文，便于用户和开发者定位
- 不改变调用方输入协议
- 避免空过滤实例参与变量、规则、跨分类约束构建
- `processProduct` 与 `inferParas` 两条入口行为一致

### 3.2 非目标

- 不处理过滤表达式语法错误。语法错误仍属于请求或系统错误，应返回 `FAILED`
- 不额外报告 `DecisionStrategy` 被跳过。过滤为空时只报告过滤失败
- 不引入 `ErrorPartCategoryInput`
- 不在本 RFC 中设计完整冲突诊断体系

---

## 4. 术语

| 术语 | 含义 |
|------|------|
| 过滤为空 | 根据 `attrWhereCondition` 过滤后没有任何匹配部件 |
| 空实例 | 某个分类实例过滤为空，因此不参与求解变量构建 |
| 有效实例 | 过滤后仍有匹配部件，可以参与求解 |
| 诊断实例 | 只用于输出错误信息的 `PartCategoryInst` |

---

## 5. 设计方案

### 5.1 输出模型扩展

新增实例错误码：

```java
package com.jmix.executor.cmodel;

public final class InstErrorCode {
    private InstErrorCode() {}

    public static final int NO_ERROR = 0;
    public static final int FILTER_EMPTY = 1;
}
```

`PartCategoryInst` 增加字段：

```java
public class PartCategoryInst extends ModuleBaseInst {
    private int errorCode = InstErrorCode.NO_ERROR;
    private String errorMessage;
}
```

字段语义：

| 字段 | 说明 |
|------|------|
| `errorCode` | `0` 表示无错误，`1` 表示过滤为空 |
| `errorMessage` | 包含分类、实例、过滤条件、原始请求等上下文 |

建议错误消息格式：

```text
No parts found for category=cpu, instanceId=0, condition=CoreNum=8, request=cpu: where CoreNum=8
```

### 5.2 结果码扩展

`Result` 新增：

```java
public static final int PARTIAL_SUCCESS = 3;
```

结果码判定：

| 场景 | `Result.code` |
|------|---------------|
| 无过滤为空，求解有解 | `SUCCESS` |
| 存在过滤为空，且至少有一个可用解 | `PARTIAL_SUCCESS` |
| 无过滤为空，但求解无解 | `NO_SOLUTION` |
| 所有请求实例均过滤为空 | `NO_SOLUTION`，但应返回诊断实例 |
| 存在过滤为空，剩余有效实例仍无解 | `NO_SOLUTION`，message 说明无可行解且存在过滤失败 |
| 模型非法、表达式语法错误、系统异常 | `FAILED` |

### 5.3 内部诊断结构

为了不改变输入协议，同时让 `filterClone` 能传递错误信息，建议把当前返回值从 `Pair<Module, List<PartCategoryInputBase>>` 升级为内部结构：

```java
public class FilterCloneResult {
    private Module filteredModule;
    private List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
    private List<PartCategoryFilterError> filterErrors = new ArrayList<>();
}
```

过滤错误结构：

```java
public class PartCategoryFilterError {
    private String categoryCode;
    private int instanceId;
    private int errorCode;
    private String errorMessage;
    private String attrWhereCondition;
    private String reqShortString;
}
```

实例级定位 key：

```text
categoryCode + "#" + instanceId
```

### 5.4 `filterClone` 改造

单实例分类：

- 过滤有结果：按现有流程创建 `PartCategoryInput`
- 过滤为空：不抛异常，记录 `PartCategoryFilterError`
- 过滤为空的分类不应作为有效分类参与后续约束构建

多实例分类：

- 每个 `PartConstraintReq` 独立过滤
- 有结果的实例加入 `MultiInstPartCategoryInput.partCategoryInputs`
- 空实例只记录错误，不加入可求解实例列表
- `SumSum` 类型请求仍作为多实例整体约束处理，但如果所有子实例均为空，应避免构建无法满足的求解模型

### 5.5 算法层处理

`ModuleAlgImpl.initData()` 只为有效分类或有效实例创建算法对象：

- 有效单实例创建 `SingleInstPartCategoryAlgImpl`
- 有效多实例创建 `MultiInstPartCategoryAlgImpl`
- 空实例不创建 `PartVar`、`ParaVar`
- 空实例不执行分类级规则
- 跨分类或模块级规则如果引用空实例，应跳过或隔离，不能因为缺变量抛出全局异常

这条约束是本 RFC 的关键：过滤为空属于业务诊断，不应变成求解模型构建异常。

### 5.6 结果组装

现有 `ModuleInstSolutionCallBack` 只会为已创建算法对象的分类生成 `PartCategoryInst`。因此空实例需要在求解后补齐。

建议新增后处理：

```java
applyFilterErrors(List<ModuleInst> solutions, List<PartCategoryFilterError> errors)
```

处理规则：

- 如果 solution 中存在对应 `PartCategoryInst`，直接写入 `errorCode` 和 `errorMessage`
- 如果不存在，创建诊断用 `PartCategoryInst`
- 诊断实例只设置 `code`、`instanceId`、`errorCode`、`errorMessage`
- 对所有 solution 都应用相同的过滤错误
- 如果没有任何 solution，但存在过滤错误，应创建一个诊断用 `ModuleInst`，用于承载错误输出

### 5.7 入口一致性

需要统一修改以下路径：

- `processProduct(Module, InferPartCategoryReq)`
- `inferParas(InferParasReq)`
- `toModuleInput(Module, InferParasReq)`
- 优先级求解路径
- 非优先级求解路径

不能只修改 `processProduct`，否则 `inferParas` 仍可能沿用旧的异常行为。

---

## 6. 处理流程

```text
请求进入
  |
  v
normalizePartConstraint
  |
  v
filterClone -> FilterCloneResult
  |         |
  |         +-- filterErrors
  v
ModuleInput(partCategoryInputs)
  |
  v
ModuleAlgImpl.initData
  |
  +-- 有效实例创建算法对象
  +-- 空实例跳过变量和规则构建
  |
  v
CP-SAT 求解
  |
  v
ModuleInstSolutionCallBack 构建正常实例
  |
  v
applyFilterErrors 补齐或标记错误实例
  |
  v
根据 solution 与 filterErrors 判定 Result.code
```

---

## 7. 验收准则

### ERR-001：单实例部分过滤为空

目的：验证某个分类过滤为空时，不影响其他分类输出。

| ID | 输入 | 预期 |
|----|------|------|
| ERR-001-1 | `cpu: where CoreNum=8`, `drive: where Speed=5400` | `PARTIAL_SUCCESS`，`drive` 正常，`cpu#0` 为 `FILTER_EMPTY` |
| ERR-001-2 | `cpu: where CoreNum=4`, `drive: where Speed=7200` | `PARTIAL_SUCCESS`，`cpu` 正常，`drive#0` 为 `FILTER_EMPTY` |
| ERR-001-3 | `cpu: where CoreNum=4`, `drive: where Speed=5400` | `SUCCESS`，无错误 |

### ERR-002：所有请求分类过滤为空

| ID | 输入 | 预期 |
|----|------|------|
| ERR-002-1 | `cpu: where CoreNum=8`, `drive: where Speed=7200` | `NO_SOLUTION`，返回诊断实例，`cpu#0` 和 `drive#0` 均为 `FILTER_EMPTY` |

### ERR-003：多实例部分过滤为空

| ID | 输入 | 预期 |
|----|------|------|
| ERR-003-1 | `driveI0: where Speed=5400`, `driveI1: where Speed=3000` | `PARTIAL_SUCCESS`，`drive#0` 正常，`drive#1` 为 `FILTER_EMPTY` |
| ERR-003-2 | `driveI0: where Speed=5400`, `driveI1: where Speed=7200` | `SUCCESS`，两个实例都正常 |

### ERR-004：过滤为空且存在关联规则

目的：验证空实例不会因为关联规则导致全局异常。

| ID | 输入 | 规则 | 预期 |
|----|------|------|------|
| ERR-004-1 | `cpu` 为空，`drive` 正常 | `inCompatible(cpu, drive)` | `PARTIAL_SUCCESS`，`drive` 正常，`cpu` 报错 |
| ERR-004-2 | `drive` 为空 | `sumFunConstraint(drive.Capacity)` | 不抛异常，输出 `drive:FILTER_EMPTY` |

### ERR-005：非法输入和边界

| ID | 输入 | 预期 |
|----|------|------|
| ERR-005-1 | `nonexist: where X=Y` | 忽略未知分类，不产生 `FILTER_EMPTY` |
| ERR-005-2 | `cpu:` | 全量匹配，`errorCode=0` |
| ERR-005-3 | 过滤表达式语法非法 | `FAILED`，不当作过滤为空 |
| ERR-005-4 | 只请求 `cpu`，未请求 `drive` | `drive` 按无过滤处理，不产生错误 |

### 断言建议

新增断言：

```java
assertSoluErrorContain(1, "cpu#0:FILTER_EMPTY");
assertSoluErrorMessageContain(1, "cpu#0", "CoreNum=8");
```

`toShortString(true)` 建议展示：

```text
drive1(Q:1,H:0,S:1),cpu#0:FILTER_EMPTY
```

---

## 8. 实现计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| 1 | 新增 `InstErrorCode` | P0 |
| 2 | `PartCategoryInst` 增加 `errorCode`、`errorMessage` | P0 |
| 3 | `Result` 增加 `PARTIAL_SUCCESS` | P0 |
| 4 | 新增内部结构 `FilterCloneResult`、`PartCategoryFilterError` | P0 |
| 5 | 改造 `filterClone`，过滤为空时记录错误，不抛异常 | P0 |
| 6 | 改造 `ModuleAlgImpl` / 多实例算法初始化，跳过空实例 | P0 |
| 7 | 新增 `applyFilterErrors`，补齐诊断实例 | P0 |
| 8 | 统一 `processProduct` 与 `inferParas` 入口行为 | P0 |
| 9 | 增强 `toShortString`、`SolutionUtils.printSolutions` 展示错误 | P1 |
| 10 | 编写 ERR-001 到 ERR-005 自动化测试 | P1 |
| 11 | 回归测试现有场景，确认正常用例 result.code 不变 | P1 |

---

## 9. 兼容性

### 9.1 输入兼容

北向输入不变，调用方无需修改请求结构。

### 9.2 输出兼容

`PartCategoryInst` 新增字段默认值为：

```text
errorCode = 0
errorMessage = null
```

已有消费者如果忽略未知字段，不受影响。

### 9.3 结果码兼容

新增 `PARTIAL_SUCCESS` 后，调用方如果只判断 `Result.SUCCESS`，可能无法识别部分成功。建议后续增加 helper：

```java
public boolean isSuccessLike() {
    return code == SUCCESS || code == PARTIAL_SUCCESS;
}
```

---

## 10. 未决问题

1. `NO_SOLUTION` 且存在过滤错误时，是否强制返回一个诊断用 `ModuleInst`？本修订稿建议返回，否则用户看不到实例级错误。
2. 跨分类规则引用空实例时，是“跳过整条规则”还是“只跳过空实例部分”？本修订稿建议先跳过或隔离整条受影响规则，避免模型构建异常。
3. `errorMessage` 是否需要国际化？当前建议先输出稳定英文诊断文本，后续再做展示层翻译。

---

## 11. 参考

- `doc/CORE-DESIGN.md`：对称设计架构、结果模型层
- `doc/ACCEPTANCE.md`：测试用例设计原则
- `doc/RFC-0001-Decision-Strategy.md`：`PartConstraintReq` 与决策策略关系
- Rust RFC 文档结构：动机、设计、兼容性、未决问题
