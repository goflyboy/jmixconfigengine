# RFC-0002: 部件分类过滤为空时的输出处理

> 状态：草案（Draft）
> 日期：2026-04-30

---

## 1. 摘要

本RFC提议：当某个 PartCategory 根据输入的过滤条件找不到匹配部件时，不应导致整体求解失败，而应在**输出端**（PartCategoryInst）上携带错误信息，同时继续输出其他满足条件的部件分类的有效解。

---

## 2. 动机

### 2.1 问题背景

当前处理流程分为两步：

1. 根据 `PartConstraintReq` 中的过滤条件（`attrWhereCondition`）对 PartCategory 进行过滤
2. 基于过滤后的部件构建约束表达式并求解

**存在的问题**：如果第1步过滤后没有匹配的部件，第2步构建约束表达式时：

- 如果该部件分类与其他部件分类存在关联规则（如 `inCompatible`、`sumFunConstraint`），会导致整体求解失败
- 即使其他部件分类（如 CPU）能正常求解，用户也**得不到任何有效信息**——既不返回部分有效解，也不提示是哪个分类的哪个过滤条件导致了问题

### 2.2 问题场景

用户输入两个部件约束请求：

```
drive: Sum_Capacity >=5 where Speed=5400   ← Speed=5400 能找到匹配部件 ✓
cpu:   Sum_Memory >=512 where CoreNum=4    ← CoreNum=4 找不到任何匹配部件 ✗
```

**当前行为**：`ModuleConstraintExecutorImpl.filterClone()` 在 cpu 分类过滤为空时抛出 `AlgExecutorException`（行 194-198），整体返回 `Result.failed`。两个分类的结果都丢失了。

**期望行为**：
- drive 分类正常求解，返回有效解
- cpu 分类对应的 Instance 上输出错误信息，包含 ErrorCode 和 ErrorMessage
- 用户可以同时看到成功的配置结果和失败的原因

### 2.3 多实例场景

多实例场景下，同一个 PartCategory 的不同实例可能有不同的过滤条件：

```
driveI0: where Speed=5400   ← 能找到匹配部件 ✓
driveI1: where Speed=3000   ← 找不到匹配部件 ✗
```

期望每个 Instance 独立报告自己的错误状态。

---

## 3. 设计方案

### 3.1 核心思路

**输入不变，只改输出**。PartConstraintReq 无需任何修改。改动集中在：

1. `filterClone`：过滤为空时不再抛异常，改为创建一个"空"的 PartCategoryInput（filteredCategory 的 atomicParts 为空），正常纳入后续流程
2. `ModuleAlgImpl.initData()`：识别 PartCategoryInput 中 filteredCategory 无部件的情况，跳过约束构建，记录错误
3. **输出端**：`PartCategoryInst` 新增 error 字段，求解完成后将错误信息写入

### 3.2 输出模型扩展

#### 3.2.1 错误码常量

```java
package com.jmix.executor.cmodel;

/**
 * PartCategory 实例错误码常量
 */
public final class InstErrorCode {
    private InstErrorCode() {}

    /** 无错误 */
    public static final int NO_ERROR = 0;

    /** 过滤条件未匹配到任何部件 */
    public static final int FILTER_EMPTY = 1;
}
```

#### 3.2.2 PartCategoryInst 增加 error 字段

```java
public class PartCategoryInst extends ModuleBaseInst {
    // ... 现有字段 ...

    /**
     * 错误码，0 表示无错误
     */
    private int errorCode = InstErrorCode.NO_ERROR;

    /**
     * 错误消息，包含详细的上下文信息方便定位
     * 示例: "No parts found for condition: CoreNum=8 in category cpu"
     */
    private String errorMessage;
}
```

#### 3.2.3 Result 增加 PARTIAL_SUCCESS

```java
public class Result<T> {
    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int NO_SOLUTION = 2;
    /** 部分成功：至少一个分类正常返回解，但存在分类过滤为空 */
    public static final int PARTIAL_SUCCESS = 3;
}
```

### 3.3 处理流程改造

```
filterClone(startModule, partConstraintReqMap)
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│  遍历每个 PartCategory                                    │
│    ├── 过滤有匹配部件 → 正常创建 PartCategoryInput         │
│    └── 过滤为空        → 创建 PartCategoryInput              │
│                          (filteredCategory 无 atomicParts)│
│                          + 收集错误信息到 errorInfoMap     │
└──────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│  ModuleAlgImpl.initData()                                 │
│    ├── filteredCategory 有部件 → 正常创建 AlgImpl          │
│    └── filteredCategory 无部件 → 跳过 AlgImpl 创建         │
│                                  记录到 emptyCategoryCodes │
└──────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│  求解（只针对有效分类）                                     │
└──────────────────────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────────┐
│  结果组装                                                  │
│    遍历 solutions，为每个 ModuleInst 的 PartCategoryInst   │
│    设置 errorCode 和 errorMessage                         │
└──────────────────────────────────────────────────────────┘
```

