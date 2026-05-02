# RFC-0004: 混合计算与后置规则执行

> 状态: 草案（Draft）
> 日期: 2026-05-02
> 相关文档: `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`

---

## 设计决议摘要

本 RFC 提议在现有约束求解架构上引入“混合计算”模式: 继续使用 CP-SAT 约束求解处理需要多解枚举和组合搜索的主计算，同时允许部分前置/后置业务规则以普通 Java 代码方式直接运行。

| 主题 | 决议 |
| --- | --- |
| 阶段枚举 | 优先复用现有 `CalcStage.PRE/MID/POST`; 业务文档中的 MAIN 对应代码中的 `MID` |
| 默认行为 | `@CodeRuleAnno` 默认仍为 `CalcStage.MID`, 保持现有约束规则行为不变 |
| 首期范围 | 优先补齐 `CalcStage.POST`: 对每个 `ModuleInst` 解执行后置计算 |
| 输出参数 | 复用现有 `AssignType.INPUT`, 表示不进入 CP 模型、可由 POST 直接写回的派生参数 |
| 访问接口 | 新增 `ModuleInstAccessor` 接口, 由 `ModuleBaseAlgImpl` 继承实现 |
| 集成模式 | 支持求解回调中自动执行 POST, 也支持对已有 `ModuleInst` 独立执行 POST |
| 兼容策略 | 现有 `CalcStage.PRE` 规则暂不改变执行语义, 首期只实现 POST |

---

## 1. 摘要

当前配置计算引擎主要依赖约束求解来表达业务规则。这适合多解枚举、组合搜索和硬约束推理，但不适合所有规则: 部分规则包含复杂循环或业务过程计算，难以建模为 CP 约束；另一些前置/后置计算并不参与求多解，若强行建模会增加变量数量和搜索复杂度。

本 RFC 提议引入 PRE/MID/POST 混合计算流程。MID 继续由现有 CP-SAT 模型完成；POST 在每个可行解生成后，通过普通 Java 规则读取该解的部件、属性和参数，并把派生参数写回 `ModuleInst`。PRE 的纯代码化能力作为同一套机制的后续扩展点，首期以 POST 为主落地。

---

## 2. 动机

### 2.1 问题背景

JMix Config Engine 目前的核心能力是基于 CP-SAT 的约束满足问题求解。规则通常以 `@CodeRuleAnno` 方法表示，在 `ModuleAlgImpl.init()` 期间被执行，并向 `AlgCPModel` 添加约束。

这条路径适合以下场景:

- 需要从输入条件推导多个可行配置解。
- 需要表达部件数量、选中状态、参数值之间的硬约束。
- 需要优化目标或优先级排序。

但以下两类规则并不适合强行进入 CP 模型:

- 规则本身包含复杂业务流程、循环、聚合、分支或外部计算，约束模型难以直接表达。
- 规则只是对输入做预处理，或对每个求解结果做补充计算，不需要参与多解搜索。

如果这类规则也通过 CP 变量和约束表达，会带来额外问题:

- 增加不必要的变量和约束，扩大搜索空间。
- 规则剪枝过程变得不可控，影响求解性能和解释性。
- 业务代码难以复用已有 Java 流程逻辑。

### 2.2 具体场景

以“求解后补充计算参数”为例。约束求解先找出 CPU、硬盘等部件组合；每个解生成后，业务需要计算模块级参数和分类级参数:

```java
@CodeRuleAnno(calcStage = CalcStage.POST)
private void postRule() {
    int cpuMemory = toInt(getDynAttr("cpu", "Memory"));
    int driveSumCapacity = toInt(getSumDynAttr("drive", "Capacity"));

    setParaValue("pCpuMemory", String.valueOf(cpuMemory));
    setParaValue("drive", "pSumCapacity", String.valueOf(driveSumCapacity));
}
```

期望行为:

- `cpu.Memory` 和 `drive.Capacity` 从当前解的部件实例及其静态动态属性中读取。
- `pCpuMemory` 写回模块级 `ParaInst`。
- `drive.pSumCapacity` 写回 `drive` 分类级 `ParaInst`。
- 这些计算不创建 CP 变量，也不影响求解器枚举解。

### 2.3 为什么需要改变

现有代码已经具备阶段概念:

- `CalcStage.PRE`: 前置计算。
- `CalcStage.MID`: 当前主要求解阶段。
- `CalcStage.POST`: 后置计算。

但当前执行链路只在模型构建阶段执行 PRE/MID 规则，POST 还没有真正对每个 `ModuleInst` 解执行。要支持业务侧的后置派生计算，需要补齐实例级执行通道，并提供一个面向 `ModuleInst` 的访问 API。

---

## 3. 设计方案

### 3.1 核心思路

混合计算分成三个阶段:

```text
InferParasReq
  |
  |-- PRE   输入预处理/控制变量准备
  |
  |-- MID   CP-SAT 约束求解, 生成一个或多个 ModuleInst
  |
  |-- POST  对每个 ModuleInst 执行普通 Java 后置规则, 写回派生参数
  |
Result<List<ModuleInst>>
```

首期实现聚焦 POST:

1. 求解器按现有方式构建模型并求解。
2. `ModuleInstSolutionCallBack` 每拿到一个解后，先构造 `ModuleInst`。
3. 回调调用 `ModulePostCalculator.doCalc(moduleInst)`。
4. POST 规则通过 `ModuleInstAccessor` 读取当前解并写回参数。
5. 计算完成后的 `ModuleInst` 再加入 `SolverResult.solutions`。

### 3.2 阶段与命名

当前代码中已经存在:

```java
public enum CalcStage {
    PRE(10),
    MID(20),
    POST(30);
}
```

本 RFC 决定复用现有 `CalcStage`，不新增重复的 `CalcType`:

| 业务说法 | 代码阶段 | 执行方式 |
| --- | --- | --- |
| pre | `CalcStage.PRE` | 现阶段保持原语义；后续可扩展为输入预处理 |
| main | `CalcStage.MID` | 现有 CP-SAT 约束求解 |
| post | `CalcStage.POST` | 新增: 对每个 `ModuleInst` 直接执行 Java 代码 |

业务文档中的 MAIN 统一映射到代码中的 `CalcStage.MID`。生成式代码、注解解析和执行链路均沿用 `@CodeRuleAnno(calcStage = ...)`。

### 3.3 数据模型扩展

#### 3.3.1 后置输出参数赋值类型

为避免后置参数进入 CP 模型，本 RFC 决定复用现有 `AssignType.INPUT`，不新增 `AssignType.DERIVED`。

行为约定:

- `INPUT`: 由请求输入赋值，不创建 CP 变量。
- `CALC`: 由 CP 模型求解赋值，创建 CP 变量。

后置输出参数也声明为 `AssignType.INPUT`。它的语义是“不创建 CP 变量”，但不一定来自外部请求输入，也可以由 POST 规则写入。

需要在本次重构中明确并修正以下行为:

- `ModuleBaseAlgImpl.initParaVar()` 遇到 `AssignType.INPUT` 时不创建 `IntVar`、`BoolVar` 或选项选择变量。
- `AssignType.INPUT` 参数未被请求赋值时，仍应在 `ModuleInst` 中生成对应 `ParaInst`，初始 `value` 可为 `null`。
- POST 执行后通过 `setParaValue(...)` 写回这些 `ParaInst.value`。
- MID 约束规则不应引用未创建 CP 变量的 `AssignType.INPUT` 参数；若引用，应在执行时给出清晰异常。

示例:

```java
@ParaAnno(code = "pCpuMemory", type = ParaType.INTEGER, assignType = AssignType.INPUT)
private ParaVar pCpuMemory;

@ParaAnno(fatherCode = "drive", code = "pSumCapacity",
        type = ParaType.INTEGER, assignType = AssignType.INPUT)
private ParaVar pSumCapacity;
```

### 3.4 接口变更

#### 3.4.1 ModuleInstAccessor

新增实例访问接口，并由 `ModuleBaseAlgImpl` 继承实现。这样生成式规则代码仍然写在 `ConstraintAlgImplTestBase` / `ModuleAlgImpl` 子类中，可以直接调用 `getDynAttr(...)`、`getSumDynAttr(...)`、`setParaValue(...)`，不需要额外注入一个 accessor 对象。

