# RFC-0010: 跨 PartCategory 总量约束输入与求解流程

> 状态：草案（Draft）
> 日期：2026-06-01
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`, `doc/RFC-0006-North-Interface-Refactor.md`, `doc/RFC-0008-Rule-Model-IR-Refactor.md`, `doc/RFC-0009-Optional-PartCategory-Whitelist-Guard.md`

---

## 设计决策摘要

| 主题 | 决策 |
| --- | --- |
| 问题定位 | 当前 `PartConstraintReq.partCategoryCode` 只能稳定表达单个 PartCategory 的数量、属性汇总和过滤条件，不能表达多个分类之间的总容量、总端口数等总量需求 |
| 核心方案 | 新增跨分类总量约束输入语义，将单分类约束与总量约束在预处理阶段拆分 |
| 分类范围 | 产品侧暂不声明可汇总分类组；跨分类输入必须显式给出分类列表，并由输入预处理校验分类和属性是否合法 |
| 构建顺序 | 先按单分类输入过滤各 PartCategory，再基于过滤后的候选集构建跨分类总量表达式 |
| 总量过滤 | 总量约束中的 `where` 只决定哪些候选项参与总量表达式，不反向覆盖单分类过滤结果 |
| 多实例语义 | 对某一逻辑 PartCategory 的所有实例求和，再与其他参与分类一起求总和 |
| 可选分类关系 | 复用 RFC-0009 的 present/absent 语义；P0 支持总量组中的可选分类自动进入 present 分支求解 |
| P0 范围 | 支持 `inferParas` 推荐求解路径；校验路径纳入遗留任务，待整体系统构建完成后统一重构 |
| 兼容性 | 存量单分类 `PartConstraintReq` 行为不变；目标命名收敛到 `PartCategoryConstraintReq` 与 `CrossCategoryPartCategoryConstraintReq` 两类标准化请求 |

---

## 1. 摘要

当前输入模型只能描述“某个分类自身”的数量、容量或过滤条件，无法表达“硬盘和主控板的 10G 端口总容量大于等于 100G”这类跨 PartCategory 的总体需求。

本 RFC 提议在输入预处理层引入跨分类总量约束：先拆分单分类要求与总量要求，保持既有单分类过滤流程，再基于过滤后的部件候选集构建总量线性表达式。多实例场景中，总量表达式应覆盖同一逻辑分类的所有实例。

---

## 2. 动机

### 2.1 问题背景

在同一个模块内，不同 PartCategory 可能拥有相同或业务等价的属性。例如：

- 硬盘或转接板有 `Capacity`。
- 主控板也有 `Capacity`。
- 二者都可能包含 `PortRate=10G`、`UplinkFunction=10G` 这类过滤维度。

客户提出的配置需求有时不是针对某一个分类，而是针对整体能力：

```text
10G 端口的总容量 >= 100G
```

这个需求可能由多种组合满足：

```text
a. 主控板 + 某个硬盘的容量总和达标
b. 单独由硬盘达标
c. 单独由主控板达标
```

现有输入如果只能写成：

```text
disk:Sum_Capacity >=100 where PortRate=10G
```

就会把整体需求误读成“硬盘分类自身必须达标”，从而漏掉主控板参与贡献的可行解。

### 2.2 当前代码限制

当前北向输入主要通过 `InferParasReq.partConstraintReqs` 承载部件分类约束：

```java
public class PartConstraintReq {
    private AttrParaType attrType = AttrParaType.Sum;
    private String attrCode;
    private String attrComparator;
    private String attrValue;
    private String attrWhereCondition;
    private String partCategoryCode;
    private List<StrategyConfig> decisionStrategies;
}
```

该模型隐含了一个前提：每条约束都有一个确定的 `partCategoryCode`。执行器也会按 `partCategoryCode` 归一化请求，再对每个 PartCategory 分别执行 `filterClone`。

这对单分类约束是合理的：

```text
disk:Sum_Quantity >=10 where PortRate in (10G,10,100)
mainBoard:where PortRate in (10G,10,100)
```

但它不能表达：

```text
total(disk, mainBoard):Sum_Capacity >=100 where PortRate=10G
```

尤其是在总量条件本身也带过滤时，不能简单把 `where PortRate=10G` 提前下推成每个分类的硬过滤。因为总量约束只要求参与求和的项满足该过滤，并不表示其他单分类候选项必须被删除。

### 2.3 具体场景

客户原始输入：

```text
总量要求：
  10G 端口的总容量 >= 100G

硬盘要求：
  10G 数量 >= 10
  候选必须是 10G、10 端口或 100 端口

主控板要求：
  候选必须是 10G、10 端口或 100 端口
```

期望执行流程：

```text
第一步：单分类过滤
  disk 原始候选 10 条，按硬盘自身条件过滤后剩余 5 条。
  mainBoard 原始候选 6 条，按主控板自身条件过滤后剩余 3 条。

第二步：总量表达式候选提取
  在 disk 剩余 5 条基础上继续套用总量 where，例如 PortRate=10G，得到 2 条可参与总量求和的 disk 候选。
  在 mainBoard 剩余 3 条基础上继续套用总量 where，得到可参与总量求和的 mainBoard 候选。

第三步：构建总量约束表达式
  sum(disk_10g_candidates.Capacity * qty)
  + sum(mainBoard_10g_candidates.Capacity * qty)
  >= 100
