# RFC-0002 增强建议：部件分类过滤为空时的输出处理

> 评审日期：2026-04-30
> 评审对象：`doc/RFC-0002-PartCategory-Filter-Empty-Handling.md`
> 参考材料：`.specstory/history/rfc002_conversation.md`、`doc/CORE-DESIGN.md`、`doc/ACCEPTANCE.md`

---

## 1. 总体判断

当前 RFC 的核心方向是正确的：输入协议不变，将过滤为空的问题从“全局异常”调整为“实例级输出诊断”，并继续返回其他可求解分类的结果。这与用户在对话中明确的四点决策一致：

- 部分成功使用新增 `Result.PARTIAL_SUCCESS`
- 不新增输入模型，不引入 `ErrorPartCategoryInput`
- 错误信息落在 `PartCategoryInst`，包含 `errorCode` 和 `errorMessage`
- 与 `DecisionStrategy` 的关系只报告过滤失败，不额外报告策略跳过

但当前文档仍偏“概念草案”，还需要补强错误信息如何流转、实例级 key 如何定义、结果码边界、空分类参与规则时如何避免误执行、以及无求解结果时如何承载错误输出等落地点。

---

## 2. 建议增强点

### 2.1 明确“输入不变”的真实含义

当前 RFC 写了“输入不变，只改输出”，但后文仍提出“创建空的 PartCategoryInput”。这容易被理解为新增输入类型或改变调用方输入。

建议改成：

- 北向请求 `InferParasReq`、`InferPartCategoryReq`、`PartConstraintReq` 不变
- 内部执行上下文可以新增错误诊断载体，例如 `PartCategoryFilterError`
- `PartCategoryInput` 只作为现有内部流程中的约束输入，不暴露给调用方
- 不新增 `ErrorPartCategoryInput`

### 2.2 增加实例级错误定位模型

现有 RFC 只说 `errorInfoMap.put(partCategoryCode, ...)`，这对多实例不够。多实例中同一个 `partCategoryCode` 可能有多个实例，必须用实例级 key。

建议定义：

```java
errorKey = partCategoryCode + "#" + instanceId
```

其中：

- 单实例默认 `instanceId = ModuleInst.DEFAULT_INSTANCE_ID`
- 显式多实例按现有 `PartCategoryInput.instId` 编号
- 枚举多实例使用当前实例实际 `PartCategoryInst.instanceId`
- 错误写回时用 `PartCategoryInst.code + instanceId` 匹配

### 2.3 明确错误诊断字段和消息规范

当前 RFC 只定义了 `errorCode` 和 `errorMessage`。建议至少补充错误上下文，便于定位：

- `categoryCode`
- `instanceId`
- `attrWhereCondition`
- `originalReqShortString`
- `matchedPartCount`

如果不希望扩展 `PartCategoryInst` 字段太多，可先放入 `errorMessage`，但 RFC 需要规定消息格式。

建议消息格式：

```text
No parts found for category=cpu, instanceId=0, condition=CoreNum=8, request=cpu: where CoreNum=8
```

### 2.4 明确结果码判定边界

当前 RFC 写了 `PARTIAL_SUCCESS`，但 `NO_SOLUTION` 场景仍不清晰。

建议定义：

| 场景 | `Result.code` | `data` |
|------|---------------|--------|
| 无过滤为空，求解有解 | `SUCCESS` | 正常 solutions |
| 存在过滤为空，且至少返回一个可用解 | `PARTIAL_SUCCESS` | 带错误的 solutions |
| 不存在过滤为空，但求解无解 | `NO_SOLUTION` | 可为空或冲突诊断解 |
| 所有请求实例均过滤为空，无法产生有效可选部件 | `NO_SOLUTION` | 建议返回一个诊断用 `ModuleInst`，其中对应 `PartCategoryInst` 带错误 |
| 存在过滤为空，但剩余分类被其他规则约束导致无解 | `NO_SOLUTION` | message 中说明既有过滤失败也无可行解 |
| 系统异常、模型非法 | `FAILED` | 无业务解 |

特别注意：如果所有分类都过滤为空，用户仍需要看到每个实例的失败原因。若 `data` 为空，会违背“输出端反馈错误”的目标。

### 2.5 明确空分类对规则构建的影响

当前 RFC 写“跳过约束构建”，但需要进一步说明：

