# RFC-0017: 智能家居计算赋值类约束模型 RuleTrans 拉通测试

> 状态：草案（Draft）
> 日期：2026-06-13
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `docbackup/Rule-Scenarios-And-SDK-Summary.md`, `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`, `doc/RFC-0014-RuleTrans-End-to-End-RuleUnit-Pipeline.md`, `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`, `doc/RFC-0016-RuleTrans-TestCase-Output-And-Diagnostics-Switch.md`

---

## 1. 摘要

本 RFC 设计一个新的 RuleTrans 拉通系统测试类，用“智能家居”模型测试计算赋值类规则在约束模型中的生成、编译、业务用例生成和 RuleUnit 执行链路。

这里的“约束模型”指运行环境是 CP-SAT/`ModuleConstraintExecutor.inferParas(...)` 这条约束求解路径；这里的“计算赋值类”指规则业务语义是根据参数、部件、分类、属性或聚合值计算数量、选择状态、整数参数、隐藏状态或约束型汇总结果。它不是兼容类规则，也不是 POST 后置计算。

本测试的第一目标不是“让用例通过”，而是站在用户视角判断：中文规则、生成 Java、生成业务测试用例和实际执行结果在业务语义上是否成立。若不成立，测试应暴露问题并帮助定位是翻译、测试用例生成、Prompt 设计，还是现有规则执行实现存在缺陷。

---

## 2. 动机

### 2.1 纠正范围口径

本 RFC 的范围是：

| 维度 | 本轮口径 |
| --- | --- |
| 业务规则类型 | 计算赋值类（Assignment） |
| 运行方式 | 基于约束模型（CONSTRAINT / CP-SAT） |
| 输入 | 中文自然语言规则 |
| 预期输出 | 一段 Java 规则代码 + 业务可读测试用例 |
| 业务判断 | 用户视角的逻辑正确性优先 |
| 不纳入 | 兼容类、优先类、POST 非约束模型写回 |

也就是说，本轮不是测试“约束类/兼容类规则”，而是测试“计算赋值类规则跑在约束模型里”。

### 2.2 当前问题

当前已有一些计算赋值类或聚合类测试，但它们还不能回答“RuleTrans 端到端是否真的理解了用户规则”这个问题：

| 现有测试 | 主要覆盖 | 缺口 |
| --- | --- | --- |
| `CalculateRuleSimpleTest` | 参数 if-else 反推部件数量 | 测试方法仍直接操作引擎 helper，不验证自然语言到 Java 和测试用例生成 |
| `CalculateRuleIfThenTest` | if-then/else 逻辑 | 模型抽象，不是业务化智能家居模型 |
| `ParaIntegerTest` | 整数参数计算 | 未接入 RuleTransPipeline 和业务用例生成 |
| `ParaIsHiddenTest` | 参数隐藏逻辑 | 没有用户视角的自然语言和业务 expected case |
| `SearchStrategyTest` / `SearchStrategyMultiTest` | 单选/多选/搜索策略 | 更像引擎能力测试，不是端到端 RuleTrans 逻辑校验 |
| `BaseOptiTest` / `MultiPCTest` | 单分类和跨分类汇总、优先级 | 模型偏电脑配置，且混有优先类 |
| `DynMultReq4MultiReqTest` / `EnumMultReq4MultiReqTest` | 多实例汇总 | 作为历史参考保留，本 RFC 不引入多实例计算引擎概念 |
| `RuleTransPipelineSystemTest` | RuleTrans 系统级诊断 | 当前以兼容类/编译类示例为主，缺少 Assignment 全场景 |

因此需要一个独立的系统测试类，从用户自然语言出发，验证“这个规则本身在逻辑上是否成立”，并在失败时反查 RuleTrans 与引擎实现。

### 2.3 用户视角优先

每个测试都必须先写出人工可判断的业务语义说明：

```text
规则：离家且安防增强时，室外摄像头数量应为 1。
输入：家庭模式=离家，安防等级=增强。
用户预期：室外摄像头数量=1。
原因：命中规则前提。
```

然后再看：

1. 生成 Java 是否表达了这个逻辑。
2. 生成业务测试用例的 `given/expect` 是否表达了这个逻辑。
3. RuleUnit 实际结果是否符合这个逻辑。

