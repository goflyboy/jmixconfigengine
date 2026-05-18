# RFC-0007: 结构化规则与组合规则 Schema

> 状态：草案（Draft）
> 日期：2026-05-17
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0006-North-Interface-Refactor.md`

---

## 设计决策摘要

本 RFC 讨论规则维护态和计算引擎之间的结构化规则表达。目标是让业务人员可以通过表格或拖拽控件维护规则，同时计算引擎仍然把规则编译为 CP-SAT 约束。

| 主题 | 决策 |
| --- | --- |
| 维护态表达 | 新增 `PairStructRuleSchema`、`TripleStructRuleSchema` 和 `CombinationStructRuleSchema` |
| 表达式粒度 | 使用 `StructExprSchema` 表示“对象 + 属性 + 比较符 + 值” |
| 父子规则关系 | 建议新增 `Rule.parentRuleCode`；不复用现有 `Rule.fatherCode`，因为 `fatherCode` 已表示规则挂载的 PartCategory |
| 关系类型 | 仅保留 `INCOMPATIBLE`、`REQUIRES`、`CO_DEPENDENT`；`CO_DEPENDENT` 同时表示兼容/配套关系 |
| 运行态表达 | 在 `ModuleGenneratorByAnno` 构建数据时生成 `Rule.exeSchema = CodependantRuleSchema` |
| 白名单组合规则 | 选中的部件组合必须命中至少一条允许组合 |
| 黑名单组合规则 | 每条禁止组合生成“不能同时选中”的约束 |
| 逻辑完备性 | 计算引擎只执行确定语义；维护态完备性策略不在本 RFC 建模 |
| 校验接口 | `ModuleConstraintExecutor` 新增 `validate(ModuleValidateReq)`，用于校验给定 `ModuleInst` 组合是否满足规则 |
| 二元/三元支持 | 首期支持二元和三元；更高元可由后续通用 `NaryStructRuleSchema` 扩展 |

---

## 1. 摘要

当前规则更偏向代码式或已有兼容规则 Schema，难以支撑客户通过表格化界面维护“CPU 核数 = 4 不兼容硬盘速率 = 5400”这类结构化规则。本 RFC 提议在 `Rule.rawCode` 中新增二元、三元结构化规则 Schema，并在 `ModuleGenneratorByAnno` 构建数据时展开为 `CodependantRuleSchema`，使维护态可读、运行态可执行。

---

## 2. 动机

### 2.1 问题背景

客户的开发人员希望用结构化表格维护规则，而不是直接编写 Java 代码或自由表达式。典型界面交互如下：

1. 选择左表达式：对象 `cpu`，属性 `CoreNum`，操作符 `=`，值 `4`。
2. 选择业务关系：例如“不兼容”。
3. 选择右表达式：对象 `drive`，属性 `Speed`，操作符 `=`，值 `5400`。

这类规则本质上是一个二元结构化表达：

```text
cpu.CoreNum = 4  INCOMPATIBLE  drive.Speed = 5400
```

当前系统中 `Rule.rawCode` 已经支持 `CompatiableRuleSchema`、`CodeRuleSchema`、`CalculateRuleSchema` 等类型，但缺少面向 UI 表格维护的通用结构化表达模型。

### 2.2 简单结构化规则场景

业务规则：

```text
4 核 CPU 不兼容 5400 转硬盘
```

维护态期望：

```json
{
  "@type": "PairStructRule",
  "expr1": {
    "objectType": "PartCategory",
    "objectCode": "cpu",
    "attrCode": "CoreNum",
    "operator": "EQ",
    "values": ["4"]
  },
  "relationType": "INCOMPATIBLE",
  "expr2": {
    "objectType": "PartCategory",
    "objectCode": "drive",
    "attrCode": "Speed",
    "operator": "EQ",
    "values": ["5400"]
  }
}
```

运行态期望：

- 过滤出所有 `CoreNum = 4` 的 CPU 部件。
- 过滤出所有 `Speed = 5400` 的硬盘部件。
- 对过滤结果中的每个二元组合生成不兼容约束。

### 2.3 组合规则场景

业务规则不总是单条二元关系，而可能是一组必须整体维护的子规则。例如 CPU 与硬盘的配套白名单：

1. 4 核 CPU 配套 5400 转硬盘。
2. 8 核 CPU 配套 7200 转或 5400 转硬盘。
3. 8 核以上 CPU 必须配套 7200 转硬盘。

这三条子规则属于同一个父规则。校验时，如果用户选择了 `cpu1(CoreNum=4)` 和 `drive1(Speed=5400)`，系统需要在该组合规则里检查是否存在一条子规则能够完全匹配。由于这是白名单，只有命中至少一条允许组合才通过；未命中的组合默认不允许。

黑名单也需要支持。例如：

```text
4 核 CPU 不配套 7000 转硬盘
```

黑名单语义下，命中黑名单的组合不允许；未命中的组合默认允许。

### 2.4 三元结构化规则场景

实际场景可能存在三元配套关系，例如：

```text
CPU、硬盘、显示器三者之间的配套关系
```

维护态需要支持类似：

```text
cpu.CoreNum = 8  CO_DEPENDENT  drive.Speed = 7200  CO_DEPENDENT  monitor.Resolution = 4K
```

组合规则中的每条子规则可以是二元，也可以是三元。二元组合生成二元 `PartCombination`；三元组合生成三元 `PartCombination`。

### 2.5 为什么需要改变

现有规则表达存在以下不足：

- 自由代码规则难以被表格 UI 稳定编辑和回显。
- 现有 `CompatiableRuleSchema` 更适合参数或对象间的直接兼容关系，不足以表达父规则下多条子规则组成的白名单/黑名单。
- 运行态缺少一个标准的“结构化表达 -> 部件集合 -> 组合约束”的编译过程。
- 维护态需要检查组合规则的唯一性和逻辑完备性，但计算引擎层不应承担维护系统的建模约束。

---

## 3. 设计方案

### 3.1 核心思路

设计分为两层：

1. 维护态 Schema：面向 UI 和持久化，描述用户选择的对象、属性、操作符、值和业务关系。
2. 运行态 Schema：面向 CP-SAT 执行，将维护态表达展开为具体部件编码组合，并生成约束。

流程如下：

```text
Rule.rawCode
  |
  |-- PairStructRuleSchema / TripleStructRuleSchema
  |-- CombinationStructRuleSchema + child rules
  |
  v
