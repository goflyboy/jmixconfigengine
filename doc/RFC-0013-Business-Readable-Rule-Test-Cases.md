# RFC-0013: 面向业务可读的规则测试用例生成

> 状态：草案（Draft）
> 日期：2026-06-12
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `docbackup/Rule-Scenarios-And-SDK-Summary.md`, `doc/RFC-0011-RuleTrans-Module.md`, `doc/RFC-0012-PartCategory-Where-Only-And-Multi-Aggregate.md`

---

## 设计决策摘要

| 主题 | 决策 |
| --- | --- |
| 用例表达 | RuleTrans 生成的测试用例先生成业务 JSON，用领域模型表达输入和预期输出，只保留关键字段 |
| 测试执行 | 新增 source 层运行服务 `RuleUnitTestExecutorService`，通过 `ModuleConstraintExecutor4SingleRule` 包装主线 `ModuleConstraintExecutor` |
| 场景路由 | 业务 JSON 不写 `testMode`，而是写包装后测试服务的方法名 `serviceMethod` |
| 规则大类 | P0 覆盖计算赋值类、兼容类、优先类三类；约束环境和非约束环境仍分开 |
| 输入输出判定 | 计算赋值类先按引用/赋值关系判定输入输出，再映射到参数、部件、部件分类等领域对象 |
| 存放方式 | 业务 JSON 用例 P0 直接保存为资源文件，不嵌入 JUnit 字符串 |
| 兼容策略 | 不保留旧 `RuleTransTestCase`/`RuleSnippetAssembler` 生成路径；当前仍是第一版代码，可以一步到位替换 |

---

## 1. 摘要

当前 RuleTrans 生成的测试用例更像面向引擎实现的 IT 测试：直接生成 `selectedParts`、`requests`、`expectedFirstPartQuantities`、`inferRecommendModule(...)` 等字段或代码。这样的测试能跑，但业务用户难以确认输入是什么、预期是什么、为什么这样测。

本 RFC 提议把“业务可读 JSON 用例”作为测试生成的中间产物和事实来源：输入与预期输出都按领域模型描述，并由正式运行代码 `RuleUnitTestExecutorService` 调用 `ModuleConstraintExecutor4SingleRule` 完成执行、结果转换和比较，最后把测试结果返回给界面或回归测试入口。

---

## 2. 动机

### 2.1 当前问题

现有生成链路大致是：

```text
自然语言规则 -> 生成规则代码 -> 生成 RuleTransTestCase -> RuleSnippetAssembler 生成 JUnit
```

当前 `RuleTransTestCase` 已经包含多种面向引擎测试的字段：

```java
selectedParts
expectedValid
requests
expectedResult
partCode
quantity
preParas
expectedFirstPartQuantities
expectedFirstParaValues
```

这些字段的问题不是“不能测试”，而是表达层级偏低：

1. 业务用户看到的是引擎调用方式，而不是业务输入和业务预期。
2. 同一个用例里输入与输出常常不是同一种领域表达，例如输入是请求 DSL，输出是 `PartInst` 短字符串。
3. 不同规则大类的合适测试方式不同，但当前生成逻辑容易按引擎场景机械套用。
4. 对计算赋值类规则，缺少“哪些是引用输入、哪些是赋值输出”的统一判定。

本 RFC 后，新的主链路调整为：

```text
自然语言规则
  -> 生成规则代码
  -> 生成业务 JSON 用例文件
  -> RuleUnitTestExecutorService 读取用例
  -> ModuleConstraintExecutor4SingleRule 调用主线引擎
  -> 业务结果比较
  -> 返回界面/测试报告
```

### 2.2 目标

P0 目标：

1. 让生成的测试用例 JSON 能被业务用户直接阅读和评审。
2. 输入和预期结果统一使用领域模型表达，字段保持最小。
3. 按规则语义自动选择测试方法，避免拗口的反推/正推混用。
4. 在 source 层提供面向单规则测试的正式执行服务，不依赖 `ModuleScenarioTestBase` 或 JUnit 基类。
5. 参数兼容类、单分类汇总校验、业务 JSON 文件化均在 P0 一步到位。

非目标：

1. 不修改南向 SDK 的规则编写 API。
2. 不保留旧 `RuleTransTestCase`/`RuleSnippetAssembler` 作为新链路兼容层。
3. 不把业务 JSON 直接作为正式北向业务接口发布。
4. 不要求一个规则同时覆盖所有引擎场景；单元测试只验证语义最合适的场景。

---

## 3. 核心设计