### 3.4 关键代码改造

#### 3.4.1 filterClone 不再抛异常

```java
// 当前 (ModuleConstraintExecutorImpl.java:194-198)
if (filterPartCategory.getAllAtomicPartShortString().isEmpty()) {
    throw new AlgExecutorException(msg);
}

// 改造后
if (filterPartCategory.getAllAtomicPartShortString().isEmpty()) {
    log.warn("Filter empty for category {}: {}", partCategoryCode, msg);
    errorInfoMap.put(partCategoryCode,
        new ErrorInfo(InstErrorCode.FILTER_EMPTY,
            "No parts found for condition: " + req.getAttrWhereCondition()));
    // 仍然创建 PartCategoryInput，但 filteredCategory 为空的克隆
    // 确保后续流程能识别并跳过
}
```

#### 3.4.2 ModuleAlgImpl.initData() 跳过空分类

```java
for (PartCategory partCategory : bModule.getPartCategorys()) {
    String categoryCode = partCategory.getCode();
    PartCategoryInputBase input = partConstraintFromReqMap.get(categoryCode);

    if (input != null && input.getFilteredCategory() != null
            && input.getFilteredCategory().getAtomicParts().isEmpty()) {
        // 过滤为空，跳过约束构建
        emptyCategoryCodes.add(categoryCode);
        continue;
    }

    // ... 正常创建 AlgImpl ...
}
```

#### 3.4.3 结果组装时写入错误

```java
// 求解完成后
for (ModuleInst solution : solutions) {
    for (PartCategoryInst pcInst : solution.getPartCategoryInsts()) {
        ErrorInfo errorInfo = errorInfoMap.get(pcInst.getCode());
        if (errorInfo != null) {
            pcInst.setErrorCode(errorInfo.errorCode);
            pcInst.setErrorMessage(errorInfo.errorMessage);
        }
    }
}
```

### 3.5 多实例场景

多实例场景下，每个实例独立判断：

```
driveI0: filter Speed=5400  → 找到部件 → 正常求解
driveI1: filter Speed=3000  → 找不到部件 → errorCode=1
```

每个 `PartCategoryInst` 有独立的 `errorCode` 和 `errorMessage`。

---

## 4. 验收准则

### 4.1 功能验收用例

#### ERR-001：单个分类过滤为空

**目的**：验证单个 PartCategory 过滤为空时，不阻断整体求解，错误信息写入输出。

**测试数据（极简）**：
```java
@PartAnno(code = "cpu")
@DAttrAnno1(code = "CoreNum", options = {"CoreNum_4:4", "CoreNum_8:8"})
private PartCategoryVar cpu;

@PartAnno(fatherCode = "cpu", attrs = {"4"}, price = 100)
private PartVar cpu4;

@PartAnno(code = "drive")
@DAttrAnno1(code = "Speed", options = {"Speed_5400:5400"})
private PartCategoryVar drive;

@PartAnno(fatherCode = "drive", attrs = {"5400"}, price = 50)
private PartVar drive1;
```

**测试用例**：

| ID | 过滤条件 | 预期行为 |
|----|----------|----------|
| ERR-001-1 | cpu: where CoreNum=8, drive: where Speed=5400 | cpu 过滤为空，drive 正常 |
| ERR-001-2 | cpu: where CoreNum=4, drive: where Speed=7200 | drive 过滤为空，cpu 正常 |
| ERR-001-3 | cpu: where CoreNum=4, drive: where Speed=5400 | 两个都匹配，无错误 |

**验证逻辑**：

新增断言方法：

```java
// 独立检查某个分类的错误码
// expect 格式: "cpu:FILTER_EMPTY" → categoryCode:errorCodeName
protected void assertSoluErrorContain(int soluIndex, String expect) { ... }

// 增强: 解字符串中直接携带错误码
// 格式: "drive1(Q:1,H:0,S:1),cpu:FILTER_EMPTY"
// 逗号分隔，后半段为分类级错误码
```

```java
// ERR-001-1: cpu 过滤为空，drive 正常
@Test
public void testErr001_CpuFilterEmpty_DriveOk() {
    inferRecommendModule(
        "cpu: where CoreNum=8",
        "drive: where Speed=5400");
    printSolutions();

    resultAssert().assertCode(Result.PARTIAL_SUCCESS);
    // 一行断言: drive1选中 + cpu报FILTER_EMPTY
    assertSoluContain(1, "drive1(Q:1,H:0,S:1),cpu:FILTER_EMPTY");
}

// ERR-001-2: drive 过滤为空，cpu 正常
@Test
public void testErr001_CpuOk_DriveFilterEmpty() {
    inferRecommendModule(
        "cpu: where CoreNum=4",
        "drive: where Speed=7200");
    printSolutions();

    resultAssert().assertCode(Result.PARTIAL_SUCCESS);
    assertSoluContain(1, "cpu4(Q:1,H:0,S:1),drive:FILTER_EMPTY");
}

// ERR-001-3: 两个都正常
@Test
public void testErr001_BothOk() {
    inferRecommendModule(
        "cpu: where CoreNum=4",
        "drive: where Speed=5400");
    printSolutions();

    resultAssert().assertCode(Result.SUCCESS);
    assertSoluContain(1, "cpu4(Q:1,H:0,S:1),drive1(Q:1,H:0,S:1)");
}
```

