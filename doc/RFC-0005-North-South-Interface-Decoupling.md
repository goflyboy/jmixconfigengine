# RFC-0005: 计算引擎北向与南向接口解耦

> 状态: 草案(Draft)
> 日期: 2026-05-16
> 相关文档: `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`

---

## 设计决议摘要

本 RFC 提议同步重构计算引擎的南向接口和北向接口，使产品算法、应用调用方、引擎内部实现三者通过稳定契约连接，而不是直接依赖当前实现类。

| 主题 | 决议 |
| --- | --- |
| 南向接口边界 | 新增生产级 `southinf` API 层，产品算法只依赖稳定接口、允许的基础求解类型和变量 facade，不再继承或导入复杂内部实现 |
| 兼容性 | 不保留 Legacy V0 长期运行层；旧算法统一迁移/重新生成到最新南向 API。从最新 API 开始建立向后兼容契约 |
| 算法版本 | `ModuleAlgArtifact` 作为算法版本和南向 API 版本的准确信息来源；算法类注解只作为源码侧辅助声明 |
| 测试基类 | `ConstraintAlgImplTestBase` 不再作为南向接口；其中临时 `PartVar` / `ParaVar` / `PartCategoryVar` 迁移到生产 API 或测试工具层 |
| 北向接口 | 保留 `inferParas(InferParasReq)` 作为兼容入口，新增语义明确的计算、反推、校验、规则执行接口 |
| 北向入参结构 | 以 `Module -> PartCategory -> Part/Parameter` 表达层级；约束不作为同级对象，而是参数、部件或属性上的输入条件 |
| 基础配置/实例访问 | 将当前引擎可直接访问的配置定义和计算实例数据抽取为接口，隐藏 `ModuleInstAccessorImpl`、`bmodel`、`cmodel` 细节 |
| 实施节奏 | 先加新接口和适配层，再迁移测试与生成器，最后收敛旧入口和硬编码类加载逻辑 |

---

## 1. 摘要

当前计算引擎的南向接口与内部实现耦合较深：产品算法通常继承 `ModuleAlgImpl` 或测试基类 `ConstraintAlgImplTestBase`，并直接使用 `AlgCPModel`、`PartVar`、`ParaVar` 等实现类型。这样会导致引擎内部结构、变量模型或求解器封装一旦调整，历史算法很容易出现编译失败、类加载失败或运行期 `NoSuchMethodError`。

北向接口也存在语义混杂问题：`ModuleConstraintExecutor.inferParas(InferParasReq)` 最初围绕参数反推设计，后续逐渐承载部件数量计算、参数计算、组合校验、分类过滤、优先级策略等能力，导致入参层级没有清楚表达“模块 -> 部件分类(PartCategory) -> 部件/参数”的业务结构。约束不应与部件、参数处于同一层级；它是对参数值、部件选择、部件数量或属性聚合结果施加的输入条件。

本 RFC 通过新增稳定南向 API、算法版本校验、旧算法迁移策略、北向 V2 请求模型和实例访问接口抽象，实现接口与实现的完全解耦。旧算法不再保留 Legacy V0 运行通道，而是直接迁移到最新南向 API；最新 API 发布后必须承担后续兼容性要求。

---

## 2. 动机

### 2.1 问题背景

当前代码中已经能看到接口边界不清晰的迹象：

- `src/main/java/com/jmix/executor/southinf/IModuleAlg.java` 只是空标记接口，真实南向能力并没有在这里声明。
- `src/test/java/com/jmix/coretest/ConstraintAlgImplTestBase.java` 继承 `ModuleAlgImpl`，并声明了测试便利用的 `ParaVar`、`PartVar`、`PartCategoryVar`。
- `ConstraintAlgImplTestBase` 的变量包装器依赖 `com.jmix.executor.impl.algmodel` 内部变量，测试代码和业务算法很容易把这些内部类型当成正式 API。
- `ModuleAlgClassLoader` 当前硬编码加载 `com.jmix.coretest.ConstraintAlgImplTestBase` 及其内部类，算法制品与测试包名、测试基类、内部实现同时耦合。
- `ModuleConstraintExecutor` 目前主要对外暴露 `inferParas(InferParasReq)`，但这个接口已经同时承载反向推理、分类约束计算和部分后置计算能力。
- `ModuleInstAccessor` 已经是一个接口，但位于 `impl` 包下，且访问模型仍然与当前 `ModuleInst` 实现绑定。

这些问题会带来两个直接风险：

1. 引擎内部演进风险。只要内部变量、求解器封装或执行流程变化，业务算法可能被迫同步升级。
2. 应用调用语义不清。调用方无法从接口名和入参结构上判断自己是在做计算、反推还是校验。

### 2.2 具体场景

南向场景：

```java
public class ServerConstraint extends ConstraintAlgImplTestBase {
    private PartVar cpu1;
    private ParaVar size;

    @CodeRuleAnno
    private void rule1() {
        model.addGreaterOrEqual(cpu1.qty, 1);
        addCompatibleConstraintRequires("rule1", size, listOf("large"), ...);
    }
}
```

当前写法的问题是：

- 算法继承了测试基类。
- 算法可直接访问 `model` 和内部变量字段。
- `PartVar.qty`、`ParaVar.value` 等字段如果改名或抽象，历史算法会失败。

期望写法：

```java
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "server-config-2026.05")
public class ServerConstraint extends ConstraintAlgBase {
    private PartVar cpu1;
    private ParaVar size;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(cpu1.quantity(), 1);
        model().compatibilityRequire("rule1", size.option("large"), ...);
    }
}
```

期望行为：

- 产品算法只依赖稳定 API 包。
- `ConstraintAlgBase` 不暴露当前复杂执行模型。
- 少量稳定基础求解类型可以作为南向 API 的一部分；复杂求解器封装和执行流程仍由引擎隐藏。
- 历史算法通过迁移或重新生成后运行在最新南向 API 上。

北向场景：

```java
InferParasReq req = new InferParasReq();
req.setModuleId(123L);
req.setPartCategoryCode("drive");
req.setPartConstraintReqs(List.of(capacityReq));
req.setPreParaInsts(preParas);
```

当前写法的问题是：

- `InferParasReq` 名称强调“参数反推”，但调用可能是在计算部件数量或校验组合。
- `partCategoryCode` 和 `partConstraintReqs` 是后续追加字段，不能完整表达模块内多个分类、多实例、校验目标和返回目标。
- `PartConstraintReq` 把属性聚合、过滤、比较、策略混在一个类中，不利于后续扩展。

期望写法：

