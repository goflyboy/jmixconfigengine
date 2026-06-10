# RFC: 冲突诊断与松弛可行解

> 状态: Draft v3
> 创建日期: 2026-04-30
> 审阅: 基于 v1 的全面修订
> 相关文档: `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`
> 涉及代码:
> - `com.jmix.executor.impl.ModuleConstraintExecutorImpl` (L280-L443)
> - `com.jmix.executor.impl.ModuleBaseConstraintExecutorImpl`
> - `com.jmix.executor.impl.algmodel.RelaxVar.java`
> - `com.jmix.executor.impl.algmodel.AlgCPModel.java` (L148-L239)
> - `com.jmix.executor.impl.algmodel.AlgCPConstraint.java`
> - `com.jmix.executor.impl.algmodel.ModuleBaseAlgImpl.java` (L260-L278, L190, L301, L979, L999)
> - `com.jmix.executor.model.ConstraintConfig.java` (`debugByRelaxVar` 字段)

---

## 摘要

本 RFC 描述如何修复并完善 JMix Config Engine 中基于松弛变量（RelaxVar）的冲突诊断能力。该能力已在代码中部分实现但长期未调试，本 RFC 的目标是：

1. 审计现有松弛链路，定位断点
2. 修复二轮冲突检测的状态处理
3. 补齐缺失的约束类型对松弛的支持
4. 编写满足 ACCEPTANCE.md 标准的回归测试

完成后，当 `ConstraintConfig.debugByRelaxVar=true` 且求解结果为 INFEASIBLE 时，引擎将自动进入诊断流程，返回冲突规则列表和一个松弛后的可行解。

---

## 动机

### 用户场景

产品配置中，用户先给出部分输入条件，再由求解器补全剩余参数。当所有硬约束共同导致无解时，仅返回"无可行解"对用户价值有限。用户需要知道：

1. **哪些规则或输入条件导致了无解？**（如"ruleA 与 ruleB 冲突"）
2. **如果暂时放宽这些约束，可行的配置是什么？**（松弛后可行解）

### 为什么现在修复

- CORE-DESIGN.md 已将"冲突诊断"列为五项核心能力之一
- 代码中已有完整的 `RelaxVar` / `AlgCPConstraint` 基础设施，但链路中存在已知断点
- 现有测试 `ConfictDebugTest` 和 `ConfictDebugIfThenTest` 的状态未知，需要验证和补全
- 新增的功能（如决策策略、多实例分类）可能与松弛链路存在交互问题

---

## 当前代码现状

### 已实现的组件

| 组件 | 文件 | 状态 | 说明 |
|------|------|------|------|
| `RelaxVar` 数据类 | `RelaxVar.java` | ✅ 完成 | 权重常量 + 名称/规则码/布尔变量/权重四字段 |
| `AlgCPConstraint` 包装类 | `AlgCPConstraint.java` | ✅ 完成 | 构造时自动附加 `onlyEnforceIf(relaxVar.not())` |
| `AlgCPModel` 松弛基础设施 | `AlgCPModel.java` | ✅ 完成 | `setCurrentRelaxVarName()`, `attachRelax()`, `setConfictedRelaxVars()` |
| 松弛目标函数构建 | `ModuleBaseAlgImpl.addRelaxObjectFunction()` | ✅ 完成 | `minimize(sum(relaxVar[i] * weight[i]))` |
| 自定义规则松弛 | `ModuleBaseAlgImpl.executeRuleMethod()` L301 | ✅ 完成 | 每条规则前调用 `setRelax4CustomRule()` |
| 输入约束松弛 | `ModuleBaseAlgImpl.addPartEquality()` L979 / `addParaEquality()` L999 | ✅ 完成 | 调用 `setRelax4SysRule()` |
| 默认可见性松弛 | `ModuleBaseAlgImpl.setDefaultVisibilityConstraints()` L190 | ✅ 完成 | 调用 `setRelax4SysRule("hiddensrule")` |
| 冲突诊断入口 | `ModuleConstraintExecutorImpl.inferParasOld()` L317-L337 | ⚠️ 需验证 | 二轮求解逻辑已实现，但路径可能未覆盖新场景 |
| `runCalcConfictRules()` | `ModuleConstraintExecutorImpl` L430 | ⚠️ 需验证 | 依赖 `runInferParas` 正确传递 `isAttachRelax` |
| `calcConfictRules()` | `ModuleConstraintExecutorImpl` L402 | ⚠️ 需验证 | 依赖 `solver.booleanValue()` 返回正确值 |
| 新路径公共求解基类 | `ModuleBaseConstraintExecutorImpl` | ⚠️ 需扩展 | `invokerSolver()` 当前固定 `setIsAttachRelax(false)`，不会触发冲突诊断 |

