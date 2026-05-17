# RFC-0005: 计算引擎南向接口重构

> 状态: 草案(Draft)
> 日期: 2026-05-17
> 相关文档: `doc/RFC-0006-North-Interface-Refactor.md`, `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`

---

## 设计决议摘要

本 RFC 只讨论计算引擎的南向接口，即产品算法如何声明规则、访问变量、访问单个配置解，以及如何与引擎内部实现解耦。北向接口拆分到 `RFC-0006`。

| 主题 | 决议 |
| --- | --- |
| 南向接口边界 | 新增生产级 `southinf` API 层，产品算法只依赖稳定接口、允许的基础求解类型和变量 facade |
| 测试基类 | `ConstraintAlgImplTestBase` 不再作为正式南向接口，测试便利能力迁移到测试 support |
| 算法版本 | 以 `ModuleAlgArtifact.southApiVersion` 为准，算法类注解只作为源码辅助声明 |
| 旧算法 | 不保留 Legacy V0 长期运行层，旧算法迁移或重新生成到最新南向 API |
| 约束模型 | 兼容关系属于 `ConstraintModel` 的一种约束表达，不单独暴露 `compatibility()` API |
| 实例访问 | POST 阶段以 `ModuleInstView` 为入口访问单个配置解，接口形态参考现有 `ModuleInst` / `PartCategoryInst` / `PartInst` / `ParaInst` |

---

## 1. 摘要

当前南向接口与内部实现耦合较深：产品算法容易继承 `ModuleAlgImpl` 或测试基类 `ConstraintAlgImplTestBase`，并直接使用 `AlgCPModel`、内部变量对象、`ModuleInstAccessorImpl` 等实现类型。一旦引擎内部变量模型、求解器封装或执行流程调整，算法制品就可能出现编译失败、类加载失败或运行期错误。

本 RFC 提议新增稳定的 `southinf` API，明确产品算法只能依赖南向接口、稳定变量 facade、少量允许的基础求解类型和单个配置解 view。引擎内部实现仍可继续演进，但不能作为产品算法的契约。

北向调用方如何请求计算、反推、后置计算，由 `RFC-0006` 单独定义。

---

## 2. 动机

### 2.1 问题背景

当前代码中南向边界不清晰：

- `src/main/java/com/jmix/executor/southinf/IModuleAlg.java` 只是空标记接口，真实南向能力没有声明。
- `src/test/java/com/jmix/coretest/ConstraintAlgImplTestBase.java` 继承 `ModuleAlgImpl`，并声明测试便利用的 `ParaVar`、`PartVar`、`PartCategoryVar`。
- 测试基类中的变量包装器依赖 `com.jmix.executor.impl.algmodel` 内部变量，产品算法容易误用这些内部类型。
- `ModuleAlgClassLoader` 当前硬编码加载测试基类和内部类，算法制品与测试包名、测试基类、内部实现同时耦合。
- POST 规则通过 `ModuleInstAccessorImpl` 这类实现访问 `ModuleInst`，业务规则与当前实例数据结构绑定。

这些问题会带来两个风险：

