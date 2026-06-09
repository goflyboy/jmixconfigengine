# RFC-0012: PartCategory Where-Only 默认数量约束与单 IEQ 多汇总条件

> 状态：草案（Draft）
> 日期：2026-06-09
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0002-PartCategory-Filter-Empty-Handling.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`, `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`

---

## 设计决策摘要


| 主题    | 决策                                                                                               |
| ----- | ------------------------------------------------------------------------------------------------ |
| 问题 1  | `category: where ...` 不再只是过滤候选集；当单条 IEQ 只有 `where` 且没有汇总条件时，标准化为 `Sum_Quantity >= 1 where ...`   |
| 问题 2  | 单条 IEQ 支持多个共享同一个 `where` 的汇总条件，例如 `drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400` |
| 语法范围  | P0 支持 `&&` 连接多个汇总条件；所有汇总条件共享同一个 `where`；不支持 OR、括号、每个汇总条件独立 `where`                                                    |
| 数据模型  | 1.0 版本直接以 `aggregateConditions` 列表作为汇总条件载体，不设置单汇总字段                   |
| 执行方式  | 过滤仍只执行一次；过滤后的同一候选集上依次构建多个 sum 约束                                                                 |
| 多实例兼容 | 单条 IEQ 的多个汇总条件不能被拆成多条 `PartConstraintReq`，否则会被现有逻辑误判为多个实例请求                                      |
| P0 范围 | 优先覆盖单 PartCategory 的运行时输入、测试 DSL、求解路径；跨 PartCategory 总量约束暂不扩展多汇总                                 |
| 过滤为空  | where-only 过滤为空时沿用 RFC-0002，继续返回 `FILTER_EMPTY` 诊断              |


---

## 1. 摘要

当前 PartCategory 输入约束的核心形状是：

```text
category:Sum_Attr comparator value where predicate
```

这个形状隐含了两个限制：

1. 当输入只有 `where` 时，系统主要执行候选部件过滤，不会为该输入构建明确的汇总约束，也不会给对应汇总参数设置输入值。
2. 一条 IEQ 只能携带一个汇总条件，无法表达“同一个过滤结果上同时满足容量和数量要求”。

本 RFC 提议：

```text
allInOne:where Memory>=512
```

标准化为：

```text
allInOne:Sum_Quantity >=1 where Memory>=512
```

并支持：

```text
drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400
```

其语义为：先按 `Speed=5400` 过滤出硬盘候选，再在同一批候选上同时构建：

```text
sum(Capacity * Quantity) >= 10
sum(Quantity) == 3
```

 

---

## 2. 动机

### 2.1 Where-Only 输入缺少可计算约束

示例：

```text
allInOne:where Memory>=512
```

当前这类输入更接近“只过滤候选集”。在执行链路中，`PartCategoryConstraintReqBase` 没有 `attrCode/attrComparator/attrValue`，`PartCategoryInputBase.comparator` 为空，因此 `ModuleBaseAlgImpl.setPartCategoryInput(...)` 不会调用 `sumFunConstraint(...)`。

这会带来两个问题：

1. 求解模型中没有来自该输入的明确表达式，后续逻辑很难判断这是一条用户请求还是一次纯内部过滤。
2. 仅靠过滤副作用无法稳定表达“用户要求至少有一个满足 where 的候选被选择”。

将 where-only 请求默认补成 `Sum_Quantity >= 1` 后，该请求具有明确、可计算、要求至少选择一个匹配候选的约束语义。

### 2.2 单 IEQ 需要支持多个汇总条件

业务需求示例：

1. 5400 转硬盘的总容量大于等于 10G
2. 且 5400 转硬盘的数量等于 3

期望输入：

```text
drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400
```

当前只能稳定承载一个汇总条件：

```text
drive:Sum_Capacity >=10 where Speed=5400
```

如果简单拆成两条请求：

```text
drive:Sum_Capacity >=10 where Speed=5400
drive:Sum_Quantity ==3 where Speed=5400
```

会触发现有 `filterClone(...)` 中“同一个 PartCategory 多条请求即多实例”的分支，语义会从“同一个过滤集上的两个条件”变成“同一分类的多个实例请求”，这是错误的。

因此，多个汇总条件必须作为单条 IEQ 的内部结构保留下来，而不能在标准化阶段拆成多条 `PartConstraintReq`。

---

## 3. 当前代码限制

### 3.1 请求模型只有一个汇总条件

`PartCategoryConstraintReqBase` 当前字段：

```java
private AttrParaType attrType = AttrParaType.Sum;
private String attrCode;
private String attrComparator;
private String attrValue;
private String attrWhereCondition;
```

它只能表达一个 `attrCode comparator attrValue`。

### 3.2 测试 DSL 解析只匹配一个表达式

`ModuleScenarioTestBase.parseAttrExpr(...)` 当前逻辑：

```java
Pattern pattern = Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=|<=|>=|<|>)\\s*(\\d+)");
Matcher matcher = pattern.matcher(trimmedExpr);