ModuleGenneratorByAnno / StructRuleBuildExpander
  |
  |-- resolve StructExprSchema to Part code set
  |-- expand Cartesian products
  |
  v
Rule.exeSchema = CodependantRuleSchema
  |
  v
CodependantConstraintExecutor
  |
  |-- WHITE: selected tuple must match at least one allowed combination
  |-- BLACK: forbidden tuple cannot be selected together
```

### 3.2 术语

| 术语 | 定义 |
| --- | --- |
| 结构化表达式 | 一个对象属性过滤条件，例如 `cpu.CoreNum = 4` |
| 结构化规则 | 二元或三元表达式加业务关系 |
| 组合规则 | 一个父规则下包含多条结构化子规则，并按白名单或黑名单执行 |
| 维护态 Schema | 存在于 `Rule.rawCode` 中，面向 UI 编辑 |
| 运行态 Schema | 存在于 `Rule.exeSchema` 中，由 `ModuleGenneratorByAnno` 构建 Module 时生成，面向约束生成 |
| PartCombination | 结构化规则展开后的部件编码元组 |

### 3.3 Rule 模型扩展

当前 `Rule` 已有 `rawCode` 和 `fatherCode`。其中 `fatherCode` 已用于表示规则挂载到哪个 PartCategory；如果为空，表示规则挂载到 Module。因此不建议再用 `fatherCode` 表示父子规则关系。

建议新增字段：

```java
public class Rule extends Extensible {
    private String code;
    private RuleSchema rawCode;

    /**
     * 现有字段：规则挂载的 PartCategory code。
     * 空字符串表示 Module 级规则。
     */
    private String fatherCode = "";

    /**
     * 新增字段：组合规则父规则 code。
     * 为空表示独立规则或组合父规则；非空表示组合规则的子规则。
     */
    private String parentRuleCode = "";

    /**
     * 新增字段：运行态编译后的 Schema。
     * 由 ModuleGenneratorByAnno 在构建 Module 数据时生成。
     */
    private RuleSchema exeSchema;
}
```

执行约定：

- `parentRuleCode` 为空且 `rawCode` 为普通结构化规则：该规则可独立执行。
- `parentRuleCode` 为空且 `rawCode` 为组合规则：该规则作为父规则执行。
- `parentRuleCode` 非空：该规则是组合子规则，只参与父规则编译，不单独执行。

### 3.4 RuleSchema 类型扩展

在 `RuleSchema` 的 JSON 子类型中新增：

```java
@JsonSubTypes({
    @JsonSubTypes.Type(value = CompatiableRuleSchema.class, name = "CompatiableRule"),
    @JsonSubTypes.Type(value = CalculateRuleSchema.class, name = "CalculateRule"),
    @JsonSubTypes.Type(value = SelectRuleSchema.class, name = "SelectRule"),
    @JsonSubTypes.Type(value = CodeRuleSchema.class, name = "CodeRule"),
    @JsonSubTypes.Type(value = PriorityRuleSchema.class, name = "PriorityRule"),

    @JsonSubTypes.Type(value = PairStructRuleSchema.class, name = "PairStructRule"),
    @JsonSubTypes.Type(value = TripleStructRuleSchema.class, name = "TripleStructRule"),
    @JsonSubTypes.Type(value = CombinationStructRuleSchema.class, name = "CombinationStructRule")
})
public abstract class RuleSchema {
}
```

`CodependantRuleSchema` 是由构建器生成的执行 Schema。它可以作为 `Rule.exeSchema` 随 Module 构建产物保存，也可以在 Module 构建后只保留在内存中；但计算引擎不得在求解时重新解析 `PairStructRuleSchema` / `TripleStructRuleSchema`。

### 3.5 结构化表达式 Schema

```java
public class StructExprSchema extends ExprSchema {
    private String objectType = RefProgObjSchema.PROG_OBJ_TYPE_PARTCATEGORY;
    private String objectCode;
    private String attrCode;
    private StructCompareOperator operator;
    private List<String> values = new ArrayList<>();
}
```

`StructExprSchema` 复用 `ExprSchema.rawCode` 保存由结构化字段生成的过滤表达式，例如 `CoreNum=4`、`Speed>=7200`。

比较操作符：

```java
public enum StructCompareOperator {
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    IN,
    NOT_IN,
    LIKE,
    NOT_LIKE
}
```

映射到现有过滤器：

| StructCompareOperator | FilterExpressionExecutor 表达 |
| --- | --- |
| `EQ` | `attr=value` |
| `NE` | `attr!=value` |
| `GT` | `attr>value` |
| `GE` | `attr>=value` |
| `LT` | `attr<value` |
| `LE` | `attr<=value` |
| `LIKE` | `attr like "value"` |
| `NOT_LIKE` | `attr not like "value"` |
| `IN` | 对多个 `EQ` 结果取并集 |
| `NOT_IN` | 对多个 `NE` 条件取交集 |

现有 `FilterExpressionExecutor` 已支持单条件和 AND 条件。`IN` 和 `NOT_IN` 可以由 `StructExprResolver` 在外层展开，不要求第一期修改过滤器语法。

### 3.6 二元结构化规则 Schema

```java
public class PairStructRuleSchema extends RuleSchema {
    private StructExprSchema expr1;
    private BusinessRelationType relationType;
    private StructExprSchema expr2;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return expr1 == null ? List.of() : expr1.getRefProgObjs();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        return expr2 == null ? List.of() : expr2.getRefProgObjs();
    }
}
```

业务关系：

```java
public enum BusinessRelationType {
    /**
     * 语义为“不能同时选择”。
     */
    INCOMPATIBLE,