```java
public interface ModuleInstAccessor {

    void setParaValue(String paraCode, String value);

    void setParaValue(String partCategoryCode, String paraCode, String value);

    void setParaValue(String partCategoryCode, int instId, String paraCode, String value);

    String getParaValue(String paraCode);

    String getParaValue(String partCategoryCode, String paraCode);

    String getParaValue(String partCategoryCode, int instId, String paraCode);

    String getDynAttr(String partCategoryCode, String attrCode);

    String getDynAttr(String partCategoryCode, int instId, String attrCode);

    String getSumDynAttr(String partCategoryCode, String attrCode);

    String getSumDynAttr(String partCategoryCode, int instId, String attrCode);
}
```

```java
public class ModuleBaseAlgImpl implements ModuleInstAccessor {

    private ModuleInst currentModuleInst;

    void bindModuleInst(ModuleInst solution) {
        this.currentModuleInst = solution;
    }

    void clearModuleInst() {
        this.currentModuleInst = null;
    }

    protected ModuleInst currentModuleInst() {
        if (currentModuleInst == null) {
            throw new AlgLoaderException(
                    "ModuleInst is not bound. This method is only available in POST context.");
        }
        return currentModuleInst;
    }

    @Override
    public String getDynAttr(String partCategoryCode, String attrCode) {
        // 从 currentModuleInst 读取第一个有效部件的静态动态属性
    }

    @Override
    public void setParaValue(String paraCode, String value) {
        // 写回 currentModuleInst 的模块级 ParaInst
    }
}
```

语义:

- `setParaValue(String paraCode, String value)`: 写模块级参数。
- `setParaValue(String partCategoryCode, String paraCode, String value)`: 写分类参数；若分类存在多个实例，默认写第一个匹配实例。
- `setParaValue(String partCategoryCode, int instId, String paraCode, String value)`: 写指定分类实例参数。
- `getDynAttr(...)`: 读取当前解中某分类选中部件的动态属性；若匹配多个有效部件，返回稳定遍历顺序下的第一个有效部件。
- `getSumDynAttr(...)`: 对当前解中有效部件的指定属性做数量加权求和。
- 带 `instId` 的重载用于多实例分类的精确访问；不带 `instId` 的重载面向单实例或简单场景，多实例下默认使用第一个匹配实例。

有效部件建议定义为:

```text
partInst.isSelected == true && partInst.quantity > 0
```

为兼容历史数据，可在实现中对 `isSelected=false` 但 `quantity>0` 的部件记录警告并纳入计算。

#### 3.4.2 后置规则辅助方法

`ModuleBaseAlgImpl` 实现 `ModuleInstAccessor` 后，后置规则可以直接调用接口方法。只需要补充少量通用转换方法:

```java
protected int toInt(String value);
```

这些方法只允许在实例计算上下文中使用。若 POST 以外阶段调用，应抛出清晰异常:

```text
ModuleInst is not bound. This method is only available in POST context.
```

### 3.5 后置计算器

新增内部组件:

```java
public class ModulePostCalculator {

    public void doCalc(ModuleInst solution) {
        // 1. 将当前解绑定到 ModuleBaseAlgImpl
        // 2. 依次执行 CalcStage.POST 规则
        // 3. 清理上下文
    }
}
```

建议构造时绑定静态模型和算法实例:

```java
public class ModulePostCalculator {
    private final Module module;
    private final ModuleAlgImpl moduleAlg;
    private final List<RuleMethod> postRuleMethods;

    public ModulePostCalculator(Module module, ModuleAlgImpl moduleAlg) {
        this.module = module;
        this.moduleAlg = moduleAlg;
        this.postRuleMethods = scanPostRuleMethods(module, moduleAlg);
    }

    public void doCalc(ModuleInst solution) {
        moduleAlg.bindModuleInst(solution);
        try {
            for (RuleMethod ruleMethod : postRuleMethods) {
                ruleMethod.invoke(moduleAlg);
            }
        } finally {
            moduleAlg.clearModuleInst();
        }
    }
}
```

`postRuleMethods` 应在求解前或回调初始化时扫描并缓存，避免每个解重复反射扫描。

### 3.6 求解链路集成

当前求解入口在 `ModuleConstraintExecutorImpl.runInferParas()` 中创建 `ModuleInstSolutionCallBack`:

```java
ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg);
CpSolverStatus status = solver.solve(model.getCpModel(), cb);
```

建议扩展为:

```java
ModulePostCalculator postCalculator = new ModulePostCalculator(module, alg);
ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg, postCalculator);
CpSolverStatus status = solver.solve(model.getCpModel(), cb);
```