### 已知问题 / 待验证点

1. **`inferParas()` (新路径) vs `inferParasOld()` (旧路径)**: 冲突诊断仅在 `inferParasOld()` 中实现。新路径 `inferParas()` 在存在 `PartConstraintReq` 时会委派给 `ModuleBaseConstraintExecutorImpl.doProcess()` 及其子类实现，最终通过 `ModuleBaseConstraintExecutorImpl.invokerSolver()` 求解。该基类当前固定 `optModel.setIsAttachRelax(false)`，且不处理 `INFEASIBLE && debugByRelaxVar` 的二次诊断流程，因此新路径不会产出冲突规则和松弛可行解。需确认两条路径的关系和长期计划。

2. **INFEASIBLE + debugByRelaxVar=false 的返回值**: 当前代码落到了 L340-343，返回 `Result.success(solutions)` 且 solutions 为空列表。应该返回 `Result.noSolution()` 以明确语义。

3. **松弛后模型仍 INFEASIBLE**: 如果存在不可松弛的硬约束（如 `maxQuantity=1` 在变量域层面），诊断模型本身也可能无解。当前 `runCalcConfictRules()` 在 `isFailed(status)` 时抛异常，但没有专门处理 `INFEASIBLE` 状态。

4. **OR-Tools 求解器在松弛模型上的 boolValue() 行为**: `calcConfictRules()` 依赖 `result.getSolver().booleanValue(relaxVar.getValue())` 读取松弛变量值。需验证在求解状态为 OPTIMAL 时这个值可靠。

5. **条件规则(If-Then)的多条内部约束**: 如果一条 CodeRule 方法内生成了多条 CP-SAT 约束，它们共享同一个 `relaxVar`（因为 `setRelax4CustomRule` 在整个方法执行前只调用一次）。需验证这是否符合预期——放松整条业务规则，而非单独的 CP-SAT 约束。

6. **与 PriorityRule 的交互**: `addRelaxObjectFunction()` 调用 `model.minimize()`，需确认这不会与之前设置的业务优化目标冲突。OR-Tools 的 `minimize()` 会**替换**之前的优化目标。

### 现有测试文件

有两个已存在的测试文件：
- `src/test/java/com/jmix/tool/confictdebug/ConfictDebugTest.java` — 基础线性约束冲突测试（2个用例）
- `src/test/java/com/jmix/tool/confictdebugifthen/ConfictDebugIfThenTest.java` — If-Then 条件约束冲突测试（2个用例）

这些测试当前编译状态未知，需在本 RFC 实施中首先验证。

---

## Guide-level 说明

### 用户视角

`ConstraintConfig.debugByRelaxVar` 控制行为：

| debugByRelaxVar | 原始模型结果 | 返回 |
|---|---|---|
| `false` (默认) | FEASIBLE/OPTIMAL | `Result.success(solutions)` |
| `false` (默认) | INFEASIBLE | `Result.noSolution()` (见下方兼容性说明) |
| `true` | FEASIBLE/OPTIMAL | `Result.success(solutions)` — 不进入诊断 |
| `true` | INFEASIBLE | 进入诊断 → `Result.noSolution(message).setData(relaxedSolutions)` |
| `true` | INFEASIBLE (诊断也失败) | `Result.failed("relaxed model is still infeasible")` |

`NO_SOLUTION + data 非空` 的语义：**严格满足所有约束无解，但存在一个在最小松弛后的可行解**。调用方应将其作为诊断信息，不可等同于 `SUCCESS` 的合法配置。

### 返回格式

第一阶段保持兼容：

```java
Result<List<ModuleInst>> r = Result.noSolution(conflictMessage);
r.setData(relaxedSolutions);
// conflictMessage 示例: "conflict rules: rule3,addPartEquality_x_1"
```

第二阶段（后续 RFC）可新增结构化诊断对象：

```java
public class ConflictDiagnosis {
    private List<ConflictRule> conflictRules;   // 冲突规则列表
    private List<ModuleInst> relaxedSolutions;  // 松弛后的可行解
    private String explanation;                  // 人类可读解释
}
```

本 RFC **不阻塞**结构化返回对象的实现。

