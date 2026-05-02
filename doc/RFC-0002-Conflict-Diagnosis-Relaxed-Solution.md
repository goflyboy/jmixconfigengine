# RFC-0002: 冲突诊断与松弛可行解

> 状态: 草案（Draft）
> 日期: 2026-04-30
> 相关文档: `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`

---

## 设计决议摘要

本 RFC 采用以下设计决议:

| 主题 | 决议 |
| --- | --- |
| 输入开关 | 在 `InferParasReq` 增加请求级 `relaxSolve`, 默认 `false` |
| 配置兼容 | `ConstraintConfig.debugByRelaxVar` 仅作为过渡兼容或默认值来源 |
| 输出载体 | 成功时返回正常解; 无解诊断时返回强类型错误诊断 `ConflictDiagnosisResult` |
| 解释内容 | 必须说明原始冲突约束、为得到部分解而放宽的约束、部分解 |
| 测试 API | 扩展 `ResultAssert` / `ModuleScenarioTestBase`, 提供冲突诊断专用断言 |
| 命名规范 | 新文档和新增 API 使用 `conflict`, 既有 `confict` 拼写作为技术债保留 |

## 摘要

本 RFC 提议完善 JMix Config Engine 的无解诊断能力: 当用户输入条件与业务规则共同导致模型无解时, 调用方可以在本次请求中显式开启“松弛求解”。引擎在开启后自动使用松弛变量重建模型, 最小化被放松的约束, 找出导致无解的规则集合, 并返回一个在放松这些规则后可行的配置解。

该能力的用户价值有两层:

- 告诉用户哪些输入或规则组合造成了无解, 例如 `ruleA` 与 `ruleB` 冲突。
- 在错误/无解返回中给出一个部分解, 让用户知道如果调整或放宽冲突约束, 当前配置可以怎样满足其余条件。

现有代码已经包含松弛变量相关实现, 主要分布在 `ModuleConstraintExecutorImpl`, `ModuleBaseConstraintExecutorImpl`, `AlgCPModel`, `AlgCPConstraint`, `RelaxVar`, `ModuleBaseAlgImpl.addRelaxObjectFunction()` 中。但由于后续功能演进, 该链路需要基于当前代码重新调通, 并补充回归测试。

## 动机

产品配置场景中, 用户经常先给出一组输入条件, 再由求解器补全剩余配置。若所有硬约束共同导致无解, 只返回“无可行解”对用户帮助有限。用户更需要知道:

- 哪些规则或输入条件导致无解。
- 是否存在一组“最小放松”的约束, 使模型重新变成可行。
- 放松后的可行解是什么, 以便业务界面给出解释或推荐调整方案。

当前项目核心设计已经把“冲突诊断”作为核心能力之一, 设计流程为: 为规则附加松弛变量, 最小化违反规则, 识别冲突规则, 报告冲突。本 RFC 将这条设计落成明确的行为契约、实现边界和测试要求。

## 术语

| 术语 | 含义 |
| --- | --- |
| 原始模型 | 按用户输入和全部业务规则构建的 CP-SAT 模型 |
| 松弛模型 | 为可诊断约束附加松弛变量后的 CP-SAT 模型 |
| 松弛变量 | 布尔变量, 用于控制某条规则是否暂时放松 |
| 冲突规则 | 在最小松弛目标下取值为“被放松”的规则 |
| 部分解 | 严格求解无解后, 系统内部放宽部分约束得到的候选配置。它不是正常解 |
| 请求级松弛开关 | 本次推理请求是否允许进入松弛诊断流程, 默认 false |
| 冲突诊断结果 | 无解/错误返回中的结构化说明, 包含原始冲突约束、被放宽约束和部分解 |
| 系统规则 | 引擎自动生成的约束, 如输入条件、默认可见性、部件数量关系 |
| 自定义规则 | 业务规则方法或规则 Schema 生成的约束 |

## Guide-level 说明

### 用户视角

松弛诊断应从“全局配置能力”升级为“请求级能力”。当前实现依赖 `ConstraintConfig.debugByRelaxVar`, 这会让所有请求共享同一个诊断开关, 不利于业务按场景控制成本和输出语义。RFC 要求最终是否执行松弛诊断, 应优先由 `InferParasReq` 中的请求字段决定。

建议新增:

```java
public class InferParasReq {
    /**
     * 是否允许本次请求在原始模型无解时执行松弛诊断。
     * 默认 false, 避免普通推理请求产生额外求解成本。
     */
    private boolean relaxSolve = false;
}
```

行为:

