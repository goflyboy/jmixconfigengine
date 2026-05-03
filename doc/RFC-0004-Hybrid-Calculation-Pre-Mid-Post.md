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
| 访问接口 | 新增 `ModuleInstAccessorImpl` 实现, `ModuleBaseAlgImpl` 通过持有并委托调用复用这套逻辑 |
| 集成模式 | 默认在 CP-SAT 求解完成、所有解收集完毕后批量执行 POST; 也支持对已有 `ModuleInst` 独立执行 POST |
| 扩展后处理 | 删除现有 `ExtensibleProcess` / `InferParasPostProcess` 链路, 由 `ModulePostCalculator` 替代 |
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
2. `ModuleInstSolutionCallBack` 只负责构造并收集 `ModuleInst`，不执行 POST 反射规则。
3. `solver.solve(...)` 返回后，执行器取得完整解列表。
4. `ModulePostCalculator` 对解列表批量执行 `CalcStage.POST` 规则。
5. POST 规则通过 `ModuleInstAccessor` 读取当前解并写回参数。
6. POST 全部成功后返回写回派生参数的 `Result<List<ModuleInst>>`。

POST 不在 CP-SAT 回调线程内执行，原因是:

- 避免 Java 反射和业务计算阻塞求解器枚举解。
- POST 异常可以在求解后统一转换成 `Result.failed(...)`。
- POST 只修改 `ModuleInst` 结果对象，不参与 CP 模型回溯和剪枝。

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
- `AssignType.INPUT` 参数的 `ParaVar` 和 `ParaInst` 构建保持现有实现逻辑，不在本 RFC 中额外重构。
- POST 执行后通过 `setParaValue(...)` 写回已定义参数的 `ParaInst.value`。
- 如果 `setParaValue(...)` 找不到对应参数定义或实例参数，应抛出明确异常，不静默创建未知参数。
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

新增实例访问接口，并单独提供 `ModuleInstAccessorImpl` 实现。`ModuleBaseAlgImpl` 不直接承载具体实例读写逻辑，而是持有一个当前上下文的 accessor 并转发调用。这样生成式规则代码仍然写在 `ConstraintAlgImplTestBase` / `ModuleAlgImpl` 子类中，可以直接调用 `getDynAttr(...)`、`getSumDynAttr(...)`、`setParaValue(...)`，同时求解后批量执行和独立 `postCalculate(...)` 两个场景复用同一套 accessor 实现。

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

    List<String> getDynAttrValues(String partCategoryCode, String attrCode);

    List<String> getDynAttrValues(String partCategoryCode, int instId, String attrCode);

    String getSumDynAttr(String partCategoryCode, String attrCode);

    String getSumDynAttr(String partCategoryCode, int instId, String attrCode);

    int getQuantity(String partCategoryCode);

    int getQuantity(String partCategoryCode, int instId);

    List<Integer> getInstanceIds(String partCategoryCode);
}
```

```java
public class ModuleInstAccessorImpl implements ModuleInstAccessor {

    private final Module module;
    private final ModuleInst moduleInst;

    public ModuleInstAccessorImpl(Module module, ModuleInst moduleInst) {
        this.module = module;
        this.moduleInst = moduleInst;
    }

    @Override
    public String getDynAttr(String partCategoryCode, String attrCode) {
        // 从 moduleInst 读取第一个有效部件的静态动态属性
    }

    @Override
    public void setParaValue(String paraCode, String value) {
        // 写回 moduleInst 的模块级 ParaInst
    }
}
```

```java
public class ModuleBaseAlgImpl {

    private ModuleInstAccessor currentModuleInstAccessor;

    void bindModuleInstAccessor(ModuleInstAccessor accessor) {
        this.currentModuleInstAccessor = accessor;
    }

    void clearModuleInstAccessor() {
        this.currentModuleInstAccessor = null;
    }