若某一层失败，不允许简单修改期望来让测试通过，必须进入归因流程。

---

## 3. 设计方案

### 3.1 新增测试类

新增系统测试类：

```text
src/test/java/com/jmix/ruletrans/SmartHomeAssignmentRuleTransSystemTest.java
```

该类继承：

```text
src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java
```

测试链路：

```text
中文自然语言计算赋值规则
  -> RuleTransPipeline
  -> RuleScenarioClassifier 识别为 Assignment / CONSTRAINT
  -> LLM 生成 Java methodBody
  -> 编译临时规则类
  -> LLM 生成 BusinessRuleTestCaseSet
  -> RuleUnit testAssignment / testPriority 不使用，本轮只用 testAssignment
  -> ModuleConstraintExecutor.inferParas(...)
  -> 比较业务 expected 与 actual
  -> 打印诊断报告
```

### 3.2 测试方法形态

目标测试方法保持 1 到 3 行业务表达：

```java
@Test
public void testAwayEnhancedAssignsOutdoorCamera() {
    assignmentRule("当家庭模式为离家且安防等级为增强时，室外4K摄像头数量为1，否则数量为0",
            expectJavaContains("addEquality"),
            caseNumsEqualThan(2),
            caseContain(params("homeMode", "离家", "securityLevel", "增强"),
                    parts("cameraOutdoor4k", 1), "命中 if 分支"),
            caseContain(params("homeMode", "在家", "securityLevel", "基础"),
                    parts("cameraOutdoor4k", 0), "命中 else 分支"));
}
```

复杂对象构造放到 helper 中，测试方法不手写 `InferParasReq`、`BusinessRuleTestCase` 或大段 JSON。

### 3.3 诊断默认开启

本测试类覆写：

```java
@Override
protected boolean diagnosticsEnabled() {
    return true;
}
```

所有测试必须通过 `assignmentRule(...)` 入口执行，入口内部统一：

```java
RuleTransPipelineRunResult result = assertRuleTrans(...);
print(result);
return result;
```

打印内容必须包含：

- 中文自然语言输入。
- 识别出的 RuleScenario。
- 最终 Java methodBody。
- 业务测试用例 `given`、`expect`、`actual`。
- RuleUnit 执行报告。
- LLM 调用摘要和耗时。

### 3.4 业务语义 helper

新增测试 helper，用结构化方式表达用户视角的业务语义判断：

```java
assignmentRule(
        naturalLanguage,
        javaExpectation,
        caseNumsEqualThan(minCaseCount),
        caseContain(given, expect, reason))
```

示例 helper：

```java
params("homeMode", "离家", "securityLevel", "增强")
parts("cameraOutdoor4k", 1)
categoryReq("camera", "Sum_Power", "<=", 30, where("Place", "室外"))
solutionParts("cameraOutdoor4k", 1, "lockPro", 1)
hiddenParam("cameraResolution", true)
caseNumsEqual(2)
caseNumsEqualThan(2)
caseContain(params("homeMode", "离家"), parts("cameraOutdoor4k", 1), "命中 if 分支")
```

`caseContain(...)` 不要求调用方写 case id。用例编号不是核心信息，helper 应按 `given/expect` 结构查找生成用例。`caseNumsEqual(n)` 用于断言生成用例数量正好为 n，`caseNumsEqualThan(n)` 用于断言生成用例数量至少为 n。

`reason` 必须是中文，用来说明用户为什么认为这个 expect 成立。它进入 case 的 `note` 或诊断打印，不参与引擎比较，但参与人工审查。

### 3.5 逻辑完备与数据约束

生成的业务用例必须服务于业务语义校验，而不是只覆盖最容易通过的一条路径。

| 要求 | 说明 | 示例 |
| --- | --- | --- |
| 正反两面 | if-else、边界比较、数量上下限等规则至少有命中分支和未命中分支 | if 分支给 `离家+增强`，else 分支给 `在家+基础` |
| 基于现有数据 | 用例输入必须优先使用当前模型中真实存在的参数值、部件和属性值 | 若模型里只有 `红、黄`，Prompt 不应生成 `绿` 作为普通业务用例 |
| 边界可解释 | 等于上限、超过上限、低于下限等 case 要写清楚人工计算过程 | 总功耗 30 通过，31 不通过 |
| 输出完整 | 多输出规则必须在同一个或成组 case 中覆盖所有输出 | 摄像头数量和门锁数量都要进入 `expect` |

