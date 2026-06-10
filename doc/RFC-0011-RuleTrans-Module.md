# RFC-0011: RuleTrans 模块 - 自然语言规则转换引擎

> 状态：评审中（Review）
> 日期：2026-06-08
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0007-Struct-Combination-Rule-Schema.md`, `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`, `docbackup/Rule-Scenarios-And-SDK-Summary.md`

---

## 设计决策摘要

| 主题 | 决策 |
| --- | --- |
| 模块边界 | 新增 `com.jmix.ruletrans` 包；不修改 `ModelHelper`、`ModuleGenerator`、`cengine/` 模板和既有 `com.jmix.tool` 行为 |
| 事实数据源 | `RuleContext` 以现有 `com.jmix.executor.bmodel.Module` / `PartCategory` 为事实源；Prompt 用投影视图只负责序列化，不维护第二套领域模型 |
| 上下文命名 | 保留用户反馈后的 `RuleContext` 命名，不再使用 `RuleInput` |
| 输出形态 | LLM 只输出可插入 rule 方法内部的 Java 方法体；`@CodeRuleAnno`、方法名、方法头和临时类均由模板根据规则元数据生成 |
| 组装边界 | `RuleSnippetAssembler` 独立负责把方法体组装成临时可编译、可执行的 `ModuleAlgBase` 子类和 JUnit harness；不放进 `RuleTransEngine` 主类 |
| 运行态依赖 | P0 允许生成的方法体依赖当前南向 facade：`ModuleAlgBase.model()`、`ModuleCPModel`、`PartCategoryVar`、`PartVar` 等；未来再评估完全脱离 `ModuleAlgBase` 的 IR |
| 模块级规则 | 模块级上下文分两阶段：Stage1 识别涉及的 PartCategory code，Stage2 只携带识别后的分类规格生成代码 |
| 编译反馈 | 现有 `ModuleCompiler` 只记录日志、不返回错误结构；`ruletrans` 新增结构化编译处理器，复用其 classpath/路径约定，不改原类 |
| 测试反馈 | 现有 `ModuleRunner` 只记录日志、不返回 JUnit 失败详情；`ruletrans` 新增测试执行处理器，复用 JUnit Launcher 思路并返回结构化失败信息 |
| 测试样例 | RFC 测试样例优先使用 `ModuleScenarioTestBase`、`inferRecommendModule(...)`、`printSimpleSolutions()`、`validData(...)`、`validateData(...)` |
| LLM 调用 | 通过构造注入复用 `LLMInvoker` / `LLMInvokerImpl`；不新增另一套 LLM 调用抽象 |
| 规则场景覆盖 | 以 `docbackup/Rule-Scenarios-And-SDK-Summary.md` 的规则场景矩阵为覆盖基线；当前 RuleTrans 只验证了最小 PartCategory 数量约束和模块级跨分类互斥，其他规则类型需要补场景测试 |
| SDK 上下文隔离 | 生成 Java 方法体时必须按 `POST` 与非 `POST` 分开构建 SDK 上下文；两者可共享领域事实投影，但不可共享南向 API 提示词 |
| 中文自然语言 | 中文是主要业务输入语言；分类识别、Prompt、测试语料必须覆盖中文、中文单位和中英混合字段 |
| 测试组织 | 新增规则场景测试必须放入独立子包，例如 `com.jmix.ruletrans.rulescenario`；每一种规则类型单独测试类，不与现有 `com.jmix.ruletrans` 单元测试混放 |

---

## 1. 摘要

当前 `ModelHelper` 同时承担自然语言/伪代码生成、完整测试类生成、编译、运行、结构化规则代码注入等职责。RuleTrans 的目标是把“自然语言规则 -> Java rule 方法体”的能力拆出来，形成独立模块：

```text
自然语言规则 + RuleContext
  -> Prompt 构建
  -> LLM 生成 Java rule 方法体
  -> 模板补齐 @CodeRuleAnno、方法名、方法头和临时 ModuleAlgBase 子类
  -> 编译验证
  -> 测试用例生成与执行
  -> 编译/测试失败反馈给 LLM 重试
  -> 返回最终 rule 方法体和模板组装结果
```

RuleTrans 不替代现有求解器和规则执行模型。它只负责生成可进入现有约束引擎的 rule 方法体；`@CodeRuleAnno`、方法名、方法头和临时类由 RuleTrans 模板根据 `RuleScenario`、`RuleContext`、`SdkProfile` 和规则元数据生成，并尽量复用当前工程已有的领域模型、Prompt 模板加载器、LLM 调用器、测试基类和南向 facade。

---

## 2. 动机

### 2.1 问题背景

`ModelHelper` 的早期目标是验证 AI 生成约束代码的可行性，因此把多种能力放在一起：

- `generatorModelFile` / `generatorRunModelFile`：从变量模型和伪代码生成完整测试类文件。
- `ModuleGenerator`：基于 `cengine/constraint_generate_prompt.jtl` 调 LLM 生成 Java 代码。
- `ModuleCompiler` / `ModuleRunner`：编译和运行生成的测试类。
- `autoInjectConstraintCode` / `StructCodeInjector`：把结构化规则展开或注入到目标类。

随着规则管理工程化，规则转换能力需要独立出来：

1. `ModelHelper` 保持不变，避免影响既有验证工具。
2. 新增 `ruletrans` 包承接自然语言规则转换。
3. Prompt 模板放入新的 `src/main/resources/ruletrans/` 目录，不修改 `cengine/`。
4. 生成目标从“完整测试类”收敛为“纯 Java rule 方法体”，方法头和注解由模板生成。
5. 编译、测试、纠错形成可复用管线。

### 2.2 用户反馈复核

SpecStory 中的人工输入对本 RFC 有三类关键修正：

| 用户修正 | 设计含义 | 本 RFC 处理 |
| --- | --- | --- |
| `RuleInput` 不合适，改回 `context` 或 `spec` | 输入不是一次请求 DTO，而是生成规则所需上下文 | 使用 `RuleContext`，删除 `RuleInput` |
| 原设计没有真正理解 PartCategory 数据结构 | 生成规则需要参数、动态属性、扩展属性和部件结构 | `RuleContext` 直接引用现有 `Module` / `PartCategory`，Prompt 投影覆盖 `paras`、`attrParas`、`dynAttrSchemas`、`dynAttr`、`atomicParts`、`partCategorys` |
| 代码组装不要放在主 Transfer/Engine 类里 | 主入口负责编排，方法体组装、注解生成和测试 harness 是独立后处理能力 | 新增独立 `RuleSnippetAssembler` 和测试 harness |
| 现有场景覆盖过窄 | 不能只覆盖 CPU 数量和 CPU/硬盘互斥两个最简单场景 | 以规则场景矩阵为覆盖基线，逐类补 RuleTrans 场景测试 |
| 实际业务环境以中文为主 | 英文 Prompt 和英文测试语料不足以验证真实输入 | 新增中文、中文单位、中英混合字段和中文别名识别要求 |
| 前期未明确 SDK 包装边界 | 提升 Java 方法体准确率需要把南向 API 分层暴露给生成器 | 增加 `POST` 与非 `POST` SDK 上下文隔离要求，禁止两类 API 混用 |
| 结构化规则逻辑明确 | 结构化规则转 Java 不需要自然语言大模型生成 | 后续通过小型确定性转换器处理，不把所有结构化组合写成 Prompt 样例 |

### 2.3 本轮调研新增问题

当前代码已经具备 RuleTrans 基础管线，但仍存在三个影响落地质量的问题：

1. 规则类型覆盖不足：当前 RuleTrans 测试主要覆盖 `CPU at most one` 和 `4-core CPU cannot use 5400 drive`，只能证明最小 PartCategory 数量约束和模块级跨分类互斥可走通，不能证明参数兼容、计算、隐藏、优先级、多实例、POST、结构化组合等规则类型可生成。
2. 中文输入不足：现有 RuleTrans 测试与 Prompt 样例主要是英文；实际业务输入会出现“最多一块”“四核”“转速 5400 转”“固态盘”“硬盘”等中文描述、中文单位和业务别名。
3. SDK 上下文混合风险：非 `POST` 规则基于约束模型构建 CP 变量、表达式和约束；`POST` 规则基于求解后的实例 view 读写参数和实例属性。两者依赖完全不同，生成 Java 方法体时必须分别构建上下文、模板和可用 API 列表。

本 RFC 的后续增强不要求把每一种旧规则场景都写成 Prompt 样例。已有类型只补覆盖测试和能力描述；只有新增语义类型、中文表达模式或新的 SDK 使用边界，才允许增加少量高价值样例。结构化规则未来优先走确定性转换器，不依赖大模型做自由代码生成。

### 2.4 具体场景

#### 场景 1：PartCategory 级规则

输入：

```text
CPU 最多配置一块
```

上下文：`cpu` 分类的 `PartCategory`，包含部件、参数、动态属性、扩展属性和选择策略。

输出分两层：

```java
model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
```

LLM 只输出以上方法体。RuleTrans 模板再根据规则元数据组装完整方法：

```java
@CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块", fatherCode = "cpu")
public void ruleCpuAtMostOne() {
    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
}
```

#### 场景 2：模块级跨分类规则

输入：

```text
四核 CPU 不能兼容转速为 5400 转的硬盘
```

上下文：整个 `Module`。执行分两阶段：

1. Stage1：从自然语言识别涉及 `cpu`、`drive` / `hdd` 等 PartCategory。
2. Stage2：只把识别到的分类规格投影给 LLM 生成 rule 方法体。

#### 场景 3：编译错误纠错

LLM 返回的方法体经模板组装后编译失败时，RuleTrans 捕获 javac 错误、上一版方法体、自然语言和上下文摘要，构建纠错 Prompt 后重试。方法头、注解和方法名仍由模板生成，不交给 LLM 修正。

#### 场景 4：测试用例驱动纠错

编译通过后，RuleTrans 可以让 LLM 生成结构化测试用例，再把测试用例转成临时 JUnit 测试。测试失败时，把失败 case 的输入、预期、实际结果反馈给 LLM 判断是否需要修正规则逻辑。

---

## 3. 范围与非目标

### 3.1 P0 范围

- 新增 `com.jmix.ruletrans` 包骨架。
- 新增 `RuleContext`、`PartCategoryRuleContext`、`ModuleRuleContext` 及工厂。
- 新增 Prompt 投影器，把现有 `Module` / `PartCategory` 转成 LLM 友好的结构化文本或 JSON。
- 新增 `RuleSnippetGenerator`，复用 `LLMInvoker`。
- 新增 `RuleSnippetAssembler`，把 rule 方法体组装为临时 `ModuleAlgBase` 子类，并生成可执行 JUnit harness。
- 新增结构化 `CompilationProcessor`，捕获编译成功/失败和错误详情。
- 新增 `RuleTestCaseGenerator`，生成结构化测试用例。
- 新增 `TestExecutionProcessor`，把测试用例转成 JUnit 测试并返回结构化测试结果。
- `CompilationProcessor` / `TestExecutionProcessor` 允许在 RuleTrans 内复制少量 `ModuleCompiler` / `ModuleRunner` 的 classpath 与 JUnit Launcher 逻辑，以换取结构化返回。
- P0 必须一步到位支持“自然语言/结构化输入 -> 方法体 -> 模板组装 -> 编译 -> 可执行测试 harness -> 结构化结果”，不能以 fake LLM 单元测试替代验收。
- 新增模块级 `CategoryIdentifier`，完成 Stage1 分类识别和结果校验。
- 保证 `ModelHelper` 与现有 `cengine/` Prompt 不受影响。

### 3.2 P1 范围

- 扩大规则族覆盖和中文语料规模。
- 为结构化二元/三元组合、结构化 POST 规则补确定性转换器。
- 评估脱离 `ModuleAlgBase` 的中间 IR 或规则 Schema 生成路径。

### 3.3 非目标

- 不重构 `ModelHelper`。
- 不修改 `LLMInvoker` 接口。
- 不在 P0 中设计完整规则管理 UI。
- 不新增第二套 `Module` / `PartCategory` 领域模型。
- 不在 RFC 中要求产品建模人员维护 `ParaSpec`、`DynamicAttrSpec` 等重复 DTO。
- 不直接暴露 OR-Tools 类型或 `com.jmix.executor.impl.*` 内部实现给生成代码。

---

## 4. 现有代码事实

### 4.1 领域模型继承链

当前 `PartCategory` 的真实结构如下：

```text
com.jmix.executor.bmodel.Onto
├── paras: List<Para>
├── attrParas: List<AttrPara>
├── rules: List<Rule>
├── dynAttr: Map<String, String>
├── dynAttrSchemas: List<DynamicAttribute>
└── dynAttrSchema: String

  └── com.jmix.executor.bmodel.ModuleBase
      ├── atomicParts: List<Part>
      └── partCategorys: List<PartCategory>

        └── com.jmix.executor.bmodel.PartCategory
            ├── partType: PartType
            ├── supportMultiInst: boolean
            └── selectionPolicy: PartCategorySelectionPolicy