    protected ModuleInstAccessor currentModuleInstAccessor() {
        if (currentModuleInstAccessor == null) {
            throw new AlgLoaderException(
                    "ModuleInstAccessor is not bound. This method is only available in POST context.");
        }
        return currentModuleInstAccessor;
    }

    public String getDynAttr(String partCategoryCode, String attrCode) {
        return currentModuleInstAccessor().getDynAttr(partCategoryCode, attrCode);
    }

    public void setParaValue(String paraCode, String value) {
        currentModuleInstAccessor().setParaValue(paraCode, value);
    }
}
```

语义:

- `setParaValue(String paraCode, String value)`: 写模块级参数。
- `setParaValue(String partCategoryCode, String paraCode, String value)`: 写分类参数；若分类存在多个实例，默认写第一个匹配实例。
- `setParaValue(String partCategoryCode, int instId, String paraCode, String value)`: 写指定分类实例参数。
- `getDynAttr(...)`: 读取当前解中某分类选中部件的动态属性；若匹配多个有效部件，返回稳定遍历顺序下的第一个有效部件。
- `getDynAttr(...)`: 若没有有效部件，返回 `null`。
- `getDynAttrValues(...)`: 返回匹配范围内所有有效部件的属性值列表；没有有效部件时返回空列表；列表不按 `quantity` 展开。
- `getSumDynAttr(...)`: 对当前解中有效部件的指定属性做数量加权求和。
- `getQuantity(...)`: 返回匹配范围内有效部件的数量总和；没有有效部件时返回 `0`。
- `getInstanceIds(...)`: 返回指定部件分类在当前 `ModuleInst` 中出现的实例 ID 列表，用于多实例 POST 规则遍历。
- 带 `instId` 的重载用于多实例分类的精确访问；不带 `instId` 的重载面向单实例或简单场景，多实例下默认使用第一个匹配实例。

有效部件建议定义为:

```text
partInst.isSelected == true && partInst.quantity > 0
```

为兼容历史数据，可在实现中对 `isSelected=false` 但 `quantity>0` 的部件记录警告并纳入计算。

#### 3.4.2 后置规则辅助方法

`ModuleBaseAlgImpl` 持有 `ModuleInstAccessorImpl` 后，后置规则可以继续直接调用 `getDynAttr(...)`、`getSumDynAttr(...)`、`setParaValue(...)` 这些转发方法。为减少业务规则中的重复解析逻辑，同时补充常用类型转换方法:

```java
protected int toInt(String value);

protected long toLong(String value);

protected double toDouble(String value);

protected String toString(Object value);

protected <T> T toValue(String value, Class<T> targetType);
```

约定:

- `toInt/toLong/toDouble` 遇到 `null` 或空字符串时抛出明确异常。
- `toString(Object value)` 使用 `String.valueOf(value)` 语义，`null` 转为 `"null"`。
- `toValue(String value, Class<T> targetType)` 首期支持 `String`、`Integer/int`、`Long/long`、`Double/double`，后续按需扩展。

这些方法只允许在实例计算上下文中使用。若 POST 以外阶段调用，应抛出清晰异常:

```text
ModuleInstAccessor is not bound. This method is only available in POST context.
```

### 3.5 后置计算器

新增内部组件:

```java
public class ModulePostCalculator {