### 配置

```java
ConstraintConfig config = new ConstraintConfig();
config.setDebugByRelaxVar(true);  // 默认 false
```

---

## Reference-level 设计

### 整体流程

```
inferParas(req)
  │
  ├─► build original model (isAttachRelax=false)
  ├─► solve original model
  │
  ├─► if FEASIBLE/OPTIMAL:
  │     return SUCCESS(solutions)
  │
  ├─► if INFEASIBLE && debugByRelaxVar=false:
  │     return NO_SOLUTION (无 data)
  │
  └─► if INFEASIBLE && debugByRelaxVar=true:
        │
        ├─► pass 1: build relaxed model (isAttachRelax=true, confictedRelaxs=[])
        │     └─► solve → minimize(sum(relaxVar * weight))
        │           → collect relax vars with value=true → confictedRelaxs_1
        │
        ├─► pass 2: rebuild relaxed model (isAttachRelax=true, confictedRelaxs=confictedRelaxs_1)
        │     └─► 已识别的冲突规则权重 +WEIGHT_ADDER(1000)
        │           → solve → collect additional conflicts → confictedRelaxs_2
        │
        ├─► if relaxed model is INFEASIBLE:
        │     return FAILED("relaxed model is still infeasible")
        │
        └─► return NO_SOLUTION(toConflictMessage(confictedRelaxs_1 ∪ confictedRelaxs_2))
              .setData(solutions_from_pass_2)
```

### 为什么需要二轮求解

第一轮：求解器找到使模型可行所需的最小权重松弛集合（例如松弛 `ruleA`）。
第二轮：将 `ruleA` 的权重 +1000，求解器被迫寻找不包含 `ruleA` 的替代松弛方案（例如松弛 `ruleB` + `ruleC`）。

两轮结果的并集给用户提供更完整的冲突图景。如果第二轮发现与第一轮相同的冲突，说明该冲突是唯一的可行松弛路径。

### 松弛变量在 CP-SAT 中的语义

`AlgCPConstraint` 构造时：

```java
// AlgCPConstraint.java L84-L86
if (relaxationVar != null && cpConstraint != null) {
    cpConstraint.onlyEnforceIf(relaxationVar.not());
}
```

- `relaxVar = false` → `relaxVar.not() = true` → 约束**生效**
- `relaxVar = true` → `relaxVar.not() = false` → 约束**被放松**

目标函数 `minimize(sum(relaxVar[i] * weight[i]))` 驱动求解器找到权重最小的可放松约束集合。

### 条件约束(If-Then)的特殊处理

If-Then 规则在 CodeRule 方法内生成了多条 CP-SAT 约束。由于 `setRelax4CustomRule(rule.getCode())` 在整个方法执行前调用一次，方法内所有约束共享同一个 `relaxVar`：

```java
// 示例: if(x > 10) then y > 7
// 整条规则共享一个 relaxVar: "relax_rule1"
BoolVar cond = model.newBoolVar("cond");
model.addGreaterThan(x.qty, 10).onlyEnforceIf(cond);           // 约束1，共享relax_rule1
model.addLessOrEqual(x.qty, 10).onlyEnforceIf(cond.not());     // 约束2，共享relax_rule1
model.addGreaterThan(y.qty, 7).onlyEnforceIf(cond);           // 约束3，共享relax_rule1
```

OR-Tools 对同一条 Constraint 的多次 `onlyEnforceIf()` 调用取 AND 语义。所以约束3最终的条件是：`cond=true AND relaxVar=false`。这意味着：
- 条件规则被"整体放松"——条件成立时结论也不再强制执行
- 这个行为是**正确的**：如果仅仅放松结论约束而保留条件约束（或反过来），规则语义会畸变

### 各约束类型的松弛覆盖

| 约束来源 | 松弛方式 | 权重 | 代码位置 |
|----------|----------|------|----------|
| 自定义规则 (`CodeRuleAnno`) | `setRelax4CustomRule(ruleCode)` | 25 | `ModuleBaseAlgImpl.executeRuleMethod()` L301 |
| 系统规则-隐藏部件 | `setRelax4SysRule("hiddensrule")` | 1 | `ModuleBaseAlgImpl.setDefaultVisibilityConstraints()` L190 |
| 用户输入-部件数量 | `setRelax4SysRule("addPartEquality_...")` | 1 | `ModuleBaseAlgImpl.addPartEquality()` L979 |
| 用户输入-参数值 | `setRelax4SysRule("addParaEquality_...")` | 1 | `ModuleBaseAlgImpl.addParaEquality()` L999 |
| `CompatiableRuleAnno` | 走 `executeRuleMethod()` → `setRelax4CustomRule` | 25 | 同上 |
| `PriorityRuleAnno` | 作为自定义规则处理 | 25 | 同上 |