### 3.1 业务 JSON 成为用例事实来源

业务 JSON 是单规则测试的事实来源。它不描述 JUnit 怎么写，也不描述 `InferParasReq` 怎么拼，而是描述：

1. 这是哪一类业务规则。
2. 应调用哪个包装后的测试服务方法。
3. 输入领域对象是什么。
4. 预期业务结果是什么。

运行链路：

```text
BusinessRuleTestCase JSON
  -> RuleUnitTestExecutorService
  -> ModuleConstraintExecutor4SingleRule
  -> ModuleConstraintExecutor
  -> RuleUnitTestResult
```

生成器输出资源文件，例如：

```text
src/test/resources/rule-unit-cases/<module-code>/<rule-code>.json
```

已有 JUnit 回归测试、命令行工具或界面都可以调用 `RuleUnitTestExecutorService` 读取这些 JSON 文件并执行。JUnit 只是一个调用者，不再是用例表达本身。

### 3.2 业务用例 JSON Schema

P0 使用一个轻量、可扩展的 JSON 结构：

```json
{
  "id": "string",
  "title": "string",
  "businessFamily": "ASSIGNMENT | COMPATIBILITY | PRIORITY",
  "scenario": "string",
  "environment": "CONSTRAINT | NON_CONSTRAINT",
  "serviceMethod": "testAssignment | testCompatibility | testPriority | testPostAssignment",
  "given": {
    "parameters": [],
    "parts": [],
    "partCategories": []
  },
  "expect": {
    "compatible": true,
    "parameters": [],
    "parts": [],
    "solutions": [],
    "diagnostics": []
  },
  "note": "string"
}
```

字段约束：

1. `given` 和 `expect` 都只使用参数、部件、需求、诊断等领域对象。
2. 只写当前规则关注的关键字段。
3. 能从上下文推导的字段不要求生成，例如 part 的父分类在可由模型查询时不重复写。
4. 只有优先类需要 `solutions.rank` 表达顺序。
5. 兼容类优先使用 `expect.compatible` 表达是否通过。
6. `serviceMethod` 是 `RuleUnitTestExecutorService` 的方法路由，不是底层引擎入口名称。

### 3.3 业务大类与现有 RuleFamily 的映射

现有 `com.jmix.ruletrans.scenario.RuleFamily` 更接近生成器内部路由，枚举值包括 `CALCULATE`、`AGGREGATE`、`SELECT`、`HIDDEN`、`POST` 等。业务 JSON 不直接复用这个枚举，而是使用更稳定的三大类 `businessFamily`。

映射规则：

| 业务大类 | 现有 `RuleFamily` |
| --- | --- |
| `ASSIGNMENT` | `CALCULATE`, `AGGREGATE`, `SELECT`, `HIDDEN`, `POST` |
| `COMPATIBILITY` | `COMPATIBLE`, `STRUCTURED` 中的白名单/黑名单/Requires/Incompatible/CoDependent |
| `PRIORITY` | `PRIORITY` |

`RuleScenarioClassifier` 仍可继续用于提示词和 SDK 路由；业务用例生成器只需要在输出前把内部 `RuleFamily` 归并为 `businessFamily`。

### 3.4 领域对象最小字段

参数：

```json
{"code": "color", "value": "red"}
```

部件：

```json
{"code": "drive1", "quantity": 1}
```

需要表达选择状态时：

```json
{"code": "cpu2", "isSelected": true}
```

PartCategory 输入：

```json
{
  "category": "drive",
  "aggregate": "Sum_Quantity",
  "operator": "==",
  "value": 2,
  "where": {"Speed": "5400"}
}
```

优先级解：

```json
{
  "rank": 1,
  "parts": [
    {"code": "sd1", "quantity": 2}
  ]
}
```

诊断：

```json
{"ruleCode": "cpu_drive_white", "reason": "cpu4 与 drive7200 不在白名单组合内"}
```

---

## 4. 输入输出判定规则

### 4.1 计算赋值类

计算赋值类按接近传统程序分析的方式判定输入和输出。

第一步：从规则逻辑中识别引用与赋值。

| 代码语义 | 用例角色 |
| --- | --- |
| 只读取参数、部件、部件属性、分类汇总 | 输入 |
| 对参数、部件数量、选择状态、隐藏状态、动态属性写入或强制赋值 | 输出 |
| `return` 或校验布尔结果 | 输出 |

第二步：把输入/输出映射回领域对象。

