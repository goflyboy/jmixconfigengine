# RFC-0005 合并版：南向接口解耦实施指导

> 状态: 合并执行稿
> 日期: 2026-05-19
> 用途: 指导另一个代码仓完成计算引擎南向接口解耦改造
> 来源文档:
> - `doc/RFC-0005-North-South-Interface-Decoupling.md`
> - `doc/RFC-0005-0.1-North-South-Interface-Decoupling-Patch.md`

---

## 1. 使用说明

本文是两篇 RFC 的合并版，面向需要执行代码改造的目标仓库。

执行时以 `RFC-0005-0.1` 的补丁方案为最终口径：南向接口最终收敛到 `ModuleCPModel`、`PartCategoryCPModel`、`ModuleAlgBase`、`ModuleInstView` 和 `AlgCPXxx` facade。原 `RFC-0005` 中的 `ConstraintModel`、`ConstraintAlgBase`、`ConstraintContext`、`ConstraintVarRegistry` 等设计只作为过渡背景理解，不作为最终实现目标。

目标不是替换 OR-Tools 求解器，也不是改写业务模型 `bmodel` / 结果模型 `cmodel` 的数据结构，而是把产品算法与引擎内部实现、测试基类、OR-Tools 具体类型解耦。

---

## 2. 最终设计决议

| 主题 | 最终决议 |
| --- | --- |
| 南向接口边界 | 产品算法只能依赖 `com.jmix.executor.southinf` 下的稳定接口、变量 facade、CP facade 和实例 view |
| 统一入口 | 产品算法继承 `ModuleAlgBase`，通过 `model()` 访问所有约束能力、变量查找能力和 PartCategory 聚合能力 |
| CP facade | `AlgCPModel`、`AlgCPConstraint`、`AlgCPLinearExpr`、`PartAlgCPLinearExpr` 等改为纯接口，实现类统一命名为 `XxxImpl` |
| OR-Tools 解耦 | 产品算法和 `southinf` API 不得出现 `com.google.ortools.*`、`IntVar`、`BoolVar`、`Literal`、`Constraint` |
| 领域模型 | `ConstraintModel` 收敛并更名为 `ModuleCPModel` |
| 变量注册 | 删除 `ConstraintVarRegistry`，变量查找方法直接放入 `ModuleCPModel` |
| PartCategory 能力 | `IPartCategoryFunction` / `PartCategoryFunction` 改造为 `PartCategoryCPModel` / `PartCategoryCPModelImpl` |
| 算法基类 | `ConstraintAlgBase` 更名为 `ModuleAlgBase`，且保持很薄，不承载业务逻辑 |
| 实现基类 | `ModuleBaseAlgImpl` 是引擎内部实现，产品算法不允许继承 |
| POST 实例访问 | 删除 `ModuleInstAccessor` / `ModuleInstAccessorImpl`，统一通过 `ModuleInstView` |
| 版本判定 | 运行期以 `ModuleAlgArtifact.southApiVersion` 为准 |
| 版本注解 | `AlgorithmApiVersion` 移到 tools 注解包，仅用于源码生成和 artifact 构建辅助 |
| 方法版本 | 南向接口类型和方法必须标记 `@SouthApiSince` |
| 兼容策略 | 当前仍视为 south API `1.0` 试验期；正式发布后只能新增 API，不能改名、改签名、移动包名或删除 |

---

## 3. 改造范围

### 3.1 必须覆盖

