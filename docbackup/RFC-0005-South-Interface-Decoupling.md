# RFC-0005: 计算引擎南向接口重构

> 状态: 已批准
> 日期: 2026-05-17
> 相关文档: `doc/RFC-0006-North-Interface-Refactor.md`, `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`

---

## 设计决议摘要

本 RFC 定义计算引擎的南向接口，即产品算法如何声明规则、访问变量、访问单个配置解，以及如何与引擎内部实现解耦。北向接口拆分到 `RFC-0006`。

|| 主题 | 决议 |
|| --- | --- |
|| 南向接口边界 | 新增生产级 `southinf` API 层，产品算法只依赖稳定接口、允许的基础求解类型和变量 facade |
|| 统一入口 | 产品算法继承 `ModuleAlgBase`，通过 `model()` 访问所有约束能力、变量查找能力和 PartCategory 聚合能力 |
|| CP facade | `AlgCPModel`、`AlgCPConstraint`、`AlgCPLinearExpr`、`PartAlgCPLinearExpr` 等改为纯接口，实现类统一命名为 `XxxImpl` |
|| OR-Tools 解耦 | 产品算法和 `southinf` API 不得出现 `com.google.ortools.*`、`IntVar`、`BoolVar`、`Literal` |
|| 领域模型 | `ConstraintModel` 收敛并更名为 `ModuleCPModel` |
|| 变量注册 | 删除 `ConstraintVarRegistry`，变量查找方法直接放入 `ModuleCPModel` |
|| PartCategory 能力 | `IPartCategoryFunction` / `PartCategoryFunction` 改造为 `PartCategoryCPModel` / `PartCategoryCPModelImpl` |
|| 算法基类 | `ModuleAlgBase` 是产品算法唯一继承基类，且保持很薄，不承载业务逻辑 |
|| 实现基类 | `ModuleBaseAlgImpl` 是引擎内部实现，产品算法不允许继承 |
|| POST 实例访问 | 删除 `ModuleInstAccessor`，统一通过 `ModuleInstView` |
|| 版本判定 | 运行期以 `ModuleAlgArtifact.southApiVersion` 为准 |
|| 版本注解 | `AlgorithmApiVersion` 移到 tools 注解包，仅用于源码生成和 artifact 构建辅助 |
|| 方法版本 | 南向接口类型和方法必须标记 `@SouthApiSince` |
|| 兼容策略 | 当前仍视为 south API `1.0` 试验期；正式发布后只能新增 API，不能改名、改签名、移动包名或删除 |

---

## 1. 摘要

当前南向接口与内部实现耦合较深：产品算法容易继承 `ModuleAlgImpl` 或测试基类 `ConstraintAlgImplTestBase`，并直接使用 `AlgCPModel`（实现类）、内部变量对象、`ModuleInstAccessorImpl` 等实现类型。一旦引擎内部变量模型、求解器封装或执行流程调整，算法制品就可能出现编译失败、类加载失败或运行期错误。

同时，南向接口仍存在两类问题：

1. 南向接口和 Google OR-Tools 解耦不干净。算法代码仍可能直接使用 `IntVar`、`BoolVar`、`Literal`，`AlgCPModel` 等类型本身也是实现类而非稳定接口。
2. 约束接口的领域概念不够清晰。`ConstraintModel`、`ConstraintFunction`、`PartCategoryFunction`、`ConstraintVarRegistry` 等类型数量较多，但没有形成以 Module 为根的统一约束编程模型，导致使用者需要理解过多内部层次。

本 RFC 提议新增稳定的 `southinf` API，明确产品算法只能依赖南向接口、稳定变量 facade、少量允许的基础求解类型和单个配置解 view。具体目标如下：

- CP facade 接口化。`AlgCPModel`、`AlgCPLinearExpr`、`AlgCPConstraint`、`PartAlgCPLinearExpr` 改为纯接口，实现类统一命名为 `XxxImpl`。
- OR-Tools 类型完全隔离。产品算法和 `southinf` API 不再出现 `IntVar`、`BoolVar`、`Literal`、`Constraint` 等 OR-Tools 类型。
- 领域模型收敛。将接口收敛到 `ModuleCPModel`、`PartCategoryCPModel`、`ModuleAlgBase`、`ModuleInstView` 三个核心领域入口。
- POST 阶段按领域对象访问配置解。

北向调用方如何请求计算、反推、后置计算，由 `RFC-0006` 单独定义。

---

## 2. 动机

### 2.1 问题背景

当前代码中南向边界不清晰：

- `src/main/java/com/jmix/executor/southinf/IModuleAlg.java` 只是空标记接口，真实南向能力没有声明。
- `src/test/java/com/jmix/coretest/ConstraintAlgImplTestBase.java` 继承 `ModuleAlgImpl`，并声明测试便利用的 `ParaVar`、`PartVar`、`PartCategoryVar`。
- `AlgCPModel`、`AlgCPLinearExpr`、`AlgCPConstraint`、`PartAlgCPLinearExpr` 是实现类，并直接依赖 OR-Tools。
- 南向算法样例和部分测试仍导入 `com.google.ortools.sat.BoolVar`、`IntVar`、`Literal`。
- `southinf.var.PartCategoryVar` 等 facade 仍可能直接引用 `impl.algmodel` 实现类型。
- `ConstraintAlgImplTestBase` 中的变量包装器依赖 `com.jmix.executor.impl.algmodel` 内部变量，产品算法容易误用这些内部类型。
- `ModuleAlgClassLoader` 当前硬编码加载测试基类和内部类，算法制品与测试包名、测试基类、内部实现同时耦合。
- POST 规则通过 `ModuleInstAccessorImpl` 这类过程式访问器间接操作结果实例，业务规则与当前实例数据结构绑定。