| 引擎对象 | 领域对象 |
| --- | --- |
| `ParaVar`, `ParaInst` | parameter |
| `PartVar`, `PartInst` | part |
| `PartCategoryVar`, 聚合表达式 | PartCategory |
| `ModuleInst` 中的诊断 | diagnostics |

示例：参数 if-else 反推部件数量规则。

业务语义：

```text
如果 color=red 且 size=M，则 tshirt.quantity=2。
```

虽然历史测试可通过反推参数验证，但业务上更自然的单元测试是正向计算：

```json
{
  "id": "ASSIGN-IF-001",
  "businessFamily": "ASSIGNMENT",
  "scenario": "参数 if-else 推导部件数量",
  "environment": "CONSTRAINT",
  "serviceMethod": "testAssignment",
  "given": {
    "parameters": [
      {"code": "color", "value": "red"},
      {"code": "size", "value": "M"}
    ]
  },
  "expect": {
    "parts": [
      {"code": "tshirt", "quantity": 2}
    ]
  }
}
```

### 4.2 兼容类

兼容类不强调“计算出什么值”，而强调给定组合是否允许成立。

参数 Requires / Incompatible 示例：

```json
{
  "id": "COMP-PARA-001",
  "businessFamily": "COMPATIBILITY",
  "scenario": "参数 Incompatible",
  "environment": "CONSTRAINT",
  "serviceMethod": "testCompatibility",
  "given": {
    "parameters": [
      {"code": "A", "value": "a1"},
      {"code": "B", "value": "b2"}
    ]
  },
  "expect": {
    "compatible": false,
    "diagnostics": [
      {"ruleCode": "A_B_incompatible"}
    ]
  }
}
```

部件互斥表达式示例：

```json
{
  "id": "COMP-PART-001",
  "businessFamily": "COMPATIBILITY",
  "scenario": "部件互斥表达式",
  "environment": "CONSTRAINT",
  "serviceMethod": "testCompatibility",
  "given": {
    "parts": [
      {"code": "drive1", "isSelected": true},
      {"code": "cpu2", "isSelected": true}
    ]
  },
  "expect": {
    "compatible": false
  }
}
```

### 4.3 优先类

优先类的预期不是单个 true/false，而是在满足硬约束的候选解之间验证排序或最优解。

示例：固态硬盘优先匹配高速率容量，用机械硬盘增配低速率容量。

```json
{
  "id": "PRI-DRIVE-001",
  "businessFamily": "PRIORITY",
  "scenario": "固态优先匹配高速率容量",
  "environment": "CONSTRAINT",
  "serviceMethod": "testPriority",
  "given": {
    "partCategories": [
      {
        "category": "drive",
        "aggregate": "Sum_Quantity",
        "operator": "==",
        "value": 2,
        "where": {"Speed": "5400"}
      }
    ]
  },
  "expect": {
    "solutions": [
      {
        "rank": 1,
        "parts": [
          {"code": "sd1", "quantity": 2}
        ]
      },
      {
        "rank": 2,
        "parts": [
          {"code": "md1", "quantity": 1},
          {"code": "sd1", "quantity": 1}
        ]
      },
      {
        "rank": 3,
        "parts": [
          {"code": "md1", "quantity": 2}
        ]
      }
    ]
  }
}
```

---

## 5. 规则场景到服务方法的路由矩阵

### 5.1 路由原则

1. 单元测试验证规则语义，不机械覆盖所有推理入口。
2. 正向推理更易懂时，优先使用正向方式。
3. 兼容类优先用校验方式表达是否兼容。
4. 优先类优先验证有序解或最优解。
5. 约束环境和非约束环境分开；非约束规则不强行用约束模型测试。
6. 业务 JSON 写 `RuleUnitTestExecutorService` 的方法，不写底层 `inferRecommendModule`、`validateData` 等测试 helper。

### 5.2 P0 路由矩阵