回调执行顺序:

```text
onSolutionCallback()
  |
  |-- buildModuleInst(...)
  |-- processOtherVariables(...)
  |-- processPriorityValues(...)
  |-- postCalculator.doCalc(moduleInst)
  |-- solverResult.addSolution(moduleInst)
```

如果 POST 规则抛出异常:

- 当前推理应返回 `Result.failed(...)`。
- 错误消息必须包含 ruleCode、ruleName/methodName、solutionIndex。
- 不应静默丢弃失败的解。

### 3.7 独立后置计算模式

除求解时自动执行 POST 外，还需要提供对已有解独立执行后置计算的能力。

新增请求模型:

```java
public class ModulePostCalcReq {
    private Long moduleId;
    private String moduleCode;
    private List<ModuleInst> solutions;
}
```

新增接口:

```java
public interface ModuleConstraintExecutor {
    Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req);
}
```

行为:

1. 根据 `moduleId/moduleCode` 加载 `Module` 和生成式算法类。
2. 创建 `ModulePostCalculator`。
3. 对 `req.solutions` 逐个执行 POST。
4. 返回写入派生参数后的 `ModuleInst` 列表。

该模式用于:

- 已有解来自缓存或外部系统，只需要补算派生参数。
- 调试 POST 规则，不希望重新执行 CP 求解。
- 业务接口需要把“求解”和“派生计算”拆开调度。

### 3.8 PRE 阶段的后续扩展

PRE 的目标是对输入进行普通 Java 预处理，例如:

- 根据业务条件补齐默认输入。
- 根据多个请求项汇总出控制参数。
- 对输入做格式化、归一化或业务派生。

但当前项目已有 `CalcStage.PRE`，且已有测试使用它在模型构建前添加 CP 约束。为避免破坏现有行为，首期建议:

- `CalcStage.PRE` 保持当前执行链路不变。
- 先落地 `CalcStage.POST` 的实例级执行。
- 后续若要实现纯 Java PRE，应新增明确的执行模式字段，或将现有 PRE 约束用例迁移到新的兼容机制。

一种后续方案:

```java
public enum RuleExecMode {
    CONSTRAINT,
    IMPERATIVE
}

@CodeRuleAnno(calcStage = CalcStage.PRE, execMode = RuleExecMode.IMPERATIVE)
private void preRule() {
    // 通过 ModuleInputAccessor 修改 InferParasReq/ModuleInput
}
```

该方案不在首期强制实现。

### 3.9 代码示例

可在 `BaseOptiTest` 上简化出一个 POST 场景:

```java
@ModuleAnno(id = 123L)
static public class BaseOptiConstraint extends ConstraintAlgImplTestBase {

    @PartAnno(code = "drive")
    @DAttrAnno2(code = "Speed", options = {
            "Speed_5400:5400:转",
            "Speed_7200:7200:转"
    })
    @DAttrAnno3(code = "Capacity", options = {
            "Capacity_1T:1:T",
            "Capacity_3T:3:T"
    })
    private PartCategoryVar drive;

    @PartAnno(fatherCode = "drive", attrs = {"5400", "1"})
    private PartVar md1;

    @PartAnno(fatherCode = "drive", attrs = {"5400", "3"})
    private PartVar sd1;

    @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
    private ParaVar Sum_Quantity;

    @ParaAnno(code = "pDriveSumCapacity", type = ParaType.INTEGER,
            assignType = AssignType.INPUT)
    private ParaVar pDriveSumCapacity;

    @ParaAnno(fatherCode = "drive", code = "pSumCapacity", type = ParaType.INTEGER,
            assignType = AssignType.INPUT)
    private ParaVar pSumCapacity;

    @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "同速率硬盘数量满足输入")
    private void mainRule() {
        // 现有 CP 约束规则, 仍然属于 CalcStage.MID
    }

    @CodeRuleAnno(calcStage = CalcStage.POST,
            normalNaturalCode = "求解后计算所选硬盘总容量")
    private void postRule() {
        int driveSumCapacity = toInt(getSumDynAttr("drive", "Capacity"));
        setParaValue("pDriveSumCapacity", String.valueOf(driveSumCapacity));
        setParaValue("drive", "pSumCapacity", String.valueOf(driveSumCapacity));
    }
}
```