这些问题会带来以下风险：

1. **引擎内部演进风险**。内部模型一改，算法可能被迫同步升级。
2. **接口兼容风险**。无法明确判断哪些方法是正式南向 API，哪些只是测试或内部实现。
3. **OR-Tools 绑定风险**。产品算法直接依赖 OR-Tools 类型，一旦替换求解器，算法需要全部重写。

### 2.2 当前写法与目标写法

当前写法：

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

目标写法：

```java
@AlgorithmApiVersion(southApiVersion = "1.0", algorithmVersion = "server-config-2026.05")
public class ServerConstraint extends ModuleAlgBase {
    private PartVar cpu1;
    private ParaVar size;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(cpu1.quantity(), 1);
        addCompatibleConstraintRequires("rule1", size, listOf("large"), cpu1.selected(), ...);
    }
}
```

POST 规则目标写法：

```java
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

---

## 3. 设计方案

### 3.0 命名与实现约束

南向接口层必须保持清晰的类型边界：

- 同一个概念不允许同时存在同名接口和同名实现类。例如不能同时存在 `Para` 接口和 `Para` 实现类。
- 如果确实需要为同一概念提供接口与实现，统一采用 `Xxx` / `XxxImpl` 命名，例如 `Para` / `ParaImpl`。
- 除 `import` 语句外，业务代码和引擎代码正文不允许通过 `com.jmix.executor.xxx...` 全限定类名绕开命名冲突。
- 不允许为了旧二进制制品兼容而在生产代码中新建同名影子接口；旧制品应迁移或重新生成到最新南向 API。

### 3.1 范围

本 RFC 覆盖：

- 产品算法入口和生命周期。
- 算法版本声明和加载校验。
- 变量 facade。
- CP facade 接口与 OR-Tools 隔离。
- `ModuleCPModel` / `PartCategoryCPModel` 领域模型。
- POST 阶段单个配置解 view。
- 旧测试基类和旧算法迁移策略。
- 南向 API 版本与兼容性。

本 RFC 不覆盖：

- 北向 `ModuleConstraintExecutor` 调用模型，见 `RFC-0006`。
- 重写 OR-Tools 求解器。
- 改写业务模型 `bmodel` 或结果模型 `cmodel` 的数据结构。
- 一次性删除所有旧测试代码。

### 3.2 包结构

```text
com.jmix.executor.southinf
  AlgorithmDescriptor
  ModuleAlgBase
  ModuleCPModel
  PartCategoryCPModel

com.jmix.executor.southinf.version
  SouthApiSince
  SouthApiVersion

com.jmix.executor.southinf.cp
  AlgCPModel
  AlgCPConstraint
  AlgCPLinearExpr
  AlgCPLinearArgument
  AlgCPIntVar
  AlgCPBoolVar
  AlgCPLiteral
  PartAlgCPLinearExpr
  DefaultAlgCPLinearExpr

com.jmix.executor.southinf.var
  Var
  ParaVar
  ParaOptionVar
  PartVar
  PartCategoryVar

com.jmix.executor.southinf.view
  OntoView
  ModuleInstView
  PartCategoryInstView
  PartCategoryInstSumView
  PartInstView
  ParameterInstView

com.jmix.executor.impl.algmodel
  ModuleBaseAlgImpl
  AlgCPModelImpl
  AlgCPConstraintImpl
  AlgCPLinearExprImpl
  PartAlgCPLinearExprImpl
  PartCategoryCPModelImpl
  AlgCPIntVarImpl
  AlgCPBoolVarImpl
  AlgCPLiteralImpl

com.jmix.executor.impl.southbridge
  SouthboundLatestBridge
  SouthboundVarHandles
  SouthboundInstViews
  SouthboundModuleAlgAdapter

com.jmix.tool.bbuilder.anno
  AlgorithmApiVersion
```

约束：

- `southinf` 包不允许导入 `com.jmix.executor.impl.*`。
- `southinf` 包不允许导入 `com.google.ortools.*`。
- OR-Tools 只允许出现在实现包、实现单测和求解器适配层。
- `AlgorithmApiVersion` 属于 tools/生成器元数据，不属于 `southinf` 南向接口。
- `SouthApiSince` 属于南向接口元数据，只用于标记接口或方法从哪个南向 API 版本开始可用。

### 3.3 算法描述与版本

```java
public final class AlgorithmDescriptor {
    private String algorithmId;
    private String algorithmVersion;
    private String southApiVersion;
}
```

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmApiVersion {
    String southApiVersion();
    String algorithmVersion() default "";
}
```

定位：