- 南向 CP facade 接口和实现类拆分。
- OR-Tools 类型从南向 API 和产品算法中移除。
- `ConstraintModel` 到 `ModuleCPModel` 的迁移。
- `ConstraintVarRegistry` 删除，变量查找能力迁入 `ModuleCPModel`。
- `IPartCategoryFunction` / `PartCategoryFunction` 到 `PartCategoryCPModel` / `PartCategoryCPModelImpl` 的迁移。
- `ConstraintAlgBase` 到 `ModuleAlgBase` 的迁移。
- `ModuleInstAccessor` 到 `ModuleInstView` 的替换。
- 旧测试基类、算法样例、规则测试按 `model().xxx` 和 `AlgCPXxx` facade 方式整改。
- 类加载、模型生成、artifact 生成逻辑识别 `ModuleAlgArtifact.southApiVersion`。
- 增加 `@SouthApiSince`、API manifest、静态依赖扫描和兼容性测试。

### 3.2 不在本次范围

- 北向 `calculate` / `infer` / `postCalculate` 接口设计，另见 RFC-0006。
- 替换 Google OR-Tools 求解器。
- 改写业务模型 `bmodel` 或结果模型 `cmodel` 的数据结构。
- 保留旧算法 Legacy V0 长期运行层。

---

## 4. 目标包结构

```text
com.jmix.executor.southinf
  ModuleAlgBase
  ModuleCPModel
  PartCategoryCPModel
  AlgorithmDescriptor

com.jmix.executor.southinf.version
  SouthApiSince
  SouthApiVersion

com.jmix.executor.southinf.cp
  AlgCPModel
  AlgCPConstraint
  AlgCPLinearExpr
  PartAlgCPLinearExpr
  AlgCPLinearArgument
  AlgCPIntVar
  AlgCPBoolVar
  AlgCPLiteral

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
  QuantityInstView

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

com.jmix.tool.bbuilder.anno
  AlgorithmApiVersion
```

硬性边界：

- `southinf` 包不得导入 `com.jmix.executor.impl.*`。
- `southinf` 包不得导入 `com.google.ortools.*`。
- OR-Tools 只允许出现在实现包、实现单测和求解器适配层。
- 产品算法不得继承 `ModuleBaseAlgImpl`、`ModuleAlgImpl`、`ConstraintAlgImplTestBase`。
- 产品算法不得导入 `com.jmix.coretest.*` 或 `com.jmix.executor.impl.*`。

---

## 5. 核心接口目标

### 5.1 ModuleCPModel

`ModuleCPModel` 是产品算法建模的唯一主入口，吸收原 `ConstraintModel`、`ConstraintVarRegistry` 和 Module 级 PartCategory 能力。

```java
public interface ModuleCPModel extends AlgCPModel, PartCategoryCPModel {
    ParaVar para(String code);
    PartVar part(String code);
    PartCategoryVar partCategory(String code);
}
```

迁移规则：

| 旧写法 | 新写法 |
| --- | --- |
| `vars().para("Color")` | `model().para("Color")` |
| `vars().part("Disk")` | `model().part("Disk")` |
| `vars().partCategory("drive")` | `model().partCategory("drive")` |
| `model.addLessOrEqual(...)` | `model().addLessOrEqual(...)` |
| `model.newBoolVar(...)` | `model().newBoolVar(...)` |
| `newBoolVar(...)` | `model().newBoolVar(...)` |
| `newIntVar(...)` | `model().newIntVar(...)` |

### 5.2 AlgCP facade

南向 CP 接口只暴露自有 facade 类型。

```java
public interface AlgCPLiteral {
    String name();
    AlgCPLiteral not();
}

public interface AlgCPLinearArgument {
    String name();
}

public interface AlgCPIntVar extends AlgCPLinearArgument {
}

public interface AlgCPBoolVar extends AlgCPIntVar, AlgCPLiteral {
}

public interface AlgCPConstraint {
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral condition);
    AlgCPConstraint onlyEnforceIf(AlgCPLiteral... conditions);
    int index();
}

public interface AlgCPLinearExpr extends AlgCPLinearArgument {
    AlgCPLinearExpr name(String name);
    AlgCPLinearExpr addTerm(AlgCPIntVar var, long coefficient);
    AlgCPLinearExpr addTerm(AlgCPBoolVar var, long coefficient);
    AlgCPLinearExpr addConstant(long value);
    boolean isEmpty();
}

public interface PartAlgCPLinearExpr extends AlgCPLinearExpr {
    @Override
    PartAlgCPLinearExpr name(String name);
}
```