```

因此 RuleTrans 的上下文必须覆盖：

| 数据 | 来源 | 生成规则时用途 |
| --- | --- | --- |
| 参数 | `Onto.paras` / `Para` | 可配置选项、输入参数、参数兼容关系 |
| 属性参数 | `Onto.attrParas` / `AttrPara` | `Sum_Quantity`、`Sum_Capacity`、`SumSum_Capacity` 等汇总语义 |
| 动态属性定义 | `Onto.dynAttrSchemas` / `DynamicAttribute` | Part 的规格字段，如 `CoreNum`、`Speed`、`Capacity` |
| 扩展属性值 | `Onto.dynAttr` | 建模侧补充信息和扩展属性 |
| 原子部件 | `ModuleBase.atomicParts` / `Part` | 部件 code、最大数量、价格、属性值 |
| 子分类 | `ModuleBase.partCategorys` | 嵌套分类和继承结构 |
| 选择策略 | `PartCategory.selectionPolicy` | 必选/可选分类语义 |
| 多实例 | `PartCategory.supportMultiInst` | 多实例规则生成和测试用例 |

### 4.2 可复用工具

| 能力 | 当前类 | 复用方式 |
| --- | --- | --- |
| LLM 调用 | `LLMInvoker` / `LLMInvokerImpl` | 构造注入，不新增抽象 |
| Prompt 模板渲染 | `PromptTemplateLoader` | 复用 `loadAndRenderTemplate(...)` |
| 注解生成 Module | `ModuleGenneratorByAnno` | 可作为 `RuleContextFactory.fromAnnotatedClass(...)` 的来源 |
| 编译 classpath 约定 | `ModuleCompiler` | 复用约定；新增结构化结果处理器 |
| JUnit 运行方式 | `ModuleRunner` | 复用思路；新增结构化结果处理器 |
| 测试 helper | `ModuleScenarioTestBase` | 生成测试优先调用高层 helper |
| 南向 facade | `ModuleAlgBase` / `ModuleCPModel` / `PartCategoryCPModel` | 生成代码只使用 facade，不导入 `impl` 或 OR-Tools |

### 4.3 现有工具的边界

`ModuleCompiler.compile(...)` 返回 `void`，只输出日志。`ModuleRunner.runTestFile(...)` 也返回 `void`，失败详情只进入日志。因此 RuleTrans 不能把它们简单包装成 `CompilationResult` / `TestResult`。正确做法是：

- 保留原类不变。
- 新增 RuleTrans 内部处理器，复用现有 classpath、输出目录和 JUnit Launcher 方式。
- 新处理器必须返回结构化结果，供纠错 Prompt 使用。

### 4.4 当前 RuleTrans 代码状态

截至 2026-06-08，`com.jmix.ruletrans` 已经实现了基础管线，RFC 早期实现计划中的“待开始”不再准确。当前代码事实如下：

| 能力 | 代码证据 | 当前状态 |
| --- | --- | --- |
| RuleTrans 编排入口 | `RuleTransEngine.translate(...)`、`translateWithRetry(...)` | 已实现基础生成、编译纠错、测试纠错编排 |
| 上下文构建 | `RuleContextFactory`、`PartCategoryRuleContext`、`ModuleRuleContext` | 已支持从 `Module` 和注解类构建上下文 |
| Prompt 投影 | `RulePromptProjector`、`PartCategoryPromptView`、`ModulePromptView` | 已覆盖参数、属性参数、动态属性、部件、子分类 |
| 模块级分类识别 | `CategoryIdentifier`、`module_stage1_prompt.jtl` | 已支持 LLM 输出分类 code 并校验存在性 |
| 非 POST 代码生成 | `part_category_prompt.jtl`、`module_stage2_prompt.jtl` | 已偏向 `model()`、`sum4Selected`、`sum4Quantity`、`inCompatible` 等约束模型 API |
| 方法体后处理 | `RuleSnippetPostProcessor` | 当前类名可保留；目标职责调整为提取/校验 method body，并拒绝 package/import/class/annotation/method declaration |
| 编译验证 | `RuleSnippetAssembler.assembleCompileUnit(...)`、`CompilationProcessor` | 已返回结构化编译结果 |
| 生成测试执行 | `RuleTestCaseGenerator`、`assembleExecutableTest(...)`、`TestExecutionProcessor` | 已支持基础 validate/recommend 测试生成和 JUnit 执行 |
| IR 评估 | `RuleTransIrEvaluator` | 已确认当前仍绑定 `ModuleAlgBase`，Schema emission 尚未 ready |

当前 RuleTrans 测试仍以 `src/test/java/com/jmix/ruletrans` 中的最小样例为主，主要验证“基础管线可运行”，还没有按规则类型建立完整场景覆盖。

### 4.5 规则场景核对结论

本节以 `docbackup/Rule-Scenarios-And-SDK-Summary.md` 的矩阵为基线。这里的“规则引擎状态”表示现有南向 SDK/执行器是否已有代表测试；“RuleTrans 状态”表示自然语言到 Java 方法体模块是否已有对应生成能力和测试。

| 场景 | 规则引擎状态 | RuleTrans 当前状态 | 后续处理 |
| --- | --- | --- | --- |
| 部件分类最小数量/单分类数量汇总 | 已覆盖：`BaseOptiTest`、`RuleSnippetAssemblerTest` 的 `cpuAtMostOneSnippet` | 已基础支持 | 补中文“CPU 最多配置一块/最多一块处理器”用例 |
| 模块级跨分类部件互斥 | 已覆盖：`MultiPCTest`、`OptionalPartCategoryWhitelistGuardTest` | 已基础支持：`CategoryIdentifierTest`、`RuleTransModuleTest` | 补中文分类别名、中文单位、validate/recommend 双路径 |
| 参数 if-else 反推部件数量 | 已覆盖：`CalculateRuleSimpleTest`、`CalculateRuleIfThenTest` | 未覆盖 | 新增规则类型场景测试，不需要新增大量 Prompt 样例 |
| 参数 Requires | 已覆盖：`CompatibleRuleRequireTest` | 未覆盖 | 新增参数兼容规则生成/编译/执行测试 |
| 参数 Incompatible | 已覆盖：`CompatibleRuleIncompatibleTest` | 未覆盖 | 新增参数互斥规则生成/编译/执行测试 |
| 参数 CoDependent | 已覆盖：`CompatibleRuleCodependentTest` | 未覆盖 | 新增参数同选同不选规则测试 |
| 整数参数计算 | 已覆盖：`ParaIntegerTest` | 未覆盖 | 新增中文“数量等于两个参数之和”等测试 |
| 参数隐藏逻辑 | 已覆盖：`ParaIsHiddenTest` | 未覆盖 | 新增隐藏 API 上下文和场景测试 |
| 单选/至多单选 | 已覆盖：`SearchStrategyTest`、`SearchStrategyMultiTest` | 部件分类最小变体已覆盖，通用选择策略未覆盖 | 补 exactly-one、at-most-one 差异测试 |
| 跨分类数量/属性汇总 | 已覆盖：`MultiPCTest`、`CrossPartCategoryTotalConstraintTest` | 未覆盖完整场景 | 补 `sum4Quantity(partCategoryCodes, attr, filter)` 生成测试 |
| 多实例分类汇总 | 已覆盖：`DynMultReq4MultiReqTest`、`EnumMultReq4MultiReqTest` | 未覆盖 | 补 `drive*`、`SumSum`、实例展开语义测试 |
| 优先级目标函数 | 已覆盖：`BaseOptiTest`、`MultiPCTest` | 未覆盖 | 补 `@PriorityRuleAnno` 或后续 Schema/转换器设计，不强塞到 CodeRule Prompt |
| CodeRule 校验型组合 | 已覆盖：`CodeRuleOnlyValidateTest` | 部分覆盖产品互斥，未覆盖完整校验诊断 | 补 violated rule code 断言 |
| 结构化二元/三元白名单 | 已覆盖：`StructCombinationRuleTest` | 未作为 RuleTrans 生成能力覆盖；建议后置 | 未来用确定性结构化转换器，不用自然语言大模型自由生成 |
| 结构化二元/三元黑名单 | 已覆盖：`StructCombinationOtherRuleTest` | 未作为 RuleTrans 生成能力覆盖；建议后置 | 同上 |
| 结构化 + 非结构化混合 | 已覆盖：`StructCodeRuleMixedValidateTest` | 未覆盖 | 先补混合校验场景，再评估转换器边界 |
| POST 后置计算写回产品参数 | 已覆盖：`PostCalcRuleTest` | 未覆盖，且当前 Prompt 不应使用非 POST API | 必须新增 POST 独立上下文和模板 |
| POST 后置计算写回分类参数 | 已覆盖：`PostCalcRuleTest` | 未覆盖，且当前 Prompt 不应使用非 POST API | 必须新增 POST 独立上下文和模板 |
| 部件级专属规则 | 文档矩阵标记 TODO | RuleTrans 未覆盖 | 先补规则引擎极简测试，再接入 RuleTrans |
| 结构化部件分类级规则 | 文档矩阵标记 TODO | RuleTrans 未覆盖 | 先补结构化执行样例，再做转换器 |
| 结构化 POST 规则 | 文档矩阵标记 TODO | RuleTrans 未覆盖 | 后置；明确结构化 POST 转换规则 |
| 显式 for/遍历生成规则 | 文档矩阵标记 TODO | RuleTrans 未覆盖 | 仅当聚合 API 不能表达时新增样例 |
| 模块级参数优先类样例 | 文档 TODO 清单标记缺失 | RuleTrans 未覆盖 | 补参数目标函数/偏好测试 |
| 部件级 POST 写回样例 | 文档 TODO 清单标记缺失 | RuleTrans 未覆盖 | 补 `part(code).setQuantity` / `setDynAttr` 测试 |

### 4.6 RuleTrans 未支持或未验证清单

以下清单是后续实现必须专门补齐的内容：

1. 参数相关非 POST 规则：参数 Requires、Incompatible、CoDependent、if-else、整数参数计算、参数隐藏。
2. 汇总/选择/优先级规则：跨分类数量/属性汇总、多实例 `SumSum`、exactly-one/at-most-one、优先级目标函数、模块级参数优先。
3. POST 规则：模块级参数写回、部件分类级参数写回、部件级实例写回、动态属性读写。POST 必须使用实例 view SDK，不得复用非 POST Prompt。
4. 结构化规则：二元/三元白名单、黑名单、结构化部件分类级、结构化 POST、结构化 + Code 混合。结构化规则不作为自由自然语言生成优先项，后续走确定性转换器。
5. 部件级规则：绑定具体 `Part` 或 Feature Code 的兼容、计算、隐藏或 POST 写回。
6. 显式遍历规则：需要 `partVars(filter)` 或 `PartCategoryVar.parts(filter)` 循环生成的规则。
7. 中文自然语言：中文规则、中文单位、中文部件别名、中英混合属性名、中文同义词识别。
8. 基础能力抽象测试：如果场景失败源于分类识别、过滤表达式、上下文投影或测试 harness，应新增对应的极简 API 测试，而不是只在端到端场景里调 Prompt。

---

## 5. 设计方案

### 5.1 总体流程

```text
RuleTransEngine.translateWithRetry(...)
  |
  |-- validate naturalLanguage + RuleContext
  |
  |-- ModuleRuleContext?
  |     |
  |     |-- yes: CategoryIdentifier.identify(...)
  |     |        validate category codes against Module.getPartCategory(...)
  |     |
  |     |-- no: use PartCategoryRuleContext.category
  |
  |-- RulePromptProjector.project(...)
  |
  |-- PromptBuilder.buildGeneratePrompt(...)
  |
  |-- RuleSnippetGenerator.generateMethodBody(...)
  |
  |-- RuleSnippetAssembler.assembleCompileUnit(...)
  |
  |-- CompilationProcessor.compile(...)
  |     |
  |     |-- failed: build compilation correction prompt and retry
  |
  |-- RuleTestCaseGenerator.generate(...)
  |
  |-- TestExecutionProcessor.execute(...)
        |
        |-- failed and is rule logic error: build test correction prompt and retry
        |-- failed but not logic error: return code + diagnostic report