```java
ModuleCalcReq req = new ModuleCalcReq();
req.setModule(ModuleSelector.byId(123L));

PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
drive.addDecisionStrategy(DecisionStrategy.asc("price"));
req.addPartCategory(drive);

req.setOutput(OutputSpec.parts("drive").parameter("totalPrice"));

Result<ModuleCalcResult> result =
        ModuleConstraintExecutor.INST.calculate(req);
```

---

## 3. 设计方案

### 3.1 核心思路

本 RFC 将系统接口分为三层：

```text
应用方
  |
  | North API: calculate / infer / validate / execute
  v
计算引擎执行层
  |
  | Bridge + Adapter
  v
South API: product constraint algorithm
  |
  | Stable facade only
  v
产品算法
```

南向接口解决“产品算法如何声明规则并被引擎调用”的问题。

北向接口解决“应用方如何请求计算、反推和校验”的问题。

内部实现可以继续使用 `ModuleConstraintExecutorImpl`、`ModuleAlgImpl`、`AlgCPModel`、`ModuleInstSolutionCallBack`、`ModuleInstAccessorImpl`，但这些类型不再作为对外契约被产品算法或应用调用方直接依赖。

### 3.2 南向接口设计

#### 3.2.1 包结构

新增生产级南向 API 包：

```text
com.jmix.executor.southinf
  AlgorithmApiVersion
  AlgorithmDescriptor
  ConstraintAlgorithm
  ConstraintAlgBase
  ConstraintContext
  ConstraintModel

com.jmix.executor.southinf.view
  OntoView
  ModuleInstView
  PartCategoryInstView
  PartCategoryInstSumView
  PartInstView
  ParameterInstView
  QuantityInstView

com.jmix.executor.southinf.var
  Var
  ParaVar
  ParaOptionVar
  PartVar
  PartCategoryVar
  IntExpr
  BoolExpr
  LinearExpr

com.jmix.executor.impl.southbridge
  SouthboundApiBridge
  SouthboundLatestBridge
```

`southinf` 包只允许依赖领域模型接口、稳定变量 facade 和经过白名单确认的基础求解类型，不允许依赖 `com.jmix.executor.impl.*` 下的复杂执行实现。

#### 3.2.2 算法描述

新增算法描述对象：

```java
public final class AlgorithmDescriptor {
    private String algorithmId;
    private String algorithmVersion;
    private String southApiVersion;
}
```

新增注解：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmApiVersion {
    String southApiVersion();
    String algorithmVersion() default "";
}
```

扩展 `ModuleAlgArtifact`：

```java
public class ModuleAlgArtifact {
    private String algorithmVersion;
    private String southApiVersion;
}
```

版本解析规则：

| 输入 | 解析结果 |
| --- | --- |
| `ModuleAlgArtifact.southApiVersion` 有值 | 优先使用该版本 |
| artifact 无版本，算法类有 `@AlgorithmApiVersion` | 使用注解版本作为补充，并在加载日志中提示 artifact 信息不完整 |
| artifact 和注解都无版本 | 视为未迁移旧算法，拒绝加载并提示重新生成到最新南向 API |
| artifact 版本低于当前最新南向 API | 不走 Legacy 运行层，提示迁移/重新生成；从最新 API 发布后再建立后续兼容契约 |
| artifact 版本高于当前引擎支持版本 | 返回明确的不兼容版本错误 |

#### 3.2.3 稳定算法基类

```java
public abstract class ConstraintAlgBase implements ConstraintAlgorithm {
    private ConstraintContext context;

    @Override
    public final void bind(ConstraintContext context) {
        this.context = context;
        afterBind();
    }

    protected void afterBind() {
    }

    protected ConstraintModel model() {
        return context.model();
    }

    protected ParaVar para(String code) {
        return context.vars().para(code);
    }

    protected PartVar part(String code) {
        return context.vars().part(code);
    }

    protected PartCategoryVar partCategory(String code) {
        return context.vars().partCategory(code);
    }

    protected List<String> listOf(String... codes) {
        return Arrays.asList(codes);
    }
}
```

算法生命周期：

```java
public interface ConstraintAlgorithm {
    AlgorithmDescriptor descriptor();
    void bind(ConstraintContext context);
}
```

规则方法仍可通过现有注解扫描和反射执行，降低迁移成本。

#### 3.2.4 变量 facade 与基础求解类型

最新南向 API 不要求完全隐藏 OR-Tools。设计原则是：

- 可以暴露少量稳定、低层但基础的求解类型，例如 `IntVar`、`BoolVar` 或引擎定义的 `IntExpr`、`BoolExpr`。
- 不暴露复杂执行对象，例如 `AlgCPModel`、`CpModel`、`CpSolver`、`ModuleAlgImpl`、`ModuleConstraintExecutorImpl`。
- 业务算法优先使用引擎 facade；只有表达复杂规则时才直接使用允许列表内的基础类型。
- 基础类型一旦进入南向 API，就必须纳入接口兼容性管理。

推荐的变量 facade：

```java
public interface ParaVar extends Var {
    IntExpr value();
    BoolExpr hidden();
    ParaOptionVar option(String code);
    Integer inputValue();
    boolean hasInput();
}

public interface PartVar extends Var {
    IntExpr quantity();
    BoolExpr selected();
    BoolExpr hidden();
    int attrAsInt(String attrCode);
    String attr(String attrCode);
}

public interface PartCategoryVar extends PartVar {
    ParaVar sumPara(String attrCode);
    ParaVar sumSumPara(String attrCode);
}
```

`ConstraintModel` 暴露稳定约束能力。它可以内部持有 OR-Tools 对象，也可以对少量基础类型提供桥接方法，但不把完整模型生命周期暴露给业务算法：

```java
public interface ConstraintModel {
    ConstraintRef equal(IntExpr left, long right);
    ConstraintRef greaterOrEqual(IntExpr left, long right);
    ConstraintRef lessOrEqual(IntExpr left, long right);
    ConstraintRef implication(BoolExpr left, BoolExpr right);
    ConstraintRef exactlyOne(Collection<BoolExpr> expressions);
    ConstraintRef compatibilityRequire(String ruleCode, BoolExpr left, BoolExpr right);
    ConstraintRef compatibilityIncompatible(String ruleCode, BoolExpr left, BoolExpr right);
    ConstraintRef compatibilityCoDependent(String ruleCode, BoolExpr left, BoolExpr right);
    LinearExpr linearExpr(String name);
    void minimize(LinearExpr expr);
}
```

兼容性关系属于约束模型的一种表达，不单独暴露 `compatibility()` 入口。内部实现通过 adapter 将南向 API 的变量和表达式映射到 `AlgCPModel` 和 OR-Tools。

#### 3.2.5 旧算法迁移策略

本 RFC 不保留 Legacy V0 运行层。设计原则如下：

- 当前依赖 `ConstraintAlgImplTestBase`、`ModuleAlgImpl`、`PartVar.qty`、`ParaVar.value` 的历史算法必须迁移或重新生成到最新南向 API。
- 引擎加载算法时以 `ModuleAlgArtifact.southApiVersion` 为准；缺失版本信息视为未迁移算法。
- 未迁移算法不进入求解流程，返回明确错误，提示算法制品需要重新生成或升级。
- 从最新南向 API 开始，才建立“接口变化不破坏旧算法”的兼容承诺。
- 最新 API 后续只能以新增默认方法、新增 facade、新增能力探测的方式演进，不能删除或改变既有方法语义。

类加载策略：

```text
ModuleAlgClassLoader
  |
  |-- read AlgorithmDescriptor
  |
  |-- latest supported -> SouthboundLatestBridge
  |
  |-- missing/old/unsupported -> Result.failed with migration/version message
