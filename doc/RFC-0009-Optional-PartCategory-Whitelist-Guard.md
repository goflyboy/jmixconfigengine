# RFC-0009: 可选 PartCategory 白名单 Guard 语义

> 状态：草案（Draft）
> 日期：2026-05-20
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`, `doc/RFC-0005-0.1-North-South-Interface-Decoupling-Patch.md`, `doc/RFC-0007-Struct-Combination-Rule-Schema.md`, `doc/RFC-0008-Rule-Model-IR-Refactor.md`

---

## 设计决策摘要

本 RFC 单独讨论“可选 PartCategory 参与白名单/黑名单组合规则时，规则何时生效”的 guard 语义。核心目标是：定义了 CPU 与鼠标的配套关系，不应导致用户只配置 CPU 时无解；但一旦用户明确选择鼠标，CPU 与鼠标的配套规则必须生效。

| 主题 | 决策 |
| --- | --- |
| 当前代码基线 | 当前 `PartCategory` 尚无 `selectionPolicy`，`@PartAnno` 尚无 `required` 字段，`ModuleConstraintExecutor` 尚无 `validate(ModuleValidateReq)` |
| 分类必选性 | 目标设计是在 `PartCategory` 增加选择策略，首期支持 `REQUIRED` 与 `OPTIONAL` |
| 规则生效条件 | 含可选分类的组合规则必须有 guard；可选分类未被要求且未被选择时，该规则不生效 |
| 白名单语义 | 只在规则维度全部 present 时要求命中允许 tuple；可选维度 absent 时不强制配置 |
| 黑名单语义 | 禁止已选择 tuple；可选维度 absent 时天然不命中，不得因为黑名单规则强制可选分类出现 |
| 首期推荐方案 | 采用“请求驱动规则分流 + 延迟变量创建 + 可选件裂变/补全”的融合方案 |
| 变量创建策略 | `present=0` 的可选分类在当前分支不创建其子项 `Select` / `Quantity` 变量，也不加载其组合规则 |
| 备选方案 | 保留求解器内 presence guard / table constraint 作为后续全局最优演进；P0 不默认把所有可选分类放入主模型 |
| 价格策略 | `MIN` 且价格非负时不含可选件的解排在前面，但仍保留带可选件分支；`MAX` 时强制配置可选件；无价格目标时返回 absent/present 两类解 |
| 依赖关系 | P0 可复用 `RFC-0007` 的 `dimensionCategoryCodes` 或当前 `RefProgObjSchema`；`RuleReferenceSet` 是 `RFC-0008` 的目标模型，后续再接入 |
| 旧兼容规则边界 | 当前 RFC 优先修正 `RFC-0007` 组合规则的可选分类 guard 语义；`CompatibleConstraintAlg.addCompatibleConstraintInCompatible` 先保持兼容，统一执行器另行演进 |

---

## 1. 摘要

当前兼容/组合类白名单规则会把参与规则的分类当成必须参与求解的维度，导致“可选鼠标未选择”也会触发 CPU 与鼠标白名单校验，从而产生不符合业务预期的无解。

本 RFC 提议为 `PartCategory` 增加必选/可选标记，并为含可选分类的白名单规则引入 guard 语义。首期推荐采用融合方案：主求解只处理必选设备和用户明确要求的可选设备；用户未要求的可选设备在主解之后按规则进行可选补全和结果裂变。对于 `present=0` 的可选分类，当前分支不创建其子项 `Select` / `Quantity` 变量，从源头减少解空间，而不是只创建变量再依赖 guard 关闭约束。

本文已按 `RFC-0005-0.1` 后的代码基线刷新：当前产品算法入口是 `ModuleAlgBase`，CP 能力通过 `ModuleCPModel` / `AlgCP*` facade 暴露；`ConstraintAlgBase`、`ConstraintModel`、`ConstraintVarRegistry` 和直接 OR-Tools 类型不再作为南向示例使用。本 RFC 中的 `PartCategorySelectionPolicy`、`@PartAnno(required=false)`、`validate(ModuleValidateReq)` 都是目标改造项，不是当前已存在接口。

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

需要区分两类当前实现：

- 部件级兼容规则在 `CompatibleConstraintAlg` 中对左右 `PartVar` 集合调用 `model.addExactlyOne(...)`，这是可选 PartCategory 被拉成必选的直接风险点。
- 参数级兼容辅助方法在 `ModuleBaseAlgImpl` 中也会对参数选项做 exactly-one，但它主要约束枚举参数选择，不是本 RFC 的可选 PartCategory 主问题。

同时也需要区分“历史兼容规则入口”和 `RFC-0007` 新增的结构化组合规则入口。`CompatibleConstraintAlg.addCompatibleConstraintInCompatible` 与 `RFC-0007` 的 `CodependantRuleSchema` 在业务上都表达跨分类组合关系，只是前者来自旧的代码式兼容 API，后者来自结构化规则编译后的运行态 Schema。两者后续应统一到同一套组合规则执行语义，但本 RFC 的直接目标是修正 `RFC-0007` 留下的可选分类 guard 问题：首期不移除 `addCompatibleConstraintInCompatible` 的既有 exactly-one 行为，也不改原有兼容规则用例；如果 P0 实现复用旧入口，可仅补强“过滤后补集为空”等边界条件，避免 present 分支漏判。旧入口的 exactly-one 副作用作为后续兼容规则统一治理范围。

同时，当前 `PartCategory` 只有 `partType`、`supportMultiInst` 等字段；`@PartAnno` 只有 `code`、`fatherCode`、`maxQuantity`、`supportMultiInst` 等属性。也就是说，可选分类的业务语义目前还没有稳定入口。

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

当前 `ModuleConstraintExecutor` 尚未提供 `validate(ModuleValidateReq)`，所以本文涉及 validate 的内容是为了保证后续校验接口与推荐求解共享同一套 guard 语义，不表示当前接口已经可调用。

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

需要注意的是，presence literal 是清晰的语义表达，不等于 P0 必须为每个可选分类都创建一组 `present`、`Select`、`Quantity` 变量。为了避免“鼠标”这类可选辅料干扰关键物料的主求解，P0 采用延迟变量创建：主求解只为必选分类和已经进入当前分支的可选分类创建变量；未进入分支的可选分类只保留元数据和规则依赖关系。也就是说，`mouse.present=0` 的最好落地效果是鼠标分类下每个子项的 `Select` 和 `Quantity` 变量都不创建。

### 3.4 CP-SAT：白名单/黑名单可映射到 table constraint

OR-Tools Java API 提供 `addAllowedAssignments` 和 `addForbiddenAssignments`。前者表达允许 tuple 集合，后者表达禁止 tuple 集合。

这与 `RFC-0007` 的 `CodependantRuleSchema` 很贴合：

- WHITE 可映射为 allowed assignments。
- BLACK 可映射为 forbidden assignments。
- 但含可选分类时，table constraint 仍必须由 presence guard 控制，避免缺席维度触发强制选择。

注意：这是引擎内部实现候选方案，不是当前南向公开 API。`ModuleCPModel` 目前没有暴露 table constraint 方法；若未来需要把 table constraint 作为南向能力，需要先在 `com.jmix.executor.southinf.cp` 中增加稳定 facade，并由 `SouthboundLatestBridge` 适配到内部 OR-Tools。

### 3.5 调研结论

综合以上资料，可选部件规则的稳定设计应满足三条原则：

1. **变量存在不等于配置存在**：求解器里可以有 `mouse` 变量，但 `mouse.present=0` 时它不参与当前解；P0 更推荐在 absent 分支直接不创建鼠标子项变量。
2. **规则必须有激活上下文**：组合白名单只在相关维度都参与当前配置时执行。
3. **主配置与可选补全可以分阶段**：当用户未明确要求可选分类时，主求解不应被可选规则拖入无解；可选补全可以作为后置分支生成。

---

## 4. 术语与语义定义

### 4.1 PartCategory 选择策略

目标新增分类选择策略：

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

当前代码状态：

- `src/main/java/com/jmix/executor/bmodel/PartCategory.java` 尚未包含 `selectionPolicy`。
- `src/main/java/com/jmix/tool/bbuilder/anno/PartAnno.java` 尚未包含 `required`。
- 因此所有现有测试数据在语义上都应继续视为 `REQUIRED`，直到本 RFC 的模型字段和生成器映射真正落地。

### 4.2 present 语义

对每个分类定义运行态存在性：

```text
category.present = sum(part.selected for part in category.parts) >= 1
```

在 CP 模型中，`present` 可以有两种落地形态：

- **显式变量形态**：为进入当前模型的可选分类创建 `category.present` 布尔变量，并通过 channeling 约束与子项选择变量绑定。
- **分支元数据形态**：在 absent 分支不创建该分类的子项变量，只在分支上下文中记录 `category.present=false`；在 present 分支再创建该分类下可行子项的 `Select` / `Quantity` 变量。

P0 推荐优先采用分支元数据形态。这样可以避免可选分类数量增加时，主求解阶段出现大量只用于关闭约束的 `present` 变量和子项变量。

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

### 5.1 推荐方案：规则分流 + 延迟变量创建 + 可选件裂变补全

首期推荐把备选方案 A 的 guard 语义与备选方案 B 的请求驱动规则加载融合起来，但执行形态不把所有可选分类提前放进主模型：

1. 请求分析先判断哪些可选分类进入当前分支。
2. 主求解只创建必选分类和当前分支 present 的可选分类变量。
3. 含 absent 可选分类的组合规则不进入主求解硬约束。
4. 对需要展示或排序的可选分类，在主解之后创建 present 分支并运行轻量补全求解。

这套方案既保留了 guard 的一致语义，也避免未要求的可选辅料放大主求解空间。

#### 5.1.1 阶段一：必选设备主求解

主求解阶段只创建变量并加载规则：

- 不含可选分类的规则。
- 用户明确要求进入 present 分支的可选分类及其相关规则。
- 价格策略、推荐策略或 `REQUIRES` 规则明确要求 present 的可选分类及其相关规则。

如果用户未要求 `mouse`，且当前分支也不是 `presentMouse` 分支，则 `mouse` 分类下的 `mouse1`、`mouse2` 等子项不创建 `Select` / `Quantity` 变量，`cpu_mouse_white` 也不进入主求解硬约束。

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
2. 将分支上下文标记为 `mouse.present = true`。
3. 为 `mouse` 分类下通过请求过滤的候选子项创建 `Select` / `Quantity` 变量。
4. 加载 `cpu_mouse_white` 等涉及 mouse 的组合规则。
5. 只求一个可行补全，或按价格策略求一个最优补全。

这样主求解仍关注高价值/必选设备，可选件作为低价值辅料在后处理阶段补全。

#### 5.1.3 结果排序

结果返回时按策略合并排序：

| 请求策略 | 可选件处理 |
| --- | --- |
| 价格最小 `MIN` | 如果可选件价格非负，不含可选件的解排在前面；仍可返回带可选件分支但排在后面 |
| 价格最大 `MAX` | 强制 present 分支；如果补全失败则该主解不返回 |
| 无价格策略 | 返回 absent 与 present 两类分支，交给用户判断 |
| 指定 `mouse` 必须配置 | 主求解或补全阶段必须 present，不返回 absent 分支 |
| 仅有 `mouse` 过滤或排序策略 | 返回 absent 与 present 两类分支；present 分支必须满足过滤或排序策略 |

注意：如果后续支持负价格、折扣、赠品或套餐返点，`MIN` 不应简单跳过可选件，而要由价格目标函数判断。

### 5.2 备选方案 A：求解器内 presence guard

将可选分类也纳入同一个 CP-SAT 模型，为每个分类创建 `present` 布尔变量，并使用 `OnlyEnforceIf` 控制组合规则。

本方案是语义最完整的长期形态，但不作为 P0 默认执行形态。原因是可选分类数量较多时，主模型会同时引入大量 `present` 变量、子项选择变量、数量变量和 tuple 约束；如果客户没有明确要求这些可选分类，过早放入主模型可能干扰关键物料的多解选择，并增加求解成本。

内部实现伪代码：

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

若这类能力需要进入产品算法或生成式南向算法，应改写为 facade 口径，例如：

```java
AlgCPBoolVar mousePresent = model().newBoolVar("mouse_present");
PartAlgCPLinearExpr mouseSelected = model().sum4Selected("mouse", "Quantity", "");