| 规则大类 | 子场景 | `serviceMethod` | `ModuleConstraintExecutor4SingleRule` 方法 | 预期表达 |
| --- | --- | --- | --- | --- |
| 计算赋值类 | 参数 if-else 推导部件数量 | `testAssignment` | `testAssignment(moduleId, Module module, RuleUnitInput input)` | `expect.parts[].quantity` |
| 计算赋值类 | 整数参数计算 | `testAssignment` | `testAssignment(moduleId, Module module, RuleUnitInput input)` | `expect.parameters[].value` |
| 计算赋值类 | 单分类数量/属性汇总约束 | `testAssignment` | `testAssignment(moduleId, PartCategory partCategory, RuleUnitInput input)` | `expect.compatible` |
| 计算赋值类 | POST 后置写回 | `testPostAssignment` | `testPostAssignment(moduleId, Module module, RuleUnitInput input)` | `expect.parameters` / `expect.parts[].attrs` |
| 兼容类 | 参数 Requires/Incompatible/CoDependent | `testCompatibility` | `testCompatibility(moduleId, Module module, RuleUnitInput input)` | `expect.compatible` |
| 兼容类 | 部件互斥表达式 | `testCompatibility` | `testCompatibility(moduleId, Module module, RuleUnitInput input)` | `expect.compatible`, `diagnostics` |
| 兼容类 | 结构化白/黑名单组合 | `testCompatibility` | `testCompatibility(moduleId, Module module, RuleUnitInput input)` | `expect.compatible`, violated rule code |
| 优先类 | 优先级目标函数 | `testPriority` | `testPriority(moduleId, PartCategory partCategory, RuleUnitInput input)` | `expect.solutions[].rank` |

### 5.3 InferParameter 与 InferModule 的处理

`InferParameter` 属于反向推理，`InferModule` 属于正向推理。P0 不按入口名称决定是否生成测试，而按规则语义决定：

1. 如果规则本质是“给定参数，计算部件数量”，使用 `testAssignment` 的正向输入更易懂。
2. 如果规则本质是“给定部件状态，反推出参数”，仍由 `testAssignment` 调用单规则执行器选择反向推理路径，但业务 JSON 不暴露 `InferParameter`。
3. 如果规则只是兼容关系，统一使用 `testCompatibility` 表达组合是否有效。
4. 如果规则是非约束 POST 写回，统一使用 `testPostAssignment`，不额外生成约束模型测试。

---

## 6. Source 层执行服务设计

### 6.1 职责

新增正式运行代码，不放在 `src/test/java`，不继承 `ModuleScenarioTestBase`。建议放在：

```text
src/main/java/com/jmix/ruleunit
```

核心分两层：

1. `ModuleConstraintExecutor4SingleRule`：对主线 `ModuleConstraintExecutor` 的单规则测试包装。
2. `RuleUnitTestExecutorService`：读取业务 JSON，调用单规则包装器，比较实际结果和预期结果，返回给界面或回归测试入口。

接口草案：

```java
public interface ModuleConstraintExecutor4SingleRule {

    RuleUnitActualResult testAssignment(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testAssignment(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testCompatibility(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testCompatibility(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testPriority(
            Long moduleId,
            Module module,
            RuleUnitInput input);

    RuleUnitActualResult testPriority(
            Long moduleId,
            PartCategory partCategory,
            RuleUnitInput input);

    RuleUnitActualResult testPostAssignment(
            Long moduleId,
            Module module,
            RuleUnitInput input);
}

public interface RuleUnitTestExecutorService {

    RuleUnitTestReport executeCase(BusinessRuleTestCase testCase);

    RuleUnitTestReport executeCaseFile(String caseFilePath);

    RuleUnitTestReport executeCaseSet(BusinessRuleTestCaseSet caseSet);

    RuleUnitTestReport testAssignment(BusinessRuleTestCase testCase);

    RuleUnitTestReport testCompatibility(BusinessRuleTestCase testCase);

    RuleUnitTestReport testPriority(BusinessRuleTestCase testCase);

    RuleUnitTestReport testPostAssignment(BusinessRuleTestCase testCase);
}
```

`RuleUnitTestExecutorService` 负责：

1. 根据 `serviceMethod` 选择 `ModuleConstraintExecutor4SingleRule` 方法。
2. 加载或接收产品模型、模块模型、部件分类模型。
3. 把业务 JSON 的 parameters、parts、partCategories 转成 `RuleUnitInput`。
4. 调用单规则执行器。
5. 把实际结果转换成业务结果。
6. 和 `expect` 比较，返回 `RuleUnitTestReport`。

`executeCase(...)` 是统一入口；`testAssignment(...)`、`testCompatibility(...)`、`testPriority(...)`、`testPostAssignment(...)` 是业务 JSON 中 `serviceMethod` 可直接引用的方法名。

### 6.2 三个维度

`ModuleConstraintExecutor4SingleRule` 的方法设计从三个维度展开：

| 维度 | 取值 | 用途 |
| --- | --- | --- |
| 运行环境 | `CONSTRAINT`, `NON_CONSTRAINT` | 决定使用 CP-SAT 求解还是 POST/实例 view 类流程 |
| 接口区分 | `ASSIGNMENT`, `COMPATIBILITY`, `PRIORITY` | 决定默认断言语义 |
| 输入类型 | `PRODUCT`, `PART_CATEGORY`, `PART` | 决定转换成模块级、分类级或部件级输入 |