if (matcher.matches()) {
    ...
} else {
    throw new IllegalArgumentException("Invalid attribute expression format: " + trimmedExpr);
}
```

`Sum_Capacity >=10 && Sum_Quantity ==3` 无法匹配。

### 3.3 执行输入也只有一个约束

`PartCategoryInputBase` 当前字段：

```java
private AttrParaType attrType = AttrParaType.Sum;
private String sumAttrCode;
private String comparator;
private int leftValue;
private PartConstraintReq orgReq;
```

`SingleInstPartCategoryAlgImpl.sumFunConstraint(...)` 和 `MultiInstPartCategoryAlgImpl.sumFunConstraint(...)` 都只构建一个表达式并应用一次比较。

---

## 4. 术语与边界

### 4.1 IEQ

本文中的 IEQ 指一条 PartCategory 输入表达式，也就是用户或测试 DSL 中的一条字符串请求：

```text
drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400
```

### 4.2 过滤谓词

`where` 后的表达式称为过滤谓词，用于决定候选部件集合：

```text
where Speed=5400
where Memory>=512
```

### 4.3 汇总条件

`where` 前的每个聚合比较称为汇总条件：

```text
Sum_Capacity >=10
Sum_Quantity ==3
```

### 4.4 P0 不处理的范围

P0 不处理以下能力：

1. `Sum_Capacity >=10 where Speed=5400 && Sum_Quantity ==3 where Type=sd`
2. `Sum_Capacity >=10 || Sum_Quantity ==3 where Speed=5400`
3. 跨 PartCategory 总量约束中的多汇总，例如 `disk,mainBoard:Sum_Capacity >=100 && Sum_Quantity >=3 where PortRate=10G`
4. 拼写纠错，例如把 `Sum_Capactity` 自动改成 `Sum_Capacity`

---

## 5. 设计方案

### 5.1 DSL 语法

P0 支持的语法：

```text
PartCategoryReq := Category ":" AggregateClause? WhereClause? StrategyClause?
AggregateClause := AggregateExpr ("&&" AggregateExpr)*
AggregateExpr := AttrParaType "_" AttrCode Comparator Integer
WhereClause := "where" FilterExpr
Comparator := "==" | "!=" | "<=" | ">=" | "<" | ">"
```

示例：

```text
drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400
allInOne:where Memory>=512
drive:Sum_Quantity ==2 where Speed=5400 [strategy=ASCENDING:price]
```

约束：

1. `&&` 只连接 `where` 之前的汇总条件。
2. 一条 IEQ 最多一个 `where`。
3. `strategy` 仍作用于整条 IEQ，不作用于某个单独汇总条件。
4. where-only 请求会补一个默认汇总条件。

### 5.2 新增汇总条件模型

新增模型：

```java
package com.jmix.executor.model;

import com.jmix.executor.bmodel.AttrParaType;
import lombok.Data;