- 空过滤实例不创建 `SingleInstPartCategoryAlgImpl`
- 不创建该实例下的 `PartVar`、`ParaVar`
- 不执行该实例的分类级规则
- 跨分类规则或模块级规则如果引用了空实例，应跳过或隔离，不能继续引用不存在变量
- `sumFunConstraint`、`inCompatible` 等关联规则不能因为某个空实例缺变量而导致全局异常

这正是原始问题的技术根因，建议在 RFC 中作为设计约束单独列出。

### 2.6 补充错误信息在执行流程中的传递位置

当前代码中 `filterClone()` 返回 `Pair<Module, List<PartCategoryInputBase>>`，不能携带错误诊断。RFC 需要给出可落地的返回结构。

建议新增内部结构：

```java
class FilterCloneResult {
    private Module filteredModule;
    private List<PartCategoryInputBase> partCategoryInputs;
    private List<PartCategoryFilterError> filterErrors;
}
```

然后替换 `Pair<Module, List<PartCategoryInputBase>>`。这不改变北向输入，只改变内部返回。

### 2.7 明确结果组装位置

当前 RFC 写“求解完成后遍历 solutions 写入错误”，但现有代码的 `ModuleInstSolutionCallBack` 是从 `moduleAlg.getPartCategoryAlgs()` 构造 `PartCategoryInst`。如果空实例根本不创建 alg，就不会自然出现在结果里。

建议改为：

- 正常实例仍由 `ModuleInstSolutionCallBack` 从 alg 构建
- 空过滤实例由后处理 `applyFilterErrors(solutions, filterErrors)` 补齐或标记
- 若某个错误实例没有对应 `PartCategoryInst`，创建一个只含 `code`、`instanceId`、`errorCode`、`errorMessage` 的 `PartCategoryInst`

### 2.8 增加与 `inferParas` 主流程的一致性说明

当前 RFC 主要描述 `processProduct`，但实际 `inferParas()` 中 `toModuleInput()` 也调用了 `filterClone()`。如果只改一个入口，会出现行为不一致。

建议明确影响范围：

- `processProduct(InferPartCategoryReq)`
- `inferParas(InferParasReq)`
- `toModuleInput(Module, InferParasReq)`
- 优先级求解和非优先级求解都需要统一处理

### 2.9 增强验收用例

当前验收用例覆盖了基本场景，但还需要补充：

- 单实例：过滤为空但无关联规则
- 单实例：过滤为空且存在跨分类 `inCompatible`
- 单实例：过滤为空且存在 `sumFunConstraint`
- 多实例：一个实例为空，一个实例正常，并存在 `SumSum` 约束
- 所有请求实例为空：验证 `NO_SOLUTION` 下仍能看到错误
- 非法分类 code：继续忽略，不误报过滤为空
- 过滤表达式语法非法：仍应 `FAILED`，不能当作 `FILTER_EMPTY`

### 2.10 增加向后兼容性与迁移说明

新增 `Result.PARTIAL_SUCCESS` 会影响调用方判断逻辑。RFC 需要明确：

- 旧调用方只判断 `SUCCESS` 时，可能会把部分成功当成非成功
- 推荐新增 `Result.isSuccessLike()` 或调用方同时接受 `SUCCESS`、`PARTIAL_SUCCESS`
- JSON 输出新增字段应保持默认值，避免破坏已有消费者

---

## 3. 建议优先级

| 优先级 | 建议项 |
|--------|--------|
| P0 | 实例级错误 key、`FilterCloneResult`、结果码边界、空实例补齐输出 |
| P0 | `processProduct` 与 `inferParas` 行为一致 |
| P0 | 空分类跳过变量与规则构建，避免跨规则引用异常 |
| P1 | 测试矩阵增强，尤其多实例与全空场景 |
| P1 | `SolutionUtils` 和 `toShortString` 展示错误 |
| P2 | 兼容性 helper，例如 `Result.isSuccessLike()` |

---

## 4. 建议结论

建议不要直接合入原 RFC，而是先将“实例级错误定位”和“空实例输出补齐”补进设计。否则实现时很容易出现两个问题：

- 多实例只能按分类 code 报错，无法区分哪个实例失败
- 空实例被跳过后，最终 `solutions` 中没有对应 `PartCategoryInst`，用户仍然看不到错误

完整修订稿见：`doc/RFC-0002-PartCategory-Filter-Empty-Handling-Revised.md`。