### 6.3 输入转换

参数输入：

```json
{"code": "color", "value": "red"}
```

转换为 `RuleUnitInput.parameters`，由 `ModuleConstraintExecutor4SingleRule` 决定是构造 `preParaInsts`、校验输入，还是 POST 输入。

```java
new RuleUnitParameterInput("color", "red")
```

部件输入：

```json
{"code": "drive1", "quantity": 1}
```

转换为：

```java
new RuleUnitPartInput("drive1", 1, true)
```

PartCategory 输入：

```json
{
  "category": "drive",
  "aggregate": "Sum_Quantity",
  "operator": "==",
  "value": 2,
  "where": {"Speed": "5400"}
}
```

转换为：

```java
new RuleUnitPartCategoryInput(
        "drive",
        "Sum_Quantity",
        "==",
        2,
        Map.of("Speed", "5400"))
```

内部可复用 RFC-0012 的聚合条件模型，但不依赖 `ModuleScenarioTestBase.parseAttrExpr(...)`。测试服务是正式运行代码，需要有自己的业务输入转换器。

### 6.4 输出转换

求解输出：

```java
List<ModuleInst>
```

转换为：

```json
{
  "solutions": [
    {
      "rank": 1,
      "parts": [
        {"code": "sd1", "quantity": 2, "isSelected": true}
      ],
      "parameters": []
    }
  ]
}
```

校验输出：

```java
ModuleValidateResp
```

转换为：

```json
{
  "compatible": false,
  "diagnostics": [
    {"ruleCode": "cpu_drive_white"}
  ]
}
```

---

## 7. RuleTrans 改造点

### 7.1 新增业务用例模型

建议新增包：

```text
src/main/java/com/jmix/ruletrans/testgen/business
```

核心类：

```java
public record BusinessRuleTestCaseSet(
        String ruleMethod,
        List<BusinessRuleTestCase> cases) {
}

public record BusinessRuleTestCase(
        String id,
        String title,
        BusinessRuleFamily businessFamily,
        String scenario,
        TestEnvironment environment,
        String serviceMethod,
        BusinessCaseGiven given,
        BusinessCaseExpect expect,
        String note) {
}
```

业务大类枚举：

```java
public enum BusinessRuleFamily {
    ASSIGNMENT,
    COMPATIBILITY,
    PRIORITY
}
```

### 7.2 Prompt 调整

`ruletrans/test_case_prompt.jtl` 的输出要求调整为：

1. 只输出 JSON。
2. JSON 必须使用业务领域对象。
3. 不生成 Java 测试代码。
4. 不暴露 `InferParasReq`、`PartConstraintReq`、`ModuleInst`、`expectedFirstPartQuantities` 等内部字段。
5. 每个用例必须明确 `given` 与 `expect`。
6. 规则场景必须按 `Rule-Scenarios-And-SDK-Summary.md` 的三大类归类。

### 7.3 生成器调整

`RuleTestCaseGenerator` 增加业务用例生成路径：

```java
BusinessRuleTestCaseSet generateBusinessCases(
        String naturalLanguage,
        RuleContext context,
        RuleScenario scenario,
        String methodBody)
```

P0 直接替换旧 `RuleTransTestCase` 生成路径。当前仍是第一版代码，不需要做旧字段兼容。

### 7.4 输出文件生成

`RuleSnippetAssembler` 不再负责生成测试用例代码。新的输出器负责写业务 JSON 文件：

```text
src/test/resources/rule-unit-cases/<module-code>/<rule-code>.json
```

现有测试框架如果需要跑这些用例，只调用：

```java
RuleUnitTestExecutorService.executeCaseFile(path)
```

### 7.5 不保留旧生成模型

本 RFC 视当前 RuleTrans 测试生成代码为第一版试验代码，因此：

1. 不保留 `RuleTransTestCase` 旧字段兼容。
2. 不保留 `RuleSnippetAssembler` 的测试代码生成职责。
3. 不把 `ModuleScenarioTestBase` 作为新执行服务的父类或依赖。
4. 已有历史测试可继续存在，但新生成链路直接使用业务 JSON + `RuleUnitTestExecutorService`。

---

## 8. 典型验收用例

### 8.1 计算赋值类：代码类型与输出 JSON

规则代码类型：约束模型中的 if-else 计算赋值。

