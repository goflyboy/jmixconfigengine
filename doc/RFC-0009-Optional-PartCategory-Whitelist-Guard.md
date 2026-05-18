# RFC-0009: 可选 PartCategory 白名单 Guard 语义

> 状态：草案（Draft）
> 日期：2026-05-18
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`, `doc/RFC-0007-Struct-Combination-Rule-Schema.md`, `doc/RFC-0008-Rule-Model-IR-Refactor.md`

---

## 设计决策摘要

本 RFC 单独讨论“可选 PartCategory 参与白名单/黑名单组合规则时，规则何时生效”的 guard 语义。核心目标是：定义了 CPU 与鼠标的配套关系，不应导致用户只配置 CPU 时无解；但一旦用户明确选择鼠标，CPU 与鼠标的配套规则必须生效。

| 主题 | 决策 |
| --- | --- |
| 分类必选性 | 在 `PartCategory` 增加选择策略，首期支持 `REQUIRED` 与 `OPTIONAL` |
| 规则生效条件 | 含可选分类的组合规则必须有 guard；可选分类未被要求且未被选择时，该规则不生效 |
| 白名单语义 | 只在规则维度全部 present 时要求命中允许 tuple；可选维度 absent 时不强制配置 |
| 黑名单语义 | 禁止已选择 tuple；可选维度 absent 时天然不命中，不得因为黑名单规则强制可选分类出现 |
| 首期推荐方案 | 采用“两阶段主求解 + 可选件裂变/补全”的执行方案 |
| 备选方案 | 保留求解器内 presence guard 方案、请求驱动规则加载方案作为后续演进 |
| 价格策略 | `MIN` 且价格非负时默认不配置可选件；`MAX` 时强制配置可选件；无价格目标时返回 absent/present 两类解 |
| 依赖关系 | P0 可在构建测试数据时写死规则依赖；正式依赖图接入 `RuleReferenceSet` 后续细化 |

---

## 1. 摘要

当前兼容/组合类白名单规则会把参与规则的分类当成必须参与求解的维度，导致“可选鼠标未选择”也会触发 CPU 与鼠标白名单校验，从而产生不符合业务预期的无解。

本 RFC 提议为 `PartCategory` 增加必选/可选标记，并为含可选分类的白名单规则引入 guard 语义。首期推荐采用两阶段方案：主求解只处理必选设备和用户明确要求的可选设备；用户未要求的可选设备在主解之后按规则进行可选补全和结果裂变。

---

## 2. 动机

### 2.1 问题背景

`RFC-0007` 定义了结构化组合规则：

- 白名单：选中的部件组合必须命中至少一条允许组合。
- 黑名单：禁止命中任一禁止组合。

这套语义对必选分类是合理的。例如 CPU 与硬盘都是必选分类时，CPU 与硬盘白名单可以直接约束主求解空间。

但对可选分类会出现语义冲突。例如鼠标是可选件：

```text
CPU 8 核以下 -> 鼠标 1
CPU 8 核以上 -> 鼠标 2
```

如果用户只要求配置 CPU，没有要求配置鼠标，系统不应该因为找不到鼠标组合而报错。只有当用户明确要求鼠标，或策略决定生成“带鼠标”的扩展方案时，CPU 与鼠标配套规则才应该参与校验。

### 2.2 当前代码中的根因

当前 `CompatibleConstraintAlg.addCompatibleConstraintInCompatible` 会对左右两侧部件集合执行 `addExactlyOneConstraint`。这意味着只要兼容规则被加载，左右分类都会被强制选择一个部件。

对于必选分类，这个副作用通常与业务目标一致；对于可选分类，它会把“变量存在”误解成“必须配置”，从而让可选分类变成隐式必选。

### 2.3 具体场景

```text
分类：
  cpu    REQUIRED
  disk   REQUIRED
  mouse  OPTIONAL

规则：
  cpu_disk_white:  CPU 与硬盘配套白名单
  cpu_mouse_white: CPU 与鼠标配套白名单

输入：
  cpu:CoreNum=4
  disk:Capacity>=1
  未输入 mouse

当前行为：
  加载 cpu_mouse_white 后，mouse 被要求参与白名单命中，可能无解。

期望行为：
  主求解返回 CPU + disk 解。
  如果需要返回可选扩展，则基于主解再生成 CPU + disk + mouse 的扩展解。
