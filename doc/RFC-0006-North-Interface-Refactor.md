# RFC-0006: 计算引擎北向接口重构

> 状态: 草案(Draft)
> 日期: 2026-05-17
> 相关文档: `doc/RFC-0005-North-South-Interface-Decoupling.md`, `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`

---

## 设计决议摘要

本 RFC 只讨论计算引擎的北向接口，即应用方如何通过 `ModuleConstraintExecutor` 请求计算、反推和后置计算。南向产品算法接口拆分到 `RFC-0005`。

| 主题 | 决议 |
| --- | --- |
| 接口边界 | 新增语义明确的 `calculate` / `infer`，保留现有 `inferParas` 和 `postCalculate` |
| 输出模型 | 不新增 `OutputSpec` 或新结果 DTO，统一返回 `Result<List<ModuleInst>>` |
| 后置计算 | 继续使用 `postCalculate(ModulePostCalcReq)`，不另起规则执行请求模型 |
| 入参层级 | 请求按 `Module -> PartCategory -> Part/Parameter` 组织 |
| 约束表达 | Requirement / Condition 是输入条件，不与 Module、PartCategory、Part、Parameter 并列 |
| 首期不做 | 不新增独立校验入口 |

---

## 1. 摘要

当前北向接口主要通过 `inferParas(InferParasReq)` 承载反推、部件数量计算、分类过滤、属性聚合和部分后置计算语义，接口名称和入参结构已经不能清楚表达调用意图。

本 RFC 提议新增语义明确的 `calculate(ModuleCalcReq)` 和 `infer(ReverseInferenceReq)`，保留现有 `inferParas(InferParasReq)` 作为兼容入口，并保留 `postCalculate(ModulePostCalcReq)` 作为已有配置解的 POST 后置计算入口。结果模型继续使用现有 `ModuleInst`，避免引入额外输出 DTO 导致落地风险增大。

---

## 2. 动机

### 2.1 当前问题

当前调用示例：

```java
InferParasReq req = new InferParasReq();
req.setModuleId(123L);
req.setPartCategoryCode("drive");
req.setPartConstraintReqs(List.of(capacityReq));
req.setPreParaInsts(preParas);
```

问题：

- `InferParasReq` 名称强调参数反推，但实际可能是在计算部件数量。
- `partCategoryCode` 和 `partConstraintReqs` 是后续追加字段，不能完整表达模块内多个 PartCategory、多实例和 Part 输入。
- `PartConstraintReq` 把属性聚合、过滤、比较、策略混在一个类里。
- 调用方需要从字段组合猜测本次是在计算、反推还是后置计算。

### 2.2 目标

目标调用：

```java
ModuleCalcReq req = new ModuleCalcReq();
req.setModule(ModuleSelector.byId(123L));

PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
drive.addDecisionStrategy(DecisionStrategy.asc("price"));
req.addPartCategory(drive);

Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.calculate(req);
```

设计目标：

- 接口名表达语义。
- 入参按领域对象组织。
- 输出继续使用现有 `ModuleInst`。
- 后置计算继续沿用 `ModulePostCalcReq`。

---

## 3. 设计方案

### 3.1 接口清单

```java
public interface ModuleConstraintExecutor {
    Result<Void> init(ConstraintConfig config);
    Result<Void> fini();
    Result<Void> addModule(Long rootModuleId, Module... modules);
    Result<Void> removeModule(Long moduleId);

    Result<List<ModuleInst>> calculate(ModuleCalcReq req);
    Result<List<ModuleInst>> infer(ReverseInferenceReq req);

    @Deprecated
    Result<List<ModuleInst>> inferParas(InferParasReq req);

    Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req);
}
```

接口语义：

| 接口 | 语义 | 输出 |
| --- | --- | --- |
| `calculate` | 计算配置结果，例如部件数量、参数值 | `Result<List<ModuleInst>>` |
| `infer` | 根据结果反推参数设定 | `Result<List<ModuleInst>>` |
| `inferParas` | 旧兼容入口 | `Result<List<ModuleInst>>` |
| `postCalculate` | 对已有配置解执行 POST 规则 | `Result<List<ModuleInst>>` |

首期不新增独立校验入口。给定组合是否可行，可先通过 `calculate/infer` 是否返回可行 `ModuleInst` 表达。

### 3.2 入参结构

```text
ModuleRequest
  |
  |-- ModuleSelector
  |-- ModuleParameterInput[]
  |-- ModuleRequirement[]
  |-- PartCategoryRequest[]
  |     |
  |     |-- PartCategoryParameterInput[]
  |     |-- PartCategoryRequirement[]
  |     |-- AttributeRequirement[]
  |     |-- PartRequest[]
  |           |
  |           |-- PartRequirement[]
  |           |-- PartQuantityInput
  |
  |-- SolveOptions
```