示例代码：

```java
@CodeRuleAnno(normalNaturalCode = "红色 M 码 T 恤数量为 2")
private void assignTshirtQuantity() {
    AlgCPBoolVar isRed = color.option("red").selectedVar();
    AlgCPBoolVar isM = size.option("M").selectedVar();
    AlgCPBoolVar matched = model().newBoolVar("color_red_size_m");
    model().addBoolAnd(isRed, isM).onlyEnforceIf(matched);
    model().addEquality(tshirt.quantityVar(), 2).onlyEnforceIf(matched);
}
```

输出 JSON：


```json
{
  "id": "ASSIGN-IF-001",
  "title": "红色 M 码 T 恤数量为 2",
  "businessFamily": "ASSIGNMENT",
  "scenario": "参数 if-else 推导部件数量",
  "environment": "CONSTRAINT",
  "serviceMethod": "testAssignment",
  "given": {
    "parameters": [
      {"code": "color", "value": "red"},
      {"code": "size", "value": "M"}
    ]
  },
  "expect": {
    "parts": [
      {"code": "tshirt", "quantity": 2}
    ]
  }
}
```

验收：

1. 生成的 JSON 不包含 `InferParasReq`、`expectedFirstPartQuantities`。
2. `RuleUnitTestExecutorService` 调用 `ModuleConstraintExecutor4SingleRule.testAssignment(...)`。
3. 断言时只比较 `tshirt.quantity=2` 这一关键输出。

### 8.2 计算赋值类：单分类数量/属性汇总验证

规则：固态硬盘必须配置同一种，并且最多配置 2 块。

规则代码类型：约束模型中的 PartCategory 汇总约束。

示例代码：

```java
@CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘必须配置同一种，并且最多配置2块")
private void sameSsdAndAtMostTwo() {
    AlgCPLinearExpr sumSelected = model().sum4Selected("fatherCode=sd");
    model().addLessOrEqual(sumSelected, 1);

    AlgCPLinearExpr sumQty = model().sum4Quantity("fatherCode=sd");
    model().addLessOrEqual(sumQty, 2);
}
```

输出 JSON：

业务用例：

```json
{
  "id": "ASSIGN-AGG-003",
  "businessFamily": "ASSIGNMENT",
  "scenario": "单分类数量/属性汇总",
  "environment": "CONSTRAINT",
  "serviceMethod": "testAssignment",
  "given": {
    "parts": [
      {"code": "drive1", "quantity": 1},
      {"code": "drive2", "quantity": 1}
    ]
  },
  "expect": {
    "compatible": false
  },
  "note": "违反固态硬盘必须配置同一种"
}
```

补充用例矩阵：

| ID | given.parts | expect.compatible | note |
| --- | --- | --- | --- |
| ASSIGN-AGG-001 | `drive1.quantity=1` | true | 单一固态硬盘，数量不超过 2 |
| ASSIGN-AGG-002 | `drive1.quantity=2` | true | 单一固态硬盘，数量等于上限 |
| ASSIGN-AGG-003 | `drive1.quantity=1`, `drive2.quantity=1` | false | 违反必须配置同一种 |
| ASSIGN-AGG-004 | `drive1.quantity=3` | false | 违反最多配置 2 块 |

### 8.3 兼容类：代码类型与输出 JSON

规则代码类型：参数 Requires / Incompatible / CoDependent 兼容关系。

示例代码：

```java
@CodeRuleAnno(normalNaturalCode = "A=a1 要求 B=b2")
private void a1RequiresB2() {
    addCompatibleConstraintRequires(
            "a1_requires_b2",
            A.option("a1").selectedVar(),
            B.option("b2").selectedVar());
}
```

输出 JSON：

业务用例：

```json
{
  "id": "COMP-PARA-REQ-001",
  "businessFamily": "COMPATIBILITY",
  "scenario": "参数 Requires",
  "environment": "CONSTRAINT",
  "serviceMethod": "testCompatibility",
  "given": {
    "parameters": [
      {"code": "A", "value": "a1"},
      {"code": "B", "value": "b2"}
    ]
  },
  "expect": {
    "compatible": true
  }
}
```

验收：

1. 用例以参数领域对象表达输入。
2. 预期输出是 `compatible=true/false`。
3. P0 必须由 `ModuleConstraintExecutor4SingleRule.testCompatibility(...)` 支持参数组合校验，不使用推理结果模拟。

### 8.4 兼容类：部件互斥表达式

业务用例：