```

### 2.4 为什么需要单独 RFC

可选分类不是单纯的“空过滤条件”问题，而是组合规则的激活条件问题。它会影响：

- `PartCategory` 领域模型。
- 规则依赖关系和差量加载。
- 白名单/黑名单运行态约束生成。
- 价格目标和枚举解排序。
- `validate(ModuleValidateReq)` 对给定配置的校验语义。

因此不应混在 `RFC-0007` 的结构化规则 Schema 中处理，而应作为独立 guard 语义设计。

---

## 3. 外部方案调研与分析

### 3.1 特征模型：可选特征与配置状态分离

软件产品线里的 Feature Model 与产品配置场景很接近。ABS 文档将 feature model 描述为一组逻辑约束，定义合法的 feature 组合；其语法中子 feature 可以用 `opt` 标记为可选，并且支持 `require`、`exclude` 等跨树依赖。

这给本系统的启发是：`PartCategory` 是否存在于模型中，不等于该分类必须出现在最终配置中。应当区分：

- 模型定义中有 `mouse`。
- 用户或策略选择了 `mouse`。
- 规则在 `mouse` 参与时才校验 `mouse` 的合法组合。

FeatureIDE 也采用 mandatory/optional 这类特征关系来表达子特征是否必须被选择。虽然它面向软件产品线，但“可选节点是否进入产品配置”与这里的可选部件分类高度一致。

### 3.2 分阶段配置：未决选择不应提前约束成最终选择

Staged Configuration 的研究把产品配置视为逐步消除变体空间的过程。用户或不同阶段的参与者可以先做部分选择，再逐步专门化到最终配置。

对本系统而言，用户只提出 CPU 需求时，配置仍然是部分配置：鼠标分类处于未决状态，而不是已选择状态。此时把 CPU 与鼠标白名单加载进主求解并强制鼠标命中，会把未决状态提前解释成“必须选择鼠标”，违反分阶段配置思想。

因此，主求解阶段应只对当前已经决策的维度施加强约束；未决可选分类可以在后置补全阶段生成扩展方案。

### 3.3 CP-SAT：presence literal 与条件约束

OR-Tools CP-SAT 官方文档推荐用 channeling constraints 表达 if-then-else 这类条件关系，并通过 `OnlyEnforceIf` 让约束只在某个布尔条件成立时生效。

OR-Tools 的 optional interval 也是同一思路：区间是否存在由 presence literal 控制，不活跃的 interval 会被相关约束忽略。

这给本系统的启发是：可选 `PartCategory` 可以建模为一个 presence 布尔量：

```text
mouse.present = 1  表示鼠标分类参与当前解
mouse.present = 0  表示鼠标分类不参与当前解
```

白名单规则不应裸奔执行，而应变成：

```text
OnlyEnforceIf(cpu.present && mouse.present):
  (cpu, mouse) in allowedTuples