```

当前硬编码常量需要逐步下线：

```java
ModuleAlgArtifact.ALG_PACKAGE
ModuleAlgArtifact.ALG_BASE_CLASS
ModuleAlgArtifact.ALG_PART_CLASS
ModuleAlgArtifact.ALG_PARA_CLASS
```

过渡期可以保留这些常量用于迁移工具或旧测试识别，但不能作为新运行时加载路径。

#### 3.2.6 `ConstraintAlgImplTestBase` 处理

`ConstraintAlgImplTestBase` 的目标状态：

- 不再是产品算法继承的接口。
- 不再包含正式南向变量定义。
- 测试代码改为继承生产级 `ConstraintAlgBase`，或使用专门的 `ConstraintAlgTestSupport`。
- `PartVar`、`ParaVar`、`PartCategoryVar` 的生产形态迁移到 `southinf.var`。
- 若某些测试需要快速字段注入，则由 `ConstraintAlgTestSupport` 负责，不污染南向 API。

迁移前后：

```java
// before
public class MyConstraint extends ConstraintAlgImplTestBase {
    private PartVar pt1;
}

// after
public class MyConstraint extends ConstraintAlgBase {
    private PartVar pt1;
}
```

测试工具仍可保留注解驱动建模能力，但建模后的算法运行必须经过正式南向接口。

#### 3.2.7 南向接口详细清单

南向接口面向“产品算法开发者”。产品算法只关心模块、PartCategory、Part、参数、规则执行上下文，不关心当前引擎内部如何创建 CP 模型、如何调用 OR-Tools、如何枚举解。

| 接口组 | 主要类型 | 说明 |
| --- | --- | --- |
| 算法入口 | `ConstraintAlgorithm`, `ConstraintAlgBase` | 产品算法继承/实现的入口 |
| 上下文 | `ConstraintContext` | 引擎注入给算法的运行上下文 |
| 变量访问 | `ConstraintVarRegistry` | 按 code 获取参数、Part、PartCategory 变量 |
| 变量 facade | `ParaVar`, `PartVar`, `PartCategoryVar` | 业务规则操作的稳定变量对象 |
| 模型接口 | `ConstraintModel`, `ConstraintRef` | 添加等式、不等式、蕴含、兼容关系、目标函数等约束 |
| 规则执行/实例访问 | `ModuleInstView`, `OntoView`, `PartCategoryInstSumView` | POST/execute 阶段以单个配置解为入口读取实例并按对象写回 |

##### 3.2.7.1 算法入口

```java
public interface ConstraintAlgorithm {
    AlgorithmDescriptor descriptor();
    void bind(ConstraintContext context);
}

public abstract class ConstraintAlgBase implements ConstraintAlgorithm, ModuleInstView {
    protected ConstraintModel model();
    protected ModuleInstView moduleInst();

    protected ParaVar para(String code);
    protected PartVar part(String code);
    protected PartCategoryVar partCategory(String code);

    protected List<String> listOf(String... codes);
}
```

使用方式：

```java
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "server-2026.05")
public class ServerConstraint extends ConstraintAlgBase {
    private PartCategoryVar drive;
    private PartVar ssd1;
    private ParaVar diskType;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(ssd1.quantity(), 1);
        model().compatibilityRequire("rule1", diskType.option("ssd"), ssd1.selected());
    }

    @CodeRuleAnno
    private void rule2() {
        PartCategoryInstSumView drive = partCategorySum("drive");
        parameter("totalCapacity").setValue(String.valueOf(drive.sumDynAttr4Int("Capacity")));
    }
}
```

##### 3.2.7.2 上下文接口

```java
public interface ConstraintContext {
    AlgorithmDescriptor descriptor();
    ConstraintModel model();
    ConstraintVarRegistry vars();
    ModuleInstView moduleInst();
    ConstraintCapabilities capabilities();
}
```

`ConstraintContext` 是南向 API 的统一入口。算法不直接持有 `ModuleAlgImpl`、`AlgCPModel`、`ModuleInstAccessorImpl`。`moduleInst()` 只在 POST/execute 等已经存在单个配置解的阶段可用；`ConstraintAlgBase` 实现 `ModuleInstView` 并将相关方法转发给当前配置解，使规则方法可以直接调用 `parameter(...)`、`partCategory(...)`、`partCategorySum(...)`。

##### 3.2.7.3 变量注册表

```java
public interface ConstraintVarRegistry {
    ParaVar para(String code);
    PartVar part(String code);
    PartCategoryVar partCategory(String code);

    List<ParaVar> paras();
    List<PartVar> parts();
    List<PartVar> partsOf(String partCategoryCode);
}
```

##### 3.2.7.4 变量接口

```java
public interface Var {
    String code();
    String name();
}

public interface ParaVar extends Var {
    IntExpr value();
    BoolExpr hidden();
    ParaOptionVar option(String optionCode);
    Integer inputValue();
    boolean hasInput();
}

public interface ParaOptionVar extends Var {
    BoolExpr selected();
}

public interface PartVar extends Var {
    IntExpr quantity();
    BoolExpr selected();
    BoolExpr hidden();
    String fatherCode();
    String attr(String attrCode);
    int attrAsInt(String attrCode);
}