```

这里需要注意：第二步得到的“2 条 disk 候选”是总量表达式的项集合，不一定等同于 disk 分类最终可选择集合。disk 自身的单分类过滤结果仍由第一步决定。

### 2.4 为什么需要改变

如果沿用现有单分类输入，会有三个问题：

1. **语义不准确**：整体容量被误绑定到某一个 `partCategoryCode`。
2. **可行解丢失**：主控板和硬盘共同满足总量的组合无法被表达。
3. **多实例不完整**：多实例分类中，同一逻辑分类的多个实例需要先汇总，再参与跨分类总量。

因此，总量需求需要成为一类独立输入语义，而不是继续挤在 `partCategoryCode` 的单分类语义里。

---

## 3. 术语与边界

### 3.1 单分类约束

单分类约束指绑定到一个具体 `PartCategory` 的输入要求，例如：

```text
disk:Sum_Quantity >=10 where PortRate in (10G,10,100)
mainBoard:where PortRate in (10G,10,100)
```

目标上使用 `PartCategoryConstraintReq` 语义；当前兼容层可以继续从现有 `PartConstraintReq` 标准化而来：

- `partCategoryCode` 表示被约束的分类。
- `attrWhereCondition` 用于过滤该分类候选部件。
- `attrCode + attrComparator + attrValue` 用于构建该分类内部的汇总约束。
- `AttrParaType.SumSum` 仍表示同一逻辑分类的所有实例汇总。

### 3.2 跨分类总量约束

跨分类总量约束指对一组 PartCategory 的候选部件统一求和，例如：

```text
code = "uplink_10g_capacity_total"
categories = ["disk", "mainBoard"]
attrCode = "Capacity"
where = "PortRate=10G"
constraint = ">=100"
```

其语义为：

```text
sum over all present category instances and all selected parts:
    part.Capacity * part.quantity
where:
    category in ["disk", "mainBoard"]
    and part matches "PortRate=10G"
must be >= 100
```

### 3.3 输入侧跨分类校验

产品侧暂不声明跨分类汇总组，也不在 `Module` 中新增 `CrossCategoryAggregateSpec`。这样可以避免在产品建模阶段引入额外复杂度。

跨分类总量由输入端显式声明参与分类、汇总属性、过滤条件和比较条件。系统在请求预处理阶段做强校验：

1. 输入中的每个 `partCategoryCode` 必须存在于当前模块。
2. `attrCode` 必须在每个参与分类上有定义，或属于系统支持的常量属性。
3. `attrCode` 必须是可数值求和的属性。
4. `attrWhereCondition` 中出现的每个过滤属性，必须在对应分类上有定义，或属于系统支持的常量属性。
5. 如果某个过滤属性只在部分分类存在，P0 按输入错误处理，不做“存在则过滤、不存在则跳过”的宽松语义。
6. 参与分类列表去重后不能为空；P0 建议至少包含两个逻辑分类，除非后续明确要复用该请求承载单分类总量。

非法输入不进入求解器，直接返回 `Result.FAILED`，错误信息应指明具体分类和缺失属性。例如：

```text
Invalid cross-category total request: attr Capacity is not defined on category mainBoard
Invalid cross-category total request: where attr PortRate is not defined on category disk
```

这意味着跨分类总量仍然不是“完全发散”的自由组合，只是约束边界从产品侧声明前移到输入侧标准化和校验。

### 3.4 多实例总量

多实例场景下，总量要求的语义是：

```text
对某一个逻辑 PartCategory 的所有实例进行汇总。
```

例如 `disk` 支持多实例，并在运行时展开为 `disk#0`、`disk#1`、`disk#2`，则：

```text
total(disk, mainBoard).Capacity
= sum(disk#0.Capacity)
+ sum(disk#1.Capacity)
+ sum(disk#2.Capacity)
+ sum(mainBoard.Capacity)
```

这与当前 `AttrParaType.SumSum` 的精神一致，但作用范围不同：

| 类型 | 范围 |
| --- | --- |
| `Sum` | 单个分类实例内部求和 |
| `SumSum` | 同一逻辑分类的多个实例求和 |
| 跨分类总量 | 多个逻辑分类求和；每个逻辑分类内部可包含单实例或多实例 |

P0 不建议把跨分类总量塞进 `AttrParaType` 枚举。`AttrParaType` 继续描述分类内部聚合方式，跨分类范围由新的 `CrossCategoryPartCategoryConstraintReq` 显式表达。

---

## 4. 设计方案

### 4.1 核心思路

执行流程调整为：

```text
InferParasReq
  |
  v
请求标准化
  |-- 单分类约束 -> ModuleInput.partCategoryInputs
  |-- 跨分类总量约束 -> ModuleInput.crossCategoryConstraintReqs
  |
  v
按 singleCategoryReqs 执行现有 PartCategory 过滤
  |
  v
创建过滤后的 CP 变量和单分类输入约束
  |
  v
基于过滤后的候选集和 ModuleInput.crossCategoryConstraintReqs 构建总量表达式
  |
  v
执行规则、优先级、求解、POST 后处理
```

关键点：

- 单分类过滤逻辑保持原有流程。
- 总量约束只在过滤后的候选集合上构建表达式。
- 总量 `where` 只影响表达式项集合，不修改单分类候选集合。
- 多实例分类需要展开所有实例。
- 可选分类沿用 RFC-0009 的分支和 present 语义。

### 4.2 输入模型扩展