`AlgCPModel` 示例：

```java
public interface AlgCPModel {
    AlgCPIntVar newIntVar(long left, long right, String name);
    AlgCPIntVar newIntVarFromDomain(long[] values, String name);
    AlgCPBoolVar newBoolVar(String name);

    AlgCPLinearExpr newLinearExpr(String name);
    PartAlgCPLinearExpr newPartLinearExpr(String name);

    AlgCPConstraint addBoolAnd(AlgCPLiteral[] literals);
    AlgCPConstraint addBoolOr(AlgCPLiteral[] literals);
    AlgCPConstraint addExactlyOne(AlgCPLiteral[] literals);
    AlgCPConstraint addAtMostOne(AlgCPLiteral[] literals);
    AlgCPConstraint addImplication(AlgCPLiteral left, AlgCPLiteral right);

    AlgCPConstraint addEquality(AlgCPLinearArgument left, long right);
    AlgCPConstraint addEquality(AlgCPLinearArgument left, AlgCPLinearArgument right);
    AlgCPConstraint addEquality(AlgCPLinearExpr left, long right);

    AlgCPConstraint addLessOrEqual(AlgCPLinearArgument left, long right);
    AlgCPConstraint addLessOrEqual(AlgCPLinearExpr left, long right);
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearArgument left, long right);
    AlgCPConstraint addGreaterOrEqual(AlgCPLinearExpr left, long right);

    void minimize(AlgCPLinearExpr expr);
    void maximize(AlgCPLinearExpr expr);
}
```

实现类在 `impl.algmodel` 中持有 OR-Tools delegate，并负责 unwrap。

```java
final class AlgCPBoolVarImpl implements AlgCPBoolVar {
    private final com.google.ortools.sat.BoolVar delegate;
}
```

### 5.3 PartCategoryCPModel

```java
public interface PartCategoryCPModel {
    PartAlgCPLinearExpr sum4Quantity(String attrCode, String filterCondition);
    AlgCPLinearExpr sum4Selected(String filterCondition);
    List<PartVar> partVars();
    List<PartVar> partVars(String filterCondition);
}
```

使用规则：

- Module 级规则中，通过 `model().sum4Quantity(...)`、`model().sum4Selected(...)` 使用聚合能力。
- PartCategory 级规则中，`model().sum4Selected(...)` 默认作用于当前 `fatherCode` 对应的 PartCategory。
- 不允许直接调用继承来的 `sum4Quantity(...)`、`sum4Selected(...)`。

### 5.4 ModuleAlgBase

产品算法只继承 `ModuleAlgBase`。

```java
public abstract class ModuleAlgBase implements ModuleInstView {
    private ModuleCPModel constraintModel;
    private ModuleInstView currentInst;

    protected final ModuleCPModel model() {
        return constraintModel;
    }

    protected final ModuleInstView currentInst() {
        return currentInst;
    }
}
```

约束：

- `ModuleAlgBase` 必须是很薄的一层，只承载绑定后的入口方法。
- 业务逻辑、OR-Tools 适配、变量创建实现不应放入 `ModuleAlgBase`。
- `ModuleBaseAlgImpl` 是引擎内部承载者，可实现 `ModuleCPModel` 和 `ModuleInstView`，但不能暴露给产品算法继承。

### 5.5 ModuleInstView

POST 阶段只通过实例 view 访问和写回当前配置解。

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

访问边界：

- `extAttr/extAttr4Int` 只读。
- `dynAttr/dynAttr4Int` 读取当前实例动态属性。
- `setDynAttr` 写当前实例动态属性。
- `ParameterInstView.setValue` 写参数值。
- `QuantityInstView.setQuantity` 写 Part 或 PartCategory 实例数量。
- 不允许通过 view 替换 `ModuleInst`、`PartCategoryInst`、`PartInst` 对象结构。