```

### 5.1.1 规则类型路由

RuleTrans 不能只按 `ModuleRuleContext` / `PartCategoryRuleContext` 选择模板。生成 Java 方法体前必须先判断规则运行环境和规则族：

```text
中文/英文自然语言 + RuleContext
  -> RuleScenarioClassifier
      -> scope: PRODUCT | PART_CATEGORY | PART
      -> calcStage: NON_POST | POST
      -> ruleFamily: COMPATIBLE | CALCULATE | AGGREGATE | SELECT | HIDDEN | PRIORITY | STRUCTURED | UNKNOWN
  -> SdkContextBuilder
      -> ConstraintSdkContext 或 PostSdkContext
  -> PromptBuilder 选择独立模板或确定性转换器
```

说明：

- `scope` 决定事实投影范围：产品、部件分类、具体部件。
- `calcStage` 决定 SDK 上下文：非 `POST` 使用约束模型 API；`POST` 使用实例 view API。
- `ruleFamily` 决定进入大模型自由代码生成、确定性转换器或暂不支持路径。
- 分类识别失败、规则族识别失败、POST/非 POST 识别不确定时，必须返回结构化失败或进入人工确认，不允许把全量 SDK API 都塞给同一个 Prompt。

### 5.1.2 POST 与非 POST SDK 上下文隔离

`POST` 和非 `POST` 规则只允许共享领域事实投影，不允许共享南向 API 提示词。两类上下文必须独立构建：

| 上下文 | 运行阶段 | 可用 SDK | 禁止内容 | 典型规则 |
| --- | --- | --- | --- | --- |
| `ConstraintSdkContext` | 非 `POST`，CP-SAT 求解前/中 | `model()`、`para(...)`、`partVar(...)`、`partCategoryVar(...)`、`partVars(...)`、`sum4Selected`、`sum4Quantity`、`addBoolAnd`、`addImplication`、`inCompatible`、`setObjectExpr` | `currentInst()`、`parameter().setValue(...)`、`partCategorySum(...)`、实例 view 写回 | 兼容、选择、隐藏、汇总、优先级、if-else 约束 |
| `PostSdkContext` | `CalcStage.POST`，已有配置解之后 | `currentInst()`、`parameter(code).value/setValue`、`part(code)`、`partCategory(code)`、`partCategorySum(code)`、`sumDynAttr4Int`、`dynAttr`、`setDynAttr`、`sumQuantity` | `model()`、CP 变量、`onlyEnforceIf`、`addLessOrEqual`、`inCompatible`、目标函数 | 后置计算、参数写回、实例属性写回 |

上下文构建要求：

- 模块级非 `POST` 与部件分类级非 `POST` 可以共享 `ConstraintSdkPromptView` 字段结构，但 target categories、可见参数和可见部件范围必须按 scope 裁剪。
- 模块级 `POST` 与部件分类级 `POST` 可以共享 `PostSdkPromptView` 字段结构，但必须区分 `parameter(code)` 和 `partCategory(code).parameter(code)` 的写回目标。
- `@CodeRuleAnno(calcStage = CalcStage.POST)` 必须由 POST 模板显式生成；非 POST 模板不得写 `calcStage = CalcStage.POST`。LLM 输出中不得包含注解。
- `RuleSnippetPostProcessor` 应增加 SDK profile 静态校验：非 POST 方法体出现 `currentInst` / `setValue` 判失败；POST 方法体出现 `model()` / CP 约束 API 判失败。

### 5.1.3 结构化规则转换边界

结构化规则的逻辑明确，后续不应依赖大模型从自然语言自由生成 Java 代码。处理策略如下：

| 输入类型 | 处理路径 | 说明 |
| --- | --- | --- |
| 结构化二元/三元组合 Schema 或注解 | 确定性转换器 | 直接展开为结构化规则或等价 `CodeRule`，不调用 LLM |
| 中文自然语言描述但可稳定解析为结构化组合 | 先生成结构化 IR，再由转换器生成 Java/Schema | LLM 可用于抽取 IR，但不直接写 Java |
| 混合结构化 + CodeRule | 结构化部分走转换器，非结构化部分走 RuleTrans | 分开测试、分开诊断 |
| POST 结构化计算 | 后置专用转换器 | 输出 `CalcStage.POST` 和实例 view API |

Prompt 样例控制原则：

- 新增类型或新增 SDK 上下文时允许加入一个最小样例。
- 已有类型只补矩阵测试，不再往 Prompt 中堆相似样例。
- 例如已有比例、汇总或 `1:20` 这类逻辑时，不再添加定制化自然语言样例。

### 5.1.4 中文自然语言支持

中文能力是 RuleTrans 的一等验收项，不是 Prompt 附属优化。设计要求：

- Stage1 分类识别必须支持中文名称、中文别名、英文 code 混写，例如“处理器/CPU/cpu”“硬盘/drive”“转速/Speed”。
- Stage2 生成必须支持中文单位和规格表达，例如“四核”“5400 转”“2T”“最多一块”“不能同时选”。
- Prompt 中的上下文视图应同时提供 `code`、`description/name`、动态属性 code、可选中文显示值；不能只依赖英文自然语言样例。
- 测试中必须包含中文输入、英文 code、中文别名、中英混合描述四类用例。
- 日志仍遵守项目规范使用英文，`normalNaturalCode` 可以保留用户中文原文。

### 5.1.5 场景测试组织

新增 RuleTrans 场景测试必须独立于现有 `com.jmix.ruletrans` 单元测试：

```text
src/test/java/com/jmix/ruletrans/rulescenario/
├── compatible/
│   ├── ParameterRequiresRuleTransScenarioTest.java
│   ├── ParameterIncompatibleRuleTransScenarioTest.java
│   └── ParameterCoDependentRuleTransScenarioTest.java
├── calculate/
│   ├── IfThenCalculateRuleTransScenarioTest.java
│   └── IntegerParameterCalculateRuleTransScenarioTest.java
├── aggregate/
│   ├── SingleCategoryAggregateRuleTransScenarioTest.java
│   ├── CrossCategoryAggregateRuleTransScenarioTest.java
│   └── MultiInstanceAggregateRuleTransScenarioTest.java
├── post/
│   ├── ModulePostCalcRuleTransScenarioTest.java
│   ├── PartCategoryPostCalcRuleTransScenarioTest.java
│   └── PartPostWriteBackRuleTransScenarioTest.java
├── structured/
│   ├── PairCombinationConverterScenarioTest.java
│   └── TripleCombinationConverterScenarioTest.java
└── identify/
    └── ChineseCategoryIdentifierScenarioTest.java