从领域模型看，当前 `PartConstraintReq` 命名不够准确。它约束的不是单个 `Part`，而是 `PartCategory` 的候选集、数量或汇总属性。目标命名建议调整为 `PartCategoryConstraintReq`。

跨分类总量不要通过 `PartConstraintScope` 混入同一个 DTO，而是新增独立请求类型 `CrossCategoryPartCategoryConstraintReq`。公共字段抽取到 base，预处理阶段只需要识别两个标准化列表，不需要再通过 scope 判断一条请求到底是哪类语义。

```java
public abstract class PartCategoryConstraintReqBase {
    private AttrParaType attrType = AttrParaType.Sum;
    private String attrCode;
    private String attrComparator;
    private String attrValue;
    private String attrWhereCondition;
    private List<StrategyConfig> decisionStrategies;
}
```

```java
public class PartCategoryConstraintReq extends PartCategoryConstraintReqBase {
    private String partCategoryCode;
}
```

```java
public class CrossCategoryPartCategoryConstraintReq
        extends PartCategoryConstraintReqBase {
    private String code;
    private List<String> partCategoryCodes;
}
```

P0 可保留当前 `PartConstraintReq` 作为兼容入口，但进入求解前必须标准化成上述两类请求：

- 当前 `PartConstraintReq` -> `PartCategoryConstraintReq`。
- 新增跨分类输入 -> `CrossCategoryPartCategoryConstraintReq`。

目标北向请求：

```java
public class InferParasReq {
    private List<PartCategoryConstraintReq> partCategoryConstraintReqs;
    private List<CrossCategoryPartCategoryConstraintReq> crossCategoryConstraintReqs;

    // Compatibility field, deprecated after migration.
    private List<PartConstraintReq> partConstraintReqs;
}
```

跨分类输入必须明确 `attrCode`，不允许只传 code 后由系统推导汇总属性：

```json
{
  "code": "uplink_10g_capacity_total",
  "partCategoryCodes": ["disk", "mainBoard"],
  "attrCode": "Capacity",
  "attrComparator": ">=",
  "attrValue": "100",
  "attrWhereCondition": "PortRate=10G"
}
```

这对输入端提出更高标准化要求，但换来更简单、更清晰的预处理逻辑。

### 4.3 请求预处理

不新增 `ResolvedConstraintRequests` 或 `CrossCategoryTotalInput` 这类额外中间 DTO。当前代码已经有 `ModuleInput`、`PartCategoryInputBase`、`PartCategoryInput`、`MultiInstPartCategoryInput` 等运行态输入模型，跨分类总量应直接纳入这套模型。

建议扩展 `ModuleInput`：

```java
public class ModuleInput implements IModuleInput {
    private List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
    private List<CrossCategoryPartCategoryConstraintReq> crossCategoryConstraintReqs =
            new ArrayList<>();
}
```

预处理流程：

```java
ModuleInput moduleInput = toModuleInput(module, req);

// Existing path: normalize old PartConstraintReq and standard PartCategoryConstraintReq
// into PartCategoryInputBase list.
moduleInput.setPartCategoryInputs(buildPartCategoryInputs(module, req));

// New path: validate and attach cross-category requests directly.
List<CrossCategoryPartCategoryConstraintReq> crossReqs =
        normalizeCrossCategoryReqs(req);
crossCategoryConstraintValidator.validate(crossReqs, module);
moduleInput.setCrossCategoryConstraintReqs(crossReqs);
```

标准化和校验规则：

| 字段 | 规则 |
| --- | --- |
| `partCategoryCodes` | 必填；每个 code 必须能在 Module 中找到逻辑 PartCategory |
| `attrCode` | 必填；每个参与分类都必须定义该属性，且属性类型可数值求和 |
| `attrComparator` / `attrValue` | 必填；比较符只接受 `==`、`!=`、`<=`、`>=`、`<`、`>` |
| `attrWhereCondition` | 可选；出现的属性必须在每个参与分类上有定义 |
| `decisionStrategies` | P0 不建议放在跨分类总量请求上；排序仍由单分类请求或优先级规则承担 |
| `code` | 可选；为空时由系统生成稳定输入约束 code |

### 4.4 过滤逻辑调整

现有 `filterClone` 的核心能力继续复用，但它只处理单分类约束，不处理跨分类总量。跨分类请求已经放在 `ModuleInput.crossCategoryConstraintReqs` 中，后续由算法层在过滤后候选集上构建表达式。

```java
FilterCloneResult filterResult = filterClone(
        module,
        singleCategoryReqs);
```

`FilterCloneResult` 不需要新增跨分类字段，继续只表达过滤后的模块、单分类输入、过滤错误和 optional 输入。

过滤顺序：

1. 对每个单分类请求执行现有 `PartCategory.filterClone(req)`。
2. 没有单分类请求的必选分类仍按当前逻辑加入过滤后模块。
3. 没有单分类请求的可选分类继续按 RFC-0009 进入 optional branch。
4. 跨分类总量约束不参与第一步过滤，也不写入 `FilterCloneResult`。
5. 构建总量表达式时，再对每个已过滤分类应用总量 `whereCondition`。

### 4.5 总量表达式构建

新增构建器：