---

## 6. 类型删除与迁移映射

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

| 旧类型 | 新类型 |
| --- | --- |
| `ConstraintModel` | `ModuleCPModel` |
| `ConstraintVarRegistry` | `ModuleCPModel` 内置 `para` / `part` / `partCategory` |
| `IPartCategoryFunction` | `PartCategoryCPModel` |
| `PartCategoryFunction` | `PartCategoryCPModelImpl` |
| `ConstraintAlgBase` | `ModuleAlgBase` |
| `ConstraintContext` | `ModuleAlgBase` 绑定的 `ModuleCPModel` / `ModuleInstView` |
| `ModuleInstAccessor` | `ModuleInstView` |
| `ModuleInstAccessorImpl` | `ModuleInstViewImpl` |
| `com.jmix.executor.southinf.AlgorithmApiVersion` | `com.jmix.tool.bbuilder.anno.AlgorithmApiVersion` |
| `BoolVar` | `AlgCPBoolVar` |
| `IntVar` | `AlgCPIntVar` |
| `Literal` | `AlgCPLiteral` |

---

## 7. 代码迁移示例

### 7.1 Module 级 PartCategory 聚合

```java
// before
PartAlgCPLinearExpr totalCapacity = model.sum4Quantity("Capacity", "").name("totalCapacity");
PartAlgCPLinearExpr objectiveExpr = model.newPartLinearExpr("ObjectiveFun");

// after
PartAlgCPLinearExpr totalCapacity = model().sum4Quantity("Capacity", "").name("totalCapacity");
PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");
```

### 7.2 PartCategory 级规则

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

### 7.3 BoolVar / Literal 解耦

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

### 7.4 POST 规则实例访问

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
}
```

---

## 8. 版本与 artifact 规则

### 8.1 运行期版本以 artifact 为准

`ModuleAlgArtifact` 需要包含：

```java
public class ModuleAlgArtifact {
    private String algorithmVersion;
    private String southApiVersion;
}
```

版本解析：

| 输入 | 处理 |
| --- | --- |
| `ModuleAlgArtifact.southApiVersion` 有值 | 以 artifact 为准 |
| artifact 无版本，算法类有注解 | 注解只作为生成期补充或校验，运行期应记录 artifact 信息不完整 |
| artifact 和注解都无版本 | 视为未迁移旧算法，拒绝加载 |
| artifact 版本低于当前最新 south API | 提示迁移或重新生成 |
| artifact 版本高于当前引擎支持版本 | 返回明确的不兼容版本错误 |

### 8.2 AlgorithmApiVersion 归属

`AlgorithmApiVersion` 移到 tools 注解包：

```java
package com.jmix.tool.bbuilder.anno;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AlgorithmApiVersion {
    String southApiVersion();
    String algorithmVersion() default "";
}
```

定位：

- 用于源码生成、模型构建和 artifact 构建阶段。
- 不属于 `southinf` 南向运行期接口。
- 若源码注解和 artifact 版本同时存在，artifact 版本优先。

### 8.3 SouthApiSince

新增南向 API 方法级版本注解：

```java
package com.jmix.executor.southinf.version;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SouthApiSince {
    String value();
}
```

版本常量：

```java
package com.jmix.executor.southinf.version;

public final class SouthApiVersion {
    public static final String V1_0 = "1.0";
    public static final String V1_1 = "1.1";
    public static final String V1_2 = "1.2";