    /**
     * 语义为“选择左侧时必须选择右侧”。
     */
    REQUIRES,

    /**
     * 语义为“左右必须同选或同不选”，也是业务口径中的“兼容/配套”关系。
     * 对应现有 CompatiableRuleSchema.Operator.CO_REFENT。
     */
    CO_DEPENDENT
}
```

说明：

- `INCOMPATIBLE`、`REQUIRES`、`CO_DEPENDENT` 可以作为独立硬约束执行。
- 业务表述中的“兼容”“配套”统一映射为 `CO_DEPENDENT`，不再单独定义 `COMPATIBLE`。

### 3.7 三元结构化规则 Schema

```java
public class TripleStructRuleSchema extends RuleSchema {
    private StructExprSchema expr1;
    private BusinessRelationType relationType;
    private StructExprSchema expr2;
    private StructExprSchema expr3;

    @Override
    public List<RefProgObjSchema> getFromLeftProgObjs() {
        return expr1 == null ? List.of() : expr1.getRefProgObjs();
    }

    @Override
    public List<RefProgObjSchema> getToRightProgObjs() {
        List<RefProgObjSchema> refs = new ArrayList<>();
        if (expr2 != null) {
            refs.addAll(expr2.getRefProgObjs());
        }
        if (expr3 != null) {
            refs.addAll(expr3.getRefProgObjs());
        }
        return refs;
    }
}
```

首期将三元关系视为“一个整体元组关系”，即 `relationType` 作用于 `expr1 + expr2 + expr3`。如果后续需要表达 `A relation12 B relation23 C` 且两个关系类型不同，可新增 `relation12` 和 `relation23` 字段，或升级为通用 N 元结构。

### 3.8 组合规则 Schema

组合规则父规则使用：

```java
public class CombinationStructRuleSchema extends RuleSchema {
    /**
     * 2 或 3。
     */
    private int arity;

    /**
     * 参与组合的 PartCategory code，按表达式顺序存储。
     * 二元示例：["cpu", "drive"]
     * 三元示例：["cpu", "drive", "monitor"]
     */
    private List<String> dimensionCategoryCodes = new ArrayList<>();

    /**
     * WHITE 表示白名单，BLACK 表示黑名单。
     */
    private PartCombinationType type;

    /**
     * 子规则 code 列表。
     * 子规则本身是 PairStructRuleSchema 或 TripleStructRuleSchema。
     */
    private List<String> subRuleCodes = new ArrayList<>();

}
```

枚举：

```java
public enum PartCombinationType {
    WHITE,
    BLACK
}

```

### 3.9 运行态组合 Schema

维护态 Schema 不能直接执行，因为它描述的是属性条件；执行 Schema 需要具体部件编码组合。本 RFC 采用 `CodependantRuleSchema` 作为组合规则执行态模型。

```java
public class CodependantRuleSchema extends RuleSchema {
    private int arity;
    private List<String> dimensionCategoryCodes = new ArrayList<>();
    private List<PartCombination> combinations = new ArrayList<>();
    private PartCombinationType type;
}

public class PartCombination {
    /**
     * 二元时使用 code1/code2，三元时使用 code1/code2/code3。
     */
    private String code1;
    private String code2;
    private String code3;

    /**
     * 可选：便于调试和追溯是哪条子规则展开出来的。
     */
    private String sourceSubRuleCode;