```java
public final class CrossCategoryTotalConstraintBuilder {
    void build(ModuleAlgImpl alg,
            List<CrossCategoryPartCategoryConstraintReq> reqs) {
        for (CrossCategoryPartCategoryConstraintReq req : reqs) {
            PartAlgCPLinearExpr expr = buildExpr(alg, req);
            addComparatorConstraint(alg.model(), expr,
                    req.getAttrComparator(), req.getAttrValue(), req.getCode());
        }
    }
}
```

表达式构建逻辑：

```java
private PartAlgCPLinearExpr buildExpr(ModuleAlgImpl alg,
        CrossCategoryPartCategoryConstraintReq req) {
    PartAlgCPLinearExpr expr = new PartAlgCPLinearExpr();

    for (String categoryCode : req.getPartCategoryCodes()) {
        List<PartCategoryAlgImpl> categoryAlgs =
                alg.getPartCategoryAlgsByLogicalCode(categoryCode);

        for (PartCategoryAlgImpl categoryAlg : categoryAlgs) {
            alg.appendSum4QuantityTerms(
                    expr,
                    categoryAlg,
                    req.getAttrCode(),
                    req.getAttrWhereCondition());
        }
    }

    return expr;
}
```

该伪代码表达目标行为，不要求 P0 暴露完全相同的方法名。当前 `PartAlgCPLinearExpr` 已能保存带 `partCategoryCode` 的 term，适合复用。

数学语义：

```text
total =
  Σ category in aggregate.categories
  Σ instance in category.instances
  Σ part in filteredCandidates(category, instance)
    if part matches aggregate.where:
        part.attr(attrCode) * part.qty
```

然后按比较符加约束：

```text
total >= 100
```

### 4.6 总量 where 的语义

总量 where 是二次过滤，只影响总量表达式项集合：

```text
disk first filter result:
  d1 PortRate=10G Capacity=40
  d2 PortRate=10G Capacity=60
  d3 PortRate=100G Capacity=80
  d4 PortRate=10-port Capacity=20
  d5 PortRate=100-port Capacity=20

aggregate where:
  PortRate=10G

aggregate disk terms:
  d1 Capacity * d1.qty
  d2 Capacity * d2.qty
```

`d3`、`d4`、`d5` 仍可能被 disk 自身约束选择，只是不贡献 `uplink_10g_capacity_total`。

正反语义：

| 条件 | 预期 |
| --- | --- |
| 总量 where 无匹配项 | 表达式为 0；如果 `>=100` 则无解，如果 `<=100` 可通过 |
| 某个分类无匹配项 | 该分类贡献 0，不立即报错 |
| 所有分类无匹配项且要求正总量 | 无解，诊断指向总量约束 |
| where 使用未声明属性 | 请求非法，返回 failed |
| attrCode 非数值属性 | 请求非法，返回 failed |
| 参与分类重复 | 预处理去重并记录 warning；如重复来自 spec 配置，应视为产品数据错误 |

新增日志必须使用英文，例如：

```text
Cross-category total constraint aggregate_uplink_10g matched 2 terms in category disk
```

### 4.7 与多实例的关系

当前系统已有多实例处理组件：

- `MultiInstPartCategoryInput`
- `MultiInstPartCategoryAlgImpl`
- `getPartCategoryAlgByInstPrefix(...)`
- `sum4Quantity("drive*", "Capacity", "...")` 这类调用风格

跨分类总量构建器需要把逻辑分类解析成一个或多个运行时分类算法实例：

```text
resolveLogicalCategory("disk")
  -> disk#0
  -> disk#1
  -> disk#2

resolveLogicalCategory("mainBoard")
  -> mainBoard
```

最终表达式：

```text
sum(disk#0 matched parts)
+ sum(disk#1 matched parts)
+ sum(disk#2 matched parts)
+ sum(mainBoard matched parts)
>= requiredValue
```

这就是用户所说的“对于某一个部件的所有实例进行汇总”。P0 不需要为多实例总量设计特殊业务例外。

### 4.8 与可选 PartCategory 的关系

如果跨分类总量组中包含可选分类，执行语义复用 RFC-0009：

- absent 分支中，该可选分类没有子项变量，对总量贡献 0。
- present 分支中，该可选分类创建变量，并按总量 where 参与表达式。
- 总量约束本身不要求组内每个分类都 present。
- 如果总量约束在 absent 分支无法满足，可通过 optional present 分支寻找可行解。

P0 可以先要求跨分类总量组优先由必选分类组成。如果需要支持可选分类参与总量，应复用现有 optional branch limit，避免可选分类组合裂变过多。

### 4.9 诊断语义

跨分类总量约束应有稳定 code：

```text
aggregate_uplink_10g_capacity_total
```

当无解或松弛诊断开启时，诊断信息应能返回：

```text
violatedRuleCodes:
  - aggregate_uplink_10g_capacity_total
```

建议将总量输入约束也纳入 `DiagnosticConstraint.TYPE_INPUT` 或新增更细的输入约束类型：

```java
public static final String TYPE_INPUT_AGGREGATE = "INPUT_AGGREGATE";
```

如果暂不新增类型，也必须在 description 中说明这是用户输入的跨分类总量约束，而不是产品基础规则。

---

## 5. 分层边界与接口矩阵

### 5.1 分层边界