核心原则：

- `Module` 是请求根。
- `PartCategory` 是模块下的分类域。
- `Part` 是 PartCategory 下的具体部件。
- `Parameter` 可挂在 Module 级或 PartCategory 级。
- Requirement / Condition 是输入条件，不作为同级领域对象。
- 输出统一使用 `List<ModuleInst>`。

### 3.3 Module 级请求

```java
public final class ModuleSelector {
    private Long moduleId;
    private String moduleCode;
    private String moduleVersion;
}

public abstract class ModuleRequest {
    private ModuleSelector module;
    private List<ModuleParameterInput> parameters;
    private List<ModuleRequirement> requirements;
    private List<PartCategoryRequest> partCategories;
    private SolveOptions options;
}

public class ModuleParameterInput {
    private String parameterCode;
    private String value;
    private ComparatorOp comparator;   // EQ by default
}

public class ModuleRequirement {
    private String targetCode;
    private RequirementTargetType targetType;
    private ComparatorOp comparator;
    private String value;
}
```

### 3.4 PartCategory 级请求

```java
public class PartCategoryRequest {
    private String partCategoryCode;
    private Integer instanceId;
    private boolean multiInstance;

    private List<PartCategoryParameterInput> parameters;
    private List<PartCategoryRequirement> requirements;
    private List<AttributeRequirement> attributeRequirements;
    private List<PartRequest> parts;
    private List<DecisionStrategy> decisionStrategies;
}

public class PartCategoryParameterInput {
    private String parameterCode;
    private String value;
    private ComparatorOp comparator;   // EQ by default
}

public class PartCategoryRequirement {
    private PartCategoryRequirementType type; // QUANTITY, SELECTED_COUNT, etc.
    private ComparatorOp comparator;
    private String value;
}

public class AttributeRequirement {
    private AttrAggType aggType;       // SUM, SUM_SUM, ORG
    private String attrCode;
    private ComparatorOp comparator;
    private String value;
    private String where;
}
```

旧 `PartConstraintReq` 映射：

| 旧字段 | 新字段 |
| --- | --- |
| `partCategoryCode` | `PartCategoryRequest.partCategoryCode` |
| `attrType` | `AttributeRequirement.aggType` |
| `attrCode` | `AttributeRequirement.attrCode` |
| `attrComparator` | `AttributeRequirement.comparator` |
| `attrValue` | `AttributeRequirement.value` |
| `attrWhereCondition` | `AttributeRequirement.where` |
| `decisionStrategies` | `PartCategoryRequest.decisionStrategies` |

### 3.5 Part 级请求

```java
public class PartRequest {
    private String partCode;
    private PartSelection selection;       // REQUIRED, FORBIDDEN, OPTIONAL
    private Integer quantity;
    private List<PartRequirement> requirements;
}

public class PartRequirement {
    private PartRequirementType type;      // QUANTITY, SELECTED, ATTRIBUTE
    private String attrCode;
    private ComparatorOp comparator;
    private String value;
}
```

### 3.6 计算请求

```java
public class ModuleCalcReq extends ModuleRequest {
}
```

```java
ModuleCalcReq req = new ModuleCalcReq();
req.setModule(ModuleSelector.byCode("Server"));

PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
req.addPartCategory(drive);

Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.calculate(req);
```

部件数量、参数值、PartCategory 实例和 Part 实例都从返回的 `ModuleInst` 中读取。

### 3.7 反向推理请求

```java
public class ReverseInferenceReq extends ModuleRequest {
    private List<String> inferParameterCodes;
}
```

```java
ReverseInferenceReq req = new ReverseInferenceReq();
req.setModule(ModuleSelector.byId(123L));

PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addPart(new PartRequest("ssd1", PartSelection.REQUIRED, 2));
req.addPartCategory(drive);
req.inferParameter("diskType");

Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.infer(req);
```

### 3.8 后置计算请求

继续沿用现有 `ModulePostCalcReq`：

```java
public class ModulePostCalcReq {
    private Long moduleId;
    private String moduleCode;
    private List<ModuleInst> solutions;
}
```

`postCalculate` 对 `solutions` 中已有的配置解执行 `CalcStage.POST` 规则，并返回写回后的 `List<ModuleInst>`。

### 3.9 旧入口适配

```text
InferParasReq
  |
  v
NorthRequestAdapter
  |
  |-- no partConstraintReqs -> ReverseInferenceReq
  |-- has partConstraintReqs -> ModuleCalcReq
  |-- partCategoryCode only -> PartCategory-scoped calc
```

