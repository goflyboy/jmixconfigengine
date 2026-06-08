# RFC-0011: RuleTrans 模块 - 自然语言规则转换引擎

> 状态：评审中（Review）
> 日期：2026-06-07
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0007-Struct-Combination-Rule-Schema.md`, `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`

---

## 设计决策摘要

| 主题 | 决策 |
| --- | --- |
| 模块边界 | 新增 `com.jmix.ruletrans` 包；不修改 `ModelHelper`、`ModuleGenerator`、`cengine/` 模板和既有 `com.jmix.tool` 行为 |
| 事实数据源 | `RuleContext` 以现有 `com.jmix.executor.bmodel.Module` / `PartCategory` 为事实源；Prompt 用投影视图只负责序列化，不维护第二套领域模型 |
| 上下文命名 | 保留用户反馈后的 `RuleContext` 命名，不再使用 `RuleInput` |
| 输出形态 | 输出一个可插入 `ModuleAlgBase` 子类的 Java rule 方法代码片段，而不是完整测试类或完整模型文件 |
| 组装边界 | `RuleSnippetAssembler` 独立负责把代码片段组装成临时可编译类；不放进 `RuleTransEngine` 主类 |
| 运行态依赖 | P0 允许生成代码依赖当前南向 facade：`ModuleAlgBase.model()`、`ModuleCPModel`、`PartCategoryVar`、`PartVar` 等；未来再评估完全脱离 `ModuleAlgBase` 的 IR |
| 产品级规则 | 产品级上下文分两阶段：Stage1 识别涉及的 PartCategory code，Stage2 只携带识别后的分类规格生成代码 |
| 编译反馈 | 现有 `ModuleCompiler` 只记录日志、不返回错误结构；`ruletrans` 新增结构化编译处理器，复用其 classpath/路径约定，不改原类 |
| 测试反馈 | 现有 `ModuleRunner` 只记录日志、不返回 JUnit 失败详情；`ruletrans` 新增测试执行处理器，复用 JUnit Launcher 思路并返回结构化失败信息 |
| 测试样例 | RFC 测试样例优先使用 `ModuleScenarioTestBase`、`inferRecommendModule(...)`、`printSimpleSolutions()`、`validData(...)`、`validateData(...)` |
| LLM 调用 | 通过构造注入复用 `LLMInvoker` / `LLMInvokerImpl`；不新增另一套 LLM 调用抽象 |

---

## 1. 摘要

当前 `ModelHelper` 同时承担自然语言/伪代码生成、完整测试类生成、编译、运行、结构化规则代码注入等职责。RuleTrans 的目标是把“自然语言规则 -> Java rule 方法片段”的能力拆出来，形成独立模块：

```text
自然语言规则 + RuleContext
  -> Prompt 构建
  -> LLM 生成 Java rule 方法片段
  -> 片段组装为临时 ModuleAlgBase 子类
  -> 编译验证
  -> 测试用例生成与执行
  -> 编译/测试失败反馈给 LLM 重试
  -> 返回最终 rule 方法片段