public interface PartCategoryVar extends Var {
    List<PartVar> parts();
    List<PartVar> parts(String filterCondition);
    ParaVar sumPara(String attrCode);
    ParaVar sumSumPara(String attrCode);
}
```

##### 3.2.7.5 模型和约束接口

```java
public interface ConstraintModel {
    ConstraintRef equal(IntExpr left, long right);
    ConstraintRef equal(IntExpr left, IntExpr right);
    ConstraintRef greaterOrEqual(IntExpr left, long right);
    ConstraintRef greaterOrEqual(IntExpr left, IntExpr right);
    ConstraintRef lessOrEqual(IntExpr left, long right);
    ConstraintRef implication(BoolExpr left, BoolExpr right);
    ConstraintRef exactlyOne(Collection<BoolExpr> expressions);
    ConstraintRef compatibilityRequire(String ruleCode, BoolExpr left, BoolExpr right);
    ConstraintRef compatibilityIncompatible(String ruleCode, BoolExpr left, BoolExpr right);
    ConstraintRef compatibilityCoDependent(String ruleCode, BoolExpr left, BoolExpr right);
    ConstraintRef compatibilityRequire(String ruleCode, String leftPartExpr, String rightPartExpr);
    ConstraintRef compatibilityIncompatible(String ruleCode, String leftPartExpr, String rightPartExpr);
    LinearExpr linearExpr(String name);
    void minimize(LinearExpr expr);
    void maximize(LinearExpr expr);
}

public interface ConstraintRef {
    ConstraintRef onlyIf(BoolExpr condition);
    ConstraintRef withRuleCode(String ruleCode);
}
```

这里的 `IntExpr`、`BoolExpr`、`LinearExpr` 可以是引擎 facade，也可以桥接到允许列表内的基础求解类型。复杂对象如 `CpModel`、`CpSolver` 不进入南向接口。

兼容性规则不再作为独立 API 出现在 `ConstraintContext` 上。调用方统一通过 `model().compatibilityRequire(...)`、`model().compatibilityIncompatible(...)`、`model().compatibilityCoDependent(...)` 表达兼容关系，这样语义上仍然是在给约束模型追加规则。

迁移示例：

```java
// before
model().greaterOrEqual(cpu1.quantity(), 1);
compatibility().requires("rule1", size.option("large"), cpu1.selected());

// after
model().greaterOrEqual(cpu1.quantity(), 1);
model().compatibilityRequire("rule1", size.option("large"), cpu1.selected());
```

##### 3.2.7.6 单个配置解访问接口

POST/execute 规则不再使用脱离领域对象的执行上下文。规则面对的是一个单个配置解：`ModuleInstView`。`ConstraintAlgBase` 实现 `ModuleInstView`，因此产品规则可以直接从模块实例出发，继续进入 PartCategory、Part、Parameter。

```java
public interface OntoView {
    String code();
    String extAttr(String extAttrKey);
    int extAttr4Int(String extAttrKey);

    String dynAttr(String dynAttrKey);
    int dynAttr4Int(String dynAttrKey);
    void setDynAttr(String dynAttrKey, String dynAttrValue);
}

public interface ModuleInstView extends OntoView {
    Long moduleId();
    String instanceConfigId();
    int quantity();

    ParameterInstView parameter(String code);
    PartInstView part(String code);

    PartCategoryInstView partCategory(String code);
    PartCategoryInstView partCategory(String code, int instId);
    PartCategoryInstSumView partCategorySum(String code);
}

public interface QuantityInstView extends OntoView {
    int quantity();
    void setQuantity(int quantity);
}

public interface PartCategoryInstView extends QuantityInstView {
    int instanceId();

    ParameterInstView parameter(String code);
    PartInstView part(String code);
    List<PartInstView> parts();
}

public interface PartCategoryInstSumView extends OntoView {
    PartCategoryInstView inst(int instId);
    List<PartCategoryInstView> insts();

    String sumDynAttr(String dynAttrKey);
    int sumDynAttr4Int(String dynAttrKey);

    List<String> dynAttrs(String dynAttrKey);
    List<Integer> dynAttrs4Int(String dynAttrKey);
}

public interface PartInstView extends QuantityInstView {
    boolean selected();
}

public interface ParameterInstView extends OntoView {
    String value();
    void setValue(String value);
}
```

属性读写边界：

- `extAttr/extAttr4Int` 读取基础扩展属性，只读，不提供修改方法。
- `dynAttr/dynAttr4Int` 读取当前实例动态属性；`setDynAttr` 允许规则写回动态属性值。
- `ParameterInstView.setValue` 写参数值。
- `QuantityInstView.setQuantity` 写 Part 或 PartCategory 实例数量。
- 不允许通过 view 替换 `ModuleInst`、`PartCategoryInst`、`PartInst` 的对象结构；只能通过明确方法修改值。

POST 规则迁移示例：

```java
// before
@CodeRuleAnno(calcStage = CalcStage.POST)
private void postRule() {
    int sum = toInt(getSumDynAttr("drive", "Capacity"));
    setParaValue("pDriveSumCapacity", String.valueOf(sum));
    setParaValue("drive", "pSumCapacity", String.valueOf(sum));
    setParaValue("pFirstCapacity", getDynAttr("drive", "Capacity"));
    setParaValue("pDriveQuantity", toString(getQuantity("drive")));
    setParaValue("pDriveAttrCount", toString(getDynAttrValues("drive", "Capacity").size()));
}

// after
@CodeRuleAnno(calcStage = CalcStage.POST)
private void postRule() {
    PartCategoryInstSumView drive = partCategorySum("drive");
    int sum = drive.sumDynAttr4Int("Capacity");

    parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
    drive.inst(0).parameter("pSumCapacity").setValue(String.valueOf(sum));
    parameter("pFirstCapacity").setValue(drive.inst(0).dynAttr("Capacity"));
    parameter("pDriveQuantity").setValue(toString(drive.inst(0).quantity()));
    parameter("pDriveAttrCount").setValue(toString(drive.dynAttrs("Capacity").size()));
}
```

### 3.3 北向接口设计

#### 3.3.1 新增接口

保留现有入口：

```java
Result<List<ModuleInst>> inferParas(InferParasReq req);
Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req);
```

新增 V2 入口：

```java
public interface ModuleConstraintExecutor {
    Result<ModuleCalcResult> calculate(ModuleCalcReq req);
    Result<ReverseInferenceResult> infer(ReverseInferenceReq req);
    Result<CombinationValidationResult> validate(CombinationValidationReq req);
    Result<RuleExecuteResult> execute(RuleExecuteReq req);
}
```

接口语义：

| 接口 | 语义 | 典型问题 |
| --- | --- | --- |
| `calculate` | 计算配置结果 | 某个部件数量是多少，某个参数值是多少 |
| `infer` | 根据结果反推参数 | 已知部件数量，反推参数设定 |
| `validate` | 校验给定组合是否可行 | 部件1 + 部件2 是否 OK，是否满足兼容关系 |
| `execute` | 执行一段规则 | 对已有上下文执行某个规则，读取定义/实例并按授权写回结果 |

北向接口按领域对象组织，整体接口清单如下：

| 领域对象 | 请求/返回对象 | 核心职责 |
| --- | --- | --- |
| Module | `ModuleRequest`, `ModuleSelector`, `ModuleResult` | 标识模块、承载模块级参数和输出 |
| PartCategory | `PartCategoryRequest`, `PartCategoryOutputRef`, `PartCategoryResult` | 承载分类级输入、多实例、属性聚合、分类输出 |
| Part | `PartRequest`, `PartRequirement`, `PartOutputRef`, `PartResult` | 承载部件选择、数量、部件级条件和输出 |
| Parameter | `ModuleParameterInput`, `PartCategoryParameterInput`, `ParameterOutputRef` | 承载模块级/分类级参数输入和输出 |
| Rule | `RuleExecuteReq`, `RuleSelector`, `RuleExecuteResult` | 执行一段规则 |

#### 3.3.2 北向领域对象结构

北向接口必须从领域对象视角组织入参，而不是从求解器实现或某一次推理算法的临时字段出发。统一结构如下：

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
  |-- OutputSpec
  |-- SolveOptions
```