---

## 4. 验收准则

### 4.1 功能验收用例

新增测试建议放在 `src/test/java/com/jmix/scenario/ruletest/PostCalcRuleTest.java`。写法参考 `CalculateRuleSimpleTest`、`PartCategoryFilterEmptyTest` 和 `BaseOptiTest`: 模型定义放在测试类内部，测试方法只保留输入、打印和断言。

```java
@Slf4j
public class PostCalcRuleTest extends ModuleScenarioTestBase {

    public PostCalcRuleTest() {
        super(PostCalcConstraint.class);
    }

    @ModuleAnno(id = 123L)
    public static class PostCalcConstraint extends ConstraintAlgImplTestBase {

        @PartAnno(code = "drive")
        @DAttrAnno2(code = "Speed", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Speed_5400:5400"})
        @DAttrAnno3(code = "Capacity", dynAttrType = DynamicAttributeType.E_INT,
                options = {"Capacity_1T:1", "Capacity_3T:3"})
        private PartCategoryVar drive;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "1"})
        private PartVar md1;

        @PartAnno(fatherCode = "drive", attrs = {"5400", "3"})
        private PartVar sd1;

        @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar Sum_Quantity;

        @ParaAnno(code = "pDriveSumCapacity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveSumCapacity;

        @ParaAnno(code = "pFirstCapacity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pFirstCapacity;

        @ParaAnno(fatherCode = "drive", code = "pSumCapacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pSumCapacity;

        @CodeRuleAnno(calcStage = CalcStage.POST)
        private void postRule() {
            int sum = toInt(getSumDynAttr("drive", "Capacity"));
            setParaValue("pDriveSumCapacity", String.valueOf(sum));
            setParaValue("drive", "pSumCapacity", String.valueOf(sum));
            setParaValue("pFirstCapacity", getDynAttr("drive", "Capacity"));
        }
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
    }

    @Test
    public void testPostCalc_ModuleAndCategoryPara() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("pDriveSumCapacity(V:6,H:0)");
        assertSoluContain("pSumCapacity(V:6,H:0)");
    }

    @Test
    public void testPostCalc_GetDynAttrUseFirstPart() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1),pFirstCapacity(V:1,H:0)");
    }

    @Test
    public void testPostCalc_InputParaDoesNotJoinCpModel() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("pDriveSumCapacity(V:6,H:0)");
    }
}
```

独立后置计算接口另加一个 API 测试，保持同样的短写法:

```java
@Test
public void testStandalonePostCalculate() {
    ModulePostCalcReq req = new ModulePostCalcReq();
    req.setModuleId(123L);
    req.setSolutions(List.of(buildSolution("sd1", 2)));

    Result<List<ModuleInst>> result =
            ModuleConstraintExecutor.INST.postCalculate(req);

    setResult(result);
    setSolutions(result.getData());
    printSolutions();

    resultAssert().assertSuccess();
    assertSoluContain("pDriveSumCapacity(V:6,H:0)");
}
```

验收点:

- `CalcStage.POST` 在每个 `ModuleInst` 生成后自动执行。
- `AssignType.INPUT` 输出参数不进入 CP 模型，但能被 POST 写回。
- `getSumDynAttr("drive", "Capacity")` 按有效部件的 `quantity * Capacity` 求和。
- `getDynAttr("drive", "Capacity")` 多个有效部件时返回第一个。
- `postCalculate(ModulePostCalcReq)` 可对已有 `ModuleInst` 独立补算。

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
| --- | --- | --- |
| POST 规则列表为空 | 普通求解请求 | 不额外处理，解列表保持现有行为 |
| 派生参数不存在 | `setParaValue("unknown", "1")` | 抛出明确异常，不自动创建未知参数 |
| 分类不存在 | `getSumDynAttr("unknown", "Capacity")` | 抛出明确异常 |
| 多实例分类未指定 instId | `getDynAttr("drive", "Capacity")` 且存在多个 drive 实例 | 默认读取第一个匹配实例；精确读取使用 `getDynAttr("drive", instId, "Capacity")` |
| 多实例分类指定 instId | `getDynAttr("drive", 1, "Capacity")` | 读取 `instanceId=1` 的分类实例 |
| 有效部件为空 | `getSumDynAttr("drive", "Capacity")` | 返回 `"0"` |
| 属性缺失 | 有效部件缺少 `Capacity` | 抛出明确异常 |
| POST 修改参数后 | 同一解继续返回 | `ModuleInst` 中参数值为 POST 写入值 |