    public Result<List<ModuleInst>> doCalc(List<ModuleInst> solutions) {
        // 1. 遍历所有解
        // 2. 对每个解创建 ModuleInstAccessorImpl
        // 3. 绑定到 ModuleBaseAlgImpl
        // 4. 依次执行 CalcStage.POST 规则
        // 5. 清理上下文
        // 6. 任一解失败则返回 Result.failed(...)
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

    public Result<List<ModuleInst>> doCalc(List<ModuleInst> solutions) {
        for (int i = 0; i < solutions.size(); i++) {
            ModuleInst solution = solutions.get(i);
            ModuleInstAccessor accessor = new ModuleInstAccessorImpl(module, solution);
            moduleAlg.bindModuleInstAccessor(accessor);
            try {
                for (RuleMethod ruleMethod : postRuleMethods) {
                    ruleMethod.invoke(moduleAlg);
                }
            } catch (RuntimeException ex) {
                return Result.failed("POST rule failed, solutionIndex=" + (i + 1)
                        + ", message=" + ex.getMessage());
            } finally {
                moduleAlg.clearModuleInstAccessor();
            }
        }
        return Result.success(solutions);
    }
}
```

`postRuleMethods` 应在求解前扫描并缓存，避免每个解重复反射扫描。扫描规则:

- 使用 `module.getAllRules(CalcStage.POST)` 获取模块、部件分类、子部件分类上的全部 POST 规则。
- 使用现有 `buildAllRuleMethods(module, moduleAlg)` 建立 ruleCode 到 Java 方法的映射。
- 按 `getAllRules(CalcStage.POST)` 返回顺序执行，保持现有 module -> PartCategory -> 子 PartCategory 的层级顺序。
- 多实例分类的 POST 规则不通过重复反射调用来隐式遍历实例；规则内部使用 `getInstanceIds(partCategoryCode)` 和带 `instId` 的 accessor 方法显式遍历。

### 3.6 求解链路集成

当前求解入口在 `ModuleConstraintExecutorImpl.runInferParas()` 中创建 `ModuleInstSolutionCallBack`:

```java
ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg);
CpSolverStatus status = solver.solve(model.getCpModel(), cb);
```

本 RFC 决定不改造 `ModuleInstSolutionCallBack` 来执行 POST。回调内只构建解，POST 在 `solver.solve(...)` 返回后批量执行:

```java
ModuleInstSolutionCallBack cb = new ModuleInstSolutionCallBack(module, alg);
CpSolverStatus status = solver.solve(model.getCpModel(), cb);

List<ModuleInst> solutions = cb.getSolverResult().getSolutions();
Result<List<ModuleInst>> postResult =
        new ModulePostCalculator(module, alg).doCalc(solutions);
if (postResult.getCode() != Result.SUCCESS) {
    return postResult;
}
return Result.success(postResult.getData());
```

执行顺序:

```text
runInferParas(req)
  |
  |-- initConstraintModel(...)
  |-- solver.solve(model, callback)
  |     |
  |     |-- callback.onSolutionCallback()
  |           |-- buildModuleInst(...)
  |           |-- processOtherVariables(...)
  |           |-- processPriorityValues(...)
  |           |-- solverResult.addSolution(moduleInst)
  |
  |-- solutions = callback.solverResult.solutions
  |-- postCalculator.doCalc(solutions)
  |-- return Result.success(solutionsWithPostValues)
```

如果 POST 规则抛出异常:

- 当前推理应返回 `Result.failed(...)`。
- 错误消息必须包含 ruleCode、ruleName/methodName、solutionIndex。
- 不应静默丢弃失败的解。

冲突诊断的松弛部分解首期保持现有返回逻辑，不自动执行 POST。若调用方需要对部分解补算派生参数，可以显式调用 `postCalculate(ModulePostCalcReq)`。

### 3.7 与 ExtensibleProcess 的关系

现有 `ModuleConstraintExecutorImpl` 中存在 `ExtensibleProcess` / `InferParasPostProcess` 链路:

```text
registerExtensible(ExtensibleProcess)
  -> executePostProcess(module, solutions)
  -> InferParasPostProcess.postProcess(module, solutions)