```

规则：

- 每一种规则类型独立测试类，不和 `RuleTransModuleTest`、`RulePromptProjectorTest` 等基础单元测试混在一起。
- `Scenario` 测试定位为整体拉通：自然语言/结构化输入 -> 分类识别 -> SDK 上下文 -> Java 方法体/转换器 -> 模板组装 -> 编译 -> validate/recommend/POST 执行。
- 如果场景失败来自基础模块能力不足，例如分类识别、过滤表达式、上下文投影或中文别名识别，应在相邻子包新增极简能力测试，先锁定基础问题，再回到场景测试。

### 5.2 包结构

```text
com.jmix.ruletrans
├── RuleTransEngine.java
├── RuleTransException.java
├── CategoryNotFoundException.java
│
├── context/
│   ├── RuleContext.java
│   ├── PartCategoryRuleContext.java
│   ├── ModuleRuleContext.java
│   └── RuleContextFactory.java
│
├── scenario/
│   ├── RuleScenarioClassifier.java
│   ├── RuleScenario.java
│   ├── RuleScope.java
│   ├── RuleCalcStage.java
│   └── RuleFamily.java
│
├── sdk/
│   ├── SdkContextBuilder.java
│   ├── SdkContext.java
│   ├── ConstraintSdkContext.java
│   ├── PostSdkContext.java
│   └── SdkProfile.java
│
├── prompt/
│   ├── PromptBuilder.java
│   ├── RulePromptProjector.java
│   ├── PartCategoryPromptView.java
│   └── ModulePromptView.java
│
├── identifier/
│   └── CategoryIdentifier.java
│
├── generator/
│   ├── RuleSnippetGenerator.java
│   └── RuleSnippetPostProcessor.java
│
├── converter/
│   ├── StructRuleConverter.java
│   ├── PairCombinationRuleConverter.java
│   ├── TripleCombinationRuleConverter.java
│   └── PostStructRuleConverter.java
│
├── assembler/
│   ├── RuleSnippetAssembler.java
│   └── RuleTransTempFileManager.java
│
├── postprocessor/
│   ├── CompilationProcessor.java
│   ├── CompilationResult.java
│   ├── TestExecutionProcessor.java
│   ├── TestExecutionResult.java
│   └── FailedTestCase.java
│
└── testgen/
    ├── RuleTestCaseGenerator.java
    └── RuleTransTestCaseSet.java
```

模板目录：

```text
src/main/resources/ruletrans/
├── part_category_prompt.jtl
├── module_stage1_prompt.jtl
├── module_stage2_prompt.jtl
├── constraint_sdk_prompt.jtl
├── post_module_prompt.jtl
├── post_part_category_prompt.jtl
├── post_part_prompt.jtl
├── correction_compilation_prompt.jtl
├── test_case_prompt.jtl
└── correction_test_prompt.jtl
```

### 5.3 RuleContext

`RuleContext` 是生成规则的上下文入口，不是重复领域 DTO。

```java
package com.jmix.ruletrans.context;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.PartCategory;

import java.util.List;

public interface RuleContext {

    boolean isModuleLevel();

    Module module();

    List<PartCategory> targetCategories();

    default List<String> categoryCodes() {
        return targetCategories().stream()
                .map(PartCategory::getCode)
                .toList();
    }

    String summary();
}
```

PartCategory 级上下文：

```java
public final class PartCategoryRuleContext implements RuleContext {
    private final Module module;
    private final PartCategory category;

    @Override
    public boolean isModuleLevel() {
        return false;
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public List<PartCategory> targetCategories() {
        return List.of(category);
    }
}
```

模块级上下文：

```java
public final class ModuleRuleContext implements RuleContext {
    private final Module module;
    private final List<PartCategory> selectedCategories;

    @Override
    public boolean isModuleLevel() {
        return true;
    }

    @Override
    public Module module() {
        return module;
    }

    @Override
    public List<PartCategory> targetCategories() {
        return selectedCategories;
    }
}
```

工厂建议：

```java
public final class RuleContextFactory {

    public PartCategoryRuleContext partCategory(Module module, String categoryCode);

    public ModuleRuleContext module(Module module);

    public ModuleRuleContext module(Module module, List<String> categoryCodes);

    public ModuleRuleContext fromAnnotatedClass(Class<?> algClass, String tempResourcePath);
}
```

说明：

- 如果调用方已有 `Module` / `PartCategory`，直接构建上下文。
- 如果调用方只有注解类，可通过 `ModuleGenneratorByAnno.build(...)` 得到 `Module`。
- 工厂必须调用 `module.init()` 或等价初始化，确保 `getPartCategory(...)`、`getAllPartCategorys()` 等查询可用。

### 5.4 Prompt 投影视图

Prompt 投影视图是 LLM 输入格式，不是领域真相来源。它可以用 record 或普通类表达：

```java
public record PartCategoryPromptView(
        String code,
        String name,
        String partType,
        boolean supportMultiInst,
        String selectionPolicy,
        List<ParaPromptView> paras,
        List<AttrParaPromptView> attrParas,
        List<DynamicAttrPromptView> dynAttrSchemas,
        Map<String, String> dynAttr,
        List<PartPromptView> atomicParts,
        List<PartCategoryPromptView> childCategories) {
}
```

投影规则：

| 投影字段 | 从哪里来 | 注意事项 |
| --- | --- | --- |
| `paras` | `PartCategory.getParas()` | `Para` 继承 `DynamicAttribute`，选项来自 `getOptions()` |
| `attrParas` | `PartCategory.getAttrParas()` | 类型是 `AttrParaType.Sum` / `SumSum` / `Org`，不是旧文档中的全大写 `SUM` |
| `dynAttrSchemas` | `PartCategory.getDynAttrSchemas()` | 必须包含 `dynAttrType`、`instType`、`options` |
| `dynAttr` | `PartCategory.getDynAttr()` | 表达扩展属性值 |
| `atomicParts` | `PartCategory.getAtomicParts()` | `Part` 自身也继承 `Onto`，可带 `dynAttr` |
| `childCategories` | `PartCategory.getPartCategorys()` | 保留嵌套结构，不扁平丢失 |

不要要求调用方手写 `ParaSpec`、`DynamicAttrSpec`、`PartSpec`。如果实现内部为了 Prompt 序列化使用这些类，必须由投影器从现有领域对象自动生成。

### 5.5 模块级 Stage1 分类识别

`CategoryIdentifier` 只负责识别 PartCategory code：

```java
public final class CategoryIdentifier {
    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;

    public List<String> identify(String naturalLanguage, Module module);
}
```

流程：

1. 从 `module.getAllPartCategorys()` 取可用分类列表。
2. Prompt 中提供分类 code、名称、动态属性摘要和示例部件短摘要。
3. LLM 输出 JSON 数组，例如 `["cpu", "drive"]`。
4. 解析后必须用 `module.getPartCategory(code)` 校验。
5. 空结果或不存在的 code 抛出 `CategoryNotFoundException`。

Stage1 不生成代码，也不读取完整部件列表，避免 Prompt 过大。

### 5.6 PromptBuilder

```java
public final class PromptBuilder {

    public String buildGeneratePrompt(
            String naturalLanguage,
            RuleContext context,
            RuleScenario scenario,
            SdkContext sdkContext);

    public String buildModuleStage1Prompt(
            ModulePromptView view,
            String naturalLanguage);

    public String buildConstraintPrompt(
            String naturalLanguage,
            RuleContext context,
            ConstraintSdkContext sdkContext);

    public String buildPostPrompt(
            String naturalLanguage,
            RuleContext context,
            PostSdkContext sdkContext);

    public String buildCompilationCorrectionPrompt(
            String previousMethodBody,
            CompilationResult compilationResult,
            String naturalLanguage,
            RuleContext context,
            SdkProfile sdkProfile);

    public String buildTestCorrectionPrompt(
            String previousMethodBody,
            TestExecutionResult testResult,
            String naturalLanguage,
            RuleContext context,
            SdkProfile sdkProfile);
}
```

模板约束：

- 使用 `PromptTemplateLoader.loadAndRenderTemplate("ruletrans/xxx.jtl", variables)`。
- 不修改 `src/main/resources/cengine/constraint_generate_prompt.jtl`。
- Prompt 必须说明生成代码运行在 `ModuleAlgBase` 子类中。
- Prompt 必须要求只输出 Java 方法体。
- Prompt 必须禁止输出 package、import、class 声明、`@CodeRuleAnno`、方法名和方法声明。
- Prompt 必须要求日志使用英文。
- 非 `POST` Prompt 只能推荐约束模型 facade，例如 `model()`、`para(...)`、`partCategoryVar("code")`、`partVar("code")`、`partVars("Attr=value")`、`model().sum4Selected(...)`、`model().sum4Quantity(...)`。
- `POST` Prompt 只能推荐实例 view facade，例如 `currentInst()`、`parameter(code).setValue(...)`、`partCategory(code).parameter(code).setValue(...)`、`partCategorySum(code).sumDynAttr4Int(...)`。
- 纠错 Prompt 必须携带 `SdkProfile`，并明确禁止跨 profile 修复，例如不能用 `model()` 修 POST 规则，也不能用 `setValue(...)` 修非 POST 规则。

### 5.7 方法体生成与模板组装契约

LLM 生成结果只允许是 Java 方法体，不允许包含注解、方法头、方法名、类、import 或 package。

非 POST 互斥规则方法体示例：

```java
inCompatible(ruleCode, "cpu:CoreNum=4", "drive:Speed=5400");
```

非 POST CP facade 方法体示例：

```java
model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
```

POST 方法体只使用实例 view，不输出 `calcStage`：

```java
int sum = partCategorySum("drive").sumDynAttr4Int("Capacity");
parameter("pDriveSumCapacity").setValue(String.valueOf(sum));
```

模板组装完整方法时负责补齐注解、方法名、方法头和稳定 rule code：

```java
@CodeRuleAnno(normalNaturalCode = "四核 CPU 不能兼容转速为 5400 转的硬盘")
public void ruleCpu4NotDrive5400() {
    String ruleCode = "ruleCpu4NotDrive5400";
    inCompatible(ruleCode, "cpu:CoreNum=4", "drive:Speed=5400");
}
```

禁止：

- 输出完整 Java 类。
- 输出 package/import。
- 输出 `@CodeRuleAnno`。
- 输出方法声明或自行生成方法名。
- 直接使用 `com.google.ortools.*` 类型。
- 直接导入或依赖 `com.jmix.executor.impl.*`。
- 使用旧样例中的内部字段形式，如 `this.partVar.qty`、`getIsSelectedVar()`。
- 在非 `POST` 方法体中使用实例 view 写回 API。
- 在 `POST` 方法体中使用 `model()`、CP 变量或约束 API。

模板组装规则：

- `RuleScenario`、`RuleContext`、`SdkProfile` 和规则元数据决定 `@CodeRuleAnno` 参数。
- 方法名和 rule code 由模板生成并保证唯一，LLM 不参与命名。
- 当方法体需要传入 `ruleCode` 参数时，只能引用模板注入的局部变量 `ruleCode`，不得自行写新的 rule code 字符串。
- 编译纠错和测试纠错只修正上一版方法体；注解、方法头和方法名仍由模板生成。

### 5.8 RuleSnippetGenerator

```java
public final class RuleSnippetGenerator {
    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;
    private final RulePromptProjector projector;