    public List<String> getCodes(int arity) {
        if (arity == 2) {
            return List.of(code1, code2);
        }
        if (arity == 3) {
            return List.of(code1, code2, code3);
        }
        throw new IllegalArgumentException("Unsupported arity: " + arity);
    }
}
```

命名说明：本 RFC 采用需求中提出的 `CodependantRuleSchema`。它表示“配套组合规则的执行态组合集合”，不要和 `BusinessRelationType.CO_DEPENDENT` 的单条关系枚举混淆。

### 3.10 组合规则数据示例

父规则：

```json
{
  "code": "cpu_drive_combo",
  "name": "CPU 与硬盘配套白名单",
  "rawCode": {
    "@type": "CombinationStructRule",
    "arity": 2,
    "dimensionCategoryCodes": ["cpu", "drive"],
    "type": "WHITE",
    "subRuleCodes": [
      "cpu_drive_combo_001",
      "cpu_drive_combo_002",
      "cpu_drive_combo_003"
    ]
  }
}
```

子规则 1：

```json
{
  "code": "cpu_drive_combo_001",
  "parentRuleCode": "cpu_drive_combo",
  "rawCode": {
    "@type": "PairStructRule",
    "expr1": {
      "objectCode": "cpu",
      "attrCode": "CoreNum",
      "operator": "EQ",
      "values": ["4"]
    },
    "relationType": "CO_DEPENDENT",
    "expr2": {
      "objectCode": "drive",
      "attrCode": "Speed",
      "operator": "EQ",
      "values": ["5400"]
    }
  }
}
```

子规则 2：

```json
{
  "code": "cpu_drive_combo_002",
  "parentRuleCode": "cpu_drive_combo",
  "rawCode": {
    "@type": "PairStructRule",
    "expr1": {
      "objectCode": "cpu",
      "attrCode": "CoreNum",
      "operator": "EQ",
      "values": ["8"]
    },
    "relationType": "CO_DEPENDENT",
    "expr2": {
      "objectCode": "drive",
      "attrCode": "Speed",
      "operator": "IN",
      "values": ["7200", "5400"]
    }
  }
}
```

子规则 3：

```json
{
  "code": "cpu_drive_combo_003",
  "parentRuleCode": "cpu_drive_combo",
  "rawCode": {
    "@type": "PairStructRule",
    "expr1": {
      "objectCode": "cpu",
      "attrCode": "CoreNum",
      "operator": "GT",
      "values": ["8"]
    },
    "relationType": "CO_DEPENDENT",
    "expr2": {
      "objectCode": "drive",
      "attrCode": "Speed",
      "operator": "EQ",
      "values": ["7200"]
    }
  }
}
```

### 3.11 构建期展开流程

结构化表达式的解析位置在 `ModuleGenneratorByAnno`，不是在计算引擎求解时。构建 Module 数据时，生成器读取 `Rule.rawCode` 中的 `PairStructRuleSchema`、`TripleStructRuleSchema`、`CombinationStructRuleSchema`，并直接生成父规则的 `Rule.exeSchema = CodependantRuleSchema`。

建议在 `ModuleGenneratorByAnno` 内部增加一个小的展开组件，例如：

```java
final class StructRuleBuildExpander {
    CodependantRuleSchema expandCombination(Rule parentRule, Module module, List<Rule> allRules) {
        CombinationStructRuleSchema schema =
                (CombinationStructRuleSchema) parentRule.getRawCode();

        List<Rule> subRules = findSubRules(parentRule, schema, allRules);
        validateArityAndDimensions(schema, subRules);

        List<PartCombination> combinations = new ArrayList<>();
        for (Rule subRule : subRules) {
            List<List<String>> partCodeSets = resolveExprsToPartCodes(subRule, module);
            combinations.addAll(cartesianProduct(partCodeSets, subRule.getCode()));
        }

        CodependantRuleSchema exe = new CodependantRuleSchema();
        exe.setArity(schema.getArity());
        exe.setDimensionCategoryCodes(schema.getDimensionCategoryCodes());
        exe.setType(schema.getType());
        exe.setCombinations(distinct(combinations));
        return exe;
    }
}
```

表达式解析只依赖 `Module` 静态数据：

```java
List<String> resolveToPartCodes(StructExprSchema expr, Module module) {
    PartCategory category = module.getPartCategory(expr.getObjectCode());
    List<Part> rawParts = category.getAllAtomicParts();

    List<Part> matchedParts = structExprResolver.doSelect(rawParts, expr);
    return matchedParts.stream()
            .map(Part::getCode)
            .distinct()
            .collect(Collectors.toList());
}
```

构建期展开步骤：

1. `ModuleGenneratorByAnno.createRulesFromMethods(...)` 或其后置阶段收集全部规则。
2. 根据 `CombinationStructRuleSchema.subRuleCodes` 或 `Rule.parentRuleCode` 找到子规则。
3. 校验父规则 `arity`、`dimensionCategoryCodes` 与子规则表达式一致。
4. 使用 `FilterExpressionExecutor` 基于 Module 静态 `Part` 数据解析每个 `StructExprSchema`。
5. 对每条子规则解析出的部件 code 集合做笛卡尔积，生成 `PartCombination`。
6. 去重后写入父规则 `exeSchema`。
7. `parentRuleCode` 非空的子规则只用于父规则展开，不进入独立执行链路。

运行时不再解析 `StructExprSchema`。如果请求态 PartCategory Filter 导致某些 part code 不存在于当前 `PartCategoryAlgImpl` 的 `partMap` 中，执行器按当前模型映射结果处理：

- BLACK：某个 tuple 中任一 code 不存在，则该禁止 tuple 当前不可能同时选中，可以跳过。
- WHITE：某个 tuple 中任一 code 不存在，则该允许 tuple 当前不可命中，不加入候选 match。
- WHITE：所有允许 tuple 都不可命中时，在相关维度被要求选择的情况下应无解。

### 3.12 CP 约束生成

新增 `CodependantConstraintExecutor`。它只消费 `CodependantRuleSchema`，并在计算引擎内根据 part code 查找当前模型里的 `PartVarImpl`：

```java
public class CodependantConstraintExecutor {
    private final AlgCPModel model;

    public void execute(String ruleCode, CodependantRuleSchema schema,
            Function<String, PartVarImpl> partVarGetter) {
        if (schema.getType() == PartCombinationType.BLACK) {
            addBlacklist(ruleCode, schema, partVarGetter);
            return;
        }
        if (schema.getType() == PartCombinationType.WHITE) {
            addWhitelist(ruleCode, schema, partVarGetter);
            return;
        }
        throw new AlgLoaderException("Unsupported PartCombinationType: " + schema.getType());
    }
}
```

黑名单约束：

```java
private void addBlacklist(String ruleCode, CodependantRuleSchema schema,
        Function<String, PartVarImpl> partVarGetter) {
    for (PartCombination combination : schema.getCombinations()) {
        List<BoolVar> selectedVars = combination.getCodes(schema.getArity()).stream()
                .map(partVarGetter)
                .filter(Objects::nonNull)
                .map(PartVarImpl::getIsSelected)
                .collect(Collectors.toList());

        if (selectedVars.size() != schema.getArity()) {
            continue;
        }

        model.addLessOrEqual(
                LinearExpr.sum(selectedVars.toArray(new BoolVar[0])),
                selectedVars.size() - 1);
    }
}
```

二元黑名单的数学语义应是：

```text
cpu1.S + drive2.S <= 1
```

也就是不允许两者同时为 1。这里确认使用 `<= 1`，不使用 `< 1`。

白名单约束：

```java
private void addWhitelist(String ruleCode, CodependantRuleSchema schema,
        Function<String, PartVarImpl> partVarGetter) {
    List<BoolVar> tupleMatches = new ArrayList<>();

    for (PartCombination combination : schema.getCombinations()) {
        List<PartVarImpl> partVars = combination.getCodes(schema.getArity()).stream()
                .map(partVarGetter)
                .collect(Collectors.toList());
        if (partVars.stream().anyMatch(Objects::isNull)) {
            continue;
        }

        BoolVar match = model.newBoolVar(ruleCode + "_match_" + tupleMatches.size());
        Literal[] selected = partVars.stream()
                .map(PartVarImpl::getIsSelected)
                .toArray(Literal[]::new);

        model.addBoolAnd(selected).onlyEnforceIf(match);
        model.addBoolOr(Arrays.stream(selected)
                .map(Literal::not)
                .toArray(Literal[]::new)).onlyEnforceIf(match.not());

        tupleMatches.add(match);
    }

    model.addBoolOr(tupleMatches.toArray(new Literal[0]));
}
```

可选 PartCategory 的 guard 语义本 RFC 暂不实现，后续单独 RFC 处理。

### 3.13 北向 validate 校验接口

`ModuleConstraintExecutor` 新增 `validate(ModuleValidateReq)`，用于校验一个已给定的配置组合是否满足当前 Module 规则。它面向“校验现有配置是否 OK”，不是为了枚举或推荐新解。

```java
public interface ModuleConstraintExecutor {
    Result<ModuleValidateResp> validate(ModuleValidateReq req);
}