这条要求也用于反向检查 Prompt：如果 LLM 生成的业务用例凭空创造不存在的数据、只生成正向 case、或漏掉 else 分支，应优先检查测试用例生成 Prompt 和业务 JSON 生成规则。

### 3.6 失败归因流程

任何失败都按以下顺序判断：

| 判断点 | 失败表现 | 优先怀疑 |
| --- | --- | --- |
| 场景识别 | 计算赋值规则被识别成兼容类、优先类或 POST | `RuleScenarioClassifier`、场景 prompt |
| Java 生成 | methodBody 没表达自然语言逻辑，或使用错误南向 API | `RuleSnippetGenerator`、SDK profile、规则生成 prompt |
| 业务用例生成 | `given/expect` 与业务语义说明不一致，或只覆盖正向、不覆盖反向 | `RuleTestCaseGenerator`、test case prompt、业务 JSON schema |
| 编译 | Java 语法、导包、变量名、API 调用错误 | `RuleSnippetAssembler`、post processor、prompt 示例 |
| 执行转换 | 业务 case 正确但转换成 `InferParasReq` 错误 | `RuleUnitInputConverter` |
| 引擎结果 | Java 与业务 case 都正确，但 actual 逻辑错误 | `ModuleConstraintExecutor`、南向 API 实现、算法模型 |
| 结果比较 | actual 正确但比较失败 | `RuleUnitResultComparator`、输出转换 |

测试失败时应保留诊断输出，不应把失败简单归咎于“LLM 不稳定”。

---

## 4. 智能家居测试模型

### 4.1 命名原则

- 业务 code 可以使用中文，只要符合 Java 编程规范和现有注解/序列化约束。
- 如果中文 code 会降低 Java 字段、方法或断言稳定性，可以使用短英文 code，但必须在模型注释和 helper 中提供清晰中文语义。
- 中文自然语言、case 标题、scenario、note、注释都使用中文。
- option 可以使用 `离家:离家`、`Away:离家` 或项目实际支持的 code/value 形式；关键是业务用例必须能回到用户可理解的中文语义。

### 4.2 模块

```java
@ModuleAnno(id = 8017L)
public static class SmartHomeAssignmentFacts extends ModuleAlgBase {
    // 智能家居计算赋值类规则模型
}
```

### 4.3 参数

| code | 中文名称 | 类型 | 取值/说明 |
| --- | --- | --- | --- |
| `homeMode` | 家庭模式 | 枚举输入 | `在家`, `离家`, `夜间` |
| `securityLevel` | 安防等级 | 枚举输入 | `基础`, `增强` |
| `houseSize` | 户型大小 | 枚举输入 | `小户型`, `大户型` |
| `roomCount` | 房间数量 | 整数输入 | 1 到 6 |
| `totalPower` | 总功耗 | 整数输出 | 由设备功耗计算 |
| `alarmLevel` | 告警等级 | 整数输出 | 由传感器/摄像头组合计算 |
| `energyMode` | 节能模式 | 枚举输出 | `普通`, `节能` |
| `cameraResolution` | 摄像清晰度 | 枚举输入/可隐藏 | `1080P`, `4K` |

### 4.4 部件分类

| 分类 code | 中文名称 | 属性 | 部件 |
| --- | --- | --- | --- |
| `hub` | 智能中枢 | `Protocol`, `Power` | `hubBasic` 基础中枢, `hubMatter` Matter 中枢 |
| `camera` | 摄像头 | `Place`, `Resolution`, `Power`, `Score` | `cameraIndoor` 室内摄像头, `cameraOutdoor4k` 室外 4K 摄像头 |
| `sensor` | 传感器 | `Type`, `Power`, `Score` | `sensorDoor` 门窗传感器, `sensorMotion` 人体传感器, `sensorSmoke` 烟雾传感器 |
| `lock` | 智能门锁 | `Secure`, `Power`, `Score` | `lockBasic` 基础门锁, `lockPro` 高安防门锁 |
| `climate` | 温控设备 | `Power`, `Type` | `climateSmall` 小功率温控, `climateHigh` 高功率温控 |
| `light` | 智能灯光 | `Room`, `Power` | `lightHall` 客厅灯光, `lightBedroom` 卧室灯光 |
| `sceneDevice` | 场景设备 | `Scene`, `Power` | `sceneAway` 离家场景设备, `sceneNight` 夜间场景设备 |