核心原则：

- `Module` 是请求根，标识本次调用针对哪个模块。
- `PartCategory` 是模块下的分类域，承载分类级过滤、属性聚合输入、多实例输入和决策策略。
- `Part` 是 PartCategory 下的具体部件，承载部件选择、数量、属性条件等输入。
- `Parameter` 可挂在 Module 级或 PartCategory 级。
- Requirement / Condition 是输入条件，不与 Module、PartCategory、Part、Parameter 并列。

```java
public final class ModuleSelector {
    private Long moduleId;
    private String moduleCode;
    private String moduleVersion;
}
```

#### 3.3.3 Module 级请求

```java
public abstract class ModuleRequest {
    private ModuleSelector module;
    private List<ModuleParameterInput> parameters;
    private List<ModuleRequirement> requirements;
    private List<PartCategoryRequest> partCategories;
    private OutputSpec output;
    private SolveOptions options;
}
```

```java
public class ModuleParameterInput {
    private String parameterCode;
    private String value;
    private ComparatorOp comparator;   // EQ by default
}

public class ModuleRequirement {
    private String targetCode;         // parameter code, partCategory code, or named output
    private RequirementTargetType targetType;
    private ComparatorOp comparator;
    private String value;
}
```

#### 3.3.4 PartCategory 级请求

北向接口直接使用 `PartCategory` 命名，与当前领域模型保持一致。

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
```

```java
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

旧 `PartConstraintReq` 映射到 `PartCategoryRequest.attributeRequirements`：

| 旧字段 | 新字段 |
| --- | --- |
| `partCategoryCode` | `PartCategoryRequest.partCategoryCode` |
| `attrType` | `AttributeRequirement.aggType` |
| `attrCode` | `AttributeRequirement.attrCode` |
| `attrComparator` | `AttributeRequirement.comparator` |
| `attrValue` | `AttributeRequirement.value` |
| `attrWhereCondition` | `AttributeRequirement.where` |
| `decisionStrategies` | `PartCategoryRequest.decisionStrategies` |

`InferPartCategoryReq` 变为内部适配对象，不再作为北向主模型。

#### 3.3.5 Part 级请求

```java
public class PartRequest {
    private String partCode;
    private PartSelection selection;       // REQUIRED, FORBIDDEN, OPTIONAL
    private Integer quantity;
    private List<PartRequirement> requirements;
}
```

```java
public class PartRequirement {
    private PartRequirementType type;      // QUANTITY, SELECTED, ATTRIBUTE
    private String attrCode;
    private ComparatorOp comparator;
    private String value;
}
```

示例：

```java
PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addPart(new PartRequest("ssd1", PartSelection.REQUIRED, 2));
drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8");
```

#### 3.3.6 输出结构

```java
public class OutputSpec {
    private List<ModuleOutputRef> moduleOutputs;
    private List<PartCategoryOutputRef> partCategoryOutputs;
    private List<PartOutputRef> partOutputs;
    private List<ParameterOutputRef> parameterOutputs;
}
```

```java
public class PartCategoryOutputRef {
    private String partCategoryCode;
    private Integer instanceId;
    private OutputMetric metric; // QUANTITY, SELECTED_PARTS, ATTR_SUM, PARAMETER
    private String code;         // attrCode or parameterCode
}
```

#### 3.3.7 计算请求

```java
public class ModuleCalcReq extends ModuleRequest {
}
```

示例：

```java
ModuleCalcReq req = new ModuleCalcReq();
req.setModule(ModuleSelector.byCode("Server"));
PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
req.addPartCategory(drive);
req.output().partQuantity("drive").parameter("totalPrice");
```

#### 3.3.8 反向推理请求

```java
public class ReverseInferenceReq extends ModuleRequest {
    private List<ParameterOutputRef> inferParameters;
}
```

示例：

```java
ReverseInferenceReq req = new ReverseInferenceReq();
req.setModule(ModuleSelector.byId(123L));
PartCategoryRequest drive = new PartCategoryRequest();
drive.setPartCategoryCode("drive");
drive.addPart(new PartRequest("ssd1", PartSelection.REQUIRED, 2));
req.addPartCategory(drive);
req.inferParameter("diskType");
```

#### 3.3.9 组合校验请求

```java
public class CombinationValidationReq extends ModuleRequest {
    private boolean explainConflicts;
}
```

返回：

```java
public class CombinationValidationResult {
    private boolean valid;
    private List<DiagnosticConstraint> diagnostics;
    private List<String> violatedRuleCodes;
    private List<ModuleInst> repairedCandidates;
}
```

校验可复用现有 relax 诊断能力：

- `valid=true`: 组合满足硬约束。
- `valid=false`: 返回冲突规则和错误说明。
- `explainConflicts=true`: 可在内部启用松弛诊断，返回可解释冲突。

#### 3.3.10 execute 规则请求

```java
public class RuleExecuteReq extends ModuleRequest {
    private RuleSelector rule;
    private RuleExecutionMode mode;
    private ModuleInstInput solutionInput;
}
```

```java
public class RuleSelector {
    private String ruleCode;
    private String ruleGroup;
    private CalcStage stage;
}
```

```java
public enum RuleExecutionMode {
    BEFORE_SOLVE,
    AFTER_SOLVE,
    STANDALONE
}
```

`execute` 的调用视角仍然是领域对象：

- `ModuleRequest.module` 指定模块。
- `ModuleRequest.partCategories` 指定规则执行所需的 PartCategory / Part 输入。
- `ModuleInstInput` 可传入已有单个配置解，供规则读取实例。
- `RuleSelector` 指定要执行哪一段规则。

返回：

```java
public class RuleExecuteResult {
    private ModuleResult moduleResult;
    private List<RuleExecutionMessage> messages;
    private Map<String, Object> ext;
}
```