```

对于必选分类，`present` 恒为 1；对于可选分类，`present` 由请求、策略或裂变阶段决定。

### 3.4 CP-SAT：白名单/黑名单可映射到 table constraint

OR-Tools Java API 提供 `addAllowedAssignments` 和 `addForbiddenAssignments`。前者表达允许 tuple 集合，后者表达禁止 tuple 集合。

这与 `RFC-0007` 的 `CodependantRuleSchema` 很贴合：

- WHITE 可映射为 allowed assignments。
- BLACK 可映射为 forbidden assignments。
- 但含可选分类时，table constraint 仍必须由 presence guard 控制，避免缺席维度触发强制选择。

### 3.5 调研结论

综合以上资料，可选部件规则的稳定设计应满足三条原则：

1. **变量存在不等于配置存在**：求解器里可以有 `mouse` 变量，但 `mouse.present=0` 时它不参与当前解。
2. **规则必须有激活上下文**：组合白名单只在相关维度都参与当前配置时执行。
3. **主配置与可选补全可以分阶段**：当用户未明确要求可选分类时，主求解不应被可选规则拖入无解；可选补全可以作为后置分支生成。

---

## 4. 术语与语义定义

### 4.1 PartCategory 选择策略

新增分类选择策略：

```java
public enum PartCategorySelectionPolicy {
    REQUIRED,
    OPTIONAL
}
```

建议在 `PartCategory` 上增加字段：

```java
public class PartCategory extends ModuleBase implements IModule, IPart {
    private PartCategorySelectionPolicy selectionPolicy =
            PartCategorySelectionPolicy.REQUIRED;
}
```

注解同步增加：

```java
public @interface PartAnno {
    boolean required() default true;
}
```

`required=false` 映射为 `PartCategorySelectionPolicy.OPTIONAL`。首期保持向后兼容：未声明时全部视为 `REQUIRED`。

### 4.2 present 语义

对每个分类定义运行态存在性：

```text
category.present = sum(part.selected for part in category.parts) >= 1
```

首期单实例分类建议约束如下：

| 分类策略 | 约束 |
| --- | --- |
| REQUIRED | `sum(selected) >= 1`，具体是否 exactly one 仍由选择规则控制 |
| OPTIONAL | `sum(selected) == 0 or sum(selected) >= 1`；当请求要求 present 时加 `sum(selected) >= 1` |
| OPTIONAL + 单选 | `sum(selected) <= 1`；present 时 `sum(selected) == 1` |

P0 不强制一次重构所有分类选择约束。关键是兼容/组合规则不得自己附带 `ExactlyOne` 把可选分类拉成必选。

### 4.3 规则激活 guard

一条组合规则的 guard 由参与维度的 present 状态组成：

```text
rule.active = all(category.present for category in rule.dimensionCategoryCodes)
```

如果所有维度都是必选分类，`rule.active` 恒为 true。

如果存在可选维度，则：

- 用户明确要求该可选分类时，该分类 `present` 被强制为 true。
- 用户未要求该可选分类时，主求解阶段不强制其 present。
- 后置裂变的“带可选件”分支会强制该分类 present，并加载/执行相关规则。

### 4.4 白名单 guard 语义

白名单规则：

```text
WHITE(cpu, mouse):
  (cpu1, mouse1)
  (cpu2, mouse2)
```

语义：

```text
if cpu.present && mouse.present:
    selectedTuple(cpu, mouse) must be in allowedTuples
else:
    rule is inactive
```

即：鼠标不出现时，不要求命中 CPU + 鼠标白名单；鼠标一旦出现，必须命中。

### 4.5 黑名单 guard 语义

黑名单规则：

```text
BLACK(cpu, mouse):
  (cpu1, mouse2)
```

语义：

```text
if cpu.present && mouse.present:
    selectedTuple(cpu, mouse) must not be in forbiddenTuples
else:
    rule is inactive or vacuously satisfied
```

黑名单不得附带 `ExactlyOne(mouse)`。如果鼠标 absent，则 `(cpu1, mouse2)` 不可能命中，规则自然通过。

---

## 5. 设计方案

### 5.1 推荐方案：主求解 + 可选件裂变补全

首期推荐采用两阶段执行。

#### 5.1.1 阶段一：必选设备主求解

主求解阶段只加载：

- 不含可选分类的规则。
- 用户明确要求的可选分类相关规则。
- 价格策略或请求策略明确要求 present 的可选分类相关规则。

如果用户未要求 `mouse`，则 `cpu_mouse_white` 不进入主求解硬约束。

主求解输出基础解：

```text
S1 = cpu1 + disk1
S2 = cpu2 + disk2
...
```

#### 5.1.2 阶段二：可选件裂变

对每个基础解，按可选分类生成分支：

```text
S1.absentMouse  = cpu1 + disk1
S1.presentMouse = cpu1 + disk1 + mouseX
```

`presentMouse` 分支不是枚举所有鼠标组合，而是运行一个轻量补全求解：

1. 固定主解中的必选部件选择和数量。
2. 强制 `mouse.present = true`。
3. 加载 `cpu_mouse_white` 等涉及 mouse 的组合规则。
4. 只求一个可行补全，或按价格策略求一个最优补全。

这样主求解仍关注高价值/必选设备，可选件作为低价值辅料在后处理阶段补全。

#### 5.1.3 结果排序

结果返回时按策略合并排序：

| 请求策略 | 可选件处理 |
| --- | --- |
| 价格最小 `MIN` | 如果可选件价格非负，默认只返回 absent 分支 |
| 价格最大 `MAX` | 强制 present 分支；如果补全失败则该主解不返回 |
| 无价格策略 | 返回 absent 与 present 两类分支，交给用户判断 |
| 指定 `mouse` 需求 | 主求解或补全阶段必须 present，不返回 absent 分支 |

注意：如果后续支持负价格、折扣、赠品或套餐返点，`MIN` 不应简单跳过可选件，而要由价格目标函数判断。

### 5.2 备选方案 A：求解器内 presence guard

将可选分类也纳入同一个 CP-SAT 模型，为每个分类创建 `present` 布尔变量，并使用 `OnlyEnforceIf` 控制组合规则。

伪代码：

```java
BoolVar mousePresent = model.newBoolVar("mouse_present");
LinearExpr mouseSelected = sumSelected("mouse");