| 层次 | 职责 |
| --- | --- |
| 产品基础数据 | 暂不声明跨分类汇总组；只提供 PartCategory、属性、可选性和多实例等基础定义 |
| 北向输入 | 提交标准化的单分类约束和跨分类总量约束 |
| 预处理层 | 拆分请求、兼容旧输入、校验分类和属性 |
| 过滤层 | 只按单分类约束执行候选过滤 |
| 算法层 | 基于过滤后候选构建单分类约束和跨分类总量约束 |
| 结果层 | 返回满足总量要求的 `ModuleInst` |
| 校验层 | P0 不扩展；后续统一重构 |

### 5.2 接口矩阵

| 入口 | 是否受影响 | P0 处理 |
| --- | --- | --- |
| 注解或 Schema 生成产品基础数据 | 否 | P0 不新增产品侧汇总组 |
| `inferParas(InferParasReq)` | 是 | P0 必须支持 |
| `PartCategoryConstraintExecutorImpl` 单分类入口 | 否 | 保持单分类语义，不处理跨分类总量 |
| `postCalculate(ModulePostCalcReq)` | 否 | 不改变 |
| `validate(ModuleValidateReq)` | 是 | 纳入遗留任务，P0 只记录不完成 |
| 序列化和反序列化 | 是 | 新增两类标准化请求；旧 `PartConstraintReq` 兼容保留 |
| 维护界面或规则管理 | 否 | P0 不引入产品侧汇总组维护 |

---

## 6. 关键代码改造

### 6.1 请求 DTO 标准化

```java
public abstract class PartCategoryConstraintReqBase {
    private AttrParaType attrType = AttrParaType.Sum;
    private String attrCode;
    private String attrComparator;
    private String attrValue;
    private String attrWhereCondition;
    private List<StrategyConfig> decisionStrategies;
}
```

```java
public class PartCategoryConstraintReq extends PartCategoryConstraintReqBase {
    private String partCategoryCode;
}
```

```java
public class CrossCategoryPartCategoryConstraintReq
        extends PartCategoryConstraintReqBase {
    private String code;
    private List<String> partCategoryCodes;
}
```

兼容规则：

- 当前 `PartConstraintReq` 暂时保留，但标记为兼容入口。
- 兼容入口只表达单分类语义，不承载跨分类总量。
- 新的跨分类总量必须通过 `CrossCategoryPartCategoryConstraintReq` 进入。
- 公共字段解析、比较符解析和 where 解析共用 base/helper。

### 6.2 输入校验器

新增输入校验组件：

```java
public final class CrossCategoryConstraintValidator {
    void validate(CrossCategoryPartCategoryConstraintReq req, Module module) {
        validateCategoryExists(req, module);
        validateAttrDefinedOnEveryCategory(req, module);
        validateAttrIsNumeric(req, module);
        validateWhereAttrsDefinedOnEveryCategory(req, module);
        validateComparatorAndValue(req);
    }
}
```

校验失败直接返回 `Result.FAILED`，不进入 `filterClone` 和求解器。

### 6.3 输入标准化替代新增拆分 DTO

当前 `normalizePartConstraint(...)` 直接按 `partCategoryCode` 分组。改造后不新增 `ResolvedConstraintRequests`，而是在 `toModuleInput(...)` 内完成标准化：

```java
ModuleInput moduleInput = new ModuleInput();

Map<String, List<PartCategoryConstraintReq>> singleReqs =
        normalizePartCategoryConstraints(req);

FilterCloneResult filterResult = filterClone(module, singleReqs);
moduleInput.setPartCategoryInputs(filterResult.partCategoryInputs());
moduleInput.setPartCategoryErrorInfoMap(filterResult.errorInfoMap());

List<CrossCategoryPartCategoryConstraintReq> crossReqs =
        normalizeCrossCategoryConstraints(req);
crossCategoryConstraintValidator.validate(crossReqs, module);
moduleInput.setCrossCategoryConstraintReqs(crossReqs);
```

原 `normalizePartConstraint` 可保留为单分类分组工具，但不能再直接把原始请求列表当成全部约束来源。

### 6.4 ModuleInput 传递总量约束

```java
public class ModuleInput implements IModuleInput {
    private List<PartCategoryInputBase> partCategoryInputs = new ArrayList<>();
    private List<CrossCategoryPartCategoryConstraintReq> crossCategoryConstraintReqs =
            new ArrayList<>();
}
```

`toModuleInput(...)` 和 `processProduct(...)` 应共享同一次 `ModuleInput` 构建结果，避免同一个请求在不同路径下重复标准化和重复过滤。

### 6.5 ModuleAlgImpl 构建总量约束

建议在变量创建、单分类输入约束完成后构建跨分类总量约束：

```java
void init(...) {
    initData();
    initInput();
    initCrossCategoryTotalInputs();
    initRules(CalcStage.MID);
}
```

目标方法：

```java
private void initCrossCategoryTotalInputs() {
    ModuleInput input = getModuleInput();
    if (input == null || input.getCrossCategoryConstraintReqs().isEmpty()) {
        return;
    }
    crossCategoryTotalConstraintBuilder.build(this,
            input.getCrossCategoryConstraintReqs());
}
```

构建器只使用当前过滤后模块和已创建变量，不再访问原始未过滤候选。

### 6.6 表达式 helper

长期建议提供清晰的内部 helper：

```java
public PartAlgCPLinearExpr sum4QuantityAcrossCategories(
        List<String> logicalPartCategoryCodes,
        String attrCode,
        String filterConditionStr) {
    // Expand logical category code to all runtime category algs.
}
```

南向公开 API 是否暴露该 helper 暂不在 P0 决策。P0 可以先由执行器内部构建，不要求产品算法作者手写跨分类表达式。