model().addGreaterOrEqual(mouseSelected, 1).onlyEnforceIf(mousePresent);
model().addEquality(mouseSelected, 0).onlyEnforceIf(mousePresent.not());
```

当前 `ModuleCPModel` 已支持 `newBoolVar`、线性表达、`onlyEnforceIf` 等基础能力，但尚未暴露 `addAllowedAssignments` / `addForbiddenAssignments`。因此 table constraint 版本只能作为内部实现研究或后续 facade 扩展，不能写进当前产品算法验收代码。

优点：

- 语义最统一。
- 可以获得全局最优解。
- 适合未来多个可选分类互相影响的复杂场景。

缺点：

- 需要先补齐分类 cardinality、choice var、tuple table 等底层建模能力。
- 当前 `CompatibleConstraintAlg` 中 `ExactlyOne` 副作用需要重构。
- 可选分类多时，主模型变量和 tuple 数量会明显膨胀。

适用场景：

- 需要严格的全局最优，而不是“主解 + 可选补全”的阶段性最优。
- 多个可选分类之间存在强耦合，后置补全难以保证一致性。
- 已经完成组合规则执行器和旧兼容规则入口的统一治理。

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

P0 推荐方案吸收 B 的低成本规则分流能力，但不止按请求硬切：当价格策略、推荐策略、`REQUIRES` 或可选裂变分支要求某个可选分类 present 时，也必须加载该分类相关规则。

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

这是目标改造。当前 `PartCategory` / `PartAnno` / `ModuleGenneratorByAnno` 尚未实现上述字段映射；实现时必须保证未声明 `required` 的存量模型默认仍为 `REQUIRED`。

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

P0 可以先在构建测试数据时写死 `dimensionCategoryCodes`，复用 `RFC-0007` 的 `CombinationStructRuleSchema.dimensionCategoryCodes`。在当前代码基线中，也可以继续通过 `RuleSchema.getFromLeftProgObjs()` / `getToRightProgObjs()` 返回的 `RefProgObjSchema` 识别规则涉及的 `PartCategory`。后续由 `RFC-0008` 目标模型中的 `RuleReferenceSet` 统一支撑规则依赖图。

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

    if (req.requiresPresent(activation.optionalCategoryCodes())
            || req.requiredByRequiresRule(activation.optionalCategoryCodes())) {
        return LOAD_IN_MAIN_SOLVE;
    }

    if (pricePolicy == PricePolicy.MAX) {
        return LOAD_IN_MAIN_SOLVE;
    }

    return LOAD_IN_OPTIONAL_AUGMENTATION;
}
```