### 4.5 注解示例

```java
@PartAnno(code = "camera")
@DAttrAnno1(code = "Place", options = {"Indoor:室内", "Outdoor:室外"})
@DAttrAnno2(code = "Resolution", options = {"P1080:1080P", "P4K:4K"})
@DAttrAnno3(code = "Power", options = {"P8:8", "P15:15"})
@DAttrAnno4(code = "Score", options = {"S1:1", "S3:3"})
private PartCategoryVar camera;

@PartAnno(fatherCode = "camera", attrs = {"室内", "1080P", "8", "1"})
private PartVar cameraIndoor;

@PartAnno(fatherCode = "camera", attrs = {"室外", "4K", "15", "3"})
private PartVar cameraOutdoor4k;
```

若实现中 `attrs` 必须使用 option code，则测试 helper 负责把中文值映射到 code。若实现允许中文 code，优先让业务用例直接显示中文值，减少用户理解成本。

---

## 5. 计算赋值类场景矩阵

本矩阵来自 `docbackup/Rule-Scenarios-And-SDK-Summary.md` 中“计算赋值类”及其约束模型子场景。每个场景给出 1 到 3 条中文自然语言规则样例。

### 5.1 参数 if-else 推导部件数量

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `newBoolVar`, `addBoolAnd`, `addBoolOr`, `addEquality`, `onlyEnforceIf`, `PartVar.quantityVar()` |
| 业务语义判断 | 输入参数命中条件时，目标部件数量等于指定值；未命中时走 else |

中文规则样例：

- 当家庭模式为离家且安防等级为增强时，室外 4K 摄像头数量为 1，否则数量为 0。
- 当户型为大户型时，Matter 中枢数量为 1，否则基础中枢数量为 1。
- 当家庭模式为夜间时，卧室灯光数量为 1，否则客厅灯光数量为 1。

验收重点：

- 正向输入能推出部件数量。
- 反向输入能推出满足条件的参数组合。
- else 分支不能错误包含 if 命中的组合。

### 5.2 多参数 if-else 推导多个输出

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `addBoolAnd`, `addEquality`, 多个 `onlyEnforceIf` |
| 业务语义判断 | 同一条件可同时约束多个部件或参数输出 |

中文规则样例：

- 当家庭模式为离家且户型为大户型时，室外 4K 摄像头数量为 1 且高安防门锁数量为 1。
- 当家庭模式为在家且安防等级为基础时，室内摄像头数量为 0 且告警等级为 1。

验收重点：

- 生成 Java 不应只覆盖第一个输出。
- 业务测试用例必须在 `expect` 中同时表达多个输出。
- 若只生成一个 expect，归因到测试用例生成或 prompt 不完整。

### 5.3 单分类数量汇总

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `sum4Quantity(filter)`, `sum4Selected(filter)`, `addLessOrEqual`, `addGreaterOrEqual` |
| 业务语义判断 | 同一分类内数量或选中数满足汇总条件 |

中文规则样例：

- 摄像头分类中最多只能选择 2 个设备。
- 传感器分类中至少要选择 1 个安防传感器。
- 智能灯光分类中卧室灯光最多配置 1 个。

验收重点：

- 这属于计算赋值类中的选择/汇总逻辑，不按兼容类处理。
- 业务 case 可以表达为给定需求后期望有解/无解，或期望输出部件数量。
- 如果场景被识别成 `COMPATIBILITY`，应修正分类器或 prompt。

### 5.4 单分类属性汇总

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `sum4Quantity(attr, filter)`, `sum4Selected(attr, filter)`, `addLessOrEqual`, `addGreaterOrEqual` |
| 业务语义判断 | 某分类内被选设备的属性总和满足约束或被计算为输出 |

中文规则样例：

- 摄像头分类中所有已选摄像头的总功耗不能超过 30 瓦。
- 传感器分类中已选传感器的安防评分总和至少为 4。
- 智能灯光分类中已选灯光的总功耗不能超过 20 瓦。