model.addGreaterOrEqual(mouseSelected, 1).onlyEnforceIf(mousePresent);
model.addEquality(mouseSelected, 0).onlyEnforceIf(mousePresent.not());

Constraint white = model.addAllowedAssignments(
        new LinearArgument[] { cpuChoice, mouseChoice });
white.addTuple(cpu1Id, mouse1Id);
white.addTuple(cpu2Id, mouse2Id);
white.onlyEnforceIf(mousePresent);
```

优点：

- 语义最统一。
- 可以获得全局最优解。
- 适合未来多个可选分类互相影响的复杂场景。

缺点：

- 需要先补齐分类 cardinality、choice var、tuple table 等底层建模能力。
- 当前 `CompatibleConstraintAlg` 中 `ExactlyOne` 副作用需要重构。
- 可选分类多时，主模型变量和 tuple 数量会明显膨胀。

### 5.3 备选方案 B：请求驱动规则加载

规则加载时先根据请求判断是否加载含可选分类的规则：

```text
if request contains mouse:
    load cpu_mouse_white
else:
    skip cpu_mouse_white
```

优点：

- 改动最小。
- 很适合“用户明确要鼠标才校验鼠标”的第一层需求。

缺点：

- 无法自然生成“带鼠标/不带鼠标”两类结果。
- 如果价格 `MAX` 或推荐策略希望主动配置可选件，单纯按请求加载会漏掉规则。
- 规则依赖关系需要准确，否则差量加载容易不完整。

### 5.4 备选方案 C：后置规则引擎校验/过滤

主求解不考虑可选规则，得到结果后用规则引擎检查和过滤可选扩展。

优点：

- 主模型最简单。
- 可快速验证业务流程。

缺点：

- 不能保证可选扩展的全局可行性和最优性。
- 可选分类之间存在联动时，后置过滤可能产生大量无效候选。
- 与 `validate(ModuleValidateReq)` 的统一语义容易分裂。

本 RFC 不推荐把 C 作为长期方案，只适合临时验证。

---

## 6. 关键代码改造

### 6.1 PartCategory 与注解扩展

```java
public enum PartCategorySelectionPolicy {
    REQUIRED,
    OPTIONAL
}

public class PartCategory extends ModuleBase implements IModule, IPart {
    private PartCategorySelectionPolicy selectionPolicy =
            PartCategorySelectionPolicy.REQUIRED;

    public boolean isRequiredSelection() {
        return selectionPolicy == PartCategorySelectionPolicy.REQUIRED;
    }

    public boolean isOptionalSelection() {
        return selectionPolicy == PartCategorySelectionPolicy.OPTIONAL;
    }
}
```

```java
public @interface PartAnno {
    boolean required() default true;
}
```

`ModuleGenneratorByAnno` 构建 `PartCategory` 时：

```java
partCategory.setSelectionPolicy(
        partAnno.required()
                ? PartCategorySelectionPolicy.REQUIRED
                : PartCategorySelectionPolicy.OPTIONAL);