这里需要区分两类请求命中：

- `requiresPresent`：请求显式声明该可选分类必须配置、请求中存在具体 `PartInst` 输入、数量约束明确要求数量大于 0，或被已生效的 `REQUIRES` 规则要求。
- `mentionsOptional`：请求只有该可选分类的属性过滤、排序策略或偏好，但没有要求一定配置该分类。

`requiresPresent` 会让可选分类进入主求解或当前补全分支，并加载相关组合规则。`mentionsOptional` 不强制 present，但说明用户关心这个分类，P0 应生成 absent 与 present 两类候选：absent 分支表示不配置该可选分类；present 分支必须满足过滤或排序策略，并在结果排序中体现策略影响。

### 6.4 可选补全器

新增补全组件：

```java
public final class OptionalPartAugmenter {
    List<ModuleInst> augment(
            Module module,
            List<ModuleInst> baseSolutions,
            List<Rule> optionalRules,
            OptionalAugmentPolicy policy) {
        // 1. absent branch: 复制主解，标记可选分类未配置，不创建子项变量
        // 2. present branch: 固定主解，创建 optional category 子项变量
        // 3. 强制 optional category present，加载 optionalRules
        // 4. 运行轻量求解，合并和排序
    }
}
```

首期约束：

- 每个主解对每个可选分类只求一个可行补全。
- 不枚举同一可选分类下的所有组合。
- 多个可选分类同时存在时，默认按配置限制最大裂变数量，避免 `2^N` 爆炸。