---

## 7. 验收准则

### 7.1 测试数据

建议新增测试：

```text
src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java
```

测试模块示例：

```java
@ModuleAnno(id = 9010)
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0010-test")
public static class CrossTotalConstraint extends ModuleAlgBase {

    @PartAnno(code = "disk", supportMultiInst = true)
    @DAttrAnno1(code = "PortRate", options = {"Rate_10G:10G", "Rate_100G:100G"})
    @DAttrAnno2(code = "Capacity", options = {"Cap_40:40", "Cap_60:60", "Cap_100:100"})
    private PartCategoryVar disk;

    @PartAnno(fatherCode = "disk", attrs = {"10G", "40"}, price = 40)
    private PartVar disk10g40;

    @PartAnno(fatherCode = "disk", attrs = {"10G", "60"}, price = 60)
    private PartVar disk10g60;

    @PartAnno(fatherCode = "disk", attrs = {"100G", "100"}, price = 100)
    private PartVar disk100g100;

    @PartAnno(code = "mainBoard")
    @DAttrAnno1(code = "PortRate", options = {"Rate_10G:10G", "Rate_100G:100G"})
    @DAttrAnno2(code = "Capacity", options = {"Cap_40:40", "Cap_80:80", "Cap_120:120"})
    private PartCategoryVar mainBoard;

    @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "80"}, price = 80)
    private PartVar board10g80;

    @PartAnno(fatherCode = "mainBoard", attrs = {"10G", "120"}, price = 120)
    private PartVar board10g120;

    @PartAnno(fatherCode = "mainBoard", attrs = {"100G", "120"}, price = 120)
    private PartVar board100g120;
}
```

跨分类总量输入：

```text
code: uplink_10g_capacity_total
partCategoryCodes: [disk, mainBoard]
attrCode: Capacity
attrComparator: >=
attrValue: 100
attrWhereCondition: PortRate=10G
```

### AC-001: 硬盘单独满足总量

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where PortRate=10G

disk:
  where PortRate in (10G,100G)

mainBoard:
  where PortRate in (10G,100G)
```

预期：

- 求解成功。
- 允许选择 `disk10g40 + disk10g60` 满足总量。
- 不要求 `mainBoard` 一定贡献容量。

### AC-002: 主控板单独满足总量

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where PortRate=10G
```

预期：

- 求解成功。
- 允许选择 `board10g120` 单独满足总量。
- 不要求 disk 分类自身满足 `Capacity >=100`。

### AC-003: 主控板与硬盘共同满足总量

测试数据增加：

```text
disk10g40
board10g80
```

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where PortRate=10G
```

预期：

- 允许 `disk10g40 + board10g80` 通过。
- 如果二者单独都不足 100，也不应被提前判无解。

### AC-004: 总量 where 不覆盖单分类候选

输入：

```text
disk:Sum_Quantity >=1 where PortRate in (10G,100G)
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where PortRate=10G
```

预期：

- `disk100g100` 可以作为 disk 单分类候选存在。
- `disk100g100` 不贡献 `uplink_10g_capacity_total`。
- 如果最终只选择 `disk100g100` 且没有其他 10G 贡献，则总量约束不通过。

### AC-005: 总量 where 在过滤后候选集上执行

输入：

```text
disk:where PortRate=10G
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where Capacity>=60
```

预期：

- 总量表达式只从 disk 已过滤出的 10G 候选中再筛 `Capacity>=60`。
- 不得把原始未过滤的 `disk100g100` 加回表达式。

### AC-006: 多实例分类汇总所有实例

输入：

```text
disk supportMultiInst = true
disk instance 0 可选 disk10g40
disk instance 1 可选 disk10g60

CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=100 where PortRate=10G
```

预期：

- `disk#0.disk10g40 + disk#1.disk10g60` 可满足总量。
- 表达式必须覆盖同一逻辑 `disk` 的所有实例。

### AC-007: 参与分类不存在时失败

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  partCategoryCodes=[disk, missingBoard]
  Sum_Capacity >=100
```

预期：

- 返回 `Result.FAILED`。
- 错误信息说明 `missingBoard` 分类不存在。
- 不进入求解器。

### AC-008: 非法属性不能做总量

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  partCategoryCodes=[disk, mainBoard]
  Sum_PortRate >=100
```

预期：

- 返回 `Result.FAILED`。
- 错误信息说明 `PortRate` 不是数值属性，不能用于总量求和。

补充输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  partCategoryCodes=[disk, mainBoard]
  Sum_Capacity >=100 where UnknownAttr=10G
```

补充预期：

- 返回 `Result.FAILED`。
- 错误信息说明 `UnknownAttr` 在参与分类上没有定义。

### AC-009: 无解诊断指向总量约束

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  Sum_Capacity >=999 where PortRate=10G
relaxSolve = true
```

预期：

- 返回无解或松弛诊断结果。
- 诊断中包含 `aggregate_uplink_10g_capacity_total` 或等价输入约束 code。
- 诊断最好包含贡献明细，例如 `disk=100, mainBoard=120, required=999`。

### AC-010: 总量组包含可选分类时自动打开 present 分支

测试数据：

```text
mainBoard 为 REQUIRED
expansionBoard 为 OPTIONAL
二者都定义 Capacity 和 PortRate
```

输入：