**注意事项**:
- 系统规则权重(1) < 自定义规则权重(25)，意味着求解器会**优先**放松系统/输入约束。这符合"输入条件可能有问题"的设计意图，但如果需要反转（优先怀疑业务规则），需要调整权重或引入配置项。
- `sumFunConstraint()` 生成的部件分类输入约束需验证是否通过 `addPartEquality()` 路径（因而自动获得松弛支持），还是走独立路径。

### Executor 层路径对齐

当前 Executor 层存在两条主要求解路径：

| 路径 | 触发条件 | 核心类 | 当前诊断状态 |
|------|----------|--------|--------------|
| 旧参数推理路径 | `InferParasReq.partConstraintReqs` 为空 | `ModuleConstraintExecutorImpl.inferParasOld()` | 已有 `debugByRelaxVar` 诊断入口 |
| 新部件约束路径 | `InferParasReq.partConstraintReqs` 非空，或直接处理 `InferPartCategoryReq` | `ModuleConstraintExecutorImpl.processModule()` → `ModuleBaseConstraintExecutorImpl` | 未接入松弛诊断 |

`ModuleBaseConstraintExecutorImpl` 的职责不是创建松弛变量本身，而是承载新路径的公共求解过程：

- `solveWithOutPriorityConstraints()` 和 `solveWithPriorityConstraints()` 负责普通求解和优先级分层求解。
- `invokerSolver()` 创建 `AlgCPModel`、初始化 `ModuleAlgImpl`、配置 `CpSolver` 并返回 `SolverResult`。
- 当前 `invokerSolver()` 中固定 `optModel.setIsAttachRelax(false)`，即便全局 `config.isDebugByRelaxVar()` 为 true，也不会构造松弛模型。
- 当前 `SolverResult` 只承载普通求解输出；当状态为 `INFEASIBLE` 时，没有等价于 `runCalcConfictRules()` 的二轮诊断流程。

因此，本 RFC 的 P0 实施范围应明确为：优先修复 `inferParasOld()` 已有诊断链路；`ModuleBaseConstraintExecutorImpl` 的诊断接入作为 P2 工程改进，除非产品要求 `PartConstraintReq` 路径在本轮同时支持冲突诊断。

若后续扩展到新路径，建议不要复制 `inferParasOld()` 中的整段诊断逻辑，而是抽取公共诊断服务，例如：

```java
class RelaxConflictDiagnoser {
    DiagnosisResult diagnose(Module module, ModuleInput input, SolverModelFactory factory);
}
```

这样 `ModuleConstraintExecutorImpl.runInferParas()` 和 `ModuleBaseConstraintExecutorImpl.invokerSolver()` 可以共用同一套松弛建模、二轮求解、冲突收集和返回转换逻辑。

### 不可松弛的硬约束

以下约束**不**被松弛：
- 变量域定义（`newIntVar(0, maxQty)` 的上下界）
- 结构性约束（部件选中与数量的基本关系）
- 直接通过 OR-Tools `CpModel` API 添加而未经过 `AlgCPModel` 包装的约束

如果硬约束本身导致无解（如 `maxQuantity=1` 且用户输入 `qty=5`），松弛诊断模型仍为 INFEASIBLE，返回 `Result.failed("relaxed model is still infeasible")`。

---

## 验收与测试要求

所有测试用例遵循 `doc/ACCEPTANCE.md` 的原则：**数据极简、用例独立、每个用例只测一个功能点**。

### 测试文件清单

| 文件 | 用途 | 状态 |
|------|------|------|
| `ConfictDebugTest.java` | 基础线性规则冲突 | 已有，需验证和补全 |
| `ConfictDebugIfThenTest.java` | If-Then + 多规则组合冲突 | 已有，需验证和补全 |
| `ConflictDiagnosisHardTest.java` | 硬约束冲突(诊断也失败) | **新增** |
| `ConflictDiagnosisParaTest.java` | 参数输入条件冲突 | **新增** |

### CONFLICT-001: 基础线性规则冲突

**目的**: 验证 `debugByRelaxVar=true` 时，单条业务规则冲突被定位。