public class ModuleValidateReq {
    private Long moduleId;
    private String moduleCode;

    /**
     * 待校验的配置实例。
     * 其中 PartInst.quantity / selected、ParaInst.value 等字段表示调用方给定的组合。
     */
    private ModuleInst moduleInst;
}

public class ModuleValidateResp {
    private boolean valid;
    private List<String> violatedRuleCodes = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
}
```

示例输入：

```java
ModuleInst inst = new ModuleInst();
inst.addPartInst(partInst("cpu4", 1));
inst.addPartInst(partInst("drive5400", 1));

ModuleValidateReq req = new ModuleValidateReq();
req.setModuleCode("Server");
req.setModuleInst(inst);

Result<ModuleValidateResp> result = ModuleConstraintExecutor.INST.validate(req);
```

校验语义：

- `validate` 使用给定 `ModuleInst` 作为固定输入，将其中的部件数量、选中状态、参数值转成输入约束。
- 如果 CP-SAT 在这些固定输入下仍有可行解，则 `valid=true`。
- 如果无可行解，则 `valid=false`，返回违反的规则 code；若开启松弛诊断，可复用现有松弛变量定位冲突规则。
- 对组合规则而言，白名单命中则通过，未命中则失败；黑名单命中则失败，未命中则通过。

需要注意：校验输入可能不仅是 selected 状态，也可能包含数量，例如 `cpu1.quantity=1`、`drive2.quantity=2`。因此请求体选择复用 `ModuleInst`，而不是只传 part code 列表。

### 3.14 独立二元/三元规则执行

非组合父规则的 `PairStructRuleSchema` / `TripleStructRuleSchema` 可以直接执行：

| relationType | 二元执行语义 |
| --- | --- |
| `INCOMPATIBLE` | 对展开后的每个二元组合生成 `a.S + b.S <= 1` |
| `REQUIRES` | 对展开后的集合生成 `leftCond -> rightCond`，复用现有兼容规则思想 |
| `CO_DEPENDENT` | 对展开后的集合生成双向同选关系 |

三元独立规则首期建议只支持 `INCOMPATIBLE`：

```text
a.S + b.S + c.S <= 2
```

三元 `REQUIRES` 和 `CO_DEPENDENT` 的业务语义容易歧义，应在维护态或后续 RFC 中明确。

### 3.15 维护态唯一性约束

计算引擎层面不限制同一维度组合规则数量；维护态需要约束。

二元组合规则默认唯一键：

```text
ownerCode + arity + dimensionCategoryCodes[0] + dimensionCategoryCodes[1] + type
```

三元组合规则默认唯一键：

```text
ownerCode + arity + dimensionCategoryCodes[0] + dimensionCategoryCodes[1] + dimensionCategoryCodes[2] + type
```

其中 `ownerCode` 建议使用：

- `Rule.fatherCode` 非空时：使用 `fatherCode`。
- `Rule.fatherCode` 为空时：使用 Module code 或 Module id。

维护态默认不允许同一唯一键下存在多个组合父规则。原因是同一组分类的白名单/黑名单应该作为一个整体维护，否则逻辑完备性难以检查。

### 3.16 维护态逻辑完备性说明

计算引擎不强制完备性，只执行白名单或黑名单语义：

- WHITE：命中允许组合才通过，未命中默认拒绝。
- BLACK：命中禁止组合则拒绝，未命中默认通过。

维护态可以提供完备性检查，但本 RFC 不在规则 Schema 中定义完备性策略字段，也不规定保存时必须警告还是阻断。后续维护系统可以独立实现覆盖报告：

1. 根据 `dimensionCategoryCodes` 计算参与维度的原始全集。
2. 根据所有子规则展开已覆盖的 `PartCombination` 集合。
3. 检查是否存在未覆盖的属性区间或部件组合。
4. 由维护态产品策略决定提示、警告或阻断保存。

示例：

```text
cpu.CoreNum 可取 4, 8, 12
drive.Speed 可取 5400, 7200

白名单覆盖：
  4 -> 5400
  8 -> 5400, 7200
  >8 -> 7200

展开后覆盖：
  (4, 5400)
  (8, 5400)
  (8, 7200)
  (12, 7200)

未覆盖：
  (4, 7200)
  (12, 5400)