```

### 6.2 规则依赖分析

组合规则需要能知道自己引用了哪些 `PartCategory`：

```java
public final class RuleActivationAnalyzer {
    RuleActivation analyze(Rule rule, Module module) {
        List<String> dimensionCodes = resolveDimensionCategoryCodes(rule);
        List<String> optionalCodes = dimensionCodes.stream()
                .filter(code -> module.getPartCategory(code).isOptionalSelection())
                .toList();

        return new RuleActivation(dimensionCodes, optionalCodes);
    }
}
```

P0 可以先在构建测试数据时写死 `dimensionCategoryCodes`，复用 `RFC-0007` 的 `CombinationStructRuleSchema.dimensionCategoryCodes`。后续由 `RuleReferenceSet` 和 `RefProgObjSchema` 统一支撑规则依赖图。

### 6.3 规则加载策略

```java
public enum RuleLoadDecision {
    LOAD_IN_MAIN_SOLVE,
    LOAD_IN_OPTIONAL_AUGMENTATION,
    SKIP
}
```

```java
RuleLoadDecision decide(Rule rule, RequestContext req, PricePolicy pricePolicy) {
    RuleActivation activation = analyzer.analyze(rule, module);
    if (activation.optionalCategoryCodes().isEmpty()) {
        return LOAD_IN_MAIN_SOLVE;
    }

    if (req.requiresAny(activation.optionalCategoryCodes())) {
        return LOAD_IN_MAIN_SOLVE;
    }

    if (pricePolicy == PricePolicy.MAX) {
        return LOAD_IN_MAIN_SOLVE;
    }

    return LOAD_IN_OPTIONAL_AUGMENTATION;
}
```

这里 `requiresAny` 首期可定义为：

- 请求中存在该 `PartCategory` 的数量/属性约束。
- 请求中存在该分类的具体 `PartInst` 输入。
- 请求显式声明该可选分类必须配置。

仅包含排序策略是否算“明确要求”，需要在本 RFC 评审中确认。

### 6.4 可选补全器

新增补全组件：

```java
public final class OptionalPartAugmenter {
    List<ModuleInst> augment(
            Module module,
            List<ModuleInst> baseSolutions,
            List<Rule> optionalRules,
            OptionalAugmentPolicy policy) {
        // 1. absent branch: 复制主解，标记可选分类未配置
        // 2. present branch: 固定主解，强制 optional category present
        // 3. 加载 optionalRules，运行轻量求解
        // 4. 合并和排序
    }
}
```

首期约束：

- 每个主解对每个可选分类只求一个可行补全。
- 不枚举同一可选分类下的所有组合。
- 多个可选分类同时存在时，默认按配置限制最大裂变数量，避免 `2^N` 爆炸。

### 6.5 CompatibleConstraintAlg 去掉隐式 ExactlyOne

当前兼容规则不应承担“分类必须选一个”的职责。建议将：

```java
addExactlyOneConstraint(leftPartsExpr.getPartVars());
addExactlyOneConstraint(rightPartsExpr.getPartVars());
```

迁移到明确的选择规则或分类 cardinality 规则中。

兼容/组合规则只表达组合合法性：

```text
WHITE: present tuple must be in allowed tuples
BLACK: selected forbidden tuple is not allowed
REQUIRES: if left tuple present then right guard must be present
```

这样才能避免可选分类被兼容规则强制选择。

---

## 7. 验收准则

### 7.1 测试数据

建议新增测试：

```text
src/test/java/com/jmix/scenario/ruletest/OptionalPartCategoryWhitelistGuardTest.java
```

极简模型：

```java
@ModuleAnno(id = 9009)
public static class OptionalMouseConstraint extends ConstraintAlgBase {

    @PartAnno(code = "cpu")
    @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8"})
    private PartCategoryVar cpu;

    @PartAnno(fatherCode = "cpu", attrs = {"4"}, price = 100)
    private PartVar cpu4;

    @PartAnno(fatherCode = "cpu", attrs = {"8"}, price = 200)
    private PartVar cpu8;

    @PartAnno(code = "disk")
    private PartCategoryVar disk;

    @PartAnno(fatherCode = "disk", price = 50)
    private PartVar disk1;

    @PartAnno(code = "mouse", required = false)
    @DAttrAnno1(code = "Level", options = {"Low:1", "High:2"})
    private PartCategoryVar mouse;

    @PartAnno(fatherCode = "mouse", attrs = {"1"}, price = 10)
    private PartVar mouse1;