```text
CROSS_CATEGORY_TOTAL uplink_10g_capacity_total:
  partCategoryCodes=[mainBoard, expansionBoard]
  Sum_Capacity >=100 where PortRate=10G
```

预期：

- absent expansionBoard 分支如果不满足总量，不应直接整体无解。
- 系统应自动生成 expansionBoard present 分支参与求解。
- present 分支中 expansionBoard 只在匹配 `PortRate=10G` 时贡献容量。
- 分支数量受 `ConstraintConfig.maxOptionalPartCategoryBranches` 控制。

### AC-011: 存量单分类输入行为不变

输入：

```text
disk:Sum_Capacity >=100 where PortRate=10G
```

预期：

- 仍按单分类约束处理。
- 不触发跨分类总量逻辑。
- 现有 `PartCategoryFilterEmptyTest`、`SearchStrategyTest`、`SearchStrategyMultiTest` 回归通过。

### 7.2 测试封装建议

测试方法不应直接拼装大量请求对象。建议在 `ModuleScenarioTestBase` 增加 helper：

```java
protected CrossCategoryPartCategoryConstraintReq totalReq(
        String code,
        List<String> partCategoryCodes,
        String attrExpr,
        String whereCondition) {
    CrossCategoryPartCategoryConstraintReq req =
            new CrossCategoryPartCategoryConstraintReq();
    req.setCode(code);
    req.setPartCategoryCodes(partCategoryCodes);
    parseAttrExpr(attrExpr, req);
    req.setAttrWhereCondition(whereCondition);
    return req;
}
```

测试写法示例：

```java
@Test
public void testCrossTotal_DiskAndBoardTogether() {
    inferRecommendModuleWithReqs(
            totalReq("uplink_10g_capacity_total",
                    List.of("disk", "mainBoard"),
                    "Sum_Capacity >=100",
                    "PortRate=10G"));

    resultAssert().assertSuccess();
    assertSoluContain("disk10g40(Q:1,H:0,S:1),board10g80(Q:1,H:0,S:1)");
}
```

具体 helper 命名可按现有测试基类风格调整。

### 7.3 边界条件

| 条件 | 输入 | 预期 |
| --- | --- | --- |
| 总量 where 无匹配 | `>=100 where PortRate=25G` | 无解，诊断指向总量约束 |
| 某一分类无匹配 | disk 无 10G，board 有 10G | board 可单独贡献，通过与否取决于总值 |
| 单分类过滤为空 | `disk:where PortRate=25G` | 沿用 RFC-0002/RFC-0009 的过滤空处理 |
| 多实例数量为 0 | 某个 disk 实例未选择部件 | 该实例贡献 0 |
| 可选分类 absent | optional category 未进入当前分支 | 对总量贡献 0 |
| 可选分类 present | optional category 进入当前分支 | 匹配 where 的候选参与求和 |
| 重复分类 | `partCategoryCodes=[disk,disk]` | 预处理去重并记录 warning |
| 单位或属性含义不一致 | disk 用 G，board 用 T | P0 不做隐式换算；输入侧应保证同一 attrCode 在各分类上单位一致，无法确认时返回输入错误 |

### 7.4 回归测试

- [ ] `PartCategoryFilterEmptyTest`
- [ ] `SearchStrategyTest`
- [ ] `SearchStrategyMultiTest`
- [ ] `OptionalPartCategoryWhitelistGuardTest`
- [ ] `StructCombinationRuleTest`
- [ ] `MultiPCTest`
- [ ] `EnumMultReq4MultiReqTest`
- [ ] `DynMultReq4MultiReqTest`

---

## 8. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 新增 `PartCategoryConstraintReqBase`、`PartCategoryConstraintReq`、`CrossCategoryPartCategoryConstraintReq` 目标 DTO | P0 | 规划中 |
| 2 | 保留旧 `PartConstraintReq` 兼容入口，并在预处理阶段标准化为 `PartCategoryConstraintReq` | P0 | 规划中 |
| 3 | 在 `toModuleInput(...)` 中完成输入标准化，把单分类约束转为 `PartCategoryInputBase`，把跨分类约束写入 `ModuleInput.crossCategoryConstraintReqs` | P0 | 规划中 |
| 4 | 实现 `CrossCategoryConstraintValidator`，校验分类存在、汇总属性存在且可求和、where 属性存在 | P0 | 规划中 |
| 5 | 调整 `normalizePartConstraint` 调用点，只让单分类请求进入现有过滤流程 | P0 | 规划中 |
| 6 | 扩展 `ModuleInput`，传递 `CrossCategoryPartCategoryConstraintReq`；`FilterCloneResult` 不承载跨分类总量 | P0 | 规划中 |
| 7 | 实现 `CrossCategoryTotalConstraintBuilder`，基于过滤后候选构建总量表达式 | P0 | 规划中 |
| 8 | 支持多实例逻辑分类展开，确保同一 PartCategory 的所有实例参与汇总 | P0 | 规划中 |
| 9 | 支持跨分类总量组中的可选分类触发 optional present 分支 | P0 | 规划中 |
| 10 | 增加错误处理、诊断 code 和贡献明细，覆盖非法分类、非法属性、无匹配等边界 | P0 | 规划中 |
| 11 | 补充 `CrossPartCategoryTotalConstraintTest` 和回归测试 | P0 | 规划中 |
| 12 | 遗留任务：系统整体构建完成后，重新设计总量约束、结构化规则、代码规则在 `validate` 路径下的统一校验机制 | P2 | 遗留 |