```json
{
  "id": "COMP-PART-001",
  "businessFamily": "COMPATIBILITY",
  "scenario": "部件互斥表达式",
  "environment": "CONSTRAINT",
  "serviceMethod": "testCompatibility",
  "given": {
    "parts": [
      {"code": "drive1", "isSelected": true},
      {"code": "cpu2", "isSelected": true}
    ]
  },
  "expect": {
    "compatible": false
  }
}
```

验收：

1. `RuleUnitTestExecutorService` 调用 `ModuleConstraintExecutor4SingleRule.testCompatibility(...)`。
2. 输出转换为 `compatible=false`。
3. 如规则有 code，补充断言 `diagnostics[].ruleCode`。

### 8.5 优先类：代码类型与输出 JSON

规则代码类型：优先级目标函数。

示例代码：

```java
@PriorityRuleAnno(fatherCode = "drive", normalNaturalCode = "固态硬盘优先匹配高速率容量", strategy = PriorityStrategy.MIN)
private void preferSsdForHighSpeedCapacity() {
    PartAlgCPLinearExpr ssTotalCapacity = model().sum4Quantity("Capacity", "fatherCode=sd");
    PartAlgCPLinearExpr mechTotalCapacity = model().sum4Quantity("Capacity", "fatherCode=md");
    PartAlgCPLinearExpr objectiveExpr = model().newPartLinearExpr("ObjectiveFun");
    objectiveExpr.addExpr(ssTotalCapacity, -100);
    objectiveExpr.addExpr(mechTotalCapacity, 1);
    model().setObjectExpr(objectiveExpr);
    updatePriorityObjectFuntion("preferSsdForHighSpeedCapacity", objectiveExpr);
}
```

输出 JSON：

业务用例：

```json
{
  "id": "PRI-DRIVE-001",
  "businessFamily": "PRIORITY",
  "scenario": "固态硬盘优先匹配高速率容量",
  "environment": "CONSTRAINT",
  "serviceMethod": "testPriority",
  "given": {
    "partCategories": [
      {
        "category": "drive",
        "aggregate": "Sum_Quantity",
        "operator": "==",
        "value": 2,
        "where": {"Speed": "5400"}
      }
    ]
  },
  "expect": {
    "solutions": [
      {"rank": 1, "parts": [{"code": "sd1", "quantity": 2}]},
      {"rank": 2, "parts": [{"code": "md1", "quantity": 1}, {"code": "sd1", "quantity": 1}]},
      {"rank": 3, "parts": [{"code": "md1", "quantity": 2}]}
    ]
  }
}
```

验收：

1. `RuleUnitTestExecutorService` 调用 `ModuleConstraintExecutor4SingleRule.testPriority(...)`。
2. `rank=1` 必须断言第一个解。
3. 显式带 `rank` 的前 N 个解必须严格按顺序断言。

### 8.6 非约束场景不强行使用约束测试

POST 写回规则示例：

```json
{
  "id": "POST-ASSIGN-001",
  "businessFamily": "ASSIGNMENT",
  "scenario": "POST 后置计算写回产品参数",
  "environment": "NON_CONSTRAINT",
  "serviceMethod": "testPostAssignment",
  "given": {
    "parts": [
      {"code": "drive1", "quantity": 2}
    ]
  },
  "expect": {
    "parameters": [
      {"code": "totalDriveQuantity", "value": 2}
    ]
  }
}
```

验收：

1. 不生成 `inferRecommendModule(...)` 作为主测试。
2. 复用 POST 计算路径。
3. 输出仍按领域模型表达。

---

## 9. 复用优先清单

### 9.1 优先复用

- `ModuleConstraintExecutor`：主线引擎执行入口。
- `ModuleValidateReq` / `ModuleValidateResp`：部件和参数组合校验的结果语义。
- `InferParasReq`：单规则执行器内部构造求解请求。
- `PartCategoryConstraintReqBase` / `AggregateConditionReq`：PartCategory 聚合输入模型。
- `ModulePostCalcReq`：POST 非约束计算入口。
- `PartCategoryWhereOnlyAndMultiAggregateTest`：需求 DSL 与多汇总样例。
- `StructCombinationRuleTest` / `StructCombinationOtherRuleTest`：兼容类校验样例。
- `BaseOptiTest`：优先级目标函数样例。
- `RuleScenarioClassifier`、`RuleFamily`：规则场景识别。
- `RuleTransTestCaseSet.fromJson(...)` 的 JSON 提取思路可迁移到新业务 JSON 解析器。

### 9.2 不新增