验收重点：

- 生成 Java 应使用属性聚合，而不是逐个硬编码部件。
- 测试用例应能说明为什么某个组合超过或未超过阈值。
- 若 actual 与业务语义说明冲突，优先检查属性映射和 `RuleUnitInputConverter`。

### 5.5 跨分类数量/属性汇总

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `sum4Quantity(partCategoryCodes, attr, filter)`, `sum4Selected(partCategoryCodes, attr, filter)` |
| 业务语义判断 | 多个分类的设备共同参与总量或属性计算 |

中文规则样例：

- 摄像头和门锁的安防评分总和至少为 5。
- 摄像头、门锁和传感器的总功耗不能超过 50 瓦。
- 中枢和温控设备的总功耗不能超过 35 瓦。

验收重点：

- 测试应验证跨分类合计，而不是分别对每个分类断言。
- 业务 case 的 reason 要写清楚人工加和过程。
- 如果生成代码只处理一个分类，归因到规则翻译或 SDK prompt 示例不足。

### 5.6 单选与至多单选

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `PartCategoryVar.parts()`, `PartVar.selectedVar()`, `addExactlyOne`, `addAtMostOne` |
| 业务语义判断 | 分类中的选择状态被计算/限制为 exactly one 或 at most one |

中文规则样例：

- 智能中枢必须且只能选择一个。
- 温控设备最多只能选择一个。
- 门锁分类必须选择一个门锁。

验收重点：

- 本场景按计算赋值类中的选择逻辑处理，不归入兼容类。
- 业务 case 应表达给定需求后期望的选择数量，而不是只说 compatible。
- 如果测试用例生成成 `testCompatibility`，应定位到 businessFamily/serviceMethod 生成错误。

### 5.7 整数参数计算

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `newLinearExpr`, `AlgCPLinearExpr.addTerm`, `addEquality`, `ParaVar.valueVar()`, `PartVar.quantityVar()` |
| 业务语义判断 | 整数参数等于部件数量或属性的线性组合 |

中文规则样例：

- 总功耗等于中枢、摄像头、门锁和温控设备功耗之和。
- 告警等级等于摄像头安防评分加上传感器安防评分。
- 节能评分等于 100 减去所有已选设备总功耗。

验收重点：

- 期望输出应写到 `expect.parameters`，例如 `totalPower=38`。
- 测试用例必须说明人工计算过程。
- 如果 Java 生成只添加比较约束而没有参数赋值等式，需检查翻译 prompt。

### 5.8 参数隐藏逻辑

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `ParaVar.hiddenVar()`, `PartVar.hiddenVar()`, `addBoolOr`, `addBoolAnd`, `addDifferent`, `addEquality`, `addVarAboutHiddenConstraints` |
| 业务语义判断 | 条件满足时某个参数或部件隐藏状态成立 |

中文规则样例：

- 未选择任何摄像头时，摄像清晰度参数应隐藏。
- 家庭模式为夜间时，节能模式参数应显示。
- 未选择温控设备时，温控模式参数应隐藏。

验收重点：

- 期望输出应写 `expect.parameters[].hidden` 或 `expect.parts[].hidden`。
- 若现有 RuleUnit comparator 不支持 hidden，应把 comparator 增强纳入实现计划。
- 不能把隐藏逻辑误生成为 POST 写回。

### 5.9 部件级专属计算赋值

| 项 | 内容 |
| --- | --- |
| 运行方式 | 基于约束模型 |
| 目标 API | `partVar(code)`, `PartVar.quantityVar()`, `PartVar.selectedVar()`, `attrAsInt(code)` |
| 业务语义判断 | 具体部件命中时触发具体输出，不能误伤同分类其他部件 |

中文规则样例：

- 当高功率温控设备被选择时，总功耗至少增加 20 瓦。
- 当高安防门锁被选择时，告警等级至少为 2。
- 当室外 4K 摄像头被选择时，摄像清晰度必须为 4K。

验收重点：

- 生成代码应定位具体部件或具体属性条件。
- 不能把 `lockPro` 的规则泛化到所有门锁。
- 如果实际解误伤其他部件，优先检查 Java 翻译。

---

## 6. 非本轮覆盖场景

以下场景不放入本轮测试：