@Data
public class AggregateConditionReq {
    private AttrParaType attrType = AttrParaType.Sum;
    private String attrCode;
    private String comparator;
    private String attrValue;
    private boolean defaulted;
}
```

扩展 `PartCategoryConstraintReqBase`。1.0 版本不设置单汇总字段，所有汇总条件都进入列表：

```java
private List<AggregateConditionReq> aggregateConditions = new ArrayList<>();
```

标准化规则：

```java
List<AggregateConditionReq> effectiveAggregateConditions(PartCategoryConstraintReqBase req) {
    if (!req.getAggregateConditions().isEmpty()) {
        return req.getAggregateConditions();
    }
    if (hasWhereOnly(req)) {
        AggregateConditionReq defaultReq = new AggregateConditionReq();
        defaultReq.setAttrType(AttrParaType.Sum);
        defaultReq.setAttrCode(PartConstantAttr.Quantity.getCode());
        defaultReq.setComparator(">=");
        defaultReq.setAttrValue("1");
        defaultReq.setDefaulted(true);
        return List.of(defaultReq);
    }
    return List.of();
}
```

### 5.3 解析流程

`parseAttrExpr(...)` 调整为：

1. 先按 `where` 拆出过滤谓词。
2. 如果 `where` 前为空，设置 `attrWhereCondition`，并写入默认 `Sum_Quantity >=1` 汇总条件。
3. 如果 `where` 前不为空，按 `&&` 拆分多个汇总条件。
4. 每个汇总条件沿用当前正则解析 `AttrParaType_AttrCode comparator value`。
5. 解析后的所有汇总条件都写入 `aggregateConditions`。

示意代码：

```java
protected void parseAttrExpr(String attrExpr, PartCategoryConstraintReqBase req) {
    ParsedWhere parsed = splitWhere(attrExpr);
    req.setAttrWhereCondition(parsed.whereCondition());

    if (parsed.aggregateClause().isBlank()) {
        addDefaultQuantityCondition(req);
        return;
    }

    for (String expr : parsed.aggregateClause().split("\\s*&&\\s*")) {
        req.getAggregateConditions().add(parseAggregateCondition(expr));
    }
}
```

非法输入应尽早抛出 `IllegalArgumentException`：

```text
drive:Sum_Capacity >=10 && where Speed=5400
drive:Sum_Capacity >=10 && Sum_Quantity where Speed=5400
drive:Sum_Capacity >=10 || Sum_Quantity ==3 where Speed=5400
```

### 5.4 执行输入模型

新增执行侧条件模型：

```java
package com.jmix.executor.impl;

import com.jmix.executor.bmodel.AttrParaType;
import lombok.Data;