**模型定义**:

```java
@ModuleAnno(id = 123L)
static public class ConflictDebugConstraint extends ConstraintAlgImplTestBase {
    @PartAnno(maxQuantity = 50)
    private PartVar x;

    @PartAnno(maxQuantity = 50)
    private PartVar y;

    // rule1: x.qty + y.qty < 20
    @CodeRuleAnno(normalNaturalCode = "x.qty + y.qty < 20")
    private void rule1() {
        AlgCPLinearExpr sumXY = model.newLinearExpr("sum_x_y");
        sumXY.addTerm(x.qty, 1);
        sumXY.addTerm(y.qty, 1);
        model.addLessThan(sumXY, 20);
    }

    // rule2: x.qty - y.qty > 10
    @CodeRuleAnno(normalNaturalCode = "x.qty - y.qty > 10")
    private void rule2() {
        AlgCPLinearExpr diffXY = AlgCPLinearExpr.weightedSum(
            new IntVar[]{x.qty, y.qty}, new long[]{1, -1});
        model.addGreaterThan(diffXY, 10);
    }

    // rule3: y.qty > 45 — 与 rule1 冲突 (y>45 且 x+y<20 且 x-y>10 不可同时满足)
    @CodeRuleAnno(normalNaturalCode = "y.qty > 45")
    private void rule3() {
        model.addGreaterThan(y.qty, 45);
    }
}
```

**测试用例**:

| ID | 配置 | 输入 | 预期 |
|----|------|------|------|
| CONFLICT-001-1 | `debugByRelaxVar=false` | (无额外输入) | `assertSuccess`，solutionSize=0 |
| CONFLICT-001-2 | `debugByRelaxVar=true` | (无额外输入) | `assertNoSolution`，message 包含 `rule3`，data 非空 |
| CONFLICT-001-3 | `debugByRelaxVar=true` | (无) | data 中解的 `y.qty ≤ 45` 被松弛但其他规则满足 |

**断言**:

```java
// CONFLICT-001-1
resultAssert().assertSuccess().assertSolutionSizeEqual(0);

// CONFLICT-001-2
resultAssert().assertNoSolution().assertMessageContains("rule3");
assertNotNull(result.getData());
assertFalse(result.getData().isEmpty());

// CONFLICT-001-3: 验证松弛解满足未松弛规则
ModuleInst relaxed = result.getData().get(0);
PartInst xInst = relaxed.getPartByCode("x");
PartInst yInst = relaxed.getPartByCode("y");
assertTrue(xInst.getQuantity() + yInst.getQuantity() < 20);     // rule1 未松弛
assertTrue(xInst.getQuantity() - yInst.getQuantity() > 10);      // rule2 未松弛
// rule3 被松弛，不检查 y.qty > 45
```

### CONFLICT-002: 多规则组合冲突

**目的**: 验证当多组规则共同导致无解时，能返回多个冲突规则。

**模型定义**（在 CONFLICT-001 基础上增加）:

```java
// rule31: y.qty > 48 — 与 rule1 的高端冲突
@CodeRuleAnno(normalNaturalCode = "y.qty > 48")
private void rule31() {
    model.addGreaterThan(y.qty, 48);
}

// rule51: y.qty < 10 — 与 rule2 (x-y>10) 和 y>45 都冲突
@CodeRuleAnno(normalNaturalCode = "y.qty < 10")
private void rule51() {
    model.addLessThan(y.qty, 10);
}
```

**测试用例**:

| ID | 配置 | 预期 |
|----|------|------|
| CONFLICT-002-1 | `debugByRelaxVar=true` | message 包含 `rule3` 或 `rule31`（高下界冲突），且包含 `rule51`（低下界冲突），data 非空 |
| CONFLICT-002-2 | `debugByRelaxVar=true` | 二轮求解后 confictedRelaxs 总数 ≥ 2 |

> **注意**: 具体哪些规则被松弛取决于权重和 CP-SAT 的求解路径。测试应验证冲突规则的**类别**（上界/下界）而非精确的规则名。由于所有自定义规则权重相同(25)，求解器会选择使目标函数最小化的组合。

### CONFLICT-003: If-Then 条件规则冲突

**目的**: 验证 `onlyEnforceIf(condition)` 与 `onlyEnforceIf(relaxVar.not())` 的 AND 语义正确。

**模型定义**:

```java
@ModuleAnno(id = 123L)
static public class ConflictDebugIfThenConstraint extends ConstraintAlgImplTestBase {
    @PartAnno(maxQuantity = 50)
    private PartVar x;
    @PartAnno(maxQuantity = 50)
    private PartVar y;
    @PartAnno(maxQuantity = 50)
    private PartVar z;

    // rule1: if x.qty > 10 then y.qty > 7
    @CodeRuleAnno(normalNaturalCode = "if(x.qty > 10) then y.qty > 7")
    private void rule1() {
        BoolVar cond = model.newBoolVar("rule1_cond");
        model.addGreaterThan(x.qty, 10).onlyEnforceIf(cond);
        model.addLessOrEqual(x.qty, 10).onlyEnforceIf(cond.not());
        model.addGreaterThan(y.qty, 7).onlyEnforceIf(cond);
    }

    // rule2: x.qty > 20 — 触发 rule1 的条件
    @CodeRuleAnno(normalNaturalCode = "x.qty > 20")
    private void rule2() {
        model.addGreaterThan(x.qty, 20);
    }

    // rule3: y.qty < 5 — 与 rule1 的结论冲突
    @CodeRuleAnno(normalNaturalCode = "y.qty < 5")
    private void rule3() {
        model.addLessThan(y.qty, 5);
    }
}
```

**测试用例**:

| ID | 配置 | 预期 |
|----|------|------|
| CONFLICT-003-1 | `debugByRelaxVar=false` | assertSuccess, solutionSize=0 |
| CONFLICT-003-2 | `debugByRelaxVar=true` | assertNoSolution, message 包含冲突规则, data 非空 |
| CONFLICT-003-3 | `debugByRelaxVar=true` | 松弛解中：如果 rule3 被松弛则 y.qty 可 ≥ 5；如果 rule1 被松弛则整条 If-Then 被放松 |

**重点验证**: 当 `rule1` 被松弛后，**所有**来自 rule1 的约束（条件和结论）同时失效。not 出现"条件仍然有效但结论被放松"的半松弛状态。

### CONFLICT-004: 参数输入与业务规则冲突

**目的**: 验证用户参数输入也能作为诊断对象。

**模型定义**:

```java
@ModuleAnno(id = 123L)
static public class ConflictDiagnosisParaConstraint extends ConstraintAlgImplTestBase {
    @ParaAnno(options = {"red", "blue"})
    private ParaVar color;

    @ParaAnno(options = {"small", "large"})
    private ParaVar size;

    // 业务规则: color=red → size=large
    @CompatRuleAnno(type = RuleType.REQUIRES)
    private void rule1() {
        requires("color:red", "size:large");
    }
}
```

**测试用例**:

| ID | 输入 | 预期 |
|----|------|------|
| CONFLICT-004-1 | `color=red, size=small` (debugByRelaxVar=false) | assertSuccess, solutionSize=0 |
| CONFLICT-004-2 | `color=red, size=small` (debugByRelaxVar=true) | assertNoSolution, message 包含冲突规则, data 非空 |

### CONFLICT-005: 硬约束冲突（诊断也失败）

**目的**: 验证当不可松弛的硬约束导致无解时，系统给出明确失败信息而非误导性可行解。

**模型定义**:

```java
@ModuleAnno(id = 123L)
static public class HardConflictConstraint extends ConstraintAlgImplTestBase {
    @PartAnno(maxQuantity = 1)  // 硬约束: 最多选1个
    private PartVar x;

    // 用户输入: qty = 5 (通过 addPartEquality 添加为可松弛的系统规则)
    // 但如果 x.qty > 1 的约束来自变量域 (newIntVar(0, maxQuantity))，
    // 则无法被松弛，诊断模型仍为 INFEASIBLE
}
```

**测试用例**:

| ID | 配置 | 输入 | 预期 |
|----|------|------|------|
| CONFLICT-005-1 | `debugByRelaxVar=true` | `x.qty=5` | assertFailed, message 包含 "infeasible" |
| CONFLICT-005-2 | `debugByRelaxVar=true` | `x.qty=5` | data 为 null 或空（无误导性解） |

### 回归测试套件

```java
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // 已有
    ConfictDebugTest.class,
    ConfictDebugIfThenTest.class,
    // 新增
    ConflictDiagnosisHardTest.class,
    ConflictDiagnosisParaTest.class,
})
public class ConflictDiagnosisRegressionSuite {
}
```

执行 `mvn test` 应全部通过。

---

## 实现计划

### P0: 修复核心链路