- `AlgorithmApiVersion` 位于 `com.jmix.tool.bbuilder.anno`，用于源码生成、模型构建和 artifact 构建阶段。
- 它不是产品算法运行期必须依赖的南向接口。
- 运行期版本判定以 `ModuleAlgArtifact.southApiVersion` 为准。
- 若源码注解和 artifact 版本同时存在，artifact 版本优先；源码注解只用于生成期补全或校验。

`ModuleAlgArtifact` 扩展：

```java
public class ModuleAlgArtifact {
    private String algorithmVersion;
    private String southApiVersion;
}
```

版本解析规则：

|| 输入 | 处理 |
|| --- | --- |
|| `ModuleAlgArtifact.southApiVersion` 有值 | 以 artifact 为准 |
|| artifact 无版本，算法类有注解 | 使用注解作为补充，并记录 artifact 信息不完整 |
|| artifact 和注解都无版本 | 视为未迁移旧算法，拒绝加载 |
|| artifact 版本低于当前最新南向 API | 提示迁移或重新生成 |
|| artifact 版本高于当前引擎支持版本 | 返回明确的不兼容版本错误 |

本阶段不引入 `minEngineVersion` / `maxEngineVersion`。

### 3.4 算法入口

```java
public abstract class ModuleAlgBase implements ModuleInstView {
    private RuntimeSupport runtimeSupport;

    public interface RuntimeSupport {
        ModuleCPModel model();
        ModuleInstView currentInst();
        List<PartVar> partVars(String filterCondition);
        void addVarAboutHiddenConstraints(ParaVar... hiddenVars);
        void addVarAboutHiddenConstraints(PartVar... hiddenVars);
        void addCompatibleConstraintRequires(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);
        void addCompatibleConstraintCoDependent(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);
        void addCompatibleConstraintInCompatible(String ruleCode, ParaVar leftParaVar,
                List<String> leftParaFilterOptionCodes, ParaVar rightParaVar,
                List<String> rightParaFilterOptionCodes);
        void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr);
        void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr);
        void addControlParaEqual(String sumParaCode, String instSumParaCode);
    }

    public final void bindSouthboundRuntime(RuntimeSupport runtimeSupport) { ... }
    public final void clearSouthboundRuntime() { ... }
    public final RuntimeSupport southboundRuntime() { ... }

    public AlgorithmDescriptor descriptor() { ... }

    protected final ModuleCPModel model() { ... }
    protected final ModuleInstView currentInst() { ... }

    protected final ParaVar para(String code) { ... }
    protected final PartVar partVar(String code) { ... }
    protected final PartCategoryVar partCategoryVar(String code) { ... }
    protected final List<PartVar> partVars() { ... }
    protected final List<PartVar> partVars(String filterCondition) { ... }

    // 兼容规则
    protected void addCompatibleConstraintRequires(...) { ... }
    protected void addCompatibleConstraintCoDependent(...) { ... }
    protected void addCompatibleConstraintInCompatible(...) { ... }

    // 其他规则支持
    protected void addVarAboutHiddenConstraints(ParaVar... hiddenVars) { ... }
    protected void addVarAboutHiddenConstraints(PartVar... hiddenVars) { ... }
    protected void inCompatible(String ruleCode, String leftPartsExprStr, String rightPartsExprStr) { ... }
    protected void updatePriorityObjectFuntion(String ruleCode, PartAlgCPLinearExpr expr) { ... }
    protected void addControlParaEqual(String sumParaCode, String instSumParaCode) { ... }

    protected List<String> listOf(String... codes) { ... }
}
```

约束：

- `ModuleAlgBase` 必须是很薄的一层，只承载绑定后的入口方法。
- 业务逻辑、OR-Tools 适配、变量创建实现不应放入 `ModuleAlgBase`。
- `ModuleBaseAlgImpl` 是引擎内部承载者，可实现 `ModuleCPModel` 和 `ModuleInstView`，但不能暴露给产品算法继承。
- `ModuleAlgBase` 实现 `ModuleInstView` 是为了让 POST 规则直接按领域对象访问单个配置解。例如 `parameter("p1")`、`partCategory("drive")`、`partCategorySum("drive")`。

### 3.5 CP facade 接口

所有 CP facade 改为纯接口，实现类统一命名为 `XxxImpl`。

#### AlgCPLinearArgument

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPLinearArgument {
    @SouthApiSince(SouthApiVersion.V1_0)
    String name();
}
```

#### AlgCPLiteral

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPLiteral {
    @SouthApiSince(SouthApiVersion.V1_0)
    String name();

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLiteral not();
}
```

#### AlgCPIntVar

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPIntVar extends AlgCPLinearArgument {
}
```

#### AlgCPBoolVar

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPBoolVar extends AlgCPIntVar, AlgCPLiteral {
}
```

#### AlgCPConstraint

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPConstraint {
    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral condition);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral... conditions);

    @SouthApiSince(SouthApiVersion.V1_0)
    int index();
}
```

#### AlgCPLinearExpr

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPLinearExpr extends AlgCPLinearArgument {
    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr addConstant(long value);

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean isEmpty();

    @SouthApiSince(SouthApiVersion.V1_0)
    static AlgCPLinearExpr weightedSum(AlgCPIntVar[] vars, long[] weights) { ... }
}
```