@Data
public class AggregateConditionInput {
    private AttrParaType attrType = AttrParaType.Sum;
    private String sumAttrCode;
    private String comparator;
    private int leftValue;
    private boolean defaulted;
}
```

扩展 `PartCategoryInputBase`。执行侧同样不设置单汇总字段：

```java
private List<AggregateConditionInput> aggregateConditions = new ArrayList<>();
```

`PartCategoryConstraintExecutorImpl.setPartCategoryInputBase(...)` 从请求中取有效汇总条件，写入 `aggregateConditions`。

### 5.5 构建多个求解约束

`ModuleBaseAlgImpl.setPartCategoryInput(...)` 不再只判断 `partCategoryInput.getComparator()`，而是判断有效汇总条件列表：

```java
protected void setPartCategoryInput(PartCategoryInputBase input) {
    if (input == null) {
        return;
    }
    setPartCategoryInputVariables(input);
    if (!input.getEffectiveAggregateConditions().isEmpty()) {
        model.addRuleSeperator("input_constraint_" + input.getPartCategoryCode() + this.getInstId());
        sumFunConstraint(this, input);
    }
}
```

`setPartCategoryInputVariables(...)` 需要为每个汇总条件设置对应输入参数：

```java
for (AggregateConditionInput condition : input.getEffectiveAggregateConditions()) {
    String paraCode = condition.getAttrType().name() + AttrPara.CODE_SEPARATOR + condition.getSumAttrCode();
    ParaVarImpl pVar = getOrCreateAttrParaVar(paraCode);
    pVar.setInputValue(condition.getLeftValue());
}
```

`SingleInstPartCategoryAlgImpl.sumFunConstraint(...)` 改成循环：

```java
for (AggregateConditionInput condition : partConstraint.getEffectiveAggregateConditions()) {
    PartAlgCPLinearExpr expr = buildSumExpr(
            singleInstPartCategoryAlgImpl,
            condition.getSumAttrCode(),
            PartVarImpl.QTY_SHORT_NAME,
            PartVarImpl::getQty,
            "");
    ComparisonOperator operator = ComparisonOperator.fromSymbol(condition.getComparator());
    operator.applyConstraint(model, expr, condition.getLeftValue());
}
```

`MultiInstPartCategoryAlgImpl.sumFunConstraint(...)` 同理循环，但 P0 不允许在同一 IEQ 中混用 `Sum` 和 `SumSum`。如果 `aggregateConditions.size() > 1` 且存在 `AttrParaType.SumSum`，输入标准化阶段直接报错。

### 5.6 Where-Only 默认条件的请求语义

当：

```text
allInOne:where Memory>=512
```

被标准化为：

```text
allInOne:Sum_Quantity >=1 where Memory>=512
```

该语义表示：用户不仅要求过滤候选集，还要求过滤后的候选中至少有一个被选择。1.0 版本不再引入额外输入态概念；业务规则应基于标准化后的请求条件和求解表达式判断请求存在性。

### 5.7 过滤为空语义

where-only 过滤为空时沿用 RFC-0002：仍记录 `FILTER_EMPTY` 诊断，并按现有过滤为空流程返回 `NO_SOLUTION` 或部分成功结果。

```text
drive:where Speed=3000
```

如果 `Speed=3000` 找不到候选部件，则不因为默认补了 `Sum_Quantity >=1` 而跳过诊断；该请求本身要求至少一个匹配候选，因此空过滤结果应视为无法满足。

---

## 6. 接口矩阵


| 入口                                          | 是否影响 | 设计                                             |
| ------------------------------------------- | ---- | ---------------------------------------------- |
| `ModuleScenarioTestBase` 字符串 DSL            | 是    | 支持 `&&` 多汇总；where-only 补默认数量条件                 |
| `InferParasReq.partConstraintReqs`          | 是    | `PartConstraintReq` 继承新增 `aggregateConditions` |
| `InferParasReq.partCategoryConstraintReqs`  | 是    | `toPartConstraintReq(...)` 复制新增列表              |
| `InferParasReq.crossCategoryConstraintReqs` | 暂不扩展 | P0 校验最多一个汇总条件                                  |
| `PartCategoryConstraintReq.toShortString()` | 是    | 输出多汇总时使用 `&&` 拼接；where-only 使用标准化后的 `Sum_Quantity >=1 where ...` |
| `filterClone(...)`                          | 是    | 单条多汇总 IEQ 仍作为一条 req 过滤一次                       |
| `PartCategoryInputBase`                     | 是    | 新增执行侧汇总条件列表                                    |
| `SingleInstPartCategoryAlgImpl`             | 是    | 对同一过滤候选集循环构建多个约束                               |
| `MultiInstPartCategoryAlgImpl`              | 是    | 对多实例聚合循环构建；P0 禁止混用 `Sum` 和 `SumSum`            |
| 注解生成                                        | 否    | 本 RFC 不新增产品侧注解字段                               |
| 规则翻译模块                                      | 暂不影响 | 仅当规则翻译要生成这种 DSL 时再补测试                          |


---

## 7. 复用优先清单

### 7.1 优先复用

- `PartCategoryConstraintReqBase`：作为新增 `aggregateConditions` 的承载点
- `PartConstraintReq` / `PartCategoryConstraintReq`：继续作为单 PartCategory 请求入口
- `ModuleScenarioTestBase.parseAttrExpr(...)`：扩展现有测试 DSL 解析
- `PartCategoryConstraintExecutorImpl.setPartCategoryInputBase(...)`：统一请求到执行输入的转换
- `ModuleBaseAlgImpl.buildSumExprInternal(...)`：继续复用数量与属性加权求和能力
- `ComparisonOperator.fromSymbol(...)`：继续复用比较符语义

### 7.2 不新增

- 不新增一个平行的 `MultiAggregatePartConstraintReq` 顶层请求类型
- 不用多条 `PartConstraintReq` 模拟单 IEQ 多汇总
- 不新增产品侧注解字段
- 不新增独立解析框架
- 不修改跨 PartCategory 总量约束的 P0 语义

### 7.3 可由上下文推导

- `partCategoryCode` 仍由 `category:` 前缀或调用上下文推导
- `AttrParaType` 由 `Sum_` / `SumSum_` 前缀推导
- `Quantity` 由 where-only 默认规则推导，用户不需要显式填写
- 多个汇总条件共享同一个 `where`

### 7.4 相似测试来源

- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`
- `src/test/java/com/jmix/scenario/ruletest/PostCalcRuleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/PartCategoryFilterEmptyTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java`