    @PartAnno(fatherCode = "mouse", attrs = {"2"}, price = 30)
    private PartVar mouse2;
}
```

白名单：

```text
cpu.CoreNum < 8  -> mouse.Level = 1
cpu.CoreNum >= 8 -> mouse.Level = 2
```

### AC-001: 未要求可选分类时，白名单不阻断主解

输入：

```text
cpu:CoreNum=4
disk:
未输入 mouse
```

预期：

- 主求解成功。
- 至少返回一个不含 mouse 的解。
- 不因为 `cpu_mouse_white` 无法命中而无解。

### AC-002: 明确要求可选分类时，白名单必须生效

输入：

```text
cpu:CoreNum=4
mouse:
```

预期：

- 返回解中 mouse 必须 present。
- 只能选择 `mouse1`。
- 不能选择 `mouse2`。

### AC-003: 明确选择不匹配可选部件时无解或校验失败

输入：

```text
cpu:CoreNum=4
mouse:code=mouse2
```

预期：

- 推荐求解无可行解，或 `validate` 返回 invalid。
- 违反规则包含 `cpu_mouse_white`。

### AC-004: 无价格策略时返回 absent 与 present 两类解

输入：

```text
cpu:CoreNum=4
disk:
未输入 mouse
```

预期：

- 返回 `cpu4 + disk1`。
- 返回 `cpu4 + disk1 + mouse1`。
- 不返回 `cpu4 + disk1 + mouse2`。

### AC-005: 价格最小时默认不配置可选件

输入：

```text
cpu:CoreNum=4
disk:
priceStrategy=MIN
未输入 mouse
```

预期：

- 只返回不含 mouse 的解，或不含 mouse 的解排在最前。
- 如果产品策略选择“MIN 下只返回最优解”，则 present 分支不生成。

### AC-006: 价格最大时强制配置可选件

输入：

```text
cpu:CoreNum=8
disk:
priceStrategy=MAX
未输入 mouse
```

预期：

- 返回解必须包含 mouse。
- 只能选择 `mouse2`。

### AC-007: 黑名单对 absent 可选分类自然通过

规则：

```text
BLACK(cpu4, mouse2)
```

输入：

```text
cpu:CoreNum=4
未输入 mouse
```

预期：

- 主求解成功。
- 不因为黑名单规则强制 mouse 出现。

### AC-008: 必选分类组合规则行为不变

输入：

```text
cpu + disk
```

预期：

- `RFC-0007` 中 CPU 与硬盘白名单/黑名单测试全部保持原语义。
- 现有 `CompatibleRuleIncompatibleTest`、`CompatibleRuleRequireTest`、`CompatibleRuleCodependentTest` 回归通过。

### AC-009: validate 遵循相同 guard 语义

给定配置：

```text
cpu4 + disk1
```

预期：

- `validate` 通过，因为 mouse absent。

给定配置：

```text
cpu4 + disk1 + mouse2
```

预期：

- `validate` 失败，因为 mouse present 且未命中白名单。

### 7.2 边界条件

| 条件 | 输入 | 预期行为 |
| --- | --- | --- |
| 可选分类无可行补全 | 主解存在，但 optional present 分支无解 | 保留 absent 分支；present 分支丢弃并记录原因 |
| 多个可选分类 | mouse、keyboard 都未要求 | 按裂变上限生成分支，避免指数膨胀 |
| 可选分类被其他规则要求 | `cpu8 REQUIRES mouse` | mouse 变为 present，相关白名单生效 |
| 可选分类价格为负 | `priceStrategy=MIN` | 不得简单跳过 present 分支，需由目标函数判断 |
| 请求只有 `where` 过滤 | `mouse: where Level=2` | 暂定视为明确要求 mouse，需评审确认 |
| 请求只有 strategy | `mouse: [strategy=ASCENDING:price]` | 暂定不视为明确要求，需评审确认 |

### 7.3 回归测试

- [ ] `StructCombinationRuleTest`
- [ ] `CompatibleRuleIncompatibleTest`
- [ ] `CompatibleRuleRequireTest`
- [ ] `CompatibleRuleCodependentTest`
- [ ] `PartCategoryFilterEmptyTest`
- [ ] `SearchStrategyTest`
- [ ] `MultiPCTest`

---

## 8. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 新增 `PartCategorySelectionPolicy` 与 `PartCategory.selectionPolicy` | P0 | 待开始 |
| 2 | 扩展 `@PartAnno(required = true)`，生成器写入分类选择策略 | P0 | 待开始 |
| 3 | 增加规则激活分析器，识别规则引用的可选分类 | P0 | 待开始 |
| 4 | 调整组合/兼容规则执行，移除隐式 `ExactlyOne` 副作用 | P0 | 待开始 |
| 5 | 实现主求解规则加载策略：未要求可选分类的规则进入 augmentation 队列 | P0 | 待开始 |
| 6 | 实现 `OptionalPartAugmenter`，支持 absent/present 两类分支 | P0 | 待开始 |
| 7 | 接入价格策略：MIN/MAX/无策略下的可选分支处理 | P1 | 待开始 |
| 8 | `validate(ModuleValidateReq)` 复用相同 guard 语义 | P1 | 待开始 |
| 9 | 用 `RuleReferenceSet` 替代 P0 写死依赖，接入差量加载 | P2 | TODO |
| 10 | 评估求解器内 presence guard 方案，作为后续全局最优路径 | P2 | TODO |

---

## 9. 风险与兼容策略

### 9.1 结果数量膨胀

多个可选分类会产生组合裂变。缓解策略：

- P0 只对明确配置为可推荐的可选分类做裂变。
- 增加最大裂变数量配置。
- 每个主解每个可选分类只求一个可行补全。
- 后续支持按价值/价格排序截断。

### 9.2 主解与可选补全不是全局最优

两阶段求解可能无法保证“必选 + 可选”整体全局最优。首期接受该限制，因为业务目标是避免可选件影响主设备求解，并且辅料通常低价值。

如果后续需要严格全局最优，应转向“备选方案 A：求解器内 presence guard”。

### 9.3 规则依赖识别不完整

如果规则引用关系不准确，可能漏加载可选规则。P0 在测试数据和 `CombinationStructRuleSchema.dimensionCategoryCodes` 中明确写死维度；后续使用 `RuleReferenceSet` 统一依赖。

### 9.4 旧规则兼容

未声明 `required=false` 的分类全部视为必选，确保已有用例行为不变。

### 9.5 兼容规则职责边界变化

移除兼容规则中的隐式 `ExactlyOne` 后，原来依赖该副作用的测试可能需要补充显式选择规则。迁移策略：

- 必选单选分类通过分类 cardinality 或 `SelectRule` 表达。
- 兼容/组合规则只表达跨分类合法性。

---

## 10. 已确认决策与待确认问题

### 10.1 已确认决策

1. `PartCategory` 需要增加必选/可选标记。
2. 可选分类未被明确要求时，相关白名单规则不应阻断主求解。
3. 可选分类一旦被选择或被请求要求，相关白名单规则必须生效。
4. P0 可以先在构建数据里写死规则依赖，正式依赖关系后续由 `RuleReferenceSet` 丰富。
5. 主求解和可选件求解可以拆成两个子过程。

### 10.2 待确认问题

Q1：`mouse: where Level=2` 这种只有过滤、没有数量约束的请求，是否视为“明确要求配置 mouse”？本 RFC 暂定为是。

Q2：`mouse: [strategy=ASCENDING:price]` 只有排序策略时，是否视为“明确要求配置 mouse”？本 RFC 暂定为否。

Q3：价格 `MIN` 时，是只返回不含可选件的最优解，还是仍返回带可选件分支但排在后面？本 RFC 暂定为只返回不含可选件分支。

Q4：多个可选分类同时存在时，P0 是只支持一个可选分类裂变，还是支持多个但设置裂变上限？本 RFC 暂定支持多个并设置上限。

Q5：当某条 `REQUIRES` 规则从必选分类指向可选分类时，是否应把该可选分类提升为 present 并加载其白名单？本 RFC 暂定为是。

---

## 11. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `doc/RFC-0007-Struct-Combination-Rule-Schema.md`
- `doc/RFC-0008-Rule-Model-IR-Refactor.md`
- `src/main/java/com/jmix/executor/bmodel/PartCategory.java`
- `src/main/java/com/jmix/tool/bbuilder/anno/PartAnno.java`
- `src/main/java/com/jmix/executor/impl/algmodel/CompatibleConstraintAlg.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- [OR-Tools: Channeling constraints](https://developers.google.com/optimization/cp/channeling)
- [OR-Tools Java API: CpModel addAllowedAssignments / addForbiddenAssignments](https://or-tools.github.io/docs/javadoc/com/google/ortools/sat/CpModel.html)
- [OR-Tools CP-SAT scheduling: Optional intervals](https://github.com/google/or-tools/blob/stable/ortools/sat/docs/scheduling.md)
- [ABS Documentation: Feature Model](https://abs-models.org/manual/feature-modeling.html)
- [FeatureIDE Wiki: Feature Diagram](https://github-wiki-see.page/m/FeatureIDE/FeatureIDE/wiki/Feature-Diagram)
- [Czarnecki et al., Staged Configuration Using Feature Models](https://gsd.uwaterloo.ca/publications/view/79.html)
- [FdConfig: A Constraint-Based Interactive Product Configurator](https://arxiv.org/abs/1108.5586)