### 6.5 组合规则职责与旧兼容规则统一

从长期语义看，兼容/组合规则不应承担“分类必须选一个”的职责。建议将旧兼容规则中的：

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

迁移时需要补齐选择职责的归属：

- 必选单选分类由显式选择规则、分类 cardinality 规则或请求输入约束表达。
- 可选分类默认允许 absent，只有请求、价格策略、`REQUIRES` 或补全阶段明确要求时才变为 present。
- 参数级兼容 helper 中的 exactly-one 属于枚举参数选项语义，本 RFC 不要求同步移除，但需要避免把它误套到可选 PartCategory 上。

不过，`CompatibleConstraintAlg.addCompatibleConstraintInCompatible` 是已有代码路径，现有 `CompatibleRuleIncompatibleTest`、`CompatibleRuleRequireTest`、`CompatibleRuleCodependentTest` 等用例可能依赖它的隐式 exactly-one 行为。为避免 RFC-0009 的落地扩大改动面，P0 不要求立即移除该方法中的 exactly-one 或重写旧兼容规则语义；本 RFC 优先要求 `RFC-0007` 的 `CodependantRuleSchema` / `CodependantConstraintExecutor` 在面对可选分类时遵守 guard 与延迟变量创建语义。若当前代码落地阶段暂时复用旧兼容入口，可允许做空过滤集、空补集这类不改变存量必选分类主语义的边界修复。