| 场景 | 原因 | 后续建议 |
| --- | --- | --- |
| 参数 Requires / Incompatible / CoDependent | 兼容类规则，不是计算赋值类 | 单独保留在兼容类测试 |
| 结构化二元/三元白黑名单 | 兼容类规则 | 单独保留在结构化组合测试 |
| 多实例分类汇总 | 本轮不引入多实例计算引擎概念 | 后续如业务确有需要再单独设计，不作为本集成测试目标 |
| 显式 for/遍历生成规则 | 本轮暂不作为约束模型重点 | 运行方式为非约束模型的场景中重点评估 |
| 优先级目标函数 | 优先类 | 后续建立 `SmartHomePriorityRuleTransSystemTest` |
| POST 写回产品参数 | 非约束模型/POST | 后续建立 `SmartHomePostAssignmentRuleTransSystemTest` |
| POST 写回分类参数 | 非约束模型/POST | 同上 |
| 结构化 POST 规则 | POST + 结构化 | 等 POST 测试类建立后处理 |

---

## 7. 验收准则

### AC-001: 新增独立智能家居模型

新增 `SmartHomeAssignmentFacts`，不复用电脑、CPU、硬盘模型。模型至少包含：

- 8 个参数，覆盖输入参数、整数输出参数、可隐藏参数。
- 7 个部件分类，覆盖普通分类、多属性分类和部件专属计算所需分类；本轮不要求多实例分类。
- 每个普通分类至少 2 个具体部件。
- 所有自然语言规则、测试标题、case note 使用中文。

### AC-002: 每个测试从中文自然语言出发

每个测试必须使用中文自然语言作为主输入：

```text
当家庭模式为离家且安防等级为增强时，室外4K摄像头数量为1。
```

不允许直接把底层表达式作为主输入：

```text
homeMode=Away && securityLevel=Enhanced => cameraOutdoor4k.qty=1
```

### AC-003: 同时验证 Java、业务用例和实际结果

每个测试至少验证：

1. `methodBody` 包含关键南向 API 或等价逻辑。
2. `BusinessRuleTestCaseSet` 包含目标 case。
3. 目标 case 的 `given/expect` 与业务语义说明一致。
4. RuleUnit actual 与业务语义说明一致。

### AC-004: 业务语义判断不因执行失败而降级

当测试失败时，不能直接修改 expected 以适配 actual。必须根据失败归因矩阵判断：

- 翻译是否错。
- 测试用例生成是否错。
- Prompt 是否缺少示例或约束。
- RuleUnit 转换是否错。
- 引擎或南向 API 实现是否错。

### AC-005: 每个测试都打印诊断

`SmartHomeAssignmentRuleTransSystemTest.diagnosticsEnabled()` 必须返回 `true`。每个测试通过统一 helper 执行并调用 `print(result)`。

### AC-006: 用例必须逻辑完备

每条自然语言规则至少生成或断言一组逻辑完整的业务用例：

- if-else 规则至少包含 if 命中 case 和 else 命中 case。
- 上下限规则至少包含命中边界、未超过/未低于边界、违反边界中的两类以上。
- 多输出规则必须覆盖所有输出字段，不能只断言第一个输出。
- 生成用例必须优先使用现有模型里的真实参数值、部件和属性值，不得凭空创造普通业务数据。

### AC-007: 计算赋值类约束模型场景覆盖完整

P0 覆盖：

1. 参数 if-else 推导部件数量。
2. 多参数 if-else 推导多个输出。
3. 单分类数量汇总。
4. 单分类属性汇总。
5. 跨分类数量/属性汇总。
6. 单选与至多单选。
7. 整数参数计算。
8. 参数隐藏逻辑。
9. 部件级专属计算赋值。

### AC-008: serviceMethod 必须是 Assignment 路径

计算赋值类约束模型测试的业务 case 默认：

```json
{
  "businessFamily": "ASSIGNMENT",
  "environment": "CONSTRAINT",
  "serviceMethod": "testAssignment"
}
```

如果生成 `COMPATIBILITY` 或 `testCompatibility`，测试应失败并归因到场景分类或测试用例生成。

### AC-009: 集成用例尽量使用真实 LLM