| 请求字段 | 全局配置 | 原始模型结果 | 返回行为 |
| --- | --- | --- | --- |
| `relaxSolve=false` | 任意 | 有解 | `SUCCESS`, 返回正常解 |
| `relaxSolve=false` | 任意 | 无解 | `NO_SOLUTION`, 不执行松弛诊断 |
| `relaxSolve=true` | 任意 | 有解 | `SUCCESS`, 返回正常解 |
| `relaxSolve=true` | 任意 | 无解 | 执行内部松弛诊断, 返回错误/无解结果, 其中包含冲突诊断和部分解 |

兼容策略:

- 第一阶段允许 `ConstraintConfig.debugByRelaxVar=true` 继续触发旧行为。
- 新代码和新测试优先使用 `InferParasReq.relaxSolve=true`。
- 后续可将全局配置降级为默认值或废弃项。
- 如果请求显式设置 `relaxSolve=false`, 则不进入松弛诊断, 即使全局配置为 true。若 Java boolean 无法区分“未设置”和“显式 false”, 可在后续改为 `Boolean relaxSolve` 或增加内部解析状态。

当请求开启 `relaxSolve=true` 时:

1. 引擎先运行原始模型。
2. 如果原始模型有可行解, 直接返回成功结果。
3. 如果原始模型被证明无解, 引擎进入冲突诊断流程。
4. 引擎构建松弛模型, 给可诊断规则附加松弛变量。
5. 引擎最小化被放松约束的总权重。
6. 引擎返回 `NO_SOLUTION`, message 中包含摘要, data 中包含强类型 `ConflictDiagnosisResult`, 其中给出冲突诊断结构和部分解。

示例:

```text
输入:
  x.qty + y.qty < 20
  x.qty - y.qty > 10
  y.qty > 45

原始模型:
  无解

返回:
  code = NO_SOLUTION
  message = "原始模型无解: rule1/rule2 与 rule3 冲突; 系统放宽 rule3 后给出部分解"
  data = {
    originalStatus: "INFEASIBLE",
    conflictConstraints: ["rule3"],
    relaxedConstraints: ["rule3"],
    partialSolutions: [...]
  }
```

### 结果语义

成功返回和冲突诊断返回必须分开理解:

- `SUCCESS`: data 是严格满足原始模型的正常解。
- `NO_SOLUTION + ConflictDiagnosisResult`: 原始模型无解。data 不是正常解集合, 而是错误/无解诊断结果; 其中 `partialSolutions` 是系统内部放宽部分约束后得到的部分解, 只能用于解释和推荐调整, 不能当成合法配置。

### 解释语义

返回解释应至少包含:

- 原始模型为什么无解: 哪些规则、输入或系统约束发生冲突。
- 为了给出部分解, 系统内部放宽了哪些约束。
- 部分解是什么。
- 可选: 每条冲突规则的自然语言描述、权重、约束表达式摘要。

第一阶段必须保证 `Result.message` 可读, 并通过强类型响应对象返回结构化诊断信息。这样业务侧既可以直接展示摘要, 也可以读取明确字段渲染更友好的解释, 不需要依赖字符串解析或 `Map<String, Object>`。

## Reference-level 设计

### 整体流程

```text
inferParas(req)
  |
  |-- build original model
  |-- solve original model
  |
  |-- if FEASIBLE/OPTIMAL:
  |     return SUCCESS(solutions)
  |
  |-- if INFEASIBLE and relaxSolve=false:
  |     return NO_SOLUTION(no relaxed solution)
  |
  |-- if INFEASIBLE and relaxSolve=true:
        |
        |-- build relaxed model
        |-- attach relax variables to diagnosable constraints
        |-- minimize weighted relax variables
        |-- solve relaxed model
        |-- collect relax variables with value = true
        |-- build ConflictDiagnosis
        |-- return NO_SOLUTION(message, ConflictDiagnosisResult with partialSolutions)
```

### 输入接口设计

新增请求字段:

```java
public class InferParasReq {
    private boolean relaxSolve = false;
}
```

字段语义:

- `false`: 默认值。原始模型无解时直接返回无解, 不做松弛诊断。
- `true`: 原始模型无解时自动进入松弛诊断流程。

命名备选:

| 字段名 | 说明 | 建议 |
| --- | --- | --- |
| `relaxSolve` | 简短, 表达“允许松弛求解” | 推荐 |
| `enableRelaxSolve` | 更像配置开关 | 可选 |
| `debugByRelaxVar` | 与现有配置一致, 但偏调试语义 | 不建议作为请求字段 |
| `diagnoseConflict` | 强调诊断, 但不体现松弛可行解 | 可选 |

请求级开关优先级:

```text
effectiveRelaxSolve = req.relaxSolve
```