1. 引擎内部演进风险。内部模型一改，算法可能被迫同步升级。
2. 接口兼容风险。无法明确判断哪些方法是正式南向 API，哪些只是测试或内部实现。

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
public class ServerConstraint extends ConstraintAlgBase {
    private PartVar cpu1;
    private ParaVar size;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(cpu1.quantity(), 1);
        model().compatibilityRequire("rule1", size.option("large"), cpu1.selected());
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

### 3.1 范围

本 RFC 覆盖：

- 产品算法入口和生命周期。
- 算法版本声明和加载校验。
- 变量 facade。
- 约束模型 facade。
- POST 阶段单个配置解 view。
- 旧测试基类和旧算法迁移策略。

本 RFC 不覆盖：

- 北向 `ModuleConstraintExecutor` 调用模型，见 `RFC-0006`。
- 重写 OR-Tools 求解器。
- 一次性删除所有旧测试代码。

### 3.2 包结构

```text
com.jmix.executor.southinf
  AlgorithmApiVersion
  AlgorithmDescriptor
  ConstraintAlgorithm
  ConstraintAlgBase
  ConstraintContext
  ConstraintModel
  ConstraintCapabilities

com.jmix.executor.southinf.var
  Var
  ParaVar
  ParaOptionVar
  PartVar
  PartCategoryVar
  IntExpr
  BoolExpr
  LinearExpr
  ConstraintRef

com.jmix.executor.southinf.view
  OntoView
  ModuleInstView
  PartCategoryInstView
  PartCategoryInstSumView
  PartInstView
  ParameterInstView
  QuantityInstView

com.jmix.executor.impl.southbridge
  SouthboundApiBridge
  SouthboundLatestBridge
```

`southinf` 包不允许依赖 `com.jmix.executor.impl.*` 下的复杂执行实现。

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

`ModuleAlgArtifact` 扩展：

```java
public class ModuleAlgArtifact {
    private String algorithmVersion;
    private String southApiVersion;
}
```

版本解析规则：

| 输入 | 处理 |
| --- | --- |
| `ModuleAlgArtifact.southApiVersion` 有值 | 以 artifact 为准 |
| artifact 无版本，算法类有注解 | 使用注解作为补充，并记录 artifact 信息不完整 |
| artifact 和注解都无版本 | 视为未迁移旧算法，拒绝加载 |
| artifact 版本低于当前最新南向 API | 提示迁移或重新生成 |
| artifact 版本高于当前引擎支持版本 | 返回明确的不兼容版本错误 |

本阶段不引入 `minEngineVersion` / `maxEngineVersion`。

### 3.4 算法入口

```java
public interface ConstraintAlgorithm {
    AlgorithmDescriptor descriptor();
    void bind(ConstraintContext context);
}
```

```java
public abstract class ConstraintAlgBase implements ConstraintAlgorithm, ModuleInstView {
    protected ConstraintModel model();
    protected ConstraintVarRegistry vars();
    protected ModuleInstView moduleInst();

    protected ParaVar para(String code);
    protected PartVar part(String code);
    protected PartCategoryVar partCategory(String code);

    protected List<String> listOf(String... codes);
}
```

`ConstraintAlgBase` 实现 `ModuleInstView` 是为了让 POST 规则直接按领域对象访问单个配置解。例如 `parameter("p1")`、`partCategory("drive")`、`partCategorySum("drive")`。

### 3.5 上下文接口

```java
public interface ConstraintContext {
    AlgorithmDescriptor descriptor();
    ConstraintModel model();
    ConstraintVarRegistry vars();
    ModuleInstView moduleInst();
    ConstraintCapabilities capabilities();
}
```

`moduleInst()` 只在已经存在单个配置解的阶段可用，例如 POST 后置计算。

### 3.6 变量 facade

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

南向 API 不要求完全隐藏 OR-Tools。少量稳定基础类型可以暴露，复杂执行对象不能暴露：

- 可以暴露 `IntExpr`、`BoolExpr`、`LinearExpr`，或经过白名单确认的基础求解类型。
- 不暴露 `AlgCPModel`、`CpModel`、`CpSolver`、`ModuleAlgImpl`、`ModuleConstraintExecutorImpl`。

### 3.7 约束模型

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

    LinearExpr linearExpr(String name);
    void minimize(LinearExpr expr);
    void maximize(LinearExpr expr);
}

public interface ConstraintRef {
    ConstraintRef onlyIf(BoolExpr condition);
    ConstraintRef withRuleCode(String ruleCode);
}
```

兼容性关系属于约束模型的一种表达，不单独提供 `compatibility()` 入口。

迁移示例：

```java
// before
model().greaterOrEqual(cpu1.quantity(), 1);
compatibility().requires("rule1", size.option("large"), cpu1.selected());

// after
model().greaterOrEqual(cpu1.quantity(), 1);
model().compatibilityRequire("rule1", size.option("large"), cpu1.selected());
```

### 3.8 单个配置解 view

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

访问和写入边界：

- `extAttr/extAttr4Int` 只读。
- `dynAttr/dynAttr4Int` 读取当前实例动态属性。
- `setDynAttr` 写实例动态属性。
- `ParameterInstView.setValue` 写参数值。
- `QuantityInstView.setQuantity` 写 Part 或 PartCategory 实例数量。
- 不允许通过 view 替换 `ModuleInst`、`PartCategoryInst`、`PartInst` 的对象结构。

### 3.9 旧算法迁移策略

本 RFC 不保留 Legacy V0 运行层：

- 当前依赖 `ConstraintAlgImplTestBase`、`ModuleAlgImpl`、`PartVar.qty`、`ParaVar.value` 的历史算法必须迁移或重新生成。
- 引擎加载算法时以 `ModuleAlgArtifact.southApiVersion` 为准。
- 未迁移算法不进入求解流程，返回明确错误。
- 从最新南向 API 开始，才建立后续接口兼容承诺。

`ConstraintAlgImplTestBase` 的目标状态：

- 不再是产品算法继承的接口。
- 不再包含正式南向变量定义。
- 测试代码改为继承生产级 `ConstraintAlgBase`，或使用专门 `ConstraintAlgTestSupport`。
- `PartVar`、`ParaVar`、`PartCategoryVar` 的生产形态迁移到 `southinf.var`。

---

## 4. 验收准则

### AC-001: 迁移后的南向算法可运行

```java
public class MigratedConstraint extends ConstraintAlgBase {
    private PartVar part1;