---

## 9. 遗留任务：统一校验机制

本 RFC 的 P0 目标是解决推荐求解路径的跨分类总量输入与表达式构建。校验路径先不在 P0 中展开。

原因是当前系统已有多类规则来源：

- 单分类输入约束。
- 跨分类总量输入约束。
- 结构化组合规则。
- 代码规则。
- PRE/MID/POST 阶段计算规则。
- optional branch 和多实例展开逻辑。

如果在每个 RFC 中零散补 `validate`，容易形成两套不一致语义：推荐求解能过，给定配置校验却走另一套逻辑。

因此保留一个明确遗留任务：

```text
等整个系统构建完成后，重新构建统一校验机制。
```

后续校验机制至少需要回答：

1. `validate(ModuleValidateReq)` 是复用同一套 CP 约束建模，还是生成独立校验 IR。
2. 跨分类总量约束的违反信息如何返回具体 aggregate code、实际总值、目标值和贡献明细。
3. 多实例配置中，校验输入如何表达实例 ID、逻辑分类和运行时展开分类之间的映射。
4. optional absent/present 在给定配置校验中如何判断。
5. POST 计算派生值是否参与校验，参与时如何保证顺序。

验收任务应单独创建 RFC 或子任务，不与本 RFC 的 P0 推荐求解实现混在一起。

---

## 10. 风险与兼容策略

### 10.1 总量约束误下推

风险：把总量 where 当成单分类过滤条件，导致非贡献候选被提前删除。

策略：总量 where 只在 `CrossCategoryTotalConstraintBuilder` 中使用，不进入 `filterClone`。

### 10.2 分类组配置发散

风险：上游随意传多个分类，导致语义不可控。

策略：P0 不在产品侧维护汇总组，但输入端必须显式传 `partCategoryCodes`，并通过 `CrossCategoryConstraintValidator` 校验分类和属性存在性。非法组合按输入错误返回。

### 10.3 多实例展开遗漏

风险：只汇总默认实例，漏掉同一逻辑分类的其他实例。

策略：跨分类总量必须通过逻辑分类解析运行时实例列表，不直接 `getPartCategoryAlg(categoryCode)` 单点查询。

### 10.4 与 optional branch 组合膨胀

风险：总量组包含多个可选分类时，present 分支增多。

策略：P0 要支持总量组中的可选分类自动进入 present 分支，但必须沿用 `ConstraintConfig.maxOptionalPartCategoryBranches` 控制分支数量。

### 10.5 单位和属性类型不一致

风险：不同分类虽然字段名相同，但单位或含义不同。

策略：P0 不做隐式单位换算。输入侧必须明确 `attrCode`，系统只校验该属性在所有参与分类上存在且可数值求和；如果发现属性类型或单位元数据不一致，按输入错误失败。

---

## 11. 已确认决策

1. 总量需求应与单分类需求在输入预处理阶段明确区分。
2. 存量 `PartConstraintReq.partCategoryCode` 单分类语义保持不变，但目标命名应收敛为 `PartCategoryConstraintReq`。
3. 不新增 `PartConstraintScope`，跨分类总量使用独立的 `CrossCategoryPartCategoryConstraintReq`。
4. 产品侧暂不维护跨分类汇总组，不新增 `Module.crossCategoryAggregateSpecs`。
5. 跨分类输入必须显式传 `partCategoryCodes` 和 `attrCode`，系统不从 code 推导汇总属性。
6. 输入预处理必须校验分类存在、汇总属性存在且可数值求和、where 属性在参与分类上存在。
7. 单分类过滤先执行，总量表达式后构建。
8. 总量 where 只影响总量表达式项集合，不反向覆盖单分类候选集合。
9. 多实例场景中，同一逻辑分类的所有实例都要参与总量汇总。
10. 跨分类总量不强制组内每个分类都必须被选中。
11. P0 支持总量组中的可选分类自动进入 optional present 分支。
12. 诊断最好返回每个分类的贡献明细，例如 `disk=40, mainBoard=80, required=100`。
13. P0 只要求推荐求解路径，校验路径作为遗留任务后续统一重构。

---

## 12. 已关闭问题

| 问题 | 结论 |
| --- | --- |
| 产品侧汇总组放在 `Module` 还是 ext schema | 产品侧先不做限制，不新增汇总组模型 |
| 是否可只传 code 并推导 `attrCode` | 不可以，跨分类输入必须明确 `attrCode` |
| 总量组包含可选分类时 P0 是否支持 | 要支持，允许自动打开 optional present 分支 |
| 诊断是否返回分类贡献明细 | 最好返回，作为 P0 诊断目标 |

---

## 13. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `doc/RFC-0006-North-Interface-Refactor.md`
- `doc/RFC-0008-Rule-Model-IR-Refactor.md`
- `doc/RFC-0009-Optional-PartCategory-Whitelist-Guard.md`
- `src/main/java/com/jmix/executor/model/PartConstraintReq.java`
- `src/main/java/com/jmix/executor/model/InferParasReq.java`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java`
- `src/main/java/com/jmix/executor/impl/ModuleInput.java`
- `src/main/java/com/jmix/executor/impl/PartCategoryInputBase.java`
- `src/main/java/com/jmix/executor/impl/MultiInstPartCategoryInput.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/MultiInstPartCategoryAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/PartAlgCPLinearExpr.java`