    private SouthApiVersion() {
    }
}
```

规则：

- 每个南向 API 接口和方法都必须显式标记 `@SouthApiSince`。
- 当前 1.0 试验期最终基线统一标记为 `@SouthApiSince("1.0")`。
- 1.0 正式发布后，已发布方法的 `since` 不可修改。
- 后续新增方法按新增版本标记，例如 `@SouthApiSince("1.2")`。
- 实现类不要求重复标记，兼容性检查以接口为准。

### 8.4 API manifest

发布时生成 API manifest：

```text
doc/api/south-api-<version>.json
```

示例：

```text
doc/api/south-api-1.0.json
doc/api/south-api-1.1.json
doc/api/south-api-1.2.json
```

manifest 内容示意：

```json
{
  "southApiVersion": "1.2",
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

## 9. 推荐实施顺序

| 阶段 | 任务 | 优先级 |
| --- | --- | --- |
| 1 | 新增 `southinf.cp` 接口：`AlgCPModel`、`AlgCPConstraint`、`AlgCPLinearExpr`、`AlgCPIntVar`、`AlgCPBoolVar`、`AlgCPLiteral` | P0 |
| 2 | 将现有 CP 类重命名为 `XxxImpl`，内部持有 OR-Tools delegate | P0 |
| 3 | 新增 `ModuleCPModel`，替换 `ConstraintModel`，吸收变量查找方法 | P0 |
| 4 | 新增 `PartCategoryCPModel` / `PartCategoryCPModelImpl`，替换 `IPartCategoryFunction` / `PartCategoryFunction` | P0 |
| 5 | 将 `ConstraintAlgBase` 重命名或迁移为 `ModuleAlgBase` | P0 |
| 6 | 调整 `ModuleBaseAlgImpl implements ModuleCPModel, ModuleInstView` | P0 |
| 7 | 删除 `ModuleInstAccessor` / `ModuleInstAccessorImpl`，以 `ModuleInstViewImpl` 承接实现 | P0 |
| 8 | 删除冗余南向类型并更新类加载、模型生成、artifact 生成逻辑 | P0 |
| 9 | 按迁移规范整改所有测试用例和算法样例 | P0 |
| 10 | 新增 `@SouthApiSince` 和 `SouthApiVersion`，为 1.0 基线方法补齐版本标记 | P0 |
| 11 | 增加 south API manifest 生成与对比工具 | P1 |
| 12 | 增加静态依赖扫描、API 兼容性测试和回归测试 | P1 |

---

## 10. 测试与验收准则

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

### AC-004: ModuleInstAccessor 删除

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

### AC-005: 冗余南向类型删除

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

### AC-006: 测试用例全部迁移到 model()

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

### AC-007: 南向 API 方法必须标记版本

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

### AC-008: API manifest 可生成并可对比

```java
@Test
void southboundApiManifestCanDetectAddedMethods() {
    SouthApiManifest oldManifest = loadManifest("doc/api/south-api-1.1.json");
    SouthApiManifest currentManifest = generateManifest("1.2");

    SouthApiDiff diff = SouthApiDiff.compare(oldManifest, currentManifest);

    assertTrue(diff.removedMethods().isEmpty());
    assertTrue(diff.changedMethods().isEmpty());
    assertEquals("1.2", diff.addedMethods().get(0).since());
}
```

### AC-009: 回归测试通过

```bash
mvn test
```

预期：

- 已迁移测试全部通过。
- 实现层 OR-Tools 单测必须放在 `impl` 或专门 adapter 测试命名空间，不能作为产品算法样例。

---

## 11. 静态扫描建议

目标仓库可先用文本扫描定位迁移范围：

```bash
rg "ConstraintAlgBase|ConstraintModel|ConstraintVarRegistry|ConstraintContext|ConstraintAlgorithm"
rg "ModuleInstAccessor|ModuleInstAccessorImpl"
rg "com.google.ortools.sat|\\bBoolVar\\b|\\bIntVar\\b|\\bLiteral\\b"
rg "newBoolVar\\s*\\(|newIntVar\\s*\\(|sum4Selected\\s*\\(|sum4Quantity\\s*\\("
rg "com.jmix.executor.impl|com.jmix.coretest"
```

注意：

- `impl` 包内部实现和实现层单测可以出现 OR-Tools。
- 产品算法样例、生成器模板、规则测试不应出现 OR-Tools 具体类型。
- `sum4Selected` / `sum4Quantity` 的扫描需要人工排除 `model().sum4Selected(...)`、`model().sum4Quantity(...)` 这类正确写法。

---

## 12. 风险与处理策略

### 12.1 源码兼容风险

本次改造会破坏仍使用 `BoolVar`、`IntVar`、`Literal`、`model.xxx` 字段、`newBoolVar(...)` 继承方法的算法源码。

处理：

- 当前仍属于 1.0 试验期，不保留源码兼容层。
- 统一迁移测试用例、样例和生成器模板。
- 旧算法优先通过生成器重新生成。

### 12.2 二进制兼容风险

旧算法可能已经将测试基类和内部变量字段打入算法 jar。

处理：

- 缺失 `southApiVersion` 时拒绝加载。
- 加载失败信息必须明确提示迁移或重新生成。
- 不建设 Legacy V0 长期运行层。

### 12.3 实现复杂度风险

`AlgCPBoolVar`、`AlgCPLiteral`、`AlgCPLinearArgument` 和 OR-Tools 类型之间需要 unwrap。

处理：

- unwrap 逻辑只允许存在于 `impl.algmodel`。
- facade impl 可提供包内可见的 delegate 访问方法。
- 单测覆盖 `not()`、`onlyEnforceIf()`、`addBoolAnd()`、线性表达式构建等核心路径。

### 12.4 API 版本标记遗漏风险

新增方法如果没有 `@SouthApiSince`，后续无法判断它属于哪个版本。

处理：

- API 兼容性测试默认扫描全部南向接口方法。
- 缺少 `@SouthApiSince` 的方法直接导致测试失败。
- manifest 作为版本发布产物随代码一起提交。

---

## 13. 最终检查清单

- [ ] `southinf` 中没有 `com.google.ortools.*` import。
- [ ] `southinf` 中没有 `com.jmix.executor.impl.*` import。
- [ ] 产品算法不继承 `ConstraintAlgImplTestBase`、`ModuleAlgImpl`、`ModuleBaseAlgImpl`。
- [ ] 产品算法统一继承 `ModuleAlgBase`。
- [ ] 产品算法统一通过 `model()` 建模。
- [ ] 产品算法不使用 `BoolVar`、`IntVar`、`Literal`。
- [ ] `ConstraintModel` 已迁移为 `ModuleCPModel`。
- [ ] `ConstraintVarRegistry` 已删除或不再作为南向 API 暴露。
- [ ] `ModuleInstAccessor` / `ModuleInstAccessorImpl` 已删除。
- [ ] POST 规则统一使用 `ModuleInstView`。
- [ ] `AlgorithmApiVersion` 已移到 `com.jmix.tool.bbuilder.anno`。
- [ ] 运行期版本判定以 `ModuleAlgArtifact.southApiVersion` 为准。
- [ ] 南向接口和方法都标记 `@SouthApiSince`。
- [ ] API manifest 能生成并能与上一版本对比。
- [ ] 静态依赖扫描测试已加入。
- [ ] `mvn test` 通过。

---

## 14. 与源 RFC 的关系

本合并稿保留 `RFC-0005` 的核心动机和边界：产品算法不得依赖测试基类、内部实现、旧 accessor 或未声明的运行期能力。

本合并稿采用 `RFC-0005-0.1` 的最终命名和实现口径：

- `ConstraintAlgBase` 的最终目标名是 `ModuleAlgBase`。
- `ConstraintModel` 的最终目标名是 `ModuleCPModel`。
- `ConstraintVarRegistry` 不保留。
- `ModuleInstAccessor` 不保留。
- OR-Tools 类型不再允许出现在南向 API 和产品算法中。
- `AlgorithmApiVersion` 不属于 `southinf`。