如果需要保留旧配置兼容, 可在过渡期使用以下解析规则:

```text
if req.relaxSolve is explicitly set:
    effectiveRelaxSolve = req.relaxSolve
else:
    effectiveRelaxSolve = config.debugByRelaxVar
```

由于当前 `InferParasReq.relaxSolve` 设计为 primitive boolean 时无法区分“未设置”和“显式 false”, 第一阶段可接受:

```text
effectiveRelaxSolve = req.relaxSolve || config.debugByRelaxVar
```

但更推荐将字段定义为 `Boolean relaxSolve`, 并在读取时默认成 `false`。新测试应显式设置 `req.setRelaxSolve(true)`。

### 输出接口设计

输出需要同时服务两类调用方:

- 简单调用方: 直接读取 `message` 展示给用户。
- 复杂调用方: 读取强类型 `ConflictDiagnosisResult` 构造结构化 UI, 展示冲突约束、被放宽约束和部分解。

废弃方案:

```java
// 不采用
result.getExtAttrs().put("conflictDiagnosis", diagnosis);
```

不建议使用 `extAttrs["conflictDiagnosis"]`, 原因是:

- `extAttrs` 是弱类型 Map, 外部调用方需要自行转换, 易产生运行时错误。
- 字段契约不明显, IDE 和编译器无法帮助发现破坏性变更。
- 诊断结果是本功能的一等输出, 不应放在扩展属性里。

建议将冲突诊断作为错误/无解分支的强类型数据:

```java
public class ConflictDiagnosisResult {
    /** 原始模型状态, 通常为 INFEASIBLE */
    private String originalStatus;

    /** 原始无解相关的业务约束或输入约束 */
    private List<ConflictConstraint> conflictConstraints;

    /** 为得到部分解实际被系统内部放宽的约束 */
    private List<RelaxedConstraint> relaxedConstraints;

    /** 部分解: 严格求解无解后, 放宽部分约束得到的候选配置 */
    private List<ModuleInst> partialSolutions;

    /** 可直接展示给用户的解释 */
    private String explanation;
}
```

保留现有成功接口语义:

```java
Result<List<ModuleInst>> inferParas(InferParasReq req);
```

返回规则:

```text
SUCCESS:
  Result.code = SUCCESS
  Result.data.solutions = 正常解 List<ModuleInst>
  Result.data.conflictDiagnosis = null

NO_SOLUTION 且 relaxSolve=false:
  Result.code = NO_SOLUTION
  Result.data.solutions = []
  Result.data.conflictDiagnosis = null

NO_SOLUTION 且 relaxSolve=true 且内部放宽后得到部分解:
  Result.code = NO_SOLUTION
  Result.data.solutions = []
  Result.data.conflictDiagnosis = ConflictDiagnosisResult

FAILED:
  Result.code = FAILED
  Result.data.conflictDiagnosis = ConflictDiagnosisResult 或 null
```

由于 `inferParas` 当前签名是 `Result<List<ModuleInst>>`, Java 泛型无法同时表达成功时 data 是 `List<ModuleInst>`、无解诊断时 data 是 `ConflictDiagnosisResult`。因此需要二选一:

1. 新增 `inferParasV2(InferParasReq req): Result<InferParasRsp>`。
2. 修改现有接口为 `Result<InferParasRsp>`。

推荐方案是新增 `InferParasRsp`, 让成功和无解诊断都有明确字段:

```java
public class InferParasRsp {
    /** 严格满足原始模型的正常解。只有 SUCCESS 时有值 */
    private List<ModuleInst> solutions;

    /** 无解诊断结果。只有 NO_SOLUTION 且 relaxSolve=true 时有值 */
    private ConflictDiagnosisResult conflictDiagnosis;
}
```

这不是“双轨正常输出”, 而是把错误/无解诊断结构化。正常成功时用户仍然只关心 `solutions`; 严格无解时才读取 `conflictDiagnosis.partialSolutions`。

当 `relaxSolve=false` 且原始模型无解时:

```text
code = NO_SOLUTION
message = "no solution"
data.conflictDiagnosis = null
```

当 `relaxSolve=true` 且松弛后有解时:

```text
code = NO_SOLUTION
message = diagnosis.explanation
data.solutions = []
data.conflictDiagnosis.partialSolutions = partialSolutions
```

当 `relaxSolve=true` 但松弛模型仍无解时:

```text
code = FAILED 或 NO_SOLUTION
message = "relaxed model is still infeasible"
data.solutions = []
data.conflictDiagnosis 可包含 originalStatus 和失败原因, 但不包含 partialSolutions
```

### 松弛变量建模

现有 `AlgCPConstraint` 中的关键语义为:

```java
if (relaxationVar != null && cpConstraint != null) {
    cpConstraint.onlyEnforceIf(relaxationVar.not());
}
```

因此约束含义为:

- `relaxVar = false`: 约束生效。
- `relaxVar = true`: 约束被放松。

目标函数由 `ModuleBaseAlgImpl.addRelaxObjectFunction()` 添加:

```java
minimize(sum(relaxVar[i] * weight[i]))
```

这会让求解器优先放松数量少、权重低的规则。当前权重建议:

| 规则类型 | 权重 | 意图 |
| --- | --- | --- |
| 系统规则 | `WEIGHT_SMALL` | 输入/系统约束可作为诊断对象, 但需明确是否允许放松 |
| 自定义规则 | `WEIGHT_MEDIUM` | 默认业务规则权重 |
| 已识别冲突规则二次运行 | `+ WEIGHT_ADDER` | 鼓励二次运行找出更多候选冲突 |

### 诊断范围

本 RFC 建议第一阶段覆盖以下约束:

- 通过 `executeRuleMethod()` 执行的自定义规则。
- `addPartEquality()` 与 `addParaEquality()` 生成的用户输入约束。
- `sumFunConstraint()` 生成的部件分类输入约束。
- 默认可见性约束, 前提是它已被统一打上系统规则标识。

不在第一阶段覆盖:

- 变量域本身, 如 `newIntVar(0, maxQty)` 的上下界。
- OR-Tools 内部推导约束。
- 未通过 `AlgCPModel` 包装层添加的原生 `CpModel` 约束。

### 与现有架构的关系

本功能沿用核心设计中的对称分层:

| 层级 | 职责 |
| --- | --- |
| Executor | 读取请求级松弛开关, 判断原始求解结果, 触发松弛诊断, 组装返回结果 |
| Algorithm | 在规则执行前设置当前松弛变量, 在模型完成后添加松弛目标函数 |
| Model Wrapper | 对 OR-Tools 约束附加 `onlyEnforceIf(relaxVar.not())`, 记录规则与变量映射 |
| Result Model | 承载严格无解状态、冲突消息和松弛可行解 |

Executor 层当前存在两条路径:

| 路径 | 触发条件 | 核心类 | 当前诊断状态 |
| --- | --- | --- | --- |
| 旧参数推理路径 | `InferParasReq.partConstraintReqs` 为空 | `ModuleConstraintExecutorImpl.inferParasOld()` | 已有松弛诊断雏形 |
| 新部件约束路径 | `InferParasReq.partConstraintReqs` 非空 | `ModuleConstraintExecutorImpl.processProduct()` -> `ModuleBaseConstraintExecutorImpl` | 需要补齐诊断入口 |

`ModuleBaseConstraintExecutorImpl` 负责新路径和优先级求解的公共求解过程。它不是创建松弛变量的地方, 但它当前会创建 `AlgCPModel` 并调用 `ModuleAlgImpl.init()`。因此若请求级 `relaxSolve=true` 要覆盖 `PartConstraintReq` 场景, 该基类也必须支持:

- 根据请求传入 `isAttachRelax=true`。
- 在原始模型无解时触发松弛诊断。
- 将 `SolverResult` 转换为统一的 `Result` 和 `ConflictDiagnosis`。

### 返回模型

推荐新增强类型响应对象, 作为本 RFC 的目标接口:

```java
public class InferParasRsp {
    /** 严格满足原始模型的正常解。只有 SUCCESS 时有值 */
    private List<ModuleInst> solutions;

    /** 无解诊断。只有 NO_SOLUTION 且请求开启 relaxSolve 时有值 */
    private ConflictDiagnosisResult conflictDiagnosis;
}
```

其中 `solutions` 的语义是“严格满足原始模型的正常解”。当原始模型无解但内部放宽后得到部分解时, `solutions` 为空, 部分解放在 `conflictDiagnosis.partialSolutions` 中。这个分离不是为了向用户暴露松弛概念, 而是为了明确: 部分解属于错误/无解诊断, 不是正常求解结果。

结构化诊断对象:

```java
public class ConflictDiagnosisResult {
    /** 原始模型状态, 通常为 INFEASIBLE */
    private String originalStatus;

    /** 原始无解相关的业务约束或输入约束 */
    private List<ConflictConstraint> conflictConstraints;

    /** 为得到部分解实际被系统内部放宽的业务约束或输入约束 */
    private List<RelaxedConstraint> relaxedConstraints;

    /** 部分解。该解不是严格合法解, 只能用于诊断和推荐 */
    private List<ModuleInst> partialSolutions;

    /** 可直接展示给用户的解释 */
    private String explanation;
}

public class ConflictConstraint {
    /** 约束编码, 如 rule3、input_para_size_small */
    private String code;

    /** 约束类型: RULE / INPUT / SYSTEM */
    private String constraintType;

    /** 人类可读描述, 优先来自 Rule.normalNaturalCode */
    private String naturalCode;

    /** 来源对象, 如 Module / PartCategory / Para / Part */
    private String source;
}

public class RelaxedConstraint {
    /** 被系统内部放宽的约束编码 */
    private String code;

    /** 约束类型: RULE / INPUT / SYSTEM */
    private String constraintType;

    /** 松弛变量名称, 如 relax_rule3 */
    private String relaxVarName;

    /** 松弛权重 */
    private int weight;

    /** 为什么被放宽: 例如最小权重松弛后取值为 true */
    private String reason;
}
```

示例说明:

```text
explanation:
  原始模型无解: rule1、rule2 与 rule3 共同冲突。
  系统放宽 rule3 后给出 1 个部分解: x.qty=12, y.qty=1。
```

输出约束:

- `conflictConstraints` 描述原始无解相关的业务约束、输入约束或系统约束, 不要求全部都被松弛。
- `relaxedConstraints` 描述本次内部诊断实际设置为 true 的 `RelaxVar` 对应的约束。对外解释为“被系统放宽的约束”。
- `solutions` 只表示严格满足原始模型的正常解。
- `partialSolutions` 只表示错误/无解诊断中的部分解, 必须与正常解分开。
- `explanation` 必须包含“原始冲突”和“松弛后有解”的信息, 不能只输出 `conflict rules: ...`。

### 需要调通的现有链路

基于当前代码, 至少需要验证和修复以下路径:

- `ModuleConstraintExecutorImpl.inferParasOld()` 在 `INFEASIBLE && debugByRelaxVar` 下能进入诊断流程。
- `InferParasReq.relaxSolve` 能控制本次请求是否进入诊断流程, 默认不松弛。
- `ModuleBaseConstraintExecutorImpl` 路径需要明确是否纳入本阶段; 如果纳入, 不能只依赖 `inferParasOld()`。
- `runCalcConfictRules()` 返回的松弛模型状态必须是 `OPTIMAL` 或 `FEASIBLE`; 若仍为 `INFEASIBLE`, 说明有不可松弛硬约束或模型构建遗漏。
- `calcConfictRules()` 中 `solver.booleanValue(relaxVar.getValue())` 能正确识别被放松规则。
- `InferParasRsp.solutions` 在成功时能被测试框架和调用方读取。
- `InferParasRsp.conflictDiagnosis` 在无解诊断时能被测试框架和调用方读取。
- 带 `onlyEnforceIf()` 的条件约束在叠加松弛变量后语义正确。
- 新增的部件约束、优先级求解、决策策略、多实例分类不破坏诊断流程。

### 技术债务

代码库中已有一批历史命名使用 `confict` 而非 `conflict`, 包括:

- 类名: `ConfictDebugTest`, `ConfictDebugIfThenTest`
- 方法名: `calcConfictRules`, `runCalcConfictRules`
- 变量名: `confictedRelaxs`
- 包名: `com.jmix.tool.confictdebug`

RFC 概念和新增代码统一使用正确拼写 `conflict`。既有代码引用保留原名, 方便定位当前实现。统一重命名列为后续独立重构, 不阻塞本 RFC 的核心功能。

## Drawbacks

- 松弛变量会扩大模型规模, 诊断流程至少需要额外一次求解。
- “最小松弛”受权重影响, 结果不是唯一的逻辑最小冲突集。
- 如果某些约束未经过 `AlgCPModel` 包装, 它们不会被松弛, 可能导致诊断模型仍无解。
- `NO_SOLUTION` 携带部分解的语义容易被调用方误用, 需要强类型响应、文档和测试明确。
- 请求级 `relaxSolve=true` 会触发额外求解成本, 调用方需要按需开启。

## Alternatives

### 使用 CP-SAT assumptions/unsat core

OR-Tools CP-SAT 支持基于 assumptions 的 unsat core 思路, 可直接分析不可满足核心。但当前 Java 封装和业务规则映射已经围绕 `RelaxVar` 建模, 且需求同时要求“松弛后可行解”。因此第一阶段继续使用松弛变量更贴合现有实现。

### 逐条禁用规则重试

可以通过逐条移除规则来定位冲突, 实现简单但复杂度高, 且难以同时处理多规则组合冲突。该方案不建议作为主路径。

### 只返回冲突规则, 不返回可行解

这能降低实现复杂度, 但无法满足“松弛之后当前部分满足的解释如下”的原始需求。因此不可作为最终方案。