#### 3.3.11 北向接口完整清单

```java
public interface ModuleConstraintExecutor {
    Result<Void> init(ConstraintConfig config);
    Result<Void> fini();
    Result<Void> addModule(Long rootModuleId, Module... modules);
    Result<Void> removeModule(Long moduleId);

    Result<ModuleCalcResult> calculate(ModuleCalcReq req);
    Result<ReverseInferenceResult> infer(ReverseInferenceReq req);
    Result<CombinationValidationResult> validate(CombinationValidationReq req);
    Result<RuleExecuteResult> execute(RuleExecuteReq req);

    @Deprecated
    Result<List<ModuleInst>> inferParas(InferParasReq req);

    @Deprecated
    Result<List<ModuleInst>> postCalculate(ModulePostCalcReq req);
}
```

新北向接口与领域对象的对应关系：

| 领域对象 | 北向接口对象 | 说明 |
| --- | --- | --- |
| Module | `ModuleRequest`, `ModuleSelector`, `ModuleParameterInput` | 请求根和模块级参数输入 |
| PartCategory | `PartCategoryRequest`, `PartCategoryRequirement`, `AttributeRequirement` | 分类级输入、属性聚合输入、多实例输入 |
| Part | `PartRequest`, `PartRequirement` | 部件选择、数量、属性条件 |
| Parameter | `ModuleParameterInput`, `PartCategoryParameterInput`, `ParameterOutputRef` | 模块级或分类级参数 |
| Rule | `RuleExecuteReq`, `RuleSelector`, `RuleExecuteResult` | 执行一段规则 |

### 3.4 基础配置与实例访问接口抽取

这里说的“定义只读 view、实例 view、execute 规则接口”，含义如下：

- 定义只读 view：业务规则可以读取模块、PartCategory、Part、Parameter 的定义数据，例如部件编码、父分类、价格、动态属性定义，但不能修改这些定义。
- 实例 view：业务规则可以读取本次求解出的单个配置解，例如某个 PartCategory 下选中了哪些 Part、数量是多少、动态属性值是什么；规则只能通过 `setValue`、`setQuantity`、`setDynAttr` 这类明确方法写值，不能直接改 `cmodel` 实例结构。
- execute 规则接口：执行一段业务规则。它不是“写参数接口”，而是一个规则执行入口；规则执行过程中可以读取定义和单个配置解，并按接口允许的能力写回结果。

换句话说，这一节要解决的是：当前 PATCFG 等引擎可直接访问的数据不要绑定在 `Module`、`PartCategory`、`Part`、`ModuleInst`、`PartInst` 的具体实现类上，而是通过一组稳定领域接口访问。这样内部数据结构升级时，业务规则仍然面对同一套读写契约。

当前 `ModuleInstAccessor` 已经抽取了部分访问能力，但仍位于 `impl` 包下，且实现直接绑定 `Module` / `ModuleInst`。本 RFC 建议新增公共访问接口：

```text
com.jmix.executor.api.view
  ModuleDefinitionView
  PartCategoryDefinitionView
  PartDefinitionView
  ParameterDefinitionView

com.jmix.executor.southinf.view
  OntoView
  ModuleInstView
  PartCategoryInstView
  PartCategoryInstSumView
  PartInstView
  ParameterInstView
  QuantityInstView

com.jmix.executor.southinf
  ModuleInstAccessor
```

实例接口与现有数据对象的关系：

| 当前数据对象 | 新接口 | 说明 |
| --- | --- | --- |
| `ModuleInst` | `ModuleInstView` | 单个配置解的稳定访问入口 |
| `PartCategoryInst` | `PartCategoryInstView` | 配置解中的 PartCategory 实例 |
| 多实例 `PartCategoryInst` | `PartCategoryInstSumView` | 同一个 PartCategory 多实例的汇总视图 |
| `PartInst` | `PartInstView` | 配置解中的 Part 实例 |
| `ParaInst` | `ParameterInstView` | 配置解中的参数值 |

```java
public interface ModuleInstAccessor {
    ModuleDefinitionView moduleDefinition();
    ModuleInstView moduleInst();
}
```

`ModuleInstAccessor` 是公共访问契约，`ModuleInstAccessorImpl` 只是内部适配器。适配器可以继续包装当前 `cmodel.ModuleInst`，但业务规则和 PATCFG 这类访问方只能依赖 `ModuleInstView` 及其下钻对象。

execute 规则使用：

```java
@CodeRuleAnno
private void calcPrice() {
    PartCategoryInstSumView drive = partCategorySum("drive");
    int driveCapacity = drive.sumDynAttr4Int("Capacity");
    parameter("driveCapacity").setValue(String.valueOf(driveCapacity));
}
```

内部实现：

```text
ModuleInstAccessorImpl
  implements ModuleInstAccessor
  wraps ModuleDefinitionView + ModuleInstView internally
```

对业务侧的约束：

- 不允许直接使用 `ModuleInstAccessorImpl`。
- 不允许直接读取 `Module`、`PartCategory`、`Part`、`ModuleInst`、`PartCategoryInst`、`PartInst` 的可变实现对象。
- 只允许通过 view/accessor 接口读取定义与实例。

首期建议只开放以下最小能力：

| 能力 | 示例 | 是否允许修改 |
| --- | --- | --- |
| 读取定义数据 | 读取部件价格、动态属性、父分类 | 否 |
| 读取实例数据 | 读取选中部件、数量、实例 id、扩展属性、动态属性值 | 否 |
| 写参数值 | `parameter("p1").setValue("10")` | 是 |
| 写实例动态属性 | `partCategory("drive").setDynAttr("Capacity", "8")` | 是 |
| 写数量 | `part("ssd1").setQuantity(2)` | 是 |
| 执行规则 | `execute` 执行一段价格、容量、校验或派生计算规则 | 按规则阶段授权 |

execute 规则接口的写能力需要单独定义权限边界。首期可以开放参数值、动态属性值、数量三类明确写入；选择状态、错误信息、诊断信息等能力需要另行设计专门接口，避免通过通用 view 暗中修改。

### 3.5 兼容适配路径

旧北向入口适配新模型：

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

旧南向算法迁移到最新 API：

```text
Old algorithm source/artifact
  |
  v
Migration or regeneration
  |
  v
Latest southinf API artifact
```

新南向算法：

```text
Latest API algorithm jar
  |
  v
SouthboundLatestBridge
  |
  v
Current or future engine implementation
```

---

## 4. 验收准则

### AC-001: 旧南向算法迁移后等价运行

目的：验证基于老版本南向接口开发的算法迁移或重新生成到最新南向 API 后，在新引擎中结果等价。

测试数据：