```

对白名单而言，未覆盖项会被运行态默认拒绝；维护态是否视为“设计缺陷”不进入计算引擎模型。

### 3.17 与依赖关系图的关系

现有 `Rule.getFromLeftProgObjs()` 和 `Rule.getToRightProgObjs()` 用于规则依赖图。新增 Schema 应保持兼容：

- `PairStructRuleSchema.getFromLeftProgObjs()` 返回 `expr1` 引用。
- `PairStructRuleSchema.getToRightProgObjs()` 返回 `expr2` 引用。
- `TripleStructRuleSchema.getFromLeftProgObjs()` 返回 `expr1` 引用。
- `TripleStructRuleSchema.getToRightProgObjs()` 返回 `expr2 + expr3` 引用。
- `CombinationStructRuleSchema` 返回所有子规则引用的并集，或在编译前通过子规则透传引用。

这能保证差量加载仍能识别结构化规则依赖的 PartCategory。

### 3.18 与现有代码生成链路的关系

现有 `ModuleAlgArtifactGenerator` 已能把部分 `CompatiableRuleSchema` 转换为生成式 Java 规则。本 RFC 新增的是“构建期展开、运行期执行”的路径：

```text
Rule.rawCode
  -> ModuleGenneratorByAnno / StructRuleBuildExpander
  -> Rule.exeSchema = CodependantRuleSchema
  -> CodependantConstraintExecutor
  -> CP constraint
```

这样 UI 不需要生成 Java 代码。若后续仍需要生成 Java，可由 `StructCodeInjector` 或 `ModuleAlgArtifactGenerator` 生成等价调用，例如：

```java
inCompatible("rule_cpu_drive_4_5400", "cpu:CoreNum=4", "drive:Speed=5400");
```

但组合规则的白名单更适合由 `CodependantRuleSchema` 直接执行，避免生成大量重复 Java 方法。

---

## 4. 验收准则

### 4.1 功能验收用例

建议新增测试文件：

```text
src/test/java/com/jmix/scenario/ruletest/StructCombinationRuleTest.java
```

测试数据保持极简：

```java
@ModuleAnno(id = 7007)
public static class StructCombinationConstraint extends ConstraintAlgBase {

    @PartAnno(code = "cpu")
    @DAttrAnno1(code = "CoreNum", options = {
            "Core_4:4",
            "Core_8:8",
            "Core_12:12"
    })
    private PartCategoryVar cpu;

    @PartAnno(fatherCode = "cpu", attrs = {"4"})
    private PartVar cpu4;

    @PartAnno(fatherCode = "cpu", attrs = {"8"})
    private PartVar cpu8;

    @PartAnno(fatherCode = "cpu", attrs = {"12"})
    private PartVar cpu12;

    @PartAnno(code = "drive")
    @DAttrAnno1(code = "Speed", options = {
            "Speed_5400:5400",
            "Speed_7200:7200",
            "Speed_7000:7000"
    })
    private PartCategoryVar drive;

    @PartAnno(fatherCode = "drive", attrs = {"5400"})
    private PartVar drive5400;

    @PartAnno(fatherCode = "drive", attrs = {"7200"})
    private PartVar drive7200;