## 验收与测试要求

测试用例遵循 `doc/ACCEPTANCE.md` 的原则: 数据极简, 每个用例只验证一个功能点, 覆盖正常路径、边界条件和异常路径, 验证通过后纳入回归测试。

### CONFLICT-001: 基础线性规则冲突

目的: 验证最简单的线性约束冲突可以定位到单条冲突规则, 并在错误/无解诊断中返回部分解。

测试数据:

```java
@PartAnno(maxQuantity = 50)
private PartVar x;

@PartAnno(maxQuantity = 50)
private PartVar y;

rule1: x.qty + y.qty < 20
rule2: x.qty - y.qty > 10
rule3: y.qty > 45
```

用例:

| ID | 配置 | 期望 |
| --- | --- | --- |
| CONFLICT-001-1 | `relaxSolve=false` | 原始模型无严格可行解, 不返回冲突诊断 |
| CONFLICT-001-2 | `relaxSolve=true` | 返回 `NO_SOLUTION`, message 包含 `rule3`, `partialSolutions` 至少包含 1 个部分解 |
| CONFLICT-001-3 | `relaxSolve=true` | `conflictDiagnosis.relaxedConstraints` 包含 `rule3` |

验收断言:

- `resultAssert().assertNoSolution()`
- `assertMessageContains("rule3")`
- `assertPartialSolutionSizeGreaterThanOrEqual(1)`
- `assertConflictConstraintsContains("rule3")`
- `assertRelaxedConstraintsContains("rule3")`
- partialSolutions 中解满足 `rule1` 和 `rule2`, 不要求满足 `rule3`

### CONFLICT-002: 多规则组合冲突

目的: 验证当多个规则共同造成无解时, 可以返回一组冲突规则。

测试数据:

```text
rule31: y.qty > 48
rule32: y.qty > 45
rule51: y.qty < 10
rule52: y.qty < 15
```

用例:

| ID | 配置 | 期望 |
| --- | --- | --- |
| CONFLICT-002-1 | `relaxSolve=true` | 返回 `NO_SOLUTION`, message 至少包含高下界冲突中的一侧规则 |

验收断言:

- message 包含 `rule31` 或 `rule32`
- message 包含 `rule51` 或 `rule52`
- partialSolutions 非空
- 部分解中未被放宽的规则均满足

### CONFLICT-003: 条件规则 If-Then 冲突

目的: 验证带 `onlyEnforceIf()` 的条件约束在松弛变量叠加后仍可诊断。

测试数据:

```text
rule1: if x.qty > 10 then y.qty > 7
rule2: x.qty > 20
rule3: y.qty < 5
```

期望:

- 原始模型无解。
- 开启诊断后返回 `NO_SOLUTION`。
- 冲突规则包含 `rule1` 或与其直接冲突的 `rule2/rule3`。
- partialSolutions 非空。

重点验证:

- `AlgCPConstraint.onlyEnforceIf(condition)` 与松弛变量组合后不是覆盖关系。
- 条件规则被放松时, 整条业务规则都应被视为放松, 而不是只放松其中一条内部约束。

### CONFLICT-004: 用户输入条件与业务规则冲突

目的: 验证用户输入本身也能作为诊断对象。

测试数据:

```text
业务规则: p=color:red -> size=large
用户输入: p=color:red, size=small
```

期望:

- 原始模型无解。
- 开启诊断后返回 `NO_SOLUTION`。
- message 能指出是用户输入约束或相关业务规则冲突。
- conflictDiagnosis 中返回一组部分解。

待确认点:

- 输入约束是否允许被放松。
- 若允许放松, 输入约束在 message 中使用什么 code, 如 `input_para_size_small`。

### CONFLICT-005: 部件分类输入约束冲突

目的: 验证 `PartConstraintReq` 和 `sumFunConstraint()` 生成的约束能进入诊断。

测试数据:

```text
disk 分类:
  disk1 capacity=1
  disk2 capacity=2

业务规则: 总容量 <= 2
用户输入: disk:Sum_Capacity >= 3
```

期望:

- 原始模型无解。
- 开启诊断后返回 `NO_SOLUTION`。
- message 能定位到容量输入约束或业务规则。
- conflictDiagnosis 中有一个满足未放宽约束的部分解。

### CONFLICT-006: 多实例分类冲突

目的: 验证 `supportMultiInst=true` 或枚举多实例路径下, 松弛诊断仍然可用。

测试数据:

```text
gpu supportMultiInst=true
实例输入要求总数量 >= 2
业务规则限制总数量 <= 1
```

期望:

- 原始模型无解。
- 开启诊断后返回 `NO_SOLUTION`。
- message 包含多实例总量相关规则或输入约束。
- partialSolutions 非空, 且实例 ID 和部件数量可正确回填。

### CONFLICT-007: 与优先级/目标函数的关系

目的: 验证冲突诊断时松弛目标函数优先于业务优化目标, 不被 `PriorityRule` 或决策策略覆盖。

期望:

- 诊断模型使用 `minimize(sum(relaxVar * weight))`。
- 如果原模型已有优先级目标, 诊断阶段应清理或隔离原目标。
- 返回的冲突规则以最小松弛为准, 不以原业务优化目标为准。

### CONFLICT-008: 无法松弛的硬冲突

目的: 验证变量域或不可松弛硬约束导致无解时, 系统给出明确失败信息。

测试数据:

```text
Part maxQuantity=1
用户输入 qty=2
且输入约束不允许放松
```

期望:

- 原始模型无解。
- 诊断模型仍无解。
- 返回 `FAILED` 或 `NO_SOLUTION` 且 message 明确说明 `relaxed model is infeasible`。
- 不返回误导性可行解。

### CONFLICT-009: 请求级松弛开关

目的: 验证是否执行松弛诊断由 `InferParasReq.relaxSolve` 控制, 默认不松弛。

用例:

| ID | 输入 | 期望 |
| --- | --- | --- |
| CONFLICT-009-1 | 不设置 `relaxSolve` | 原始模型无解, 返回 `NO_SOLUTION`, `conflictDiagnosis == null`, 无部分解 |
| CONFLICT-009-2 | `relaxSolve=false` | 行为同默认值 |
| CONFLICT-009-3 | `relaxSolve=true` | 返回 `NO_SOLUTION`, 有 `conflictDiagnosis`, 有部分解 |

### CONFLICT-010: 输出解释完整性

目的: 验证输出同时包含“原始冲突”和“松弛后结果”。

期望:

- `message` 可直接说明“哪些规则冲突导致无解, 松弛哪些项后有解”。
- `conflictDiagnosis.conflictConstraints` 非空。
- `conflictDiagnosis.relaxedConstraints` 非空。
- `conflictDiagnosis.partialSolutions` 非空。
- `InferParasRsp.solutions` 为空, 避免部分解被误认为正常解。

### 测试辅助方法

为让测试代码简洁易读, 需要扩展 `ResultAssert` 或 `ModuleScenarioTestBase` 的断言方法。建议新增:

```java
resultAssert()
    .assertNoSolution()
    .assertMessageContains("rule3")
    .assertConflictConstraintsContains("rule3")
    .assertRelaxedConstraintsContains("rule3")
    .assertPartialSolutionSizeGreaterThanOrEqual(1);
```

建议断言 API:

| 方法 | 验证内容 |
| --- | --- |
| `assertHasConflictDiagnosis()` | `InferParasRsp.conflictDiagnosis` 存在 |
| `assertConflictConstraintsContains(String... codes)` | `conflictConstraints` 包含指定 code |
| `assertRelaxedConstraintsContains(String... codes)` | `relaxedConstraints` 包含指定 code |
| `assertPartialSolutionSizeEqual(int size)` | 部分解数量等于指定值 |
| `assertPartialSolutionSizeGreaterThanOrEqual(int minSize)` | 部分解数量不少于指定值 |
| `assertNoConflictDiagnosis()` | 默认不松弛时不应返回诊断对象 |

对于解内容比较, 可复用现有 `assertSoluContain(...)`, 但命名上建议增加语义化包装:

```java
assertPartialSoluContain("x(Q:12,H:0,S:1),y(Q:1,H:0,S:1)");
```

这样测试能表达“这是错误/无解诊断中的部分解”, 不需要在每个测试里手动访问诊断对象内部结构。

测试辅助 API 的实现要求:

- `ResultAssert` 负责检查 `Result.code`, `message`, `InferParasRsp.conflictDiagnosis` 和正常/部分解。
- `ModuleScenarioTestBase` 负责提供更贴近场景测试的封装, 如 `inferParasByParaRelax(...)`、`inferRecommendRelax(...)`、`assertPartialSoluContain(...)`。
- 断言失败时应输出当前 `message`、`conflictDiagnosis` 摘要和所有可用解的短格式, 避免测试失败后还要手动打印。

建议新增测试调用方式:

```java
inferParasByParaRelax();

resultAssert()
    .assertNoSolution()
    .assertHasConflictDiagnosis()
    .assertConflictConstraintsContains("rule3")
    .assertRelaxedConstraintsContains("rule3")
    .assertPartialSolutionSizeGreaterThanOrEqual(1);

assertPartialSoluContain("x(Q:12", "y(Q:1");
```