建议后续单独发起“兼容规则与结构化组合规则执行器统一”RFC 或子任务，目标包括：

- 将 `CompatibleConstraintAlg` 与 `CodependantConstraintExecutor` 的 WHITE / BLACK / REQUIRES 语义统一。
- 把分类选择 cardinality 从组合规则执行器中剥离，迁移到 `PartCategory` 选择策略或显式选择规则。
- 为旧兼容规则提供迁移开关，保证存量用例在默认 `REQUIRED` 分类下行为不变。
- 逐步让结构化组合规则和旧兼容 API 共享同一套规则依赖分析、guard 判断和错误诊断。

---

## 7. 验收准则

### 7.1 测试数据

建议新增测试：

```text
src/test/java/com/jmix/scenario/ruletest/OptionalPartCategoryWhitelistGuardTest.java
```

极简模型：

```java
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.AlgorithmApiVersion;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

@ModuleAnno(id = 9009)
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "rfc-0009-test")
public static class OptionalMouseConstraint extends ModuleAlgBase {

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

注意：上面的 `required = false` 是本 RFC 的目标注解写法，当前 `PartAnno` 尚未提供该属性；在实现前，这段代码用于表达预期 API，不是当前可直接编译的测试代码。可执行测试落地时必须继承 `ModuleAlgBase`，并用 `@AlgorithmApiVersion` 声明南向 API 版本。

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

### AC-005: 价格最小时不含可选件分支排在前面

输入：

```text
cpu:CoreNum=4
disk:
priceStrategy=MIN
未输入 mouse
```

预期：

- 不含 mouse 的解排在最前。
- 仍可返回 `cpu4 + disk1 + mouse1` 等带可选件分支，但排序应晚于不含 mouse 的最低价分支。
- 不返回违反白名单的 `cpu4 + disk1 + mouse2`。

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

说明：`validate(ModuleValidateReq)` 是目标接口。当前 `ModuleConstraintExecutor` 仅提供 `init/fini/addModule/removeModule/inferParas/postCalculate`，因此本用例用于约束后续接口设计，不是当前可执行回归。

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
| 请求只有 `where` 过滤 | `mouse: where Level=2` | 不强制 mouse present；返回 absent 与 present 两类候选，present 分支必须满足 `Level=2` |
| 请求只有 strategy | `mouse: [strategy=ASCENDING:price]` | 不强制 mouse present；返回 absent 与 present 两类候选，present 分支按策略排序 |

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
| 0 | 保持 `RFC-0005-0.1` 南向边界：新增示例和生成代码使用 `ModuleAlgBase` / `ModuleCPModel` / `AlgCP*` facade | P0 | 已建立 |
| 1 | 新增 `PartCategorySelectionPolicy` 与 `PartCategory.selectionPolicy` | P0 | 规划中 |
| 2 | 扩展 `@PartAnno(required = true)`，生成器写入分类选择策略 | P0 | 规划中 |
| 3 | 增加规则激活分析器，识别规则引用的可选分类；首期可用 `dimensionCategoryCodes` / `RefProgObjSchema` | P0 | 规划中 |
| 4 | 调整组合/兼容规则执行，移除部件级隐式 `ExactlyOne` 副作用 | P0 | 规划中 |
| 5 | 实现主求解规则加载策略：未要求可选分类的规则进入 augmentation 队列 | P0 | 规划中 |
| 6 | 实现 `OptionalPartAugmenter`，支持 absent/present 两类分支 | P0 | 规划中 |
| 7 | 接入价格策略：MIN/MAX/无策略下的可选分支处理 | P1 | 规划中 |
| 8 | `ModuleConstraintExecutor` 新增 `validate(ModuleValidateReq)`，并复用相同 guard 语义 | P1 | 规划中 |
| 9 | 用 `RFC-0008` 的 `RuleReferenceSet` 替代 P0 写死依赖，接入差量加载 | P2 | 后续演进 |
| 10 | 评估求解器内 presence guard 和 table constraint facade，作为后续全局最优路径 | P2 | 后续演进 |

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

如果规则引用关系不准确，可能漏加载可选规则。P0 在测试数据和 `CombinationStructRuleSchema.dimensionCategoryCodes` 中明确写死维度，也可以复用当前 `RefProgObjSchema` 引用列表；后续使用 `RFC-0008` 的 `RuleReferenceSet` 统一依赖。

### 9.4 旧规则兼容

当前所有分类都没有 `required=false` 入口，迁移后也必须默认视为必选，确保已有用例行为不变。

### 9.5 兼容规则职责边界变化

移除兼容规则中的隐式 `ExactlyOne` 后，原来依赖该副作用的测试可能需要补充显式选择规则。迁移策略：

- 必选单选分类通过分类 cardinality 或 `SelectRule` 表达。
- 兼容/组合规则只表达跨分类合法性。

---

## 10. 已确认决策

### 10.1 已确认决策

1. `PartCategory` 需要增加必选/可选标记。
2. 可选分类未被明确要求时，相关白名单规则不应阻断主求解。
3. 可选分类一旦被选择或被请求要求，相关白名单规则必须生效。
4. P0 可以先在构建数据里写死规则依赖，或复用当前 `RefProgObjSchema`；正式依赖关系后续由 `RFC-0008` 的 `RuleReferenceSet` 丰富。
5. 主求解和可选件求解拆成两个子过程；P0 不把未要求的可选分类默认放入主模型。
6. `present=0` 的可选分类在当前分支不创建其子项 `Select` / `Quantity` 变量。
7. `mouse: where Level=2` 这类只有过滤、没有数量约束的请求，不等同于“必须配置 mouse”；但它表示用户关心 mouse，应返回“不含 mouse”和“含 mouse 且满足 Level=2”两类候选。
8. `mouse: [strategy=ASCENDING:price]` 这类只有排序策略的请求，也不等同于“必须配置 mouse”；应返回“不含 mouse”和“含 mouse”两类候选，含 mouse 分支按策略排序。
9. 价格 `MIN` 时，不含可选件的分支排在前面，但仍可返回带可选件分支并排在后面。
10. 多个可选分类同时存在时，P0 支持多个可选分类裂变，但必须设置裂变上限。
11. 当某条 `REQUIRES` 规则从必选分类指向可选分类时，应把该可选分类提升为 present，并加载其白名单/黑名单等相关组合规则。
12. 本 RFC 的代码示例必须遵守 `RFC-0005-0.1` 后的南向边界：产品算法继承 `ModuleAlgBase`，不使用旧 `ConstraintAlgBase`。
13. `CompatibleConstraintAlg.addCompatibleConstraintInCompatible` 与 `RFC-0007` 的 `CodependantRuleSchema` 语义需要后续统一；P0 先不移除旧兼容规则入口的 exactly-one，也不改既有用例，但可以补强空集合等边界行为。

---

## 11. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `doc/RFC-0005-0.1-North-South-Interface-Decoupling-Patch.md`
- `doc/RFC-0007-Struct-Combination-Rule-Schema.md`
- `doc/RFC-0008-Rule-Model-IR-Refactor.md`
- `src/main/java/com/jmix/executor/ModuleConstraintExecutor.java`
- `src/main/java/com/jmix/executor/bmodel/PartCategory.java`
- `src/main/java/com/jmix/executor/bmodel/logic/Rule.java`
- `src/main/java/com/jmix/executor/bmodel/logic/RuleSchema.java`
- `src/main/java/com/jmix/executor/bmodel/logic/RefProgObjSchema.java`
- `src/main/java/com/jmix/tool/bbuilder/anno/PartAnno.java`
- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java`
- `src/main/java/com/jmix/executor/southinf/ModuleCPModel.java`
- `src/main/java/com/jmix/executor/southinf/cp/AlgCPModel.java`
- `src/main/java/com/jmix/executor/southinf/var/PartVar.java`
- `src/main/java/com/jmix/executor/impl/algmodel/CompatibleConstraintAlg.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundLatestBridge.java`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundModuleAlgAdapter.java`
- `src/test/java/com/jmix/scenario/ruletest/SouthboundApiDecouplingTest.java`
- `src/test/java/com/jmix/scenario/ruletest/PostCalcRuleTest.java`
- [OR-Tools: Channeling constraints](https://developers.google.com/optimization/cp/channeling)
- [OR-Tools Java API: CpModel addAllowedAssignments / addForbiddenAssignments](https://or-tools.github.io/docs/javadoc/com/google/ortools/sat/CpModel.html)
- [OR-Tools CP-SAT scheduling: Optional intervals](https://github.com/google/or-tools/blob/stable/ortools/sat/docs/scheduling.md)
- [ABS Documentation: Feature Model](https://abs-models.org/manual/feature-modeling.html)
- [FeatureIDE Wiki: Feature Diagram](https://github-wiki-see.page/m/FeatureIDE/FeatureIDE/wiki/Feature-Diagram)
- [Czarnecki et al., Staged Configuration Using Feature Models](https://gsd.uwaterloo.ca/publications/view/79.html)
- [FdConfig: A Constraint-Based Interactive Module Configurator](https://arxiv.org/abs/1108.5586)