该类作为集成测试，默认尽量使用真实 LLM，以暴露真实翻译、测试用例生成和 Prompt 设计问题。对于需要稳定复现的边界问题，可以补充 fake/录制响应测试，但不能替代真实 LLM 集成用例。

### AC-010: 不破坏既有测试

新增测试不得破坏：

- `RuleTransPipelineSystemTest`
- `RuleBusinessTestCaseGeneratorTest`
- `RuleUnitBusinessCaseTest`
- `CalculateRuleSimpleTest`
- `ParaIntegerTest`
- `ParaIsHiddenTest`
- `SearchStrategyTest`
- `SearchStrategyMultiTest`

---

## 8. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| P0-1 | 新增 `SmartHomeAssignmentRuleTransSystemTest` 并开启诊断 | P0 | 待开始 |
| P0-2 | 新增 `SmartHomeAssignmentFacts` 注解模型 | P0 | 待开始 |
| P0-3 | 增加 `assignmentRule(...)` 高层 helper，统一 assert + print | P0 | 待开始 |
| P0-4 | 增加 `caseContain(...)`、`caseNumsEqual(...)`、`caseNumsEqualThan(...)`、`params(...)`、`parts(...)`、`categoryReq(...)` 等 helper | P0 | 待开始 |
| P0-5 | 为每个测试写中文业务语义说明，并覆盖正反两面 | P0 | 待开始 |
| P0-6 | 覆盖 if-else、多个输出、单分类汇总、跨分类汇总 | P0 | 待开始 |
| P0-7 | 覆盖选择逻辑、整数参数计算、隐藏逻辑、部件级专属计算 | P0 | 待开始 |
| P0-8 | 检查 RuleScenarioClassifier 是否稳定识别 Assignment / CONSTRAINT | P0 | 待开始 |
| P0-9 | 检查 RuleTestCaseGenerator 是否生成 `testAssignment`、正确 `given/expect` 和正反用例 | P0 | 待开始 |
| P0-10 | 检查业务用例是否只使用当前模型已有参数值、部件和属性值 | P0 | 待开始 |
| P0-11 | 使用真实 LLM 运行 `SmartHomeAssignmentRuleTransSystemTest`，并保留诊断输出用于归因 | P0 | 待开始 |
| P0-12 | 运行 `mvn test -Dtest=SmartHomeAssignmentRuleTransSystemTest,RuleTransPipelineSystemTest,RuleUnitBusinessCaseTest` | P0 | 待开始 |

---

## 9. 受影响入口矩阵

| 入口/组件 | 本 RFC 关注点 |
| --- | --- |
| `RuleScenarioClassifier` | 中文计算赋值规则必须识别为 Assignment / CONSTRAINT |
| `PromptBuilder` | 规则生成 prompt 必须给足计算赋值类示例 |
| `RuleSnippetGenerator` | 生成正确南向 API 调用 |
| `RuleSnippetPostProcessor` | 不破坏 methodBody 中的变量和 API |
| `CompilationProcessor` | 编译生成规则类 |
| `RuleTestCaseGenerator` | 生成 `businessFamily=ASSIGNMENT`、`serviceMethod=testAssignment` |
| `BusinessRuleTestCaseSet` | 表达用户视角 `given/expect/note` |
| `RuleUnitInputConverter` | 把业务输入转换为正确的 `InferParasReq` |
| `DefaultModuleConstraintExecutor4SingleRule.testAssignment(...)` | 调用 `inferParas(...)` 跑约束模型 |
| `RuleUnitResultComparator` | 比较参数、部件、隐藏状态、solutions |
| `ModuleConstraintExecutor` | 真实约束求解结果是否符合规则逻辑 |

---

## 10. 复用优先清单

### 10.1 优先复用

- `RuleTransPipelineTestBase`
- `RuleTransPipelineResultPrinter`
- `RuleTransJavaExpectation`
- `RuleTransBusinessCaseExpectation`
- `RuleTransTestFixtures` 的 fixture 风格
- `CalculateRuleSimpleTest`
- `ParaIntegerTest`
- `ParaIsHiddenTest`
- `SearchStrategyTest`
- `SearchStrategyMultiTest`
- `BaseOptiTest`
- `MultiPCTest`
- `DynMultReq4MultiReqTest`
- `EnumMultReq4MultiReqTest`
- `DefaultRuleUnitTestExecutorService`
- `DefaultModuleConstraintExecutor4SingleRule.testAssignment(...)`