---

## 8. 正反语义矩阵


| 输入 | 预期 |
| --- | --- |
| `drive:where Speed=5400` | 等价于 `drive:Sum_Quantity >=1 where Speed=5400` |
| `drive:Sum_Quantity >=1 where Speed=5400` | 与 where-only 默认语义一致 |
| `drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400` | 同一过滤集上同时添加容量和数量约束 |
| `drive:Sum_Capacity >=10 && Sum_Quantity ==3` | 不过滤候选集，在全量 drive 候选上同时添加两个约束 |
| `drive:Sum_Capacity >=10 && where Speed=5400` | 非法输入 |
| `drive:Sum_Capacity >=10 OR Sum_Quantity ==3 where Speed=5400` | P0 非法输入，不支持 OR |
| `drive:Sum_Capacity >=10 && SumSum_Quantity >=3 where Speed=5400` | P0 非法输入，禁止同一 IEQ 混用 `Sum` 和 `SumSum` |
| `drive:Sum_Capactity >=10 && Sum_Quantity ==3 where Speed=5400` | 非法输入，不做拼写纠错 |


---

## 9. 验收标准

### 9.1 Where-Only 会补默认数量条件

新增测试建议放在 `src/test/java/com/jmix/scenario/ruletest/PartCategoryWhereOnlyAndMultiAggregateTest.java`。

```java
@Test
public void testWhereOnly_DefaultsToQuantityGreaterOrEqualOne() {
    inferRecommendModule("drive:where Speed=5400");
    printSimpleSolutions();

    resultAssert().assertSuccess();
    assertSoluContain("d5400");
}
```

预期：

1. 解析结果包含一个默认汇总条件：`Sum_Quantity >=1`
2. 求解链路调用 `sumFunConstraint(...)`
3. 默认约束要求至少选择一个满足 `Speed=5400` 的候选
4. 非 5400 候选不参与该 PartCategory 的求解候选集

### 9.2 单 IEQ 多汇总会构建两个约束

```java
@Test
public void testSingleIeq_MultiAggregateConditionsShareSameWhere() {
    inferRecommendModule("drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400");
    printSimpleSolutions();

    resultAssert().assertSuccess();
    assertSoluContain("d5400A");
    assertSoluContain("d5400B");
}
```

预期：

1. 只执行一次 `Speed=5400` 过滤
2. `aggregateConditions.size() == 2`
3. 构建 `sum(Capacity * Quantity) >= 10`
4. 构建 `sum(Quantity) == 3`
5. `Speed=7200` 的候选不参与两个表达式

### 9.3 多汇总不能被误判为多实例

```java
@Test
public void testSingleIeq_MultiAggregateDoesNotCreateMultiInstInput() {
    inferRecommendModule("drive:Sum_Capacity >=10 && Sum_Quantity ==3 where Speed=5400");
    printSimpleSolutions();

    resultAssert().assertSuccess();
    // 断言执行输入为单个 PartCategoryInput，而不是 MultiInstPartCategoryInput。
}
```