```

RuleTrans 不替代现有求解器和规则执行模型。它只负责生成可进入现有约束引擎的 `@CodeRuleAnno` rule 方法片段，并尽量复用当前工程已有的领域模型、Prompt 模板加载器、LLM 调用器、测试基类和南向 facade。

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
4. 生成目标从“完整测试类”收敛为“纯 Java rule 方法片段”。
5. 编译、测试、纠错形成可复用管线。

### 2.2 用户反馈复核

SpecStory 中的人工输入对本 RFC 有三类关键修正：

| 用户修正 | 设计含义 | 本 RFC 处理 |
| --- | --- | --- |
| `RuleInput` 不合适，改回 `context` 或 `spec` | 输入不是一次请求 DTO，而是生成规则所需上下文 | 使用 `RuleContext`，删除 `RuleInput` |
| 原设计没有真正理解 PartCategory 数据结构 | 生成规则需要参数、动态属性、扩展属性和部件结构 | `RuleContext` 直接引用现有 `Module` / `PartCategory`，Prompt 投影覆盖 `paras`、`attrParas`、`dynAttrSchemas`、`dynAttr`、`atomicParts`、`partCategorys` |
| 代码片段组装不要放在主 Transfer/Engine 类里 | 主入口负责编排，组装是独立后处理能力 | 新增独立 `RuleSnippetAssembler` 和测试 harness |

### 2.3 具体场景

#### 场景 1：PartCategory 级规则

输入：

```text
CPU 最多配置一块
```

上下文：`cpu` 分类的 `PartCategory`，包含部件、参数、动态属性、扩展属性和选择策略。

输出：一个 `@CodeRuleAnno` 方法，例如：

```java
@CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块")
public void ruleCpuAtMostOne() {
    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
}
```

#### 场景 2：产品级跨分类规则

输入：

```text
四核 CPU 不能兼容转速为 5400 转的硬盘
```

上下文：整个 `Module`。执行分两阶段：

1. Stage1：从自然语言识别涉及 `cpu`、`drive` / `hdd` 等 PartCategory。
2. Stage2：只把识别到的分类规格投影给 LLM 生成 rule 方法片段。

#### 场景 3：编译错误纠错

LLM 返回的代码片段编译失败时，RuleTrans 捕获 javac 错误、上一版代码、自然语言和上下文摘要，构建纠错 Prompt 后重试。

#### 场景 4：测试用例驱动纠错

编译通过后，RuleTrans 可以让 LLM 生成结构化测试用例，再把测试用例转成临时 JUnit 测试。测试失败时，把失败 case 的输入、预期、实际结果反馈给 LLM 判断是否需要修正规则逻辑。

---

## 3. 范围与非目标

### 3.1 P0 范围

- 新增 `com.jmix.ruletrans` 包骨架。
- 新增 `RuleContext`、`PartCategoryRuleContext`、`ProductRuleContext` 及工厂。
- 新增 Prompt 投影器，把现有 `Module` / `PartCategory` 转成 LLM 友好的结构化文本或 JSON。
- 新增 `RuleSnippetGenerator`，复用 `LLMInvoker`。
- 新增 `RuleSnippetAssembler`，把 rule 方法片段组装为临时 `ModuleAlgBase` 子类进行编译验证。
- 新增结构化 `CompilationProcessor`，捕获编译成功/失败和错误详情。
- 新增产品级 `CategoryIdentifier`，完成 Stage1 分类识别和结果校验。
- 保证 `ModelHelper` 与现有 `cengine/` Prompt 不受影响。

### 3.2 P1 范围

- 新增 `RuleTestCaseGenerator`，生成结构化测试用例。
- 新增 `TestExecutionProcessor`，把测试用例转成 JUnit 测试并返回结构化测试结果。
- 实现 `translateWithRetry(...)` 的编译纠错和测试纠错循环。

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

---

## 5. 设计方案

### 5.1 总体流程

```text
RuleTransEngine.translateWithRetry(...)
  |
  |-- validate naturalLanguage + RuleContext
  |
  |-- ProductRuleContext?
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
  |-- RuleSnippetGenerator.generate(...)
  |
  |-- RuleSnippetAssembler.assembleCompileUnit(...)
  |
  |-- CompilationProcessor.compile(...)
  |     |
  |     |-- failed: build compilation correction prompt and retry
  |
  |-- RuleTestCaseGenerator.generate(...)          [P1]
  |
  |-- TestExecutionProcessor.execute(...)          [P1]
        |
        |-- failed and is rule logic error: build test correction prompt and retry
        |-- failed but not logic error: return code + diagnostic report