### 4.3 回归测试

- [ ] `BaseOptiTest` 现有用例解数量和排序不变。
- [ ] `EnumMultReq4MultiReqTest` 中 `CalcStage.PRE` 现有行为不变。
- [ ] `CalculateRuleSimpleTest` / `CalculateRuleIfThenTest` 默认 MID 规则行为不变。
- [ ] 冲突诊断与松弛求解返回的部分解不因 POST 规则改变严格可行语义。
- [ ] `mvn test` 可执行新增 POST 规则测试。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 确认命名: 复用 `CalcStage` 还是新增 `CalcType` | P0 | 已确认: 复用 `CalcStage` |
| 2 | 梳理 `AssignType.INPUT` 参数初始化和结果写回, 确保不创建 CP 变量 | P0 | 待开始 |
| 3 | 新增 `ModuleInstAccessor` 接口, 并由 `ModuleBaseAlgImpl` 继承实现 | P0 | 待开始 |
| 4 | 在 `ModuleBaseAlgImpl` 增加当前 `ModuleInst` 绑定与清理逻辑 | P0 | 待开始 |
| 5 | 新增 `ModulePostCalculator`, 扫描并执行 `CalcStage.POST` 规则 | P0 | 待开始 |
| 6 | 在 `ModuleInstSolutionCallBack` 中集成 POST 执行 | P0 | 待开始 |
| 7 | 新增独立 `postCalculate(ModulePostCalcReq)` 对外接口 | P1 | 待开始 |
| 8 | 补充 `BaseOptiTest` 简化验收用例 | P0 | 待开始 |
| 9 | 梳理 PRE 纯代码执行模式并决定是否另起 RFC | P2 | 已确认: 首期不做 |

---

## 6. 参考资料

- [项目核心设计文档](doc/CORE-DESIGN.md)
- [项目验收准则](doc/ACCEPTANCE.md)
- `src/main/java/com/jmix/executor/bmodel/logic/CalcStage.java`
- `src/main/java/com/jmix/tool/bbuilder/anno/CodeRuleAnno.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/ModuleInstSolutionCallBack.java`
- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`

---

## 7. 已确认问题

Q1. 命名上是否接受复用现有 `CalcStage.PRE/MID/POST`？还是必须新增用户口述中的 `CalcType.PRE/MAIN/POST`？

结论: 复用现有 `CalcStage`，不新增 `CalcType`。业务文档中的 MAIN 对应代码中的 `CalcStage.MID`。

Q2. 后置输出参数是否接受新增 `AssignType.DERIVED`？如果不新增，是否允许暂时复用 `AssignType.INPUT` 来避免创建 CP 变量？

结论: 不新增 `AssignType.DERIVED`，复用现有 `AssignType.INPUT`。本次实现需要把 INPUT 类型不创建 CP 变量、不参与约束求解的行为整理清楚，并支持 POST 写回。

Q3. `getDynAttr(partCategoryCode, attrCode)` 在匹配多个有效部件时应该抛异常，还是返回第一个有效部件的属性？

结论: 返回第一个有效部件的属性。实现需要保证遍历顺序稳定。

Q4. 多实例分类的默认语义如何处理？未传 `instId` 时是聚合全部实例、只取实例 0，还是要求必须显式传 `instId`？

结论: 增加带 `instId` 的 API 用于多实例精确访问，例如 `getDynAttr(partCategoryCode, instId, attrCode)`、`getSumDynAttr(partCategoryCode, instId, attrCode)` 和 `setParaValue(partCategoryCode, instId, paraCode, value)`。不带 `instId` 的方法保留为简单场景 API。

Q5. 首期是否只实现 POST？PRE 的纯 Java 输入预处理是否需要与 POST 同期落地？

结论: 首期只实现 POST。PRE 的纯 Java 输入预处理后续再设计。

Q6. `ModuleInstAccessor` 应独立实现，还是由现有算法基类实现？

结论: 由 `ModuleBaseAlgImpl` 继承实现 `ModuleInstAccessor`。POST 执行时将当前 `ModuleInst` 绑定到算法实例，规则方法直接调用继承来的访问方法。