    @PartAnno(fatherCode = "drive", attrs = {"7000"})
    private PartVar drive7000;
}
```

#### AC-001: 二元结构化不兼容规则

目的：验证 `cpu.CoreNum = 4 INCOMPATIBLE drive.Speed = 5400` 可以被编译为二元不兼容约束。

```java
@Test
public void testPairStructRule_Incompatible() {
    installRule(pairStructRule(
            expr("cpu", "CoreNum", EQ, "4"),
            INCOMPATIBLE,
            expr("drive", "Speed", EQ, "5400")));

    inferRecommendModule("cpu:code=cpu4,drive:code=drive5400");
    resultAssert().assertSolutionSizeEqual(0);

    inferRecommendModule("cpu:code=cpu4,drive:code=drive7200");
    resultAssert().assertSuccess();
}
```

#### AC-002: 二元组合白名单

目的：验证白名单组合中必须命中至少一条子规则。

测试组合：

| 输入 | 预期 |
| --- | --- |
| `cpu4 + drive5400` | 通过 |
| `cpu4 + drive7200` | 不通过 |
| `cpu8 + drive5400` | 通过 |
| `cpu8 + drive7200` | 通过 |
| `cpu12 + drive5400` | 不通过 |
| `cpu12 + drive7200` | 通过 |

```java
@Test
public void testCombinationWhiteList_Pair() {
    installRule(cpuDriveWhiteListRule());

    assertPass("cpu:code=cpu4,drive:code=drive5400");
    assertNoSolution("cpu:code=cpu4,drive:code=drive7200");
    assertPass("cpu:code=cpu8,drive:code=drive5400");
    assertPass("cpu:code=cpu8,drive:code=drive7200");
    assertNoSolution("cpu:code=cpu12,drive:code=drive5400");
    assertPass("cpu:code=cpu12,drive:code=drive7200");
}
```

#### AC-003: 二元组合黑名单

目的：验证黑名单命中组合会被禁止，未命中的组合默认允许。

```java
@Test
public void testCombinationBlackList_Pair() {
    installRule(blackListRule(
            pair(expr("cpu", "CoreNum", EQ, "4"),
                    INCOMPATIBLE,
                    expr("drive", "Speed", EQ, "7000"))));

    assertNoSolution("cpu:code=cpu4,drive:code=drive7000");
    assertPass("cpu:code=cpu4,drive:code=drive5400");
    assertPass("cpu:code=cpu8,drive:code=drive7000");
}
```

#### AC-004: 三元组合白名单

目的：验证三元结构化规则可以展开为三元 `PartCombination` 并执行白名单。

```java
@Test
public void testCombinationWhiteList_Triple() {
    installRule(cpuDriveMonitorWhiteListRule(
            triple(expr("cpu", "CoreNum", EQ, "8"),
                    CO_DEPENDENT,
                    expr("drive", "Speed", EQ, "7200"),
                    expr("monitor", "Resolution", EQ, "4K"))));

    assertPass("cpu:code=cpu8,drive:code=drive7200,monitor:code=monitor4k");
    assertNoSolution("cpu:code=cpu8,drive:code=drive5400,monitor:code=monitor4k");
}
```

#### AC-005: validate 校验二元白名单

目的：验证 `ModuleConstraintExecutor.validate(ModuleValidateReq)` 可以校验给定 `ModuleInst` 组合是否满足白名单组合规则。

```java
@Test
public void testValidateCombinationWhiteList_Pair() {
    installRule(cpuDriveWhiteListRule());

    ModuleInst validInst = moduleInst(
            partInst("cpu4", 1),
            partInst("drive5400", 1));
    Result<ModuleValidateResp> valid =
            ModuleConstraintExecutor.INST.validate(validateReq(validInst));
    assertTrue(valid.getData().isValid());

    ModuleInst invalidInst = moduleInst(
            partInst("cpu4", 1),
            partInst("drive7200", 1));
    Result<ModuleValidateResp> invalid =
            ModuleConstraintExecutor.INST.validate(validateReq(invalidInst));
    assertFalse(invalid.getData().isValid());
    assertTrue(invalid.getData().getViolatedRuleCodes().contains("cpu_drive_combo"));
}
```

#### AC-006: validate 支持数量输入

目的：验证校验接口复用 `ModuleInst`，不仅能校验 selected 组合，也能校验数量输入。

```java
@Test
public void testValidateCombination_WithQuantityInput() {
    installRule(cpuDriveWhiteListRule());

    ModuleInst inst = moduleInst(
            partInst("cpu4", 1),
            partInst("drive5400", 2));

    Result<ModuleValidateResp> result =
            ModuleConstraintExecutor.INST.validate(validateReq(inst));

    assertEquals(Result.SUCCESS, result.getCode());
}
```

#### AC-007: 与请求态 PartCategory Filter 取交集

目的：验证运行态执行 `CodependantRuleSchema` 时会按当前 `PartCategoryAlgImpl` 中可见的 `PartVarImpl` 解析 part code。如果请求态过滤让某个 code 不存在，则对应 tuple 不可命中。

```java
@Test
public void testCombinationRule_IntersectWithRuntimeFilter() {
    installRule(cpuDriveWhiteListRule());

    // 当前请求把 drive 过滤为 Speed=7200。
    // 虽然规则中存在 cpu4 -> drive5400，但 drive5400 已不在运行态候选集合里。
    inferRecommendModule("drive:Sum_Quantity == 1 where Speed=7200; cpu:code=cpu4");

    resultAssert().assertSolutionSizeEqual(0);
}
```

#### AC-008: 子规则不独立执行

目的：验证 `parentRuleCode` 非空的子规则不会被普通规则执行链路重复执行。

```java
@Test
public void testChildRule_NotExecutedIndependently() {
    Rule parent = cpuDriveWhiteListRule();
    Rule child = childRule("cpu_drive_combo_001", parent.getCode());

    installRules(parent, child);

    // 如果 child 被独立执行，可能会额外生成 requires/codependent 等约束。
    // 预期只有 parent 组合规则的白名单语义生效。
    assertPass("cpu:code=cpu4,drive:code=drive5400");
}
```

#### AC-009: 维护态唯一性检查

目的：验证相同 owner 和相同维度的组合规则不能重复维护。

```java
@Test
public void testMaintenanceUniqueKey_DuplicatePairCombinationRule() {
    Rule rule1 = cpuDriveWhiteListRule("r1");
    Rule rule2 = cpuDriveWhiteListRule("r2");

    MaintenanceValidationResult result =
            validator.validateUniqueCombinationRules(List.of(rule1, rule2));

    assertFalse(result.isSuccess());
    assertTrue(result.getMessage().contains("duplicate combination rule"));
}
```

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
| --- | --- | --- |
| 表达式无匹配部件，黑名单 | `cpu.CoreNum = 99` | 不生成约束，记录 debug 日志 |
| 表达式无匹配部件，白名单 | 所有子规则展开为空 | 若相关维度被选择则无解；维护态应提示规则无有效组合 |
| `IN` 多值 | `Speed IN [5400, 7200]` | 对两个值分别过滤后取并集并去重 |
| 子规则维度不一致 | 父规则维度为 `cpu,drive`，子规则为 `cpu,monitor` | 维护态校验失败；运行态抛清晰异常 |
| 子规则元数不一致 | 父规则 `arity=2`，子规则为三元 | 维护态校验失败；运行态抛清晰异常 |
| 重复 PartCombination | 多条子规则展开出同一组合 | 去重后只生成一份约束 |
| 三元黑名单 | `cpu4 + drive5400 + monitor4k` | 生成 `cpu4.S + drive5400.S + monitor4k.S <= 2` |
| 可选分类白名单 | 部分维度未选择 | 首期不实现 guard 语义；后续单独 RFC 处理 |
| 多实例分类 | `drive` 支持多实例 | 首期不作为 P0；作为 TODO，由后续单独 RFC 跟踪 |

### 4.3 回归测试

- [ ] `CompatibleRuleIncompatibleTest` 现有不兼容规则行为不变。
- [ ] `CompatibleRuleRequireTest` 现有 Requires 行为不变。
- [ ] `CompatibleRuleCodependentTest` 现有 CoDependent 行为不变。
- [ ] `FilterExpressionExecutorTest` 现有过滤表达式行为不变。
- [ ] `PartCategoryFilterEmptyTest` 对空过滤集合的处理不变。
- [ ] `BaseOptiTest` 中现有 `inCompatible("logicAB1", "cpu:CoreNum=4", "drive:Type=sd")` 行为不变。
- [ ] `mvn test` 可执行新增结构化规则测试。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 新增 `StructExprSchema`、`PairStructRuleSchema`、`TripleStructRuleSchema`、`CombinationStructRuleSchema` | P0 | 待开始 |
| 2 | 新增 `Rule.parentRuleCode` 和运行态 `Rule.exeSchema` | P0 | 待开始 |
| 3 | 在 `ModuleGenneratorByAnno` 中新增 `StructRuleBuildExpander`，构建期展开结构化表达为 `CodependantRuleSchema` | P0 | 待开始 |
| 4 | 新增 `StructExprResolver`，支持 EQ/NE/GT/GE/LT/LE/LIKE/IN 等表达式解析为 Part code 集合 | P0 | 待开始 |
| 5 | 新增 `CodependantConstraintExecutor`，基于 `CodependantRuleSchema` 生成 WHITE/BLACK 二元组合约束 | P0 | 待开始 |
| 6 | 补齐三元 BLACK 和三元 WHITE 组合约束 | P0 | 待开始 |
| 7 | 集成规则执行链路，跳过 `parentRuleCode` 非空的子规则独立执行 | P0 | 待开始 |
| 8 | `ModuleConstraintExecutor` 新增 `validate(ModuleValidateReq)`，校验给定 `ModuleInst` 是否满足规则 | P0 | 待开始 |
| 9 | 新增维护态校验器：唯一性、元数一致、维度一致、表达式有效性、重复组合 | P1 | 待开始 |
| 10 | 兼容 `ModuleAlgArtifactGenerator` 或 `StructCodeInjector` 的生成式路径 | P2 | 待开始 |
| 11 | 补充 RFC 中列出的自动化测试 | P0 | 待开始 |
| 12 | TODO：可选 PartCategory 白名单 guard 语义单独 RFC 设计 | P2 | 待开始 |
| 13 | TODO：多实例分类组合规则单独 RFC 设计 | P2 | 待开始 |

---

## 6. 风险与兼容策略

### 6.1 `fatherCode` 语义冲突

风险：用户初稿中提到用 `fatherCode` 表示子规则，但现有 `Rule.fatherCode` 已表示规则挂载位置。

策略：新增 `parentRuleCode` 表示规则父子关系，保留 `fatherCode` 原语义。

### 6.2 关系类型命名兼容风险

风险：业务人员可能仍使用“兼容”“配套”表达关系，但模型中不再保留 `COMPATIBLE` 枚举。

策略：维护态展示文案可以继续叫“兼容/配套”，但持久化和执行模型统一写入 `CO_DEPENDENT`。

### 6.3 白名单可能过度约束可选分类

风险：如果参与组合的分类不是必选，直接要求命中白名单会让“未选择该分类”的配置无解。

策略：本 RFC 暂不实现可选分类 guard 语义，作为后续独立 RFC 跟踪。

### 6.4 组合数量膨胀

风险：结构化表达展开为笛卡尔积后，组合数量可能很大。

策略：

- 构建期基于 Module 静态数据展开并去重。
- 运行期如果请求态过滤导致 tuple 中某个 part code 不存在于当前 `PartCategoryAlgImpl`，该 tuple 不参与 match。
- 对组合去重。
- 对黑名单按 tuple 生成线性约束。
- 对白名单优先使用 tuple match OR，而不是生成全集补集黑名单。
- 维护态提供组合数量预估，超过阈值给出警告。

### 6.5 多实例分类语义复杂

风险：`drive` 支持多实例时，同一规则需要作用于每个实例，还是跨实例整体作用，需要业务确认。

策略：首期不作为 P0 范围，作为 TODO 由后续单独 RFC 设计和跟踪。

---

## 7. 已确认决策与 TODO

| 问题 | 结论 |
| --- | --- |
| 父子规则关系 | 新增 `Rule.parentRuleCode`，不复用 `fatherCode` |
| `COMPATIBLE` 与 `CO_DEPENDENT` | 只保留 `CO_DEPENDENT`，业务上的“兼容/配套”统一映射到 `CO_DEPENDENT` |
| 三元规则关系符 | 首期三元使用一个整体 `relationType`，不增加两个不同关系符 |
| 可选 PartCategory 白名单 guard | 本 RFC 暂不实现，后续单独 RFC 处理 |
| 黑名单二元公式 | 确认使用 `a.S + b.S <= 1` |
| 完备性策略字段 | 不在本 RFC 中定义，删除完备性策略字段 |
| 组合规则唯一键顺序 | 对 `REQUIRES` 等方向性关系保留顺序；白名单/黑名单是否归一化由维护态产品决策 |
| 多实例分类 | 不作为首期 P0，放入 TODO，后续单独 RFC 跟踪 |

后续 TODO：

- 可选 PartCategory 下白名单何时生效的 guard 语义。
- 多实例 PartCategory 的组合规则展开和校验语义。
- 维护态完备性覆盖报告如何展示、是否阻断保存。

---

## 8. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0006-North-Interface-Refactor.md`
- `src/main/java/com/jmix/executor/bmodel/logic/Rule.java`
- `src/main/java/com/jmix/executor/bmodel/logic/RuleSchema.java`
- `src/main/java/com/jmix/executor/bmodel/logic/CompatiableRuleSchema.java`
- `src/main/java/com/jmix/executor/bmodel/logic/ExprSchema.java`
- `src/main/java/com/jmix/executor/bmodel/logic/RefProgObjSchema.java`
- `src/main/java/com/jmix/executor/impl/util/FilterExpressionExecutor.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/CompatibleConstraintAlg.java`
- `src/main/java/com/jmix/tool/artbuilder/ModuleAlgArtifactGenerator.java`
- `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleIncompatibleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleCodependentTest.java`
- `src/test/java/com/jmix/tool/FilterExpressionExecutorTest.java`