```

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
│   ├── ProductRuleContext.java
│   └── RuleContextFactory.java
│
├── prompt/
│   ├── PromptBuilder.java
│   ├── RulePromptProjector.java
│   ├── PartCategoryPromptView.java
│   └── ProductPromptView.java
│
├── identifier/
│   └── CategoryIdentifier.java
│
├── generator/
│   ├── RuleSnippetGenerator.java
│   └── RuleSnippetPostProcessor.java
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
├── product_stage1_prompt.jtl
├── product_stage2_prompt.jtl
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

    boolean isProductLevel();

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
    public boolean isProductLevel() {
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

产品级上下文：

```java
public final class ProductRuleContext implements RuleContext {
    private final Module module;
    private final List<PartCategory> selectedCategories;

    @Override
    public boolean isProductLevel() {
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

    public ProductRuleContext product(Module module);

    public ProductRuleContext product(Module module, List<String> categoryCodes);

    public ProductRuleContext fromAnnotatedClass(Class<?> algClass, String tempResourcePath);
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

### 5.5 产品级 Stage1 分类识别

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

    public String buildPartCategoryPrompt(
            PartCategoryPromptView view,
            String naturalLanguage);

    public String buildProductStage1Prompt(
            ProductPromptView view,
            String naturalLanguage);

    public String buildProductStage2Prompt(
            ProductPromptView view,
            String naturalLanguage);

    public String buildCompilationCorrectionPrompt(
            String previousCode,
            CompilationResult compilationResult,
            String naturalLanguage,
            RuleContext context);

    public String buildTestCorrectionPrompt(
            String previousCode,
            TestExecutionResult testResult,
            String naturalLanguage,
            RuleContext context);
}
```

模板约束：

- 使用 `PromptTemplateLoader.loadAndRenderTemplate("ruletrans/xxx.jtl", variables)`。
- 不修改 `src/main/resources/cengine/constraint_generate_prompt.jtl`。
- Prompt 必须说明生成代码运行在 `ModuleAlgBase` 子类中。
- Prompt 必须禁止输出 package、import、class 声明。
- Prompt 必须要求日志使用英文。
- Prompt 应推荐使用南向 facade，例如 `model()`、`partCategoryVar("code")`、`partVar("code")`、`partVars("Attr=value")`、`model().sum4Selected(...)`、`model().sum4Quantity(...)`。

### 5.7 代码片段格式

生成结果只允许是方法片段：

```java
@CodeRuleAnno(normalNaturalCode = "四核 CPU 不能兼容转速为 5400 转的硬盘")
public void ruleCpu4NotDrive5400() {
    inCompatible("ruleCpu4NotDrive5400", "cpu:CoreNum=4", "drive:Speed=5400");
}
```

或使用 CP facade：

```java
@CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块")
public void ruleCpuAtMostOne() {
    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
}
```

禁止：

- 输出完整 Java 类。
- 输出 package/import。
- 直接使用 `com.google.ortools.*` 类型。
- 直接导入或依赖 `com.jmix.executor.impl.*`。
- 使用旧样例中的内部字段形式，如 `this.partVar.qty`、`getIsSelectedVar()`。

### 5.8 RuleSnippetGenerator

```java
public final class RuleSnippetGenerator {
    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;
    private final RulePromptProjector projector;

    public String generate(String naturalLanguage, RuleContext context);

    public String generateWithCompilationCorrection(
            String previousCode,
            CompilationResult compilationResult,
            String naturalLanguage,
            RuleContext context);

    public String generateWithTestCorrection(
            String previousCode,
            TestExecutionResult testResult,
            String naturalLanguage,
            RuleContext context);
}
```

构造要求：

- 默认构造可使用 `new LLMInvokerImpl()`。
- 单元测试必须能注入 fake `LLMInvoker`，避免测试依赖真实大模型。

### 5.9 RuleSnippetAssembler

`RuleSnippetAssembler` 独立负责组装，不属于 `RuleTransEngine` 内部私有逻辑。

```java
public final class RuleSnippetAssembler {

    public AssembledRuleClass assembleCompileUnit(
            String ruleCode,
            RuleContext context,
            String className);