#### PartAlgCPLinearExpr

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface PartAlgCPLinearExpr extends AlgCPLinearExpr {
    @Override
    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr name(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr addExpr(PartAlgCPLinearExpr expr, long coefficient);

    @SouthApiSince(SouthApiVersion.V1_0)
    String exprStr();

    @SouthApiSince(SouthApiVersion.V1_0)
    String partTermsStr();
}
```

#### AlgCPModel

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface AlgCPModel {
    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPIntVar newIntVar(long left, long right, String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPIntVar newIntVarFromDomain(long[] values, String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPBoolVar newBoolVar(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr newLinearExpr(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr newPartLinearExpr(String name);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addBoolAnd(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addBoolOr(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addExactlyOne(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addAtMostOne(AlgCPLiteral[] literals);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addImplication(AlgCPLiteral left, AlgCPLiteral right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearArgument right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearExpr right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addEquality(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessOrEqual(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessOrEqual(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessThan(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addLessThan(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, AlgCPLinearExpr right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterThan(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addGreaterThan(AlgCPLinearExpr left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addDifferent(AlgCPLinearArgument left, long right);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPConstraint addDifferent(AlgCPLinearArgument left, AlgCPLinearArgument right);

    @SouthApiSince(SouthApiVersion.V1_0)
    void minimize(AlgCPLinearExpr expr);

    @SouthApiSince(SouthApiVersion.V1_0)
    void maximize(AlgCPLinearExpr expr);

    @SouthApiSince(SouthApiVersion.V1_0)
    void setObjectExpr(PartAlgCPLinearExpr expr);
}
```

实现类负责在内部 unwrap 到 OR-Tools 类型：

```java
final class AlgCPBoolVarImpl implements AlgCPBoolVar {
    private final com.google.ortools.sat.BoolVar delegate;
}
```

### 3.6 ModuleCPModel

`ModuleCPModel` 是产品算法建模的唯一主入口，吸收原 `ConstraintModel`、`ConstraintVarRegistry` 和 Module 级 PartCategory 能力。

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface ModuleCPModel extends AlgCPModel, PartCategoryCPModel {
    @SouthApiSince(SouthApiVersion.V1_0)
    ParaVar para(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartVar part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryVar partCategory(String code);
}
```

迁移规则：

|| 旧写法 | 新写法 |
|| --- | --- |
|| `vars().para("Color")` | `model().para("Color")` |
|| `vars().part("Disk")` | `model().part("Disk")` |
|| `vars().partCategory("drive")` | `model().partCategory("drive")` |
|| `model.addLessOrEqual(...)` | `model().addLessOrEqual(...)` |
|| `model.newBoolVar(...)` | `model().newBoolVar(...)` |
|| `newBoolVar(...)` | `model().newBoolVar(...)` |
|| `newIntVar(...)` | `model().newIntVar(...)` |

### 3.7 PartCategoryCPModel

原 `IPartCategoryFunction` / `PartCategoryFunction` 改为领域化的 `PartCategoryCPModel`：

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface PartCategoryCPModel {
    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Quantity(String partCategoryCodes, String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    AlgCPLinearExpr sum4Selected(String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Selected(String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartAlgCPLinearExpr sum4Selected(String partCategoryCodes, String attrCode, String filterCondition);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars();

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartVar> partVars(String filterCondition);
}
```

使用规则：

- Module 级规则中，通过 `model().sum4Quantity(...)`、`model().sum4Selected(...)` 使用聚合能力。
- PartCategory 级规则中，`model().sum4Selected(...)` 默认作用于当前 `fatherCode` 对应的 PartCategory。
- 不允许直接调用继承来的 `sum4Quantity(...)`、`sum4Selected(...)`。

### 3.8 变量 facade

南向 API 暴露稳定的变量 facade 类型。

#### Var

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface Var {
    @SouthApiSince(SouthApiVersion.V1_0)
    String code();

    @SouthApiSince(SouthApiVersion.V1_0)
    String name();
}
```

#### ParaVar

```java
public class ParaVar implements Var {
    private final Delegate delegate;

    public interface Delegate {
        String code();
        AlgCPIntVar valueVar();
        AlgCPBoolVar hiddenVar();
        ParaOptionVar option(String optionCode);
        Integer inputValue();
        boolean hasInput();
    }

    public AlgCPIntVar value() { ... }
    public AlgCPBoolVar hidden() { ... }
    public AlgCPIntVar valueVar() { ... }
    public AlgCPBoolVar hiddenVar() { ... }
    public ParaOptionVar option(String optionCode) { ... }
    public Integer inputValue() { ... }
    public boolean hasInput() { ... }
    public String code() { ... }
    public String name() { ... }
}
```

#### ParaOptionVar

```java
public class ParaOptionVar implements Var {
    private final Delegate delegate;

    public interface Delegate {
        String code();
        AlgCPBoolVar selectedVar();
    }

    public AlgCPBoolVar selected() { ... }
    public AlgCPBoolVar selectedVar() { ... }
    public String code() { ... }
    public String name() { ... }
}
```

#### PartVar

```java
public class PartVar implements Var {
    private final Delegate delegate;

    public interface Delegate {
        String code();
        String fatherCode();
        String attr(String attrCode);
        int attrAsInt(String attrCode);
        AlgCPIntVar quantityVar();
        AlgCPBoolVar selectedVar();
        AlgCPBoolVar hiddenVar();
    }

    public AlgCPIntVar quantity() { ... }
    public AlgCPBoolVar selected() { ... }
    public AlgCPBoolVar hidden() { ... }
    public AlgCPIntVar quantityVar() { ... }
    public AlgCPBoolVar selectedVar() { ... }
    public AlgCPBoolVar hiddenVar() { ... }
    public String fatherCode() { ... }
    public String attr(String attrCode) { ... }
    public int attrAsInt(String attrCode) { ... }
    public String code() { ... }
    public String name() { ... }
}
```

#### PartCategoryVar

```java
public class PartCategoryVar extends PartVar {
    private final Delegate delegate;

    public interface Delegate extends PartVar.Delegate {
        List<PartVar> parts(String filterCondition);
        ParaVar sumPara(String attrCode);
        ParaVar sumSumPara(String attrCode);
    }

    public List<PartVar> parts() { ... }
    public List<PartVar> parts(String filterCondition) { ... }
    public ParaVar sumPara(String attrCode) { ... }
    public ParaVar sumSumPara(String attrCode) { ... }
}
```

### 3.9 单个配置解 view

#### OntoView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    String code();

    @SouthApiSince(SouthApiVersion.V1_0)
    String extAttr(String extAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int extAttr4Int(String extAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    String dynAttr(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int dynAttr4Int(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    void setDynAttr(String dynAttrKey, String dynAttrValue);
}
```

#### ModuleInstView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface ModuleInstView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    Long moduleId();

    @SouthApiSince(SouthApiVersion.V1_0)
    String instanceConfigId();

    @SouthApiSince(SouthApiVersion.V1_0)
    int quantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    ParameterInstView parameter(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartInstView part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView partCategory(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView partCategory(String code, int instId);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstSumView partCategorySum(String code);
}
```

#### PartCategoryInstView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface PartCategoryInstView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    int sumQuantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    int instanceId();

    @SouthApiSince(SouthApiVersion.V1_0)
    ParameterInstView parameter(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    PartInstView part(String code);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartInstView> parts();
}
```

#### PartCategoryInstSumView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface PartCategoryInstSumView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    int sumSumQuantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    PartCategoryInstView inst(int instId);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<PartCategoryInstView> insts();

    @SouthApiSince(SouthApiVersion.V1_0)
    String sumDynAttr(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    int sumDynAttr4Int(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<String> dynAttrs(String dynAttrKey);

    @SouthApiSince(SouthApiVersion.V1_0)
    List<Integer> dynAttrs4Int(String dynAttrKey);
}
```

#### PartInstView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface PartInstView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    int quantity();

    @SouthApiSince(SouthApiVersion.V1_0)
    void setQuantity(int quantity);

    @SouthApiSince(SouthApiVersion.V1_0)
    boolean selected();
}
```

#### ParameterInstView

```java
@SouthApiSince(SouthApiVersion.V1_0)
public interface ParameterInstView extends OntoView {
    @SouthApiSince(SouthApiVersion.V1_0)
    String value();

    @SouthApiSince(SouthApiVersion.V1_0)
    void setValue(String value);
}
```

访问和写入边界：

- `extAttr/extAttr4Int` 只读。
- `dynAttr/dynAttr4Int` 读取当前实例动态属性。
- `setDynAttr` 写当前实例动态属性。
- `ParameterInstView.setValue` 写参数值。
- `PartInstView.setQuantity` 写 Part 实例数量。
- 不允许通过 view 替换 `ModuleInst`、`PartCategoryInst`、`PartInst` 的对象结构。

### 3.10 南向 API 版本与兼容性

#### SouthApiSince

新增南向接口方法级版本注解：

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SouthApiSince {
    String value();
}
```

版本常量：

```java
public final class SouthApiVersion {
    public static final String V1_0 = "1.0";

    private SouthApiVersion() {
    }
}
```

标记规则：

- 每个南向 API 接口和方法都必须显式标记 `@SouthApiSince`。
- 当前 1.0 试验期最终基线统一标记为 `@SouthApiSince("1.0")`。
- 1.0 正式发布后，已发布方法的 `since` 不可修改。
- 后续新增方法按新增版本标记，例如 `@SouthApiSince("1.2")`。
- 实现类不要求重复标记，兼容性检查以接口为准。
- 禁止通过修改 `@SouthApiSince` 掩盖 API 变更；老方法的版本标记一旦发布不可修改。

#### 兼容性策略

当前仍处于南向接口 1.0 试验阶段：

- 初始版本为 `1.0`，不升级 `southApiVersion`。
- 试验阶段不要求兼容旧的 1.0 测试用例，测试和样例可以一次性迁移到新南向接口。

1.0 正式发布后的硬性约束：

- 已发布接口方法不能改名。
- 已发布接口方法的参数类型、返回类型、异常契约不能修改。
- 已发布接口方法不能删除。
- 已发布接口类型不能移动包名。
- 新能力只能通过新增接口类型、新增方法或新增默认方法提供。

#### API manifest

发布时生成 API manifest：

```text
doc/api/south-api-<version>.json
```

manifest 内容示意：

```json
{
  "southApiVersion": "1.0",
  "methods": [
    {
      "owner": "com.jmix.executor.southinf.ModuleCPModel",
      "name": "newBoolVar",
      "descriptor": "(java.lang.String)com.jmix.executor.southinf.cp.AlgCPBoolVar",
      "since": "1.0"
    }
  ]
}
```

对比规则：

- 当前版本必须包含上一版本 manifest 中的全部接口方法。
- 老方法的 owner、name、descriptor、since 必须完全一致。
- 当前版本新增方法的 `since` 必须等于当前 `southApiVersion`。
- 没有 `@SouthApiSince` 的南向接口方法直接视为失败。

---

## 4. 类型删除与迁移映射

以下类型不再作为南向 API 保留：

```text
PartCategoryAlg
ModuleAlg
IModuleAlg
IConstraintFunction
ConstraintFunction
ConstraintContext
ConstraintAlgorithm
ConstraintVarRegistry
ConstraintModel
com.jmix.executor.southinf.AlgorithmApiVersion
ModuleInstAccessor
ModuleInstAccessorImpl
```

迁移映射：

|| 旧类型 | 新类型 |
|| --- | --- |
|| `ConstraintModel` | `ModuleCPModel` |
|| `ConstraintVarRegistry` | `ModuleCPModel` 内置 `para` / `part` / `partCategory` |
|| `IPartCategoryFunction` | `PartCategoryCPModel` |
|| `PartCategoryFunction` | `PartCategoryCPModelImpl` |
|| `ConstraintAlgBase` | `ModuleAlgBase` |
|| `ConstraintContext` | `ModuleAlgBase` 绑定的 `ModuleCPModel` / `ModuleInstView` |
|| `ModuleInstAccessor` | `ModuleInstView` |
|| `ModuleInstAccessorImpl` | `SouthboundInstViews.ModuleInstViewImpl` |
|| `com.jmix.executor.southinf.AlgorithmApiVersion` | `com.jmix.tool.bbuilder.anno.AlgorithmApiVersion` |
|| `BoolVar` | `AlgCPBoolVar` |
|| `IntVar` | `AlgCPIntVar` |
|| `Literal` | `AlgCPLiteral` |

---

## 5. 代码迁移示例

### 5.1 Module 级 PartCategory 聚合

```java
// before
PartAlgCPLinearExpr totalCapacity = model.sum4Quantity("Capacity", "").name("totalCapacity");
PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");

// after
PartAlgCPLinearExpr totalCapacity = model().sum4Quantity("Capacity", "").name("totalCapacity");
PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");
```

### 5.2 PartCategory 级规则

```java
// before
@CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
private void logicB1() {
    AlgCPLinearExpr sdTypeSumNum = sum4Selected("Type=sd").name("sdTypeSumNum");
    model.addLessOrEqual(sdTypeSumNum, 1);
}

// after
@CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
private void logicB1() {
    AlgCPLinearExpr sdTypeSumNum = model().sum4Selected("Type=sd").name("sdTypeSumNum");
    model().addLessOrEqual(sdTypeSumNum, 1);
}
```

### 5.3 BoolVar / Literal 解耦

```java
// before
@CodeRuleAnno
public void addConstraintRule2() {
    BoolVar redAndSmall = newBoolVar("rule2_redAndSmall");

    model.addBoolAnd(new Literal[] {
            this.colorVar.option("Red").selectedVar(),
            this.sizeVar.option("Small").selectedVar()
    }).onlyEnforceIf(redAndSmall);
}

// after
@CodeRuleAnno
public void addConstraintRule2() {
    AlgCPBoolVar redAndSmall = model().newBoolVar("rule2_redAndSmall");

    model().addBoolAnd(new AlgCPLiteral[] {
            this.colorVar.option("Red").selectedVar(),
            this.sizeVar.option("Small").selectedVar()
    }).onlyEnforceIf(redAndSmall);
}
```

### 5.4 POST 规则实例访问

```java
// before
@CodeRuleAnno(calcStage = CalcStage.POST)
private void postRule() {
    ModuleInstAccessor accessor = moduleInstAccessor();
    int sum = accessor.partCategorySum("drive").sumDynAttr4Int("Capacity");
    accessor.parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
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

---

## 6. 旧算法迁移策略

本 RFC 不保留 Legacy V0 运行层：

- 当前依赖 `ConstraintAlgImplTestBase`、`ModuleAlgImpl`、`PartVar.qty`、`ParaVar.value` 的历史算法必须迁移或重新生成。
- 引擎加载算法时以 `ModuleAlgArtifact.southApiVersion` 为准。
- 未迁移算法不进入求解流程，返回明确错误。
- 从最新南向 API 开始，才建立后续接口兼容承诺。

`ConstraintAlgImplTestBase` 的目标状态：

- 不再是产品算法继承的接口。
- 不再包含正式南向变量定义。
- 测试代码改为继承生产级 `ModuleAlgBase`。
- `PartVar`、`ParaVar`、`PartCategoryVar` 的生产形态迁移到 `southinf.var`。
- 算法 jar 不再打包或预加载 `com.jmix.coretest.ConstraintAlgImplTestBase` 及其内部变量类。

---

## 7. 验收准则

### AC-001: southinf 不依赖 OR-Tools

```java
@Test
void southinfMustNotImportGoogleOrTools() {
    List<String> imports = scanImports("src/main/java/com/jmix/executor/southinf");

    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.google.ortools.")));
}
```

预期：

- `southinf` 包没有 `com.google.ortools.*` import。
- `IntVar`、`BoolVar`、`Literal` 不出现在南向接口方法签名中。

### AC-002: southinf 不依赖 impl

```java
@Test
void southinfMustNotImportImplPackages() {
    List<String> imports = scanImports("src/main/java/com/jmix/executor/southinf");

    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.executor.impl.")));
}
```

预期：

- `southinf.var` 和 `southinf.cp` 不直接引用 `impl` 类型。
- bridge 或 adapter 只能存在于实现包。

### AC-003: 产品算法只通过 ModuleCPModel 建模

```java
public class MigratedModuleAlg extends ModuleAlgBase {
    private PartCategoryVar drive;

    @CodeRuleAnno(fatherCode = "drive")
    private void rule() {
        AlgCPLinearExpr sdTypeSumNum = model().sum4Selected("Type=sd").name("sdTypeSumNum");
        model().addLessOrEqual(sdTypeSumNum, 1);
    }
}
```

预期：

- 编译通过。
- 算法不导入 `ConstraintModel`、`ConstraintVarRegistry`、`ConstraintContext`。
- 算法不导入 OR-Tools 类型。

### AC-004: 未迁移旧算法拒绝加载

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

### AC-005: ModuleInstAccessor 删除

```java
@Test
void moduleInstAccessorMustBeRemoved() {
    assertFalse(fileExists("src/main/java/com/jmix/executor/impl/ModuleInstAccessor.java"));
    assertFalse(fileExists("src/main/java/com/jmix/executor/impl/ModuleInstAccessorImpl.java"));
    assertNoImport("ModuleInstAccessor");
}
```

预期：

- POST 规则只使用 `ModuleInstView`。
- 实现层如需适配结果模型，类名和接口也应体现 `View`，不再保留 accessor 概念。

### AC-006: 冗余南向类型删除

```java
@Test
void redundantSouthboundTypesMustBeRemoved() {
    assertNoType("com.jmix.executor.southinf.PartCategoryAlg");
    assertNoType("com.jmix.executor.southinf.ModuleAlg");
    assertNoType("com.jmix.executor.southinf.IModuleAlg");
    assertNoType("com.jmix.executor.southinf.IConstraintFunction");
    assertNoType("com.jmix.executor.southinf.ConstraintFunction");
    assertNoType("com.jmix.executor.southinf.ConstraintContext");
    assertNoType("com.jmix.executor.southinf.ConstraintAlgorithm");
    assertNoType("com.jmix.executor.southinf.ConstraintVarRegistry");
    assertNoType("com.jmix.executor.southinf.ConstraintModel");
    assertNoType("com.jmix.executor.southinf.AlgorithmApiVersion");
}
```

### AC-007: 测试用例全部迁移到 model()

```java
@Test
void southboundRuleTestsMustUseModelMethod() {
    List<String> ruleSources = scanJavaSources("src/test/java");

    assertFalse(containsPattern(ruleSources, "\\bnewBoolVar\\s*\\("));
    assertFalse(containsPattern(ruleSources, "\\bnewIntVar\\s*\\("));
    assertFalse(containsPattern(ruleSources, "(?<!\\.)\\bsum4Selected\\s*\\("));
    assertFalse(containsPattern(ruleSources, "(?<!\\.)\\bsum4Quantity\\s*\\("));
}
```

预期：

- 规则内使用 `model().newBoolVar(...)`。
- 规则内使用 `model().sum4Selected(...)`、`model().sum4Quantity(...)`。
- 不再直接访问继承字段 `model`。

### AC-008: 南向 API 方法必须标记版本

```java
@Test
void southboundApiMethodsMustHaveSinceVersion() {
    List<Method> methods = scanPublicInterfaceMethods("com.jmix.executor.southinf");

    assertTrue(methods.stream().allMatch(m -> m.isAnnotationPresent(SouthApiSince.class)));
}
```

预期：

- `ModuleCPModel`、`PartCategoryCPModel`、`AlgCPModel`、`ModuleInstView` 等南向接口方法都有 `@SouthApiSince`。
- 1.0 基线方法统一标记 `@SouthApiSince("1.0")`。

### AC-009: 回归测试通过

```bash
mvn test
```

预期：

- 已迁移测试全部通过。
- 实现层 OR-Tools 单测必须放在 `impl` 或专门 adapter 测试命名空间，不能作为产品算法样例。

---

## 8. 实现计划

|| 阶段 | 任务 | 优先级 | 状态 |
|| --- | --- | --- | --- |
|| 1 | 定义 `southinf` 稳定 API、变量 facade、CP facade 接口和版本注解 | P0 | 已完成 |
|| 2 | 将现有 CP 类重命名为 `XxxImpl`，内部持有 OR-Tools delegate | P0 | 已完成 |
|| 3 | 实现 `SouthboundLatestBridge` 作为南向桥接器，隔离 OR-Tools 类型 | P0 | 已完成 |
|| 4 | 实现 `SouthboundInstViews` 作为 POST 阶段配置解 view | P0 | 已完成 |
|| 5 | 实现旧算法迁移/重新生成路径，并拒绝加载缺失版本的旧算法 | P0 | 已完成 |
|| 6 | 将 `ConstraintAlgImplTestBase` 中的临时变量迁移到生产 API 或测试 support | P0 | 已完成 |
|| 7 | 删除 `ModuleInstAccessor` / `ModuleInstAccessorImpl` | P0 | 已完成 |
|| 8 | 删除冗余南向类型并更新类加载、模型生成、artifact 生成逻辑 | P0 | 已完成 |
|| 9 | 按迁移规范整改所有测试用例和算法样例 | P0 | 已完成 |
|| 10 | 增加 south API manifest 生成与对比工具，manifest 存放到 `doc/api/south-api-<version>.json` | P1 | 已完成 |
|| 11 | 增加静态依赖扫描、API 兼容性测试和回归测试 | P1 | 已完成 |

---

## 9. 风险与兼容策略

### 9.1 源码兼容风险

本次改造会破坏仍使用 `BoolVar`、`IntVar`、`Literal`、`model.xxx` 字段、`newBoolVar(...)` 继承方法的算法源码。

缓解：

- 当前仍属于 1.0 试验期，不保留源码兼容层。
- 统一迁移测试用例、样例和生成器模板。
- 旧算法优先通过生成器重新生成。

### 9.2 二进制兼容风险

旧算法可能已经将测试基类和内部变量字段打入算法 jar。

缓解：

- 缺失 `southApiVersion` 时拒绝加载。
- 加载失败信息必须明确提示迁移或重新生成。
- 不建设 Legacy V0 长期运行层。

### 9.3 实现复杂度风险

`AlgCPBoolVar`、`AlgCPLiteral`、`AlgCPLinearArgument` 和 OR-Tools 类型之间需要 unwrap。

缓解：

- unwrap 逻辑只允许存在于 `impl.algmodel`。
- facade impl 可提供包内可见的 delegate 访问方法。
- 单测覆盖 `not()`、`onlyEnforceIf()`、`addBoolAnd()`、线性表达式构建等核心路径。

### 9.4 API 版本标记遗漏风险

新增方法如果没有 `@SouthApiSince`，后续无法判断它属于哪个版本。

缓解：

- API 兼容性测试默认扫描全部南向接口方法。
- 缺少 `@SouthApiSince` 的方法直接导致测试失败。
- manifest 作为版本发布产物随代码一起提交。

---

## 10. 已确认决策

1. 南向接口以 `southinf` 为边界，产品算法只能依赖 `southinf` 下的稳定接口、变量 facade、CP facade 和实例 view。
2. 产品算法统一继承 `ModuleAlgBase`，通过 `model()` 访问所有约束能力。
3. `AlgCPModel`、`AlgCPLinearExpr`、`AlgCPConstraint`、`PartAlgCPLinearExpr` 等改为纯接口，实现类统一命名为 `XxxImpl`。
4. OR-Tools 类型不暴露在南向 API 和产品算法中。
5. `ConstraintModel` 收敛并更名为 `ModuleCPModel`。
6. `ConstraintVarRegistry` 不保留，变量查找方法直接放入 `ModuleAlgBase` 和 `ModuleCPModel`。
7. `IPartCategoryFunction` / `PartCategoryFunction` 改造为 `PartCategoryCPModel` / `PartCategoryCPModelImpl`。
8. `ConstraintAlgBase` 更名为 `ModuleAlgBase`，且保持很薄，不承载业务逻辑。
9. `ModuleBaseAlgImpl` 是引擎内部实现，产品算法不允许继承。
10. `ModuleInstAccessor` / `ModuleInstAccessorImpl` 删除；`SouthboundInstViews.ModuleInstViewImpl` 基于最新接口重新实现。
11. `AlgorithmApiVersion` 移到 `com.jmix.tool.bbuilder.anno`，不属于 `southinf` 南向接口。
12. 运行期版本判定以 `ModuleAlgArtifact.southApiVersion` 为准。
13. 南向接口方法需要增加版本标记，使用 `@SouthApiSince` 标明方法从哪个版本开始支持。
14. 工具类或自动化测试需要能生成 API manifest，并快速比较当前版本相对上一版本新增了哪些接口。
15. 实现层单测允许导入 OR-Tools，但必须限制在 `impl` 或专门 adapter 测试中，不能作为产品算法样例。
16. API manifest 文件固定存放在 `doc/api/south-api-<version>.json`。

---

## 11. 参考资料

- `doc/RFC-0006-North-Interface-Refactor.md`
- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java`
- `src/main/java/com/jmix/executor/southinf/ModuleCPModel.java`
- `src/main/java/com/jmix/executor/southinf/PartCategoryCPModel.java`
- `src/main/java/com/jmix/executor/southinf/cp/AlgCPModel.java`
- `src/main/java/com/jmix/executor/southinf/view/ModuleInstView.java`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundLatestBridge.java`
- `src/main/java/com/jmix/executor/impl/southbridge/SouthboundInstViews.java`
- `src/main/java/com/jmix/executor/impl/algmodel/ModuleBaseAlgImpl.java`
- `src/main/java/com/jmix/tool/bbuilder/anno/AlgorithmApiVersion.java`