| 步骤 | 内容 | 验证方式 |
|------|------|----------|
| P0-1 | 确认 `ConfictDebugTest` 和 `ConfictDebugIfThenTest` 当前编译和执行状态 | `mvn test -Dtest=ConfictDebugTest` |
| P0-2 | 修复 `inferParasOld()` L340-343: INFEASIBLE + debug=false 时返回 `Result.noSolution()` 而非 `Result.success(empty)` | 单元测试 |
| P0-3 | 修复松弛模型 INFEASIBLE 时的错误处理: `runCalcConfictRules()` 中增加 INFEASIBLE 的专门处理 | 单元测试 |
| P0-4 | 验证 `addRelaxObjectFunction()` 的 `minimize()` 不会与 PriorityRule 目标冲突 | 集成测试 |
| P0-5 | 验证 `calcConfictRules()` 中 `solver.booleanValue()` 读取值的正确性 | 集成测试 |

### P1: 补齐测试覆盖

| 步骤 | 内容 | 验证方式 |
|------|------|----------|
| P1-1 | 补全 CONFLICT-001～CONFLICT-003 的测试断言（验证松弛解满足未松弛约束） | 自动化测试 |
| P1-2 | 新增 CONFLICT-004 (参数冲突) | 自动化测试 |
| P1-3 | 新增 CONFLICT-005 (硬约束冲突) | 自动化测试 |
| P1-4 | 验证所有已有测试（包括非冲突诊断的测试）不被破坏 | `mvn test` 全量 |

### P2: 工程改进（后续 RFC）

| 步骤 | 内容 |
|------|------|
| P2-1 | 增加结构化诊断返回对象 `ConflictDiagnosis` |
| P2-2 | 统一 `confict` → `conflict` 拼写，包含类名、方法名、变量名、包名和测试路径 |
| P2-3 | 增加诊断结果的自然语言解释 |
| P2-4 | 将诊断能力扩展到 `inferParas()` 新路径 |

---

## 技术债务

### `confict` 拼写兼容

代码库中已有一批历史命名使用了 `confict`（缺少字母 `l`），包括：

- 测试类: `ConfictDebugTest`, `ConfictDebugIfThenTest`
- 方法名: `calcConfictRules`, `runCalcConfictRules`
- 变量名: `confictedRelaxs`
- 包名: `com.jmix.tool.confictdebug`, `com.jmix.tool.confictdebugifthen`

本 RFC 在概念、文档和新增 API 命名中统一使用正确拼写 `conflict`。对既有代码引用保持原名，以便读者能直接定位当前实现。例如在描述当前方法时仍写 `runCalcConfictRules()`，但新建类、测试包和结构化诊断对象应使用 `ConflictDiagnosis`、`ConflictRule` 等正确拼写。

重命名不纳入本 RFC 的 P0 修复范围，原因是它会影响包路径、测试类名、IDE 引用和可能存在的外部脚本。建议作为 P2 独立重构处理，步骤如下：

1. 新增正确拼写的类名、方法名或包名，并保留旧入口的兼容转发。
2. 更新内部引用和测试路径。
3. 运行全量 `mvn test`。
4. 若确认没有外部依赖，再删除旧拼写入口。

### `ModuleBaseConstraintExecutorImpl` 诊断缺口

`ModuleBaseConstraintExecutorImpl` 是新部件约束路径和优先级求解路径的公共基类，但目前它只执行普通求解，不执行松弛诊断。该缺口意味着：

- `PartConstraintReq` 相关无解场景可能无法返回冲突规则。
- `processModule()` 路径即使开启 `debugByRelaxVar`，也可能只得到普通 `SolverResult`。
- 优先级分层求解与松弛目标函数的关系尚未统一。

本 RFC 建议将该缺口作为 P2 独立扩展，前提是 P0 先稳定旧路径的冲突诊断行为。

## Drawbacks

- **模型规模**: N 条可诊断规则对应 N 个 BoolVar，增加约 10% 变量数和约束数
- **额外求解**: 诊断流程至少增加一次求解（典型是两次），总耗时约 2x
- **权重敏感性**: 冲突集不唯一时，"最小松弛"结果受权重影响，不是逻辑最小冲突集
- **语义风险**: `NO_SOLUTION + data 非空` 的语义需调用方理解，否则可能误用

## Alternatives