    public AssembledRuleTest assembleExecutableTest(
            String ruleCode,
            RuleContext context,
            RuleTransTestCaseSet testCases,
            String className);
}
```

P0 至少支持编译单元：

```java
package com.jmix.ruletrans.generated;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.cp.PartAlgCPLinearExpr;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;

public class RuleTransCandidate001 extends ModuleAlgBase {
    // generated rule method is inserted here
}
```

P1 支持可执行测试单元：

- 生成一个临时 JUnit 测试类。
- 测试类继承 `ModuleScenarioTestBase`。
- 内部类继承 `ModuleAlgBase` 并包含从 `RuleContext` 投影出的 `@ModuleAnno`、`@PartAnno`、`@DAttrAnnoN` 和生成 rule 方法。
- 测试方法使用 `inferRecommendModule(...)`、`printSimpleSolutions()`、`validData(...)`、`validateData(...)` 等 helper。

实现风险：

- 现有 `@DAttrAnno1` 到 `@DAttrAnno13` 对动态属性数量有上限。P1 harness 需要明确超过上限时的失败信息，不能静默漏属性。
- 对复杂 `Module` 反向生成注解类可能不是完全保真。P0 可先支持编译验证，P1 再扩大可执行测试覆盖。

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

`translate(...)` 只做生成与基本后处理，不强制编译。`translateWithRetry(...)` 执行完整管线。

边界：

- 主类不负责临时 Java 类字符串拼装细节。
- 主类不直接读写测试文件。
- 主类不直接拼 javac 命令。
- 主类只编排各组件。

---

## 6. Prompt 模板要点

### 6.1 PartCategory 模板

必须包含：

- 自然语言规则。
- 分类 code、名称、选择策略、多实例标记。
- `paras`：code、name、`paraType`、`assignType`、options。
- `dynAttrSchemas`：code、name、`dynAttrType`、`instType`、options。
- `attrParas`：`attrCode`、`AttrParaType`，例如 `Sum`、`SumSum`、`Org`。
- `dynAttr` 扩展属性。
- 原子部件：code、name、fatherCode、maxQuantity、price、dynAttr。
- 推荐使用的南向 API 示例。

### 6.2 产品级 Stage1 模板

只输出 JSON 数组：

```json
["cpu", "drive"]
```

如果无法识别，输出：

```json
[]
```

### 6.3 产品级 Stage2 模板

只携带 Stage1 已识别分类的完整投影视图。必须说明跨分类规则可优先使用：

```java
inCompatible("ruleCode", "cpu:CoreNum=4", "drive:Speed=5400");
```

或使用 `model().sum4Selected(partCategoryCodes, attrCode, filterCondition)` / `model().sum4Quantity(...)` 等 facade。

### 6.4 编译纠错模板

必须包含：

- 上一次代码片段。
- javac stderr。
- 自然语言原文。
- 上下文摘要。
- 明确要求只输出修正后的 rule 方法片段。
- 明确要求不改变已正确的业务语义，优先修语法、类型、facade API 使用错误。

### 6.5 测试纠错模板

必须包含：

- 上一次代码片段。
- 失败测试用例列表。
- 每个用例的输入、预期、实际。
- 失败是否被处理器判定为可能的规则逻辑错误。
- 明确要求只在规则逻辑错误时修正代码。

---

## 7. 关键设计决策

### 决策 1：上下文使用领域对象，不再设计平行 DTO

`PartCategoryRuleContext` 不应要求调用方手写 `ParaSpec`、`AttrParaSpec`、`DynamicAttrSpec`、`PartSpec`。这些结构如果存在，只能是 Prompt 投影器自动生成的视图。

依据：

- 用户明确指出原设计没有体现当前 PartCategory 的参数、动态属性和扩展属性。
- 当前代码已有完整领域模型继承链。
- 重复 DTO 会带来双写、字段遗漏和语义漂移。

### 决策 2：当前生成代码绑定 `ModuleAlgBase`，但组装独立

用户反馈中提到代码片段最终是在 `ModuleAlgBase` 中运行。为了当前程序简单，P0 继续生成 `ModuleAlgBase` rule 方法片段。

同时，组装为完整类的工作独立在 `RuleSnippetAssembler` 中，不放进 `RuleTransEngine`。

### 决策 3：编译和测试处理器返回结构化结果

现有 `ModuleCompiler` / `ModuleRunner` 不能直接满足纠错循环，因为它们不返回错误结构。RuleTrans 新增处理器，但不修改原类，避免影响 `ModelHelper`。

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

使用 fake `LLMInvoker` 返回确定代码：

```java
@CodeRuleAnno(normalNaturalCode = "CPU 最多配置一块")
public void ruleCpuAtMostOne() {
    model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
}
```

预期：

- `engine.translate(...)` 返回纯方法片段。
- 片段不含 package/import/class。
- 片段包含 `@CodeRuleAnno`。

### AC-003：产品级 Stage1 识别并校验分类

输入：

```text
四核 CPU 不能兼容转速为 5400 转的硬盘
```

fake LLM 返回：

```json
["cpu", "drive"]
```

预期：

- `CategoryIdentifier.identify(...)` 返回 `cpu` 和 `drive`。
- 返回不存在的分类 code 时抛出 `CategoryNotFoundException`。
- 返回空数组时抛出 `CategoryNotFoundException` 或返回明确失败结果。

### AC-004：生成片段可编译

```java
@Test
public void testGeneratedSnippetCompiles() {
    String snippet = fakeGeneratedRule();
    AssembledRuleClass assembled =
            assembler.assembleCompileUnit(snippet, context, "RuleTransCandidate001");

    CompilationResult result = compilationProcessor.compile(assembled.sourceFile());

    assertTrue(result.success(), String.join("\n", result.errors()));
}
```

预期：

- 编译失败时返回 stderr。
- 不依赖 `ModuleCompiler.compile(...)` 的日志文本。

### AC-005：产品级禁止组合规则可执行

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

fake LLM 第一次返回错误 API：

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
- 最终结果返回第二次代码。

### AC-008：测试失败驱动重试

fake LLM 第一次生成反向逻辑，测试失败；第二次修正。

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
| Product Stage1 未识别分类 | `[]` | 抛出 `CategoryNotFoundException` |
| Stage1 返回非法分类 | `["missing"]` | 抛出 `CategoryNotFoundException`，错误信息包含 `missing` |
| 编译重试超过上限 | 连续编译失败 | 返回失败结果或抛出 `RuleTransException`，包含最后一次错误 |
| LLM 调用失败 | API key 或网络错误 | 抛出 `RuleTransException`，不写入业务源码 |
| Prompt 投影超过注解支持上限 | 动态属性超过可生成 `DAttrAnnoN` 上限 | P1 测试 harness 明确失败，不静默丢字段 |

---

## 9. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| 1 | 创建 `com.jmix.ruletrans` 包骨架和资源目录 `src/main/resources/ruletrans/` | P0 | 待开始 |
| 2 | 实现 `RuleContext`、`PartCategoryRuleContext`、`ProductRuleContext`、`RuleContextFactory` | P0 | 待开始 |
| 3 | 实现 `RulePromptProjector` 和 Prompt view，自动从 `Module` / `PartCategory` 投影 | P0 | 待开始 |
| 4 | 实现 `PromptBuilder`，复用 `PromptTemplateLoader` 加载 `ruletrans/*.jtl` | P0 | 待开始 |
| 5 | 实现 `CategoryIdentifier`，完成产品级 Stage1 分类识别与校验 | P0 | 待开始 |
| 6 | 实现 `RuleSnippetGenerator`，通过构造注入复用 `LLMInvoker` | P0 | 待开始 |
| 7 | 实现 `RuleSnippetPostProcessor`，提取代码块并校验只包含 rule 方法片段 | P0 | 待开始 |
| 8 | 实现 `RuleSnippetAssembler.assembleCompileUnit(...)` | P0 | 待开始 |
| 9 | 实现 `CompilationProcessor` 和 `CompilationResult`，返回结构化 javac 结果 | P0 | 待开始 |
| 10 | 实现 `RuleTransEngine.translate(...)` 和编译纠错版 `translateWithRetry(...)` 的 P0 子集 | P0 | 待开始 |
| 11 | 用 fake `LLMInvoker` 编写 P0 单元测试，避免依赖真实大模型 | P0 | 待开始 |
| 12 | 实现 `RuleTestCaseGenerator` 与 `RuleTransTestCaseSet` JSON schema | P1 | 待开始 |
| 13 | 实现 `RuleSnippetAssembler.assembleExecutableTest(...)`，生成 `ModuleScenarioTestBase` 风格测试 | P1 | 待开始 |
| 14 | 实现 `TestExecutionProcessor` 和结构化 JUnit 失败结果 | P1 | 待开始 |
| 15 | 实现测试失败纠错循环 | P1 | 待开始 |
| 16 | 端到端集成测试：自然语言 -> 代码片段 -> 编译 -> 测试通过 | P1 | 待开始 |
| 17 | 评估脱离 `ModuleAlgBase` 的中间 IR 或规则 Schema 生成路径 | P2 | 待评估 |

---

## 10. 风险与兼容策略

### 10.1 Prompt 投影信息过多

风险：产品级上下文包含大量分类和部件，Prompt 过长。

策略：

- Stage1 只投影分类摘要。
- Stage2 只投影识别后的分类。
- 部件列表可提供 short code 和必要属性，不把无关字段全部展开。

### 10.2 反向生成注解测试类不完全保真

风险：现有 `Module` 可能包含无法完整反向表达为注解的字段。

策略：

- P0 先保证编译验证。
- P1 明确支持范围，无法生成测试 harness 时返回结构化失败。
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

- P1 引入 LLM 生成测试用例。
- 测试用例必须覆盖正向、反向、边界。
- 失败结果结构化反馈给 LLM 重试。

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
- `com.jmix.coretest.ModuleScenarioTestBase`

### 不新增或不修改

- 不修改 `ModelHelper`。
- 不修改 `ModuleGenerator` 现有行为。
- 不修改 `cengine/` Prompt 模板。
- 不新增第二套 LLM 调用抽象。
- 不要求调用方维护平行领域 DTO。
- 不在测试方法中重复拼大量 `InferParasReq` / `PartConstraintReq`。

### 可由上下文推导

- `ParaPromptView` 由 `Para` 推导。
- `DynamicAttrPromptView` 由 `DynamicAttribute` 推导。
- `AttrParaPromptView` 由 `AttrPara` 推导。
- `PartPromptView` 由 `Part` 推导。
- 产品级可用分类由 `Module.getAllPartCategorys()` 推导。
- Stage2 目标分类由 Stage1 识别结果经 `Module.getPartCategory(code)` 校验后推导。

---

## 12. 待用户确认

1. P0 的可执行测试 harness 是否必须完整支持从任意 `Module` 反向生成注解类，还是可以先以“编译验证 + fake LLM 单元测试”为主，P1 再扩大运行态测试？
2. `CompilationProcessor` / `TestExecutionProcessor` 是否允许在 `ruletrans` 内复制少量 `ModuleCompiler` / `ModuleRunner` 的 classpath 与 JUnit Launcher 逻辑，以换取结构化返回？
3. 生成代码方法名是否需要由 RuleTrans 强制规范，例如 `rule` + 语义 slug，还是允许 LLM 自行命名后由后处理器校验唯一性？

---

## 13. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0007-Struct-Combination-Rule-Schema.md`
- `doc/RFC-0010-Cross-PartCategory-Total-Constraint.md`
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
- `src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java`
- `src/test/java/com/jmix/scenario/ruletest/CodeRuleOnlyValidateTest.java`
- `src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java`
- `src/test/java/com/jmix/scenario/ruletest/StructCombinationRuleTest.java`