如果测试基类暂时没有暴露执行输入，可通过日志或新增包内可见断言 helper 验证。

### 9.4 单汇总语法仍然有效

```java
@Test
public void testSingleAggregateRequestStillWorks() {
    inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
    printSimpleSolutions();

    resultAssert().assertSuccess();
    assertSoluContain("d5400");
}
```

### 9.5 非法多汇总输入会失败

```java
@Test
public void testInvalidMultiAggregateSyntaxFailsFast() {
    assertThrows(IllegalArgumentException.class,
            () -> inferRecommendModule("drive:Sum_Capacity >=10 && where Speed=5400"));
}
```

### 9.6 Where-Only 过滤为空的语义测试

```java
@Test
public void testWhereOnly_FilterEmptyKeepsDiagnostic() {
    inferRecommendModule("drive:where Speed=3000");
    printSimpleSolutions();

    resultAssert().assertNoSolution();
    assertSoluContain("drive:FILTER_EMPTY");
}
```

---

## 10. 实现计划

### P0-1 请求模型扩展

- 新增 `AggregateConditionReq`
- `PartCategoryConstraintReqBase` 新增 `aggregateConditions`
- 移除单汇总字段，统一通过 `aggregateConditions` 承载汇总条件
- `PartCategoryConstraintReq.toShortString()` 支持多汇总输出

### P0-2 DSL 解析增强

- `ModuleScenarioTestBase.parseAttrExpr(...)` 支持 `&&`
- where-only 自动补 `Sum_Quantity >=1`
- 非法语法 fail fast
- 策略语法 `[strategy=...]` 仍在解析汇总前剥离

### P0-3 执行输入扩展

- 新增 `AggregateConditionInput`
- `PartCategoryInputBase` 新增 `aggregateConditions`
- `PartCategoryConstraintExecutorImpl.setPartCategoryInputBase(...)` 写入所有条件
- `ModuleConstraintExecutorImpl.toPartConstraintReq(...)` 复制新增列表

### P0-4 求解构建增强

- `ModuleBaseAlgImpl.setPartCategoryInputVariables(...)` 循环设置输入参数
- `SingleInstPartCategoryAlgImpl.sumFunConstraint(...)` 循环构建多个 sum 约束
- `MultiInstPartCategoryAlgImpl.sumFunConstraint(...)` 循环构建多个 sum 约束
- 禁止同一 IEQ 混用 `Sum` 与 `SumSum`

### P0-5 回归测试

- where-only 默认数量约束
- 单 IEQ 多汇总共享同一 where
- 多汇总不触发多实例分支
- 单汇总语法回归
- 非法语法 fail fast
- where-only 过滤为空沿用 RFC-0002 诊断

---

## 11. 已确认决策

1. where-only 默认补 `Sum_Quantity >=1`。
2. where-only 过滤为空时沿用 RFC-0002，继续报告 `FILTER_EMPTY`。
3. 1.0 版本不引入额外输入态概念。
4. 多汇总条件之间 P0 仅支持 `&&`，即所有条件必须同时满足。
5. P0 暂不扩展跨 PartCategory 总量约束的多汇总能力。

跨 PartCategory 多汇总示例暂不支持：

```text
disk,mainBoard:Sum_Capacity >=100 && Sum_Quantity >=3 where PortRate=10G
```

该能力可作为 RFC-0010 的后续增强单独实现。

---

## 12. 参考资料

- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReqBase.java`
- `src/main/java/com/jmix/executor/model/PartCategoryConstraintReq.java`
- `src/main/java/com/jmix/executor/impl/PartCategoryInputBase.java`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java`
- `src/main/java/com/jmix/executor/impl/ModuleBaseConstraintExecutorImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/SingleInstPartCategoryAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/MultiInstPartCategoryAlgImpl.java`
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java`
- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`
- `doc/RFC-0002-PartCategory-Filter-Empty-Handling.md`
- `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`