| 方案 | 优点 | 缺点 | 选择 |
|------|------|------|------|
| 松弛变量 (RelaxVar) | 一次求解得到最小冲突+可行解；与现有代码一致 | 模型规模增加；权重影响结果 | **采纳**（主线） |
| CP-SAT assumptions/unsat core | 标准 API；无额外变量 | 不直接给出松弛后可行解；Java 封装不足 | 备选 |
| 逐条禁用重试 | 实现简单 | O(N) 次求解；无法处理多规则组合冲突 | 不推荐 |
| 只返回冲突不返回可行解 | 实现最简单 | 不满足用户原始需求 | 不推荐 |

当前选择松弛变量方案的主要理由：代码已有完整基础设施，修复成本低；同时满足"定位冲突"和"松弛可行解"两个需求。

---

## 向后兼容性

- `ConstraintConfig.debugByRelaxVar` 默认 `false`，默认行为完全不变
- `ModuleConstraintExecutor` 接口不变
- `Result` 结构不变（`setData()` 承载松弛解是已有能力）
- 已有非冲突诊断测试不受影响

唯一的语义变更：`debugByRelaxVar=false` 且 INFEASIBLE 时，从 `Result.success(emptyList)` 改为 `Result.noSolution()`。调用方若依赖于检查 `result.getData().isEmpty()` 来判断无解，需改为检查 `result.getCode() == Result.NO_SOLUTION`。

---

## 未解决问题

按优先级排列：

### 阻塞性问题（需在实现前确认）

**Q1** (P0): 用户输入约束是否允许被松弛？
- 当前权重设计下，输入约束权重(1) < 业务规则权重(25)，求解器会优先松弛输入约束
- 如果输入约束允许被松弛 → 维持当前设计
- 如果不允许 → 将输入约束的 `setRelax4SysRule` 调用移除，或提高其权重至 1000+
- **建议**: 允许松弛，但在 message 中区分 `input_*` 和 `rule_*` 前缀，让调用方识别

**Q2** (P0): `inferParas()` (新路径) 是否也需要冲突诊断？
- 如果是，本 RFC 的 P0 范围是否需要扩展到新路径？
- 如果不是，需要在新路径的 INFEASIBLE 场景中给出明确提示
- **建议**: 本 RFC 范围仅覆盖 `inferParasOld()` 路径，新路径的扩展列入 P2

### 设计决策问题（可在实现中确定）

**Q3** (P1): 需要几轮诊断？
- 当前硬编码二轮，但可以简化为单轮（第二轮增加的冲突发现率未知）
- **建议**: 先保留二轮，通过实际测试数据评估第二轮是否有增量价值

**Q4** (P1): 松弛可行解返回几个？
- 当前代码返回最后一轮的全部解（可能多个）
- **建议**: 第一阶段至少保证 ≥1 个，不作上限限制

**Q5** (P2): 权重的具体数值是否需要调整？
- 当前: 系统=1, 业务=25, 已冲突=+1000
- 需要实际使用后根据反馈调整

**Q6** (P2): 拼写修复 `confict` → `conflict` 是否在本 RFC 范围内？
- **建议**: 不在本 RFC 的 P0 修复范围内；文档和新增代码使用正确拼写，既有代码重命名列入 P2 后续统一重构

---

## 参考资料

- `doc/CORE-DESIGN.md` §7.2: 冲突检测流程设计
- `doc/ACCEPTANCE.md`: 测试数据极简、用例独立、回归自动化
- `RelaxVar.java`: 松弛变量数据类及权重常量
- `AlgCPConstraint.java`: 带 `onlyEnforceIf` 的约束包装
- `AlgCPModel.java` L148-L239: 松弛变量创建与约束附加
- `ModuleBaseAlgImpl.java` L260-L278: 松弛目标函数构建
- `ModuleConstraintExecutorImpl.java` L280-L443: 冲突诊断入口与二轮求解
- Rust RFC 模板: Summary, Motivation, Guide-level, Reference-level, Drawbacks, Alternatives, Unresolved questions

---

## 修订历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1 | 2026-04-30 | 初始版本（大模型S 生成） |
| v2 | 2026-04-30 | 新增"当前代码现状"章节；测试用例改为实际JMix格式；新增硬约束冲突用例；明确二轮求解策略；补充向后兼容性声明；未解决问题按优先级排序；修复拼写不一致 |
| v3 | 2026-04-30 | 根据 review 补充 `confict` 拼写技术债；补充 `ModuleBaseConstraintExecutorImpl` 在新路径中的职责和诊断缺口 |