#### ERR-002：两个分类都过滤为空

| ID | 过滤条件 | 预期行为 |
|----|----------|----------|
| ERR-002-1 | cpu: where CoreNum=8, drive: where Speed=7200 | 两个都为空，无有效分类 |

```java
@Test
public void testErr002_BothFilterEmpty() {
    inferRecommendModule(
        "cpu: where CoreNum=8",
        "drive: where Speed=7200");
    printSolutions();

    resultAssert().assertCode(Result.NO_SOLUTION);
    // 也可只检查错误
    assertSoluErrorContain(1, "cpu:FILTER_EMPTY");
    assertSoluErrorContain(1, "drive:FILTER_EMPTY");
}
```

#### ERR-003：多实例部分过滤为空

**目的**：验证多实例场景下，每个实例独立报告错误。

**测试数据（极简）**：
```java
@PartAnno(code = "drive", instCodes = "driveI0,driveI1", supportMultiInst = true)
@DAttrAnno1(code = "Speed", options = {"Speed_5400:5400", "Speed_7200:7200"})
private PartCategoryVar drive;

@PartAnno(fatherCode = "drive", attrs = {"5400"}, price = 50)
private PartVar drive1;

@PartAnno(fatherCode = "drive", attrs = {"7200"}, price = 80)
private PartVar drive2;
```

| ID | 过滤条件 | 预期行为 |
|----|----------|----------|
| ERR-003-1 | driveI0: where Speed=5400, driveI1: where Speed=3000 | driveI0 正常，driveI1 过滤为空 |
| ERR-003-2 | driveI0: where Speed=5400, driveI1: where Speed=7200 | 两个都正常 |

**验证逻辑**：

```java
// ERR-003-1: 多实例，第1个实例正常，第2个实例过滤为空
@Test
public void testErr003_MultiInst_PartialEmpty() {
    inferRecommendModule(
        "driveI0: where Speed=5400",
        "driveI1: where Speed=3000");
    printSolutions();

    resultAssert().assertCode(Result.PARTIAL_SUCCESS);
    assertSoluContain(1, "drive1(Q:1,H:0,S:1),driveI1:FILTER_EMPTY");
}

// ERR-003-2: 多实例，两个都正常
@Test
public void testErr003_MultiInst_BothOk() {
    inferRecommendModule(
        "driveI0: where Speed=5400",
        "driveI1: where Speed=7200");
    printSolutions();

    resultAssert().assertCode(Result.SUCCESS);
    assertSoluContain(1, "drive1(Q:1,H:0,S:1),drive2(Q:1,H:0,S:1)");
}
```

---

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
|------|------|----------|
| 分类代码不存在 | `nonexist: where X=Y` | 忽略，不产生错误 |
| 无过滤条件（全匹配） | `cpu:` | 返回所有部件，errorCode=0 |
| 部分分类无请求 | 有 cpu/drive 两个分类，只请求 cpu | drive 正常处理（无过滤，全量），不产生错误 |

### 4.3 回归测试

- 现有所有测试用例的 result.code 不变（不会出现意外的 PARTIAL_SUCCESS）
- `SolutionUtils.printSolutions()` 输出中增加 errorCode/errorMessage 的展示

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 |
|------|------|--------|
| 1 | `InstErrorCode` 常量类 | P0 |
| 2 | `PartCategoryInst` 增加 `errorCode` + `errorMessage` | P0 |
| 3 | `Result` 增加 `PARTIAL_SUCCESS = 3` | P0 |
| 4 | 改造 `filterClone` — 不抛异常，收集空分类信息 | P0 |
| 5 | 改造 `ModuleAlgImpl.initData()` — 跳过空分类 | P0 |
| 6 | 结果组装阶段写入错误信息到 PartCategoryInst | P0 |
| 7 | `SolutionUtils.printSolutions()` 展示错误信息 | P1 |
| 8 | 编写测试 ERR-001 ~ ERR-003 | P1 |
| 9 | 回归测试 | P1 |

---

## 6. 参考资料

- JMix CORE-DESIGN.md — 对称设计架构
- JMix ACCEPTANCE.md — 测试用例设计规范
- Rust RFC Format: https://github.com/rust-lang/rfcs