```java
public class MigratedConstraint extends ConstraintAlgBase {
    private PartVar part1;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(part1.quantity(), 1);
    }
}
```

验证逻辑：

```java
@Test
void migratedAlgorithmCanRunOnNewEngine() {
    Module module = buildModule(MigratedConstraint.class);
    ModuleConstraintExecutor.INST.addModule(module.getId(), module);

    InferParasReq req = new InferParasReq();
    req.setModuleId(module.getId());

    Result<List<ModuleInst>> result = ModuleConstraintExecutor.INST.inferParas(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertFalse(result.getData().isEmpty());
}
```

预期：

- 迁移后的算法可以正常加载和求解。
- 求解结果与迁移前基准结果一致。
- 迁移后的算法不依赖 `ConstraintAlgImplTestBase`。

### AC-001B: 未迁移旧算法拒绝加载

目的：验证缺少 `ModuleAlgArtifact.southApiVersion` 的旧算法不会悄悄按旧实现运行。

验证逻辑：

```java
@Test
void oldAlgorithmWithoutApiVersionMustBeRejected() {
    Module module = buildOldModuleWithoutSouthApiVersion();
    Result<Void> result = ModuleConstraintExecutor.INST.addModule(module.getId(), module);

    assertEquals(Result.FAILED, result.getCode());
    assertTrue(result.getMessage().contains("southApiVersion"));
    assertTrue(result.getMessage().contains("regenerate"));
}
```

### AC-002: 新南向算法不依赖复杂内部实现

目的：验证新算法只依赖 `southinf` API 和允许列表内的基础求解类型。

验证逻辑：

```java
@Test
void newAlgorithmMustNotImportImplPackages() {
    List<String> imports = scanImports("src/test/java/.../NewConstraint.java");

    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.executor.impl.")));
    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.coretest.")));
}
```

预期：

- 新算法允许导入 `com.jmix.executor.southinf.*`。
- 新算法不允许导入 `com.jmix.executor.impl.*`。
- 新算法不允许继承 `ConstraintAlgImplTestBase`。
- 新算法如需直接使用基础求解类型，必须来自南向 API 的允许列表。

### AC-003: 算法版本可识别

目的：验证引擎能按 `ModuleAlgArtifact` 的版本信息加载或拒绝算法。

测试用例：

| ID | 输入 | 预期 |
| --- | --- | --- |
| AC-003-1 | artifact `southApiVersion=latest` | 使用 `SouthboundLatestBridge` |
| AC-003-2 | artifact 无版本，类无注解 | 拒绝加载，并提示重新生成算法制品 |
| AC-003-3 | artifact `southApiVersion=99.0` | 返回明确的不兼容版本错误 |

### AC-004: 北向计算部件数量和参数值

目的：验证 `calculate` 可以表达配置计算。

验证逻辑：

```java
@Test
void calculatePartQuantityAndParameterValue() {
    ModuleCalcReq req = new ModuleCalcReq();
    req.setModule(ModuleSelector.byCode("Server"));

    PartCategoryRequest drive = new PartCategoryRequest();
    drive.setPartCategoryCode("drive");
    drive.addAttributeRequirement(AttrAggType.SUM, "Capacity", ComparatorOp.GE, "8", "Speed=7200");
    req.addPartCategory(drive);

    req.output().partQuantity("drive").parameter("totalPrice");

    Result<ModuleCalcResult> result = ModuleConstraintExecutor.INST.calculate(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertNotNull(result.getData().getPartQuantity("drive"));
    assertNotNull(result.getData().getParameterValue("totalPrice"));
}
```

### AC-005: 北向反向推理

目的：验证 `infer` 可以根据结果反推出参数。