`inferParas` 保留为兼容入口，但新业务优先使用 `calculate` / `infer`。

---

## 4. 验收准则

### AC-001: calculate 返回现有 ModuleInst

```java
@Test
void calculatePartQuantityAndParameterValue() {
    ModuleCalcReq req = new ModuleCalcReq();
    req.setModule(ModuleSelector.byCode("Server"));

    PartCategoryRequest drive = new PartCategoryRequest();
    drive.setPartCategoryCode("drive");
    drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
    req.addPartCategory(drive);

    Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.calculate(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertFalse(result.getData().isEmpty());
}
```

### AC-002: infer 返回现有 ModuleInst

```java
@Test
void inferParameterByKnownPartQuantity() {
    ReverseInferenceReq req = new ReverseInferenceReq();
    req.setModule(ModuleSelector.byCode("Server"));

    PartCategoryRequest drive = new PartCategoryRequest();
    drive.setPartCategoryCode("drive");
    drive.addPart(new PartRequest("ssd1", PartSelection.REQUIRED, 2));
    req.addPartCategory(drive);
    req.inferParameter("diskType");

    Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.infer(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertFalse(result.getData().isEmpty());
}
```

### AC-003: postCalculate 写回 ModuleInst

```java
@Test
void postCalculateWritesBackToModuleInst() {
    ModulePostCalcReq req = new ModulePostCalcReq();
    req.setModuleCode("Server");
    req.setSolutions(buildSolvedModuleInsts());

    Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.postCalculate(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertFalse(result.getData().isEmpty());
}
```

### AC-004: 旧 inferParas 可适配

```java
@Test
void inferParasCanBeAdaptedToNewRequestModel() {
    InferParasReq oldReq = buildOldInferParasReq();

    Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(oldReq);

    assertEquals(Result.SUCCESS, result.getCode());
}
```

### AC-005: 不新增输出 DTO

预期：

- 北向计算结果返回 `Result<List<ModuleInst>>`。
- 文档和接口中不引入独立输出选择模型。
- 应用方通过现有 `ModuleInst` 读取部件、PartCategory 和参数结果。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 定义 `ModuleCalcReq`、`ReverseInferenceReq` 和共享请求对象 | P0 | 待开始 |
| 2 | 新增 `calculate` / `infer` 到 `ModuleConstraintExecutor` | P0 | 待开始 |
| 3 | 实现 `InferParasReq` 到新请求模型的适配层 | P0 | 待开始 |
| 4 | 保留并回归 `postCalculate(ModulePostCalcReq)` | P0 | 待开始 |
| 5 | 补齐计算、反推、后置计算回归测试 | P0 | 待开始 |
| 6 | 更新调用方文档和示例 | P1 | 待开始 |

---

## 6. 风险与兼容策略

### 6.1 输出模型发散风险

如果新增一套输出 DTO，调用方和引擎内部需要同时维护两套结果模型。

缓解：

- 首期只返回 `List<ModuleInst>`。
- 后续如需要视图层输出，可以在应用层或独立查询接口中补充，不进入首期主计算接口。

### 6.2 旧入口迁移风险

大量已有调用依赖 `inferParas`。

缓解：

- `inferParas` 保留兼容入口。
- 新增适配层，不要求调用方一次性迁移。
- 新业务和新文档优先使用 `calculate` / `infer`。

### 6.3 独立校验入口缺失风险

部分调用方可能希望直接校验组合是否可行。

缓解：

- 首期通过 `calculate/infer` 是否返回可行 `ModuleInst` 表达。
- 后续如需求明确，再基于 `ModuleInst` 结果模型设计独立校验入口。

---

## 7. 已确认决策

- 北向命名使用 `PartCategory`，不引入 `Classifier`。
- 北向入参按 `Module -> PartCategory -> Part/Parameter` 组织。
- 约束不是独立领域对象，而是参数、部件或属性聚合上的输入条件。
- 输出统一使用 `List<ModuleInst>`。
- 保留 `postCalculate(ModulePostCalcReq)`。
- 首期不新增独立校验入口和规则执行入口。

---

## 8. 参考资料

- `doc/RFC-0005-North-South-Interface-Decoupling.md`
- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `src/main/java/com/jmix/executor/ModuleConstraintExecutor.java`
- `src/main/java/com/jmix/executor/model/InferParasReq.java`
- `src/main/java/com/jmix/executor/model/ModulePostCalcReq.java`
- `src/main/java/com/jmix/executor/cmodel/ModuleInst.java`