其中 `inferParasByParaRelax()` 只是测试基类便捷方法, 等价于构造 `InferParasReq` 后执行 `req.setRelaxSolve(true)`。

### 回归要求

必须运行并通过:

```bash
mvn test
```

至少新增或修复以下测试文件:

- `src/test/java/com/jmix/tool/confictdebug/ConfictDebugTest.java`
- `src/test/java/com/jmix/tool/confictdebugifthen/ConfictDebugIfThenTest.java`
- 新增 `src/test/java/com/jmix/tool/conflictdiagnosis/ConflictDiagnosisTest.java` 或等价文件

## 实现计划

| 阶段 | 内容 | 优先级 |
| --- | --- | --- |
| 1 | 梳理现有松弛链路, 明确哪些约束未经过 `AlgCPModel` 包装 | P0 |
| 2 | 在 `InferParasReq` 新增请求级 `relaxSolve` 字段, 默认 false | P0 |
| 3 | 修复原始无解后进入诊断流程的状态处理和返回语义 | P0 |
| 4 | 新增 `InferParasRsp`/`ConflictDiagnosisResult`/`ConflictConstraint`/`RelaxedConstraint` 强类型输出结构 | P0 |
| 5 | 确保条件约束和多条内部约束共享同一个规则级松弛变量 | P0 |
| 6 | 补齐用户输入约束、部件分类约束的松弛标识 | P0 |
| 7 | 处理诊断目标函数与 `PriorityRule`/业务目标函数的关系 | P0 |
| 8 | 扩展 `ResultAssert`/`ModuleScenarioTestBase` 的冲突诊断断言方法 | P1 |
| 9 | 补充基础、条件、多规则、输入、多实例、请求开关、输出解释测试 | P1 |
| 10 | 统一 `confict` -> `conflict` 拼写 | P2 |

## 未解决问题

这些问题需要项目维护者确认后才能进入最终实现:

1. 用户输入约束是否允许被松弛?  
   如果允许, 返回给用户时应描述为“用户输入冲突”还是“系统规则冲突”?

2. 系统规则和自定义规则的权重策略是否合理?  
   当前系统规则权重更小, 求解器可能优先放松输入/系统约束, 而不是业务规则。

3. 多个等价最小冲突集时, 是否需要稳定排序?  
   例如同时放松 `rule31` 或 `rule32` 都可行, message 是否要求确定性。

4. 是否新增 `inferParasV2()` 以返回 `Result<InferParasRsp>`?  
   还是直接修改 `inferParas()` 的返回泛型。建议新增 V2 方法兼容迁移。

5. 调试模式下是否只返回 1 个松弛可行解?  
   当前需求说“一个可行解”, 建议第一阶段只保证至少一个。

6. 是否需要展示自然语言解释?  
   当前 `Rule.normalNaturalCode` 可以作为解释来源, 但结果对象还没有结构化承载。

7. `PartConstraintReq` 路径目前可能走 `processProduct()` 而不是 `inferParasOld()`。  
   该路径是否也必须支持松弛诊断, 还是第一阶段只覆盖 `inferParasOld()`?

8. 请求级字段命名是否采用 `relaxSolve`?  
   如果希望更业务化, 可改为 `diagnoseConflict` 或 `enableRelaxSolve`。

## 参考资料

- `doc/CORE-DESIGN.md`: 核心分层、冲突诊断流程、`AlgCPModel` 职责。
- `doc/ACCEPTANCE.md`: 测试数据极简、用例独立、回归自动化要求。
- Rust RFCs: RFC 通常包含 Summary, Motivation, Guide-level explanation, Reference-level explanation, Drawbacks, Rationale and alternatives, Unresolved questions。
- Google OR-Tools CP-SAT: 约束建模、目标函数、条件约束与求解状态。

---

## 修订历史

| 版本 | 日期 | 变更内容 |
| --- | --- | --- |
| Draft v1 | 2026-04-30 | 初始草案 |
| Draft v2 | 2026-04-30 | 增加请求级松弛开关、结构化输出、测试断言设计 |
| Draft v3 | 2026-04-30 | 迁移到 `doc/` 并按 RFC 编号命名; 强化输入/输出/测试 API 设计决议 |
| Draft v4 | 2026-04-30 | 废弃 `extAttrs["conflictDiagnosis"]`; 改为强类型 `InferParasResult`; 将 `conflictItems/relaxedItems` 更名为业务化的 `conflictConstraints/relaxedConstraints` |
| Draft v5 | 2026-04-30 | 将松弛结果重新定义为无解/错误返回中的 `partialSolutions`; 正常成功结果不暴露松弛概念 |