    @CodeRuleAnno
    private void rule1() {
        model().greaterOrEqual(part1.quantity(), 1);
    }
}
```

预期：

- 算法可正常加载和求解。
- 算法不继承 `ConstraintAlgImplTestBase`。
- 结果与迁移前基准一致。

### AC-002: 未迁移旧算法拒绝加载

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

### AC-003: 新算法不依赖复杂内部实现

```java
@Test
void newAlgorithmMustNotImportImplPackages() {
    List<String> imports = scanImports("src/test/java/.../NewConstraint.java");

    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.executor.impl.")));
    assertFalse(imports.stream().anyMatch(i -> i.startsWith("com.jmix.coretest.")));
}
```

### AC-004: 算法版本可识别

| ID | 输入 | 预期 |
| --- | --- | --- |
| AC-004-1 | artifact `southApiVersion=latest` | 使用最新南向 bridge |
| AC-004-2 | artifact 无版本，类无注解 | 拒绝加载，并提示重新生成 |
| AC-004-3 | artifact `southApiVersion=99.0` | 返回明确的不兼容版本错误 |

### AC-005: POST 规则通过 view 访问和写回

```java
@CodeRuleAnno(calcStage = CalcStage.POST)
private void postRule() {
    PartCategoryInstSumView drive = partCategorySum("drive");
    int sum = drive.sumDynAttr4Int("Capacity");
    parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
}
```

预期：

- 规则不直接导入 `ModuleInstAccessorImpl`。
- 规则不直接导入 `cmodel` 或 `bmodel` 可变实现对象。
- 参数值写回到当前 `ModuleInst` 对应配置解。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 定义 `southinf` 稳定 API、变量 facade、版本注解和 descriptor | P0 | 待开始 |
| 2 | 扩展 `ModuleAlgArtifact`，让类加载以 artifact 版本为准 | P0 | 待开始 |
| 3 | 实现旧算法迁移/重新生成路径，并拒绝加载缺失版本的旧算法 | P0 | 待开始 |
| 4 | 将 `ConstraintAlgImplTestBase` 中的临时变量迁移到生产 API 或测试 support | P0 | 待开始 |
| 5 | 抽取 `ModuleInstView` 等单个配置解访问接口 | P0 | 待开始 |
| 6 | 更新 POST 规则访问逻辑，使其通过 view 读写配置解 | P1 | 待开始 |
| 7 | 更新算法代码生成模板，生成最新南向 API 代码 | P1 | 待开始 |
| 8 | 增加静态依赖扫描，禁止新算法导入 `impl` 和 `coretest` | P1 | 待开始 |

---

## 6. 风险与兼容策略

### 6.1 二进制兼容风险

旧算法可能已经将测试基类和内部变量字段打入算法 jar。继续运行这类算法会让引擎内部实现无法演进。

缓解：

- 缺失 `southApiVersion` 时拒绝加载。
- 提供迁移检查工具，扫描旧算法是否继承测试基类或导入内部包。
- 旧算法优先通过生成器重新生成。

### 6.2 API 过度抽象风险

如果 V1 API 一次抽象过大，会拖慢迁移。

缓解：

- 先覆盖现有业务算法实际使用的方法。
- 新增 API 只能追加默认方法或新 facade。
- 通过 `ConstraintCapabilities` 做能力探测。

---

## 7. 已确认决策

- 旧算法不长期支持 Legacy V0，统一迁移或重新生成到最新南向 API。
- 南向 API 不要求完全隐藏 OR-Tools，但复杂执行对象必须隐藏。
- 算法版本以 `ModuleAlgArtifact` 为准。
- 兼容规则并入 `ConstraintModel`。
- POST 规则按 `ModuleInstView` 领域对象访问单个配置解。

---

## 8. 参考资料

- `doc/RFC-0006-North-Interface-Refactor.md`
- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md`
- `src/main/java/com/jmix/executor/southinf/IModuleAlg.java`
- `src/main/java/com/jmix/executor/impl/ModuleAlgClassLoader.java`
- `src/main/java/com/jmix/executor/impl/ModuleInstAccessor.java`
- `src/test/java/com/jmix/coretest/ConstraintAlgImplTestBase.java`