验证逻辑：

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

    Result<ReverseInferenceResult> result = ModuleConstraintExecutor.INST.infer(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertTrue(result.getData().hasParameter("diskType"));
}
```

### AC-006: 北向组合校验

目的：验证 `validate` 可以校验给定部件组合是否满足兼容关系。

验证逻辑：

```java
@Test
void validatePartCombination() {
    CombinationValidationReq req = new CombinationValidationReq();
    req.setModule(ModuleSelector.byCode("Server"));

    PartCategoryRequest cpu = new PartCategoryRequest();
    cpu.setPartCategoryCode("cpu");
    cpu.addPart(new PartRequest("cpu_i9", PartSelection.REQUIRED, 1));
    req.addPartCategory(cpu);

    PartCategoryRequest cooler = new PartCategoryRequest();
    cooler.setPartCategoryCode("cooler");
    cooler.addPart(new PartRequest("air_cooler", PartSelection.REQUIRED, 1));
    req.addPartCategory(cooler);

    req.setExplainConflicts(true);

    Result<CombinationValidationResult> result = ModuleConstraintExecutor.INST.validate(req);

    assertEquals(Result.SUCCESS, result.getCode());
    assertFalse(result.getData().isValid());
    assertFalse(result.getData().getViolatedRuleCodes().isEmpty());
}
```

### AC-007: 北向层级结构明确

目的：验证请求中同一模块可以同时表达多个 PartCategory 的输入。

测试用例：

| ID | 输入 | 预期 |
| --- | --- | --- |
| AC-007-1 | module=Server, partCategory=cpu + drive | 两个分类都进入求解 |
| AC-007-2 | partCategory=drive, instanceId=1/2 | 多实例分类被正确拆分 |
| AC-007-3 | 旧 `PartConstraintReq` | 可适配为新 `PartCategoryRequest` |

### AC-008: 基础配置和实例访问只通过接口

目的：验证业务规则不能直接绑定当前实现。

验证逻辑：

```java
@Test
void postRuleUsesPublicAccessorOnly() {
    List<String> imports = scanImports("src/test/java/.../PostConstraint.java");

    assertFalse(imports.contains("com.jmix.executor.impl.ModuleInstAccessorImpl"));
    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.executor.bmodel.")));
    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.executor.cmodel.")));
}
```

### AC-009: 测试基类临时变量迁移完成

目的：验证 `ConstraintAlgImplTestBase` 不再承载正式变量 API。

预期：

- `ConstraintAlgImplTestBase.PartVar` 删除或移入测试工具层。
- `ConstraintAlgImplTestBase.ParaVar` 删除或移入测试工具层。
- 生产变量定义位于 `southinf.var`。
- 现有测试通过迁移后的生产 API 运行。

### 回归测试

必须纳入回归：

- `ModuleScenarioTestBase` 现有场景测试。
- `CompatibleRuleRequireTest`、`CompatibleRuleIncompatibleTest`、`CompatibleRuleCodependentTest`。
- `CalculateRuleSimpleTest`。
- `PartCategoryFilterEmptyTest`。
- `ParaIntegerTest`、`ParaIsHiddenTest`。
- `RFC-0003` 冲突诊断相关测试。
- `RFC-0004` POST 计算相关测试。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 定义 `southinf` 稳定 API、变量 facade、版本注解和 descriptor | P0 | 待开始 |
| 2 | 扩展 `ModuleAlgArtifact`，让 `ModuleAlgClassLoader` 以 artifact 版本为准进行版本识别 | P0 | 待开始 |
| 3 | 实现旧算法迁移/重新生成路径，并拒绝加载缺失南向 API 版本的旧算法 | P0 | 待开始 |
| 4 | 将 `ConstraintAlgImplTestBase` 中的临时变量迁移到生产 API 或测试 support | P0 | 待开始 |
| 5 | 新增 `calculate` / `infer` / `validate` 北向接口和请求模型 | P0 | 待开始 |
| 6 | 实现 `InferParasReq` 到 V2 请求模型的适配层 | P0 | 待开始 |
| 7 | 抽取配置定义 / ModuleInst 访问 view/accessor 公共接口 | P1 | 待开始 |
| 8 | 更新算法代码生成模板，生成 V1 南向 API 代码 | P1 | 待开始 |
| 9 | 增加静态依赖扫描，禁止新算法导入 `impl` 和 `coretest` | P1 | 待开始 |
| 10 | 补齐兼容性、计算、反推、校验、execute 规则访问回归测试 | P0 | 待开始 |

### 5.1 第一阶段边界

第一阶段只要求做到：

- 新接口可用。
- 老接口可用。
- 老算法迁移/重新生成后可运行，未迁移算法明确拒绝加载。
- 新算法不依赖实现。
- 北向新接口能覆盖计算、反推、校验、规则执行四个入口。

第一阶段不要求删除所有旧类，但旧类只能用于迁移工具或测试工具层，不能继续作为正式南向接口。

### 5.2 不在本 RFC 首期范围

- 替换 OR-Tools 求解器。
- 重写现有规则 DSL。
- 一次性删除 `inferParas(InferParasReq)`。
- 一次性删除所有 `PartConstraintReq` 调用点。
- 手工修改所有历史算法源码；历史算法优先通过生成器重新生成。

---

## 6. 风险与兼容策略

### 6.1 二进制兼容风险

旧算法可能已经将 `ConstraintAlgImplTestBase`、`PartVar`、`ParaVar` 打入算法 jar。如果继续运行这类算法，引擎内部就仍然被旧测试基类和旧变量字段绑住。因此本 RFC 不保留 Legacy V0 运行通道，而是要求旧算法迁移或重新生成。

缓解：

- 缺失 `southApiVersion` 时拒绝加载，并提示使用算法生成器重新生成。
- 提供迁移检查工具，扫描旧算法是否继承 `ConstraintAlgImplTestBase` 或导入 `impl.algmodel`。
- 对无法迁移的算法返回明确错误，包括算法版本、引擎版本和缺失 API 信息。
- 从最新南向 API 开始，后续引擎升级必须保持基于该 API 开发的算法可运行。

### 6.2 API 过度抽象风险

如果 V1 API 一次性抽象过大，会拖慢迁移。

缓解：

- 先覆盖现有业务算法实际使用的方法。
- 通过 `ConstraintContext.capabilities()` 做能力探测。
- 新增 API 只能追加默认方法，避免破坏主版本兼容。

### 6.3 输入条件层级风险

如果把约束写成与 Part、Parameter 同级的模型对象，会误导调用方，以为约束是一类独立业务实体。实际上约束是“参数输入”“部件输入”“属性聚合输入”的表达方式。

缓解：

- 北向请求结构只保留 `Module -> PartCategory -> Part/Parameter` 主层级。
- `Requirement` / `Condition` 作为 `PartCategoryRequest` 或 `ParameterBinding` 的字段存在。
- 文档和 API 命名避免使用 `Part/Parameter/Constraint` 并列表达。

---

## 7. 已确认决策与待明确问题

### 7.1 已确认决策

Q1. 旧算法 Legacy V0 是否长期支持？

结论：不需要长期支持 Legacy V0，旧算法直接迁移/重新生成到最新南向 API。从最新 API 开始承担向后兼容要求。

Q2. 南向 API 是否必须完全隐藏 OR-Tools？

结论：不需要完全隐藏。少量基础类型可以暴露，复杂执行对象和模型生命周期必须隐藏。

Q3. 算法版本以哪里为准？

结论：以 `ModuleAlgArtifact` 为准。算法类注解只作为源码辅助声明和一致性校验。

Q4. 北向命名是否使用 `PartCategory`？

结论：使用 `PartCategory`，不引入 `Classifier` 作为北向主命名。

Q5. 北向接口是否采用 `calculate` / `infer` / `validate` 三个方法？

结论：采用三个方法，分别表达计算、反推和校验。

Q6. “部件/参数/约束” 是否作为并列层级？

结论：不作为并列层级。北向主层级是 `Module -> PartCategory -> Part/Parameter`；约束是参数、部件、属性聚合上的输入条件。

### 7.2 待进一步明确

Q7. 基础配置与实例访问接口首期具体开放哪些读写能力？

当前建议首期开放以下能力：

- 读取配置定义：读模块、PartCategory、Part、Parameter 的定义属性。
- 读取计算实例：读本次解中的 PartCategory 实例、Part 实例、数量、扩展属性、动态属性值。
- 写参数值：通过 `ParameterInstView.setValue` 写回。
- 写动态属性：通过 `OntoView.setDynAttr` 写回。
- 写数量：通过 `QuantityInstView.setQuantity` 写回。
- execute 规则接口：执行一段业务规则。它不是单独的写参数接口；写参数、写动态属性、写数量都是 execute 规则可能产生的结果。

仍需确认：选择状态、错误信息、诊断信息是否需要进入首期接口。当前建议不要放入通用 view，后续如需要应设计专门接口。

---

## 8. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0003-Conflict-Diagnosis-Relaxed-Solution.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `src/main/java/com/jmix/executor/ModuleConstraintExecutor.java`
- `src/main/java/com/jmix/executor/model/InferParasReq.java`
- `src/main/java/com/jmix/executor/model/PartConstraintReq.java`
- `src/main/java/com/jmix/executor/southinf/IModuleAlg.java`
- `src/main/java/com/jmix/executor/impl/ModuleConstraintExecutorImpl.java`
- `src/main/java/com/jmix/executor/impl/ModuleAlgClassLoader.java`
- `src/main/java/com/jmix/executor/impl/ModuleInstAccessor.java`
- `src/test/java/com/jmix/coretest/ConstraintAlgImplTestBase.java`