### 10.2 不新增

- 不新增第二套 RuleTrans pipeline。
- 不新增第二套 RuleUnit 执行器。
- 不在测试方法里手写 `InferParasReq`。
- 不在测试方法里塞大段 JSON。
- 不把兼容类、优先类、POST 混进本测试类。
- 不把“测试能通过”当成唯一验收标准。

### 10.3 可由上下文推导

- `moduleId` 从注解模型生成。
- part 所属分类从 Module 查询。
- `businessFamily` 从 RuleScenario 推导为 `ASSIGNMENT`。
- `serviceMethod` 从 family/environment 推导为 `testAssignment`。
- 业务语义说明的 reason 不参与执行比较，但必须进入诊断输出。

---

## 11. 风险与边界

### 11.1 计算赋值与兼容语义容易混淆

风险：类似“最多选择一个”既可被理解成选择约束，也可能被误分到兼容类。

策略：本测试中把选择数量、汇总数量、参数计算、隐藏状态都归入 Assignment；只有 A/B 不能同时成立、Requires、白黑名单组合才归入 Compatibility。

### 11.2 业务用例生成可能比 Java 更容易错

风险：LLM 可能生成了正确 Java，但测试用例的 `given/expect` 不符合用户逻辑。

策略：每个 case 必须带中文 reason；测试失败时优先比较 reason 与业务语义说明，再定位 `RuleTestCaseGenerator` 或 test case prompt。

### 11.3 RuleUnit 可能缺少部分输出比较能力

风险：隐藏状态、多个 solution 排序或部分聚合输出可能没有 comparator 支持。

策略：如果 actual 明显正确但 comparator 不支持，补 `RuleUnitResultComparator`；不要降低测试语义。

### 11.4 真实 LLM 输出不稳定

风险：真实 LLM 输出等价但字符串不同，导致 Java 断言脆弱。

策略：集成用例尽量使用真实 LLM，以暴露真实翻译、业务用例生成和 Prompt 问题；断言层面优先使用关键 API 包含、`caseContain(...)` 和 `caseNums...(...)` 这类语义化断言降低脆弱性。fake LLM 或录制响应只用于补充稳定复现边界问题。

---

## 12. 已确认决策

1. 本轮不引入多实例计算引擎概念，多实例分类汇总不作为本集成测试目标。

2. `for/遍历` 暂不作为约束模型场景重点，后续在运行方式为非约束模型的场景中重点评估。

3. 部件专属计算赋值是本轮需要覆盖的计算赋值类场景，纳入 P0。

4. 集成用例尽量使用真实 LLM，以真实暴露翻译、测试用例生成和 Prompt 设计问题。

5. code 可以使用中文，只要符合 Java 编程规范和现有注解、序列化、断言链路约束；如果实现上使用英文短码，也必须能回到清晰中文业务语义。

---

## 13. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `docbackup/Rule-Scenarios-And-SDK-Summary.md`
- `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`
- `doc/RFC-0014-RuleTrans-End-to-End-RuleUnit-Pipeline.md`
- `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`
- `doc/RFC-0016-RuleTrans-TestCase-Output-And-Diagnostics-Switch.md`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineSystemTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CalculateRuleSimpleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CalculateRuleIfThenTest.java`
- `src/test/java/com/jmix/scenario/ruletest/ParaIntegerTest.java`
- `src/test/java/com/jmix/scenario/ruletest/ParaIsHiddenTest.java`
- `src/test/java/com/jmix/scenario/ruletest/SearchStrategyTest.java`
- `src/test/java/com/jmix/scenario/ruletest/SearchStrategyMultiTest.java`
- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`
- `src/test/java/com/jmix/opti/base/MultiPCTest.java`
- `src/test/java/com/jmix/opti/multireq/DynMultReq4MultiReqTest.java`
- `src/test/java/com/jmix/opti/multireq/EnumMultReq4MultiReqTest.java`
- `src/main/java/com/jmix/ruleunit/DefaultRuleUnitTestExecutorService.java`
- `src/main/java/com/jmix/ruleunit/DefaultModuleConstraintExecutor4SingleRule.java`