    public String generateMethodBody(String naturalLanguage, RuleContext context);

    public String generateMethodBodyWithCompilationCorrection(
            String previousMethodBody,
            CompilationResult compilationResult,
            String naturalLanguage,
            RuleContext context);

    public String generateMethodBodyWithTestCorrection(
            String previousMethodBody,
            TestExecutionResult testResult,
            String naturalLanguage,
            RuleContext context);
}
```

构造要求：

- 默认构造可使用 `new LLMInvokerImpl()`。
- 单元级测试可注入受控 `LLMInvoker` 结果验证边界条件，但 P0 验收必须走完整可执行 harness，不能以 fake 路径替代。

### 5.9 RuleSnippetAssembler

`RuleSnippetAssembler` 独立负责组装，不属于 `RuleTransEngine` 内部私有逻辑。

```java
public final class RuleSnippetAssembler {

    public AssembledRuleClass assembleCompileUnit(
            String methodBody,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            String className);

    public AssembledRuleTest assembleExecutableTest(
            String methodBody,
            RuleContext context,
            RuleScenario scenario,
            RuleMetadata metadata,
            RuleTransTestCaseSet testCases,
            String className);
}
```

P0 必须同时支持编译单元和可执行测试单元。编译单元示例：

```java
package com.jmix.ruletrans.generated;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;

public class RuleTransCandidate001 extends ModuleAlgBase {
    @CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块", fatherCode = "cpu")
    public void ruleCpuAtMostOne() {
        String ruleCode = "ruleCpuAtMostOne";
        model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
    }
}
```

可执行测试单元要求：

- 生成一个临时 JUnit 测试类。
- 测试类继承 `ModuleScenarioTestBase`。
- 内部类继承 `ModuleAlgBase` 并包含从 `RuleContext` 投影出的 `@ModuleAnno`、`@PartAnno`、`@DAttrAnnoN` 和生成 rule 方法。
- 测试方法使用 `inferRecommendModule(...)`、`printSimpleSolutions()`、`validData(...)`、`validateData(...)` 等 helper。

实现风险：

- 现有 `@DAttrAnno1` 到 `@DAttrAnno13` 对动态属性数量有上限。P0 harness 需要明确超过上限时的失败信息，不能静默漏属性。
- 对复杂 `Module` 反向生成注解类可能不是完全保真。P0 仍要求一步到位支持从任意 `Module` 反向生成可执行 harness；无法表达的字段必须返回结构化失败，不允许退化为“仅编译通过”。

### 5.10 CompilationProcessor

```java
public record CompilationResult(
        boolean success,
        Path sourceFile,
        int exitCode,
        List<String> stdout,
        List<String> stderr) {

    public List<String> errors() {
        return stderr;
    }
}
```

`CompilationProcessor` 要求：

- 复用 `ModuleCompiler` 的 classpath 约定：`target/classes`、`target/test-classes`、`lib/*`。
- 使用 UTF-8 编译。
- 返回 `exitCode`、stdout、stderr。
- 不修改现有 `ModuleCompiler`。

### 5.11 RuleTestCaseGenerator 与测试用例格式

测试用例不应使用低层字段，如 `{ "cpu1.quantity": 1 }`。应使用业务输入和现有测试 helper 能表达的格式：

```json
{
  "ruleMethod": "ruleCpu4NotDrive5400",
  "cases": [
    {
      "id": "TC-001",
      "type": "validate",
      "selectedParts": ["cpu4", "drive5400"],
      "expectedValid": false,
      "expectedViolatedRuleCodes": ["ruleCpu4NotDrive5400"]
    },
    {
      "id": "TC-002",
      "type": "validate",
      "selectedParts": ["cpu8", "drive7200"],
      "expectedValid": true
    },
    {
      "id": "TC-003",
      "type": "recommend",
      "requests": [
        "cpu:Sum_Quantity ==1 where CoreNum=4",
        "drive:Sum_Quantity ==1 where Speed=5400"
      ],
      "expectedResult": "NO_SOLUTION"
    }
  ]
}
```

生成的 JUnit 样例应接近现有项目测试风格：

```java
@Test
public void testGeneratedRule_ForbiddenPair() {
    ModuleValidateResp resp = validateData("cpu4", "drive5400");
    assertFalse(resp.isValid());
    assertTrue(resp.getViolatedRuleCodes().contains("ruleCpu4NotDrive5400"));
}

@Test
public void testGeneratedRule_RuntimeFilter() {
    inferRecommendModule(
            "cpu:Sum_Quantity ==1 where CoreNum=4",
            "drive:Sum_Quantity ==1 where Speed=5400");
    printSimpleSolutions();
    resultAssert().assertNoSolution();
}
```

### 5.12 TestExecutionProcessor

```java
public record TestExecutionResult(
        boolean success,
        long testsFound,
        long testsSucceeded,
        long testsFailed,
        List<FailedTestCase> failedCases) {
}

public record FailedTestCase(
        String id,
        String displayName,
        String input,
        String expected,
        String actual,
        String reason,
        boolean likelyRuleLogicError) {
}
```

实现要求：

- 使用 JUnit Platform `Launcher` 和 `SummaryGeneratingListener`。
- 返回失败用例详情，而不只写日志。
- `isRuleLogicError(...)` 先采用保守策略：断言失败、无解/有解与预期相反、违反规则 code 不符合预期，归为可能的规则逻辑错误；ClassNotFound、编译失败、上下文投影失败不归为规则逻辑错误。

### 5.13 RuleTransEngine

```java
public final class RuleTransEngine {
    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleSnippetAssembler assembler;
    private final CompilationProcessor compilationProcessor;
    private final RuleTestCaseGenerator testCaseGenerator;
    private final TestExecutionProcessor testExecutionProcessor;

    public String translate(String naturalLanguage, RuleContext context);

    public RuleTransResult translateWithRetry(
            String naturalLanguage,
            RuleContext context,
            int maxRetries);
}
```

`translate(...)` 返回经基本后处理的方法体。`translateWithRetry(...)` 执行完整管线，返回方法体、模板组装结果、编译结果和测试执行结果。

边界：

- 主类不负责临时 Java 类字符串拼装细节。
- 主类不直接读写测试文件。
- 主类不直接拼 javac 命令。
- 主类只编排各组件。

---

## 6. Prompt 模板要点

### 6.1 非 POST 约束模板

必须包含：

- 自然语言规则。
- 分类 code、名称、选择策略、多实例标记。
- `paras`：code、name、`paraType`、`assignType`、options。
- `dynAttrSchemas`：code、name、`dynAttrType`、`instType`、options。
- `attrParas`：`attrCode`、`AttrParaType`，例如 `Sum`、`SumSum`、`Org`。
- `dynAttr` 扩展属性。
- 原子部件：code、name、fatherCode、maxQuantity、price、dynAttr。
- `ConstraintSdkContext` 中允许的约束模型 API。
- 明确禁止 `POST` 实例 view API。

### 6.2 模块级 Stage1 模板

只输出 JSON 数组：

```json
["cpu", "drive"]
```

如果无法识别，输出：

```json
[]
```

Stage1 必须支持中文别名和中英混合输入。Prompt 中的分类列表应同时包含 code、中文/英文描述、动态属性 code 与描述、代表部件短摘要。Stage1 仍然只负责分类识别，不生成规则代码。

### 6.3 模块级 Stage2 模板

只携带 Stage1 已识别分类的完整投影视图。必须说明跨分类规则可优先使用模板注入的 `ruleCode`：

```java
inCompatible(ruleCode, "cpu:CoreNum=4", "drive:Speed=5400");
```

或使用 `model().sum4Selected(partCategoryCodes, attrCode, filterCondition)` / `model().sum4Quantity(...)` 等 facade。

### 6.4 POST 模板

POST 模板必须与非 POST 模板分离，且必须包含：

- 自然语言规则。
- `PostSdkContext` 中允许的实例 view API。
- 产品参数、分类参数、具体部件实例和动态属性的可写目标。
- 当前 scope 是产品、部件分类还是具体部件。
- 明确要求只输出方法体；`@CodeRuleAnno(calcStage = CalcStage.POST, normalNaturalCode = "...")` 由模板生成。
- 明确禁止 `model()`、CP 变量、`onlyEnforceIf`、`inCompatible`、目标函数等非 POST API。

POST 样例只保留一个最小写回样例，不把所有 `PostCalcRuleTest` 中的 API 全量变成样例。完整 API 覆盖由 `rulescenario/post` 测试保证。

### 6.5 编译纠错模板

必须包含：

- 上一次方法体。
- javac stderr。
- 自然语言原文。
- 上下文摘要。
- `SdkProfile` 和允许/禁止 API 列表。
- 明确要求只输出修正后的方法体，不输出注解、方法头或方法名。
- 明确要求不改变已正确的业务语义，优先修语法、类型、facade API 使用错误。

### 6.6 测试纠错模板

必须包含：

- 上一次方法体。
- 失败测试用例列表。
- 每个用例的输入、预期、实际。
- 失败是否被处理器判定为可能的规则逻辑错误。
- `SdkProfile` 和对应规则类型。
- 明确要求只在规则逻辑错误时修正方法体；注解、方法头和方法名仍由模板生成。

### 6.7 Prompt 样例控制

Prompt 样例必须遵循“少而能泛化”的原则：

- 新增规则类型、新增 SDK profile、新增中文表达模式时可以补一个最小样例。
- 已有类型的相似表达，例如简单数量上限、已有汇总比例或已有互斥表达，不继续堆样例。
- 结构化规则不通过 Prompt 样例定制化解决；需要转换时使用结构化 IR 和确定性转换器。
- 测试覆盖不足通过 `rulescenario` 测试补齐，不通过把测试数据写死进 Prompt 补齐。

---

## 7. 关键设计决策

### 决策 1：上下文使用领域对象，不再设计平行 DTO

`PartCategoryRuleContext` 不应要求调用方手写 `ParaSpec`、`AttrParaSpec`、`DynamicAttrSpec`、`PartSpec`。这些结构如果存在，只能是 Prompt 投影器自动生成的视图。

依据：

- 用户明确指出原设计没有体现当前 PartCategory 的参数、动态属性和扩展属性。
- 当前代码已有完整领域模型继承链。
- 重复 DTO 会带来双写、字段遗漏和语义漂移。

### 决策 2：当前方法体绑定 `ModuleAlgBase`，但组装独立

用户反馈中提到生成逻辑最终是在 `ModuleAlgBase` 中运行。为了当前程序简单，P0 继续生成可插入 `ModuleAlgBase` rule 方法内部的方法体。

同时，`@CodeRuleAnno`、方法名、方法头、稳定 rule code 和完整临时类的组装工作独立在 `RuleSnippetAssembler` 中，不放进 `RuleTransEngine`，也不交给 LLM 自行生成。

### 决策 3：编译和测试处理器返回结构化结果

现有 `ModuleCompiler` / `ModuleRunner` 不能直接满足纠错循环，因为它们不返回错误结构。RuleTrans 新增处理器，但不修改原类，避免影响 `ModelHelper`。`CompilationProcessor` / `TestExecutionProcessor` 允许复制少量 classpath、临时目录和 JUnit Launcher 逻辑，以换取结构化返回。

### 决策 4：测试样例优先复用项目 helper

RuleTrans 的测试不是为了展示请求对象拼装，而是验证生成规则语义。因此 RFC 和生成测试都优先使用：

- `inferRecommendModule(...)`
- `printSimpleSolutions()`
- `validData(...)`
- `validateData(...)`
- `resultAssert()`
- 语义化 short code

### 决策 5：Prompt 模板独立

所有 RuleTrans Prompt 放在 `src/main/resources/ruletrans/`，不修改 `cengine/`。这保证既有 `ModelHelper` 生成完整测试类的能力不被扰动。

### 决策 6：POST 与非 POST SDK profile 独立

`POST` 和非 `POST` 规则的 API 依赖完全不同。RuleTrans 必须先识别 `SdkProfile`，再选择模板和后处理校验：

- 非 `POST`：只允许约束模型 API，如 `model()`、`ParaVar`、`PartVar`、`PartCategoryVar`、`sum4Selected`、`sum4Quantity`、`onlyEnforceIf`。
- `POST`：只允许实例 view API，如 `currentInst()`、`parameter().setValue(...)`、`partCategorySum(...)`、`part(...).setQuantity(...)`、`setDynAttr(...)`。
- 两者可以复用 `RuleContext` 和事实投影，但不能共用 Prompt 中的 SDK 列表。

### 决策 7：中文输入是一等场景

RuleTrans 的主要业务输入是中文。所有新增场景测试必须至少包含一个中文自然语言版本，并覆盖中文别名或中文单位。英文样例只能作为兼容测试，不能代表验收完成。

### 决策 8：规则场景测试独立组织

新增场景测试放入 `src/test/java/com/jmix/ruletrans/rulescenario` 子包，并按规则类型命名。基础单元测试继续保留在 `src/test/java/com/jmix/ruletrans`，不要把端到端场景塞回 `RuleTransModuleTest`。

### 决策 9：结构化规则走确定性转换器

结构化二元/三元组合、结构化 POST 计算等规则不通过堆 Prompt 样例解决。优先设计结构化 IR 和小型转换器，再用场景测试验证转换结果。

---

## 8. 验收准则

### AC-001：RuleContext 从现有领域模型构建

测试目标：

- 通过注解类或现有 `Module` 构建 `RuleContext`。
- 投影结果包含 `paras`、`attrParas`、`dynAttrSchemas`、`dynAttr`、`atomicParts`、`partCategorys`。

示例：

```java
@Test
public void testBuildPartCategoryRuleContextFromAnnotatedClass() {
    Module module = ModuleGenneratorByAnno.build(CpuDriveConstraint.class, tempPath);
    RuleContext ctx = RuleContextFactory.partCategory(module, "cpu");

    PartCategoryPromptView view = projector.projectPartCategory(ctx);

    assertEquals("cpu", view.code());
    assertTrue(view.dynAttrSchemas().stream().anyMatch(attr -> attr.code().equals("CoreNum")));
    assertFalse(view.atomicParts().isEmpty());
}
```

### AC-002：PartCategory 级规则生成

LLM 返回候选方法体：

```java
model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
```

预期：

- `engine.translate(...)` 返回纯方法体。
- 方法体不含 package/import/class、`@CodeRuleAnno`、方法声明或方法名。
- `RuleSnippetAssembler` 根据 `RuleScenario`、`RuleContext` 和规则元数据生成 `@CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块", fatherCode = "cpu")`、方法名和方法头。
- 完整组装结果必须进入编译和可执行 harness 验证，不能只断言字符串形态。

### AC-003：模块级 Stage1 识别并校验分类

输入：

```text
四核 CPU 不能兼容转速为 5400 转的硬盘
```

Stage1 分类识别结果示例：

```json
["cpu", "drive"]
```

预期：

- `CategoryIdentifier.identify(...)` 返回 `cpu` 和 `drive`。
- 返回不存在的分类 code 时抛出 `CategoryNotFoundException`。
- 返回空数组时抛出 `CategoryNotFoundException` 或返回明确失败结果。

### AC-004：生成方法体可编译

```java
@Test
public void testGeneratedMethodBodyCompiles() {
    String methodBody = generatedMethodBody();
    AssembledRuleClass assembled =
            assembler.assembleCompileUnit(methodBody, context, scenario, metadata, "RuleTransCandidate001");

    CompilationResult result = compilationProcessor.compile(assembled.sourceFile());

    assertTrue(result.success(), String.join("\n", result.errors()));
}
```

预期：

- 编译失败时返回 stderr。
- 不依赖 `ModuleCompiler.compile(...)` 的日志文本。

### AC-005：模块级禁止组合规则可执行

临时生成的测试类应使用 `validateData(...)`：

```java
@Test
public void testGeneratedRule_ValidateForbiddenPair() {
    ModuleValidateResp resp = validateData("cpu4", "drive5400");
    assertFalse(resp.isValid());
    assertTrue(resp.getViolatedRuleCodes().contains("ruleCpu4NotDrive5400"));
}
```

预期：

- `cpu4 + drive5400` 无效。
- `cpu8 + drive7200` 有效。
- 失败诊断包含生成 rule method code。

### AC-006：推荐路径与运行时过滤取交集

```java
@Test
public void testGeneratedRule_IntersectWithRuntimeFilter() {
    inferRecommendModule(
            "cpu:Sum_Quantity ==1 where CoreNum=4",
            "drive:Sum_Quantity ==1 where Speed=5400");
    printSimpleSolutions();
    resultAssert().assertNoSolution();
}
```

预期：

- 推荐路径走现有 `inferRecommendModule(...)`。
- 不在测试中手写 `InferParasReq` 和 `PartConstraintReq`。

### AC-007：编译错误驱动重试

第一次生成返回错误 API：

```java
model().addLessOrEquals(model().sum4Selected("cpu", "", ""), 1);
```

第二次返回正确 API：

```java
model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
```

预期：

- 第一次编译失败。
- 纠错 Prompt 包含 javac stderr。
- 第二次编译通过。
- 最终结果返回第二次方法体，模板仍负责注解、方法头和方法名。

### AC-008：测试失败驱动重试

第一次生成反向逻辑，测试失败；第二次修正。

预期：

- `TestExecutionResult.failedCases()` 包含失败用例输入、预期和实际。
- 失败被标记为可能的规则逻辑错误。
- 重试后测试通过。

### AC-009：原有 ModelHelper 不受影响

```java
@Test
public void testModelHelperUnchanged() throws Exception {
    String source = Files.readString(Path.of("src/main/java/com/jmix/tool/ModelHelper.java"));
    assertFalse(source.contains("ruletrans"));
    assertFalse(source.contains("RuleTransEngine"));
}
```

回归：

- `ModuleGenerator` 现有测试通过。
- `ModuleCompilerTest` 现有测试通过。
- `ModuleRunnerTest` 现有测试通过。

### AC-010：边界条件

| 条件 | 输入 | 预期 |
| --- | --- | --- |
| 自然语言为空 | `""` | 抛出 `IllegalArgumentException` |
| `RuleContext` 为空 | `null` | 抛出 `IllegalArgumentException` |
| Module Stage1 未识别分类 | `[]` | 抛出 `CategoryNotFoundException` |
| Stage1 返回非法分类 | `["missing"]` | 抛出 `CategoryNotFoundException`，错误信息包含 `missing` |
| 编译重试超过上限 | 连续编译失败 | 返回失败结果或抛出 `RuleTransException`，包含最后一次错误 |
| LLM 调用失败 | API key 或网络错误 | 抛出 `RuleTransException`，不写入业务源码 |
| Prompt 投影超过注解支持上限 | 动态属性超过可生成 `DAttrAnnoN` 上限 | P0 测试 harness 明确失败，不静默丢字段 |

### AC-011：规则场景覆盖矩阵

新增 `src/test/java/com/jmix/ruletrans/rulescenario` 场景测试包，并按规则类型分别建测试类。最低覆盖要求：

| 规则族 | 必须覆盖 |
| --- | --- |
| 参数兼容 | Requires、Incompatible、CoDependent，各自包含正向命中、反向推理、未命中默认语义 |
| 计算 | if-else、整数参数计算、无解边界 |
| 汇总 | 单分类、跨分类、多实例 `SumSum` |
| 选择 | exactly-one、at-most-one、可选分类边界 |
| 隐藏 | 参数隐藏、隐藏值归零、隐藏变量参与推理 |
| 优先级 | 部件聚合目标函数、模块级参数优先 |
| POST | 产品参数写回、分类参数写回、部件实例写回 |
| 结构化 | 二元/三元白名单、二元/三元黑名单、结构化 + Code 混合 |

预期：

- 每一种规则类型有独立测试类，类名体现规则族。
- `Scenario` 测试走完整链路：输入 -> 分类/场景识别 -> SDK profile -> 方法体或转换器 -> 模板组装 -> 编译 -> 执行。
- 基础能力不足时补极简 API 测试，例如中文分类识别、过滤表达式、上下文投影，不在大场景里混测。

### AC-012：POST 与非 POST SDK profile 隔离

测试目标：

- 非 `POST` 方法体只允许 `ConstraintSdkContext` API。
- `POST` 方法体只允许 `PostSdkContext` API。
- 纠错 Prompt 保持同一 `SdkProfile`，不得跨 profile 修复。

示例：

```java
@Test
public void testRejectConstraintMethodBodyUsingPostApi() {
    String methodBody = """
            parameter("p1").setValue("1");
            """;

    assertThrows(RuleTransException.class,
            () -> postProcessor.processMethodBody(methodBody, SdkProfile.CONSTRAINT));
}
```

预期：

- 非 `POST` profile 出现 `currentInst()`、`parameter(...).setValue(...)`、`partCategorySum(...)` 时失败。
- `POST` profile 出现 `model()`、`onlyEnforceIf`、`inCompatible`、目标函数时失败。
- POST 方法体不包含 `@CodeRuleAnno`；模板组装后的方法必须包含 `@CodeRuleAnno(calcStage = CalcStage.POST, ...)`。

### AC-013：中文自然语言场景

必须至少覆盖以下中文输入：

| 输入 | 预期 |
| --- | --- |
| `CPU 最多配置一块` | 识别 `cpu`，生成单分类数量约束 |
| `四核 CPU 不能兼容转速为 5400 转的硬盘` | 识别 `cpu`、`drive`，生成跨分类互斥 |
| `处理器是四核时不能选择 5400 转硬盘` | 中文别名“处理器/硬盘”映射到 `cpu` / `drive` |
| `把硬盘容量总和写回产品参数` | 识别为 POST 计算，使用 POST profile |

预期：

- `normalNaturalCode` 可保留中文原文。
- 日志仍为英文。
- 分类识别不依赖 Prompt 中硬编码单一英文样例。

### AC-014：结构化规则确定性转换

测试目标：

- 结构化二元/三元组合不依赖 LLM 直接生成 Java。
- 输入结构化 IR 或注解元数据后，由转换器输出等价规则或 Schema。

预期：

- WHITE 命中组合通过，未命中默认失败。
- BLACK 命中组合失败，未命中默认通过。
- 二元、三元、重复组合、无匹配表达式、结构化 + Code 混合分别有测试。
- 结构化 POST 规则走 POST 转换器，不使用非 POST API。

### AC-015：P0 可执行 harness 与 LLM 测试策略

P0 验收必须一步到位走完整可执行 harness：自然语言/结构化输入 -> 分类/场景识别 -> SDK profile -> 方法体/转换器 -> 模板组装 -> 编译 -> JUnit 执行 -> 结构化结果。

要求：

- 不以 fake LLM 单元测试替代 P0 验收。
- 组件级单元测试可以使用受控方法体或录制响应验证边界条件，但它们只是辅助，不作为“规则场景已覆盖”的唯一证据。
- 真实 LLM 端到端测试应单独命名或打标签，避免普通回归被 API key / 网络状态影响。
- 真实 LLM 或录制响应失败时必须输出 prompt、response、profile、场景编号、组装源码路径和测试失败详情，便于复现。

---

## 9. 实现计划

### 9.1 已实现基础管线

| 阶段 | 任务 | 优先级 | 当前代码状态 |
| --- | --- | --- | --- |
| 1 | 创建 `com.jmix.ruletrans` 包骨架和资源目录 `src/main/resources/ruletrans/` | P0 | 已实现 |
| 2 | 实现 `RuleContext`、`PartCategoryRuleContext`、`ModuleRuleContext`、`RuleContextFactory` | P0 | 已实现 |
| 3 | 实现 `RulePromptProjector` 和 Prompt view，自动从 `Module` / `PartCategory` 投影 | P0 | 已实现 |
| 4 | 实现 `PromptBuilder`，复用 `PromptTemplateLoader` 加载 `ruletrans/*.jtl` | P0 | 已实现 |
| 5 | 实现 `CategoryIdentifier`，完成模块级 Stage1 分类识别与校验 | P0 | 已实现 |
| 6 | 实现 `RuleSnippetGenerator`，通过构造注入复用 `LLMInvoker` | P0 | 已实现 |
| 7 | 实现 `RuleSnippetPostProcessor`，提取并校验只包含 Java 方法体 | P0 | 已实现基础版本；需迁移到“禁止注解/方法声明”的方法体契约 |
| 8 | 实现 `RuleSnippetAssembler.assembleCompileUnit(...)` | P0 | 已实现 |
| 9 | 实现 `CompilationProcessor` 和 `CompilationResult`，返回结构化 javac 结果 | P0 | 已实现 |
| 10 | 实现 `RuleTransEngine.translate(...)` 和编译纠错版 `translateWithRetry(...)` 的基础子集 | P0 | 已实现 |
| 11 | 实现 `RuleTestCaseGenerator` 与 `RuleTransTestCaseSet` JSON schema | P0 | 已实现基础版本；P0 需扩展为完整 harness 输入 |
| 12 | 实现 `RuleSnippetAssembler.assembleExecutableTest(...)`，生成 `ModuleScenarioTestBase` 风格测试 | P0 | 已实现基础版本；P0 需支持从任意 `Module` 反向生成可执行注解测试类 |
| 13 | 实现 `TestExecutionProcessor` 和结构化 JUnit 失败结果 | P0 | 已实现；允许复用少量 `ModuleRunner` / JUnit Launcher 逻辑 |
| 14 | 实现测试失败纠错循环 | P0 | 已实现基础版本；P0 需按方法体契约输出纠错结果 |
| 15 | 端到端集成测试：自然语言 -> 方法体 -> 模板组装 -> 编译 -> 测试通过 | P0 | 已有最小样例，覆盖不足 |
| 16 | 评估脱离 `ModuleAlgBase` 的中间 IR 或规则 Schema 生成路径 | P2 | 已有 `RuleTransIrEvaluator` 初步评估，Schema emission 未 ready |

### 9.2 后续增强任务

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| A1 | 引入 `RuleScenarioClassifier`、`RuleScenario`、`RuleFamily`、`RuleCalcStage`，在生成前识别 scope、rule family、POST/非 POST | P0 | 待实现 |
| A2 | 引入 `SdkContextBuilder`、`ConstraintSdkContext`、`PostSdkContext`、`SdkProfile` | P0 | 待实现 |
| A3 | 拆分非 POST 与 POST Prompt，新增 `post_module_prompt.jtl`、`post_part_category_prompt.jtl`、`post_part_prompt.jtl` | P0 | 待实现 |
| A4 | 增强 `RuleSnippetPostProcessor`，按 `SdkProfile` 静态拒绝跨 profile API | P0 | 待实现 |
| A5 | 将 `RuleSnippetGenerator`、Prompt 和后处理迁移为“只生成方法体”；模板负责 `@CodeRuleAnno`、方法名、方法头和 rule code | P0 | 待实现 |
| A6 | 建立 `src/test/java/com/jmix/ruletrans/rulescenario` 子包，按规则类型拆分场景测试 | P0 | 待实现 |
| A7 | 补中文分类识别与中文自然语言场景，包括中文单位、中文别名、中英混合字段 | P0 | 待实现 |
| A8 | 补参数兼容、计算、隐藏、汇总、选择、优先级、多实例规则场景 | P0 | 待实现 |
| A9 | 补 POST 产品参数写回、分类参数写回、部件实例写回场景 | P0 | 待实现 |
| A10 | 完善 P0 可执行 harness：从任意 `Module` 反向生成注解测试类，编译并执行 JUnit，返回结构化结果 | P0 | 待实现 |
| A11 | 为结构化二元/三元组合、结构化 POST 设计确定性转换器 | P1 | 待实现 |
| A12 | 建立真实 LLM、录制响应和受控方法体的分层测试策略；受控输入只做组件边界测试，不替代 P0 验收 | P1 | 待实现 |
| A13 | 当分类识别、过滤表达式、上下文投影等基础能力不足时，新增极简 API 测试 | P1 | 持续执行 |

---

## 10. 风险与兼容策略

### 10.1 Prompt 投影信息过多

风险：模块级上下文包含大量分类和部件，Prompt 过长。

策略：

- Stage1 只投影分类摘要。
- Stage2 只投影识别后的分类。
- 部件列表可提供 short code 和必要属性，不把无关字段全部展开。

### 10.2 反向生成注解测试类不完全保真

风险：现有 `Module` 可能包含无法完整反向表达为注解的字段。

策略：

- P0 必须支持完整可执行 harness，而不仅是编译验证。
- 无法生成测试 harness 时返回结构化失败，说明无法反向表达的字段或注解上限。
- 后续可考虑直接使用 `Module` 数据和生成算法类组装测试，而不是完全反向生成注解。

### 10.3 LLM 生成内部 API

风险：LLM 使用 `impl` 类型、OR-Tools 类型或旧字段名。

策略：

- Prompt 明确只允许南向 facade。
- 后处理器扫描禁止 import/package/class 和明显的 forbidden package。
- 编译错误进入纠错 Prompt。

### 10.4 编译和测试处理器与现有工具重复

风险：新增处理器看起来像重复 `ModuleCompiler` / `ModuleRunner`。

策略：

- 不改原类，不影响 `ModelHelper`。
- 新处理器只服务 RuleTrans 的结构化纠错循环。
- 复用现有 classpath 和 JUnit Launcher 约定，减少行为分叉。

### 10.5 生成规则逻辑错误

风险：编译通过但业务语义错误。

策略：

- P0 harness 引入结构化测试用例生成与执行。
- 测试用例必须覆盖正向、反向、边界。
- 失败结果结构化反馈给 LLM 重试。

### 10.6 POST 与非 POST API 混用

风险：模型把 `model()`、CP 变量、`parameter().setValue(...)`、`partCategorySum(...)` 混在同一个方法体里，导致编译通过但运行阶段语义错误，或编译阶段直接失败。

策略：

- 先分类再构建 `SdkProfile`。
- Prompt 模板按 profile 拆分。
- `RuleSnippetPostProcessor` 按 profile 做静态禁用 API 扫描。
- 场景测试中同时覆盖“正确 profile 可通过”和“跨 profile API 被拒绝”。

### 10.7 Prompt 样例过拟合

风险：为了补覆盖而把所有业务样例写进 Prompt，导致模型记忆样例而不是泛化规则类型。

策略：

- 新增规则类型或 SDK profile 才补一个最小样例。
- 已有类型只补测试，不继续堆 Prompt 样例。
- 结构化规则用 IR 和确定性转换器，不靠 Prompt 样例定制。

### 10.8 中文识别不稳定

风险：中文别名、单位和中英混合字段导致 Stage1 分类识别或 Stage2 过滤表达式生成不稳定。

策略：

- 上下文投影同时提供 code、中文描述、属性 code、属性描述和代表部件摘要。
- 建立 `ChineseCategoryIdentifierScenarioTest` 与中文过滤表达式极简测试。
- 对中文单位做标准化层，例如“四核” -> `CoreNum=4`、“5400 转” -> `Speed=5400`。

### 10.9 真实 LLM 测试不确定

风险：真实模型、API key、网络和模型版本变化会影响普通回归测试稳定性。

策略：

- P0 验收不使用 fake LLM 作为替代路径，必须走完整可执行 harness。
- 普通回归可使用录制响应、缓存响应或受控方法体做组件边界测试，但不能作为规则场景覆盖的唯一证据。
- 真实 LLM 端到端测试单独命名或打标签。
- 真实 LLM 测试失败时输出 prompt、response、`SdkProfile`、场景编号、组装源码路径和 JUnit 失败详情。

---

## 11. 复用优先清单

### 优先复用

- `com.jmix.executor.bmodel.Module`
- `com.jmix.executor.bmodel.PartCategory`
- `com.jmix.executor.bmodel.Part`
- `com.jmix.executor.bmodel.para.Para`
- `com.jmix.executor.bmodel.attr.DynamicAttribute`
- `com.jmix.executor.bmodel.AttrPara`
- `com.jmix.tool.impl.llm.LLMInvoker` / `LLMInvokerImpl`
- `com.jmix.tool.impl.PromptTemplateLoader`
- `com.jmix.tool.bbuilder.ModuleGenneratorByAnno`
- `com.jmix.executor.southinf.ModuleAlgBase`
- `com.jmix.executor.southinf.ModuleCPModel`
- `com.jmix.executor.southinf.PartCategoryCPModel`
- `com.jmix.executor.southinf.cp.*`
- `com.jmix.executor.southinf.var.*`
- `com.jmix.executor.southinf.view.*`
- `com.jmix.coretest.ModuleScenarioTestBase`
- `src/test/java/com/jmix/scenario/ruletest/*`
- `src/test/java/com/jmix/opti/base/*`
- `src/test/java/com/jmix/opti/multireq/*`

### 不新增或不修改

- 不修改 `ModelHelper`。
- 不修改 `ModuleGenerator` 现有行为。
- 不修改 `cengine/` Prompt 模板。
- 不新增第二套 LLM 调用抽象。
- 不要求调用方维护平行领域 DTO。
- 不在测试方法中重复拼大量 `InferParasReq` / `PartConstraintReq`。
- 不把新增规则场景测试混入 `RuleTransModuleTest` 等基础单元测试。
- 不把结构化规则全量写成 Prompt 样例。
- 不在非 `POST` Prompt 中暴露实例 view 写回 API。
- 不在 `POST` Prompt 中暴露 CP 约束模型 API。

### 可由上下文推导

- `ParaPromptView` 由 `Para` 推导。
- `DynamicAttrPromptView` 由 `DynamicAttribute` 推导。
- `AttrParaPromptView` 由 `AttrPara` 推导。
- `PartPromptView` 由 `Part` 推导。
- 模块级可用分类由 `Module.getAllPartCategorys()` 推导。
- Stage2 目标分类由 Stage1 识别结果经 `Module.getPartCategory(code)` 校验后推导。
- `ConstraintSdkContext` / `PostSdkContext` 的事实部分由 `RuleContext` 和 `RuleScenario` 推导。
- 中文别名优先由 `description/name`、动态属性描述和可选值描述推导。

---

## 12. 用户确认结论

2026-06-08 本轮用户已明确以下设计结论，后续实现不再按开放问题处理：

1. P0 可执行测试 harness 必须一步到位：完整支持从任意 `Module` 反向生成可执行注解测试类，完成编译和 JUnit 执行；不能先以 fake LLM 单元测试作为验收替代。
2. `CompilationProcessor` / `TestExecutionProcessor` 允许在 `ruletrans` 内复制少量 `ModuleCompiler` / `ModuleRunner` 的 classpath、临时目录和 JUnit Launcher 逻辑，以换取结构化返回。
3. LLM 生成 Java 时只生成方法体内容，不生成方法名、方法头、注解或完整方法；方法头、`@CodeRuleAnno`、方法名和稳定 rule code 必须由模板根据规则元数据构造。
4. `POST` 与非 `POST` 的 SDK 上下文必须独立构建。两者可以共享领域事实投影，但南向 API 列表、Prompt 模板、后处理校验和测试场景必须分开。
5. 后续排期建议先完成方法体契约迁移、P0 可执行 harness、SDK profile 隔离和中文场景覆盖，再扩大复杂规则族与结构化转换器。

当前没有阻塞实现的待确认问题。

---

## 13. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `docbackup/Rule-Scenarios-And-SDK-Summary.md`
- `doc/RFC-0007-Struct-Combination-Rule-Schema.md`
- `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`
- `src/main/java/com/jmix/ruletrans/RuleTransEngine.java`
- `src/main/java/com/jmix/ruletrans/context/RuleContextFactory.java`
- `src/main/java/com/jmix/ruletrans/prompt/RulePromptProjector.java`
- `src/main/java/com/jmix/ruletrans/prompt/PromptBuilder.java`
- `src/main/java/com/jmix/ruletrans/identifier/CategoryIdentifier.java`
- `src/main/java/com/jmix/ruletrans/generator/RuleSnippetPostProcessor.java`
- `src/main/java/com/jmix/ruletrans/assembler/RuleSnippetAssembler.java`
- `src/main/java/com/jmix/ruletrans/postprocessor/CompilationProcessor.java`
- `src/main/java/com/jmix/ruletrans/postprocessor/TestExecutionProcessor.java`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTestCaseGenerator.java`
- `src/main/java/com/jmix/tool/ModelHelper.java`
- `src/main/java/com/jmix/tool/impl/ModuleGenerator.java`
- `src/main/java/com/jmix/tool/impl/ModuleCompiler.java`
- `src/main/java/com/jmix/tool/impl/ModuleRunner.java`
- `src/main/java/com/jmix/tool/impl/PromptTemplateLoader.java`
- `src/main/java/com/jmix/tool/impl/llm/LLMInvoker.java`
- `src/main/java/com/jmix/executor/bmodel/Onto.java`
- `src/main/java/com/jmix/executor/bmodel/ModuleBase.java`
- `src/main/java/com/jmix/executor/bmodel/Module.java`
- `src/main/java/com/jmix/executor/bmodel/PartCategory.java`
- `src/main/java/com/jmix/executor/bmodel/Part.java`
- `src/main/java/com/jmix/executor/bmodel/para/Para.java`
- `src/main/java/com/jmix/executor/bmodel/attr/DynamicAttribute.java`
- `src/main/java/com/jmix/executor/bmodel/AttrPara.java`
- `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java`
- `src/main/java/com/jmix/executor/southinf/ModuleCPModel.java`
- `src/main/java/com/jmix/executor/southinf/PartCategoryCPModel.java`
- `src/main/java/com/jmix/executor/southinf/var/PartCategoryVar.java`
- `src/main/java/com/jmix/executor/southinf/var/PartVar.java`
- `src/main/java/com/jmix/executor/southinf/view/ModuleInstView.java`
- `src/main/java/com/jmix/executor/southinf/view/PartCategoryInstView.java`
- `src/main/java/com/jmix/executor/southinf/view/PartInstView.java`
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java`
- `src/test/java/com/jmix/ruletrans/RuleTransModuleTest.java`
- `src/test/java/com/jmix/ruletrans/RuleSnippetAssemblerTest.java`
- `src/test/java/com/jmix/ruletrans/RulePromptProjectorTest.java`
- `src/test/java/com/jmix/ruletrans/CategoryIdentifierTest.java`
- `src/test/java/com/jmix/scenario/ruletest/PostCalcRuleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleRequireTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleIncompatibleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleCodependentTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CalculateRuleSimpleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CalculateRuleIfThenTest.java`
- `src/test/java/com/jmix/scenario/ruletest/ParaIntegerTest.java`
- `src/test/java/com/jmix/scenario/ruletest/ParaIsHiddenTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CodeRuleOnlyValidateTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java`
- `src/test/java/com/jmix/scenario/ruletest/StructCombinationRuleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/StructCombinationOtherRuleTest.java`
- `src/test/java/com/jmix/scenario/ruletest/StructCodeRuleMixedValidateTest.java`
- `src/test/java/com/jmix/opti/base/BaseOptiTest.java`
- `src/test/java/com/jmix/opti/base/MultiPCTest.java`
- `src/test/java/com/jmix/opti/multireq/DynMultReq4MultiReqTest.java`
- `src/test/java/com/jmix/opti/multireq/EnumMultReq4MultiReqTest.java`