```

本 RFC 决定删除这套链路，由 `CalcStage.POST` + `ModulePostCalculator` 取代。理由:

- `ExtensibleProcess` 是执行器外部扩展点，不和规则模型、`@CodeRuleAnno`、`CalcStage` 统一。
- 现有使用较少，和本 RFC 的规则式 POST 能力重叠。
- 删除后避免出现两套后处理入口和顺序问题。

实现要求:

- 移除 `ModuleConstraintExecutor.registerExtensible/unregisterExtensible` 及实现类中的注册列表。
- 移除 `executePostProcess(...)` 和 `applyPostProcess(...)`。
- 移除或迁移 `ExtensibleProcess`、`InferParasPostProcess` 及相关 demo 测试。
- 原 `executePostProcess(module, solutions)` 的位置改为调用 `ModulePostCalculator.doCalc(solutions)`。

### 3.8 独立后置计算模式

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
3. 调用 `ModulePostCalculator.doCalc(req.solutions)` 批量执行 POST。
4. 返回写入派生参数后的 `ModuleInst` 列表。

该模式用于:

- 已有解来自缓存或外部系统，只需要补算派生参数。
- 调试 POST 规则，不希望重新执行 CP 求解。
- 业务接口需要把“求解”和“派生计算”拆开调度。

### 3.9 PRE 阶段的后续扩展

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

### 3.10 代码示例

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

        @ParaAnno(code = "pDriveQuantity", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveQuantity;

        @ParaAnno(code = "pDriveAttrCount", type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pDriveAttrCount;

        @ParaAnno(fatherCode = "drive", code = "pSumCapacity",
                type = ParaType.INTEGER, assignType = AssignType.INPUT)
        private ParaVar pSumCapacity;

        @CodeRuleAnno(calcStage = CalcStage.POST)
        private void postRule() {
            int sum = toInt(getSumDynAttr("drive", "Capacity"));
            setParaValue("pDriveSumCapacity", String.valueOf(sum));
            setParaValue("drive", "pSumCapacity", String.valueOf(sum));
            setParaValue("pFirstCapacity", getDynAttr("drive", "Capacity"));
            setParaValue("pDriveQuantity", toString(getQuantity("drive")));
            setParaValue("pDriveAttrCount", toString(getDynAttrValues("drive", "Capacity").size()));
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
        assertSoluContain("pDriveQuantity(V:2,H:0)");
    }

    @Test
    public void testPostCalc_GetDynAttrUseFirstPart() {
        inferRecommendModule("drive:Sum_Quantity ==2 where Speed=5400");
        printSolutions();

        resultAssert().assertSuccess();
        assertSoluContain("md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1),pFirstCapacity(V:1,H:0)");
        assertSoluContain("md1(Q:1,H:0,S:1),sd1(Q:1,H:0,S:1),pDriveAttrCount(V:2,H:0)");
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

- `CalcStage.POST` 在 CP-SAT 求解完成后对每个 `ModuleInst` 批量执行。
- `AssignType.INPUT` 输出参数不进入 CP 模型，但能被 POST 写回。
- `getSumDynAttr("drive", "Capacity")` 按有效部件的 `quantity * Capacity` 求和。
- `getDynAttr("drive", "Capacity")` 多个有效部件时返回第一个。
- `getDynAttrValues("drive", "Capacity")` 能返回所有有效部件属性值。
- `getQuantity("drive")` 能返回有效部件数量总和。
- `postCalculate(ModulePostCalcReq)` 可对已有 `ModuleInst` 独立补算。

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
| --- | --- | --- |
| POST 规则列表为空 | 普通求解请求 | 不额外处理，解列表保持现有行为 |
| 派生参数不存在 | `setParaValue("unknown", "1")` | 抛出明确异常，不自动创建未知参数 |
| 分类不存在 | `getSumDynAttr("unknown", "Capacity")` | 抛出明确异常 |
| 多实例分类未指定 instId | `getDynAttr("drive", "Capacity")` 且存在多个 drive 实例 | 默认读取第一个匹配实例；精确读取使用 `getDynAttr("drive", instId, "Capacity")` |
| 多实例分类指定 instId | `getDynAttr("drive", 1, "Capacity")` | 读取 `instanceId=1` 的分类实例 |
| 有效部件为空 | `getDynAttr("drive", "Capacity")` | 返回 `null` |
| 有效部件为空 | `getDynAttrValues("drive", "Capacity")` | 返回空列表 |
| 有效部件为空 | `getSumDynAttr("drive", "Capacity")` | 返回 `"0"` |
| 有效部件为空 | `getQuantity("drive")` | 返回 `0` |
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
| 3 | 新增 `ModuleInstAccessor` 接口和 `ModuleInstAccessorImpl` 实现 | P0 | 待开始 |
| 4 | 在 `ModuleBaseAlgImpl` 增加 accessor 绑定、清理和转发逻辑 | P0 | 待开始 |
| 5 | 补充 `getDynAttrValues/getQuantity/getInstanceIds` 和类型转换方法 | P0 | 待开始 |
| 6 | 新增 `ModulePostCalculator`, 使用 `module.getAllRules(CalcStage.POST)` 扫描并执行 POST 规则 | P0 | 待开始 |
| 7 | 在 `solver.solve(...)` 返回后批量执行 POST, 不在 `ModuleInstSolutionCallBack` 中执行 | P0 | 待开始 |
| 8 | 删除 `ExtensibleProcess` / `InferParasPostProcess` / `executePostProcess` 旧链路 | P0 | 待开始 |
| 9 | 新增独立 `postCalculate(ModulePostCalcReq)` 对外接口 | P1 | 待开始 |
| 10 | 补充 `PostCalcRuleTest` 简化验收用例 | P0 | 待开始 |
| 11 | 梳理 PRE 纯代码执行模式并决定是否另起 RFC | P2 | 已确认: 首期不做 |

---

## 6. 参考资料

- [项目核心设计文档](doc/CORE-DESIGN.md)
- [项目验收准则](doc/ACCEPTANCE.md)
- `src/main/java/com/jmix/executor/bmodel/logic/CalcStage.java`
- `src/main/java/com/jmix/tool/bbuilder/anno/CodeRuleAnno.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleAlgImpl.java`
- `src/main/java/com/jmix/executor/impl/ModuleInstSolutionCallBack.java`
- `src/main/java/com/jmix/executor/model/ExtensibleProcess.java`
- `src/main/java/com/jmix/executor/model/InferParasPostProcess.java`
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

结论: 增加独立的 `ModuleInstAccessorImpl` 实现 `ModuleInstAccessor`。`ModuleBaseAlgImpl` 只负责持有当前 accessor 并转发调用，这样求解后批量执行和独立 `postCalculate(...)` 两种场景可以复用同一套实例访问逻辑。

Q7. POST 应在 CP-SAT 回调内执行，还是求解完成后批量执行？

结论: 求解完成后批量执行。`ModuleInstSolutionCallBack` 只收集解，不执行 POST 反射规则。

Q8. 现有 `ExtensibleProcess` / `InferParasPostProcess` 链路如何处理？

结论: 删除旧链路，由 `CalcStage.POST` 和 `ModulePostCalculator` 统一承接后置计算。

Q9. `getDynAttr` 在没有有效部件时如何返回？

结论: `getDynAttr` 返回 `null`，`getDynAttrValues` 返回空列表，`getSumDynAttr` 返回 `"0"`，`getQuantity` 返回 `0`。

Q10. POST 规则是否需要遍历/聚合 API？

结论: 需要。新增 `getDynAttrValues(...)`、`getQuantity(...)`、`getInstanceIds(...)`。

Q11. 类型转换方法是否只保留 `toInt`？

结论: 不只保留 `toInt`。新增 `toLong`、`toDouble`、`toString(Object)`、`toValue(String, Class<T>)`。

Q12. POST 规则扫描是否递归覆盖 PartCategory 层规则？

结论: 是。使用 `module.getAllRules(CalcStage.POST)` 获取模块和所有层级 PartCategory 的 POST 规则。