- 不新增一套平行的引擎执行器。
- 不在每个生成测试方法里手写 `InferParasReq` 或 `ModuleValidateReq`。
- 不让业务 JSON 暴露 `ModuleInst`、`PartInst`、`PartConstraintReq` 等内部对象名。
- 不要求一个业务用例同时测试正向和反向推理。
- 不把 `RuleUnitTestExecutorService` 放进 `src/test/java`。
- 不让 `RuleUnitTestExecutorService` 继承或依赖 `ModuleScenarioTestBase`。
- 不保留 `RuleSnippetAssembler` 的测试代码生成职责。

### 9.3 可由上下文推导

- part 的分类可由模块模型查询。
- PartCategory 输入的 `category` 可由当前规则上下文补全。
- `where` 表达式可由结构化对象转换为现有 DSL。
- 规则 code 可从注解、方法名或生成规则上下文推导。

---

## 10. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| P0-1 | 新增业务用例 JSON 模型与解析器 | P0 | 待开始 |
| P0-2 | 新增 `ModuleConstraintExecutor4SingleRule` | P0 | 待开始 |
| P0-3 | 新增 `RuleUnitTestExecutorService` 和结果比较器 | P0 | 待开始 |
| P0-4 | 建立规则场景到 `serviceMethod` 的路由矩阵 | P0 | 待开始 |
| P0-5 | 调整 `ruletrans/test_case_prompt.jtl`，让 LLM 输出业务 JSON 文件内容 | P0 | 待开始 |
| P0-6 | 替换旧 `RuleTransTestCase`/`RuleSnippetAssembler` 测试生成链路 | P0 | 待开始 |
| P0-7 | 支持参数组合兼容校验 facade | P0 | 待开始 |
| P0-8 | 支持业务 JSON 写入 `src/test/resources/rule-unit-cases` | P0 | 待开始 |
| P0-9 | 覆盖计算赋值类、兼容类、优先类各一组回归测试 | P0 | 待开始 |

---

## 11. 风险与边界

### 11.1 规则代码依赖分析不一定完美

通过方法体判断“引用”和“赋值”在复杂 CP 约束里不总是可靠。P0 策略：

1. 优先使用 `RuleScenario`、规则大类、注解上下文和自然语言描述判断测试方式。
2. 对明显的 `parameter().setValue(...)`、`PartVar.quantityVar()` 赋值、`addEquality(target, value)` 建立启发式。
3. 无法判定时生成 `note`，并让用例进入人工评审。

### 11.2 兼容类参数校验入口需要一步到位

当前测试 helper 对部件组合校验更自然，但参数组合兼容校验在本 RFC 中必须作为正式能力补齐。P0 不使用推理结果模拟 `compatible=true/false`，而是在 `ModuleConstraintExecutor4SingleRule.testCompatibility(...)` 内提供参数、部件、混合组合的统一校验路径。

### 11.3 优先类顺序可能受求解器策略影响

优先类必须区分两种断言：

1. 严格断言前 N 个解顺序。
2. 只断言第一解最优，其他解只要求包含或满足约束。

P0 默认严格断言用户明确写出的 `rank`，特别是前 N 个有序解；未写 `rank` 的解只做包含校验。

---

## 12. 已确认决策

1. 参数兼容类 P0 一步到位新增真正的参数组合校验能力，不用包装层模拟。
2. 优先类需要严格校验显式带 `rank` 的前 N 个解。
3. 业务 JSON P0 一步到位保存到 `src/test/resources/rule-unit-cases`，不嵌入 JUnit。
4. 单分类数量/属性汇总这类“计算赋值类但输出是 true/false”的场景，统一使用 `testAssignment`。
5. 不保留当前 `RuleTransTestCase` 和 `RuleSnippetAssembler` 的旧测试生成模型。
6. 新包装服务是 source 层正式运行代码，不放在测试目录，不继承 `ModuleScenarioTestBase`。

---

## 13. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `docbackup/Rule-Scenarios-And-SDK-Summary.md`
- `doc/RFC-0011-RuleTrans-Module.md`
- `doc/RFC-0012-PartCategory-Where-Only-And-Multi-Aggregate.md`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTransTestCase.java`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTransTestCaseSet.java`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTestCaseGenerator.java`
- `src/main/java/com/jmix/ruletrans/assembler/RuleSnippetAssembler.java`（当前测试代码生成职责将被替换）
- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`
- `src/test/java/com/jmix/scenario/ruletest/StructCombinationRuleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/PartCategoryWhereOnlyAndMultiAggregateTest.java`
