# RFC-0014: RuleTrans 到 RuleUnit 的端到端主流程重构

> 状态：草案（Draft）
> 日期：2026-06-13
> 相关文档：`doc/RFC-0011-RuleTrans-Module.md`, `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`, `doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`

---

## 1. 摘要

RFC-0013 已经把“业务可读 JSON 用例 + RuleUnit 执行服务”的核心能力搭起来。当前剩余问题是主流程还没有彻底收束：`RuleTransEngine` 虽然已经能调用 `RuleUnitTestExecutorService`，但构造器、字段、旧测试生成器和 `RuleSnippetAssembler` 中仍保留了直接生成并运行 JUnit 的路径。

本 RFC 设计一个新的端到端主流程：

```text
用户自然语言规则
  -> RuleTrans 原有翻译链路生成 Java 规则方法体
  -> 组装临时 Java 规则类并编译
  -> 由注解生成 Module
  -> 生成业务 JSON 单元测试用例
  -> RuleUnitTestExecutorService 执行业务用例
  -> 返回翻译、编译、单元测试统一报告
```

重构后，RuleTrans 主流程不再生成 JUnit 测试类，也不再通过 JUnit Launcher 执行校验。JUnit 只作为项目自身回归测试框架存在，不作为 RuleTrans 运行时主链路的一部分。

---

## 2. 当前问题

### 2.1 主流程存在混合状态

当前代码中已经出现新旧两条链路并存：

- 新链路：`RuleTransEngine.translateWithRetry(...)` 编译生成的规则类后，通过 `DefaultRuleUnitTestExecutorService` 执行业务 JSON 用例。
- 旧链路：`RuleTransEngine` 仍注入 `TestExecutionProcessor`；`RuleSnippetAssembler` 仍提供 `assembleExecutableTest(...)`；`TestExecutionProcessor` 仍直接依赖 JUnit Launcher。

这会带来三个问题：

1. 主流程依赖关系不清晰：看构造器很难判断真正的执行入口是 RuleUnit 还是 JUnit。
2. 旧模型持续泄漏：`RuleTransTestCase`、`selectedParts`、`requests`、`expectedFirstPartQuantities` 等实现型字段仍被 source 层保留。
3. 后续修复容易走错路径：开发者可能继续补 `RuleSnippetAssembler` 的 JUnit 生成逻辑，而不是补正式的 RuleUnit 执行能力。

### 2.2 直接调用 JUnit 不适合做业务主链路

JUnit 适合做项目回归测试，不适合承担 RuleTrans 运行期验证：

- JUnit 测试类是代码载体，不是业务用例事实来源。
- JUnit 执行报告面向测试框架，不面向业务界面或服务接口。
- JUnit 断言失败只能说明某个生成测试失败，不天然表达规则用例里的 `given`、`expect`、`diagnostics`。
- 生成 JUnit 需要把测试 helper、断言和引擎调用细节塞回 Java 源码，违背 RFC-0013 的业务可读 JSON 方向。

---

## 3. 目标与非目标

### 3.1 目标

P0 目标：

1. 提供一个从自然语言规则到 RuleUnit 测试报告的统一主入口。
2. 保留并复用原 RuleTrans 翻译能力：分类识别、场景分类、prompt 生成、LLM 生成规则方法体、方法体后处理、Java 编译。
3. 主流程使用 RFC-0013 的 `BusinessRuleTestCaseSet` 和 `RuleUnitTestExecutorService` 执行验证。
4. 从 `src/main/java` 的 RuleTrans 主链路中删除直接调用 JUnit 的代码。
5. 明确编译失败、用例生成失败、RuleUnit 执行失败、规则逻辑失败的重试策略。
6. 让最终结果同时包含：生成的 Java 方法体、编译结果、业务 JSON 用例、RuleUnit 执行报告、失败分类。

### 3.2 非目标

P0 不做：

1. 不重写 `RuleSnippetGenerator` 的 LLM 生成策略。
2. 不改变 `ModuleConstraintExecutor` 的生产求解语义。
3. 不把业务 JSON 升级为正式北向业务 API，仅作为 RuleTrans 单规则验证事实来源。
4. 不要求每次成功翻译都自动写入源码目录；是否持久化由配置决定。
5. 不要求一次性删除所有历史测试类里的 JUnit 使用；只删除主流程直接运行 JUnit 的实现。

---

## 4. 新主流程

### 4.1 总体流程

```text
RuleTransPipeline.execute(request)
  1. validateRequest
  2. prepareContext
     - module level 时复用 CategoryIdentifier 识别目标分类
  3. classifyScenario
     - 复用 RuleScenarioClassifier
  4. generateMethodBody
     - 复用 RuleSnippetGenerator
  5. assembleCompileUnit
     - 复用 RuleSnippetAssembler 只组装规则类，不组装 JUnit
  6. compile
     - 复用 CompilationProcessor
  7. buildModule
     - 用 ModuleGenneratorByAnno 从生成类构建 Module
  8. generateBusinessCases
     - 复用 RuleTestCaseGenerator.generateBusinessCases(...)
  9. executeRuleUnitCases
     - 调用 RuleUnitTestExecutorService.executeCaseSet(...)
  10. classifyResultAndRetry
      - 根据失败类型决定修复规则代码、重生成用例或直接返回失败
```

### 4.2 数据流

```text
RuleTransRequest
  naturalLanguage
  RuleContext
  maxRetries
  options

    |
    v

RuleScenario + RuleMetadata
    |
    v
methodBody
    |
    v
AssembledRuleClass
    |
    v
CompilationResult
    |
    v
Generated Module
    |
    v
BusinessRuleTestCaseSet
    |
    v
RuleUnitTestCaseSetReport
    |
    v
RuleTransPipelineResult
```

### 4.3 成功条件

一次端到端执行成功必须同时满足：

1. 生成的 Java 方法体不为空。
2. 组装后的 Java 规则类编译通过。
3. 业务 JSON 用例生成成功；若用例为空，必须按配置允许“只编译不执行”。
4. RuleUnit 执行报告 `passed=true`。
5. 没有基础设施错误，例如 Module 加载失败、serviceMethod 不支持、JSON schema 不合法。

---

## 5. 新增与调整的核心类型

### 5.1 RuleTransRequest

新增请求对象，作为自然语言主入口的唯一入参。

```java
public record RuleTransRequest(
        String naturalLanguage,
        RuleContext context,
        int maxRetries,
        RuleTransPipelineOptions options) {
}
```

`RuleTransPipelineOptions`：

```java
public record RuleTransPipelineOptions(
        boolean generateBusinessCases,
        boolean executeBusinessCases,
        boolean persistBusinessCases,
        Path businessCaseOutputRoot,
        boolean allowEmptyBusinessCases) {
}
```

默认值：

| 字段 | 默认值 | 说明 |
| --- | --- | --- |
| `generateBusinessCases` | `true` | 生成业务 JSON 用例 |
| `executeBusinessCases` | `true` | 编译通过后执行 RuleUnit |
| `persistBusinessCases` | `false` | 默认不写源码目录，避免主流程产生未审阅文件 |
| `businessCaseOutputRoot` | `src/test/resources/rule-unit-cases` | 显式持久化时的根目录 |
| `allowEmptyBusinessCases` | `false` | 端到端验证默认要求至少一个用例 |

### 5.2 RuleTransPipelineResult

替代当前偏混合的 `RuleTransResult`，或者作为 `RuleTransResult` 的下一版字段结构。

```java
public record RuleTransPipelineResult(
        boolean success,
        String methodBody,
        int attempts,
        RuleScenario scenario,
        RuleMetadata metadata,
        CompilationResult compilationResult,
        BusinessRuleTestCaseSet businessCaseSet,
        RuleUnitTestCaseSetReport ruleUnitReport,
        RuleTransFailureKind failureKind,
        List<String> messages) {
}
```

失败分类：

```java
public enum RuleTransFailureKind {
    NONE,
    INVALID_REQUEST,
    CATEGORY_IDENTIFICATION_FAILED,
    CODE_GENERATION_FAILED,
    COMPILATION_FAILED,
    BUSINESS_CASE_GENERATION_FAILED,
    BUSINESS_CASE_SCHEMA_INVALID,
    RULE_UNIT_INFRA_FAILED,
    RULE_LOGIC_FAILED,
    RETRY_EXHAUSTED
}
```

### 5.3 RuleTransPipeline

建议新增专门编排类，把 `RuleTransEngine` 从“所有事情都做”的角色里拆出来。

```java
public final class RuleTransPipeline {

    public RuleTransPipelineResult execute(RuleTransRequest request);
}
```

职责：

1. 编排从自然语言到 RuleUnit 报告的完整流程。
2. 管理重试状态。
3. 做失败分类。
4. 管理生成类加载器和 `ModuleConstraintExecutor` 生命周期。
5. 根据配置持久化业务 JSON。

`RuleTransEngine` 可以保留为兼容 facade：

```java
public RuleTransPipelineResult translateAndVerify(
        String naturalLanguage,
        RuleContext context,
        int maxRetries) {
    return pipeline.execute(new RuleTransRequest(...));
}
```

旧的 `translate(...)` 继续只返回方法体；`translateWithRetry(...)` 可以迁移为调用新 pipeline。

### 5.4 RuleUnitCaseExecutionProcessor

新增一个非 JUnit 的执行适配器，替换 `TestExecutionProcessor` 在主流程中的角色。

```java
public final class RuleUnitCaseExecutionProcessor {

    public RuleUnitExecutionResult execute(
            Class<?> generatedRuleClass,
            BusinessRuleTestCaseSet caseSet);
}
```

内部流程：

```text
Class<?> generatedRuleClass
  -> ModuleGenneratorByAnno.buildModule(generatedRuleClass)
  -> ModuleConstraintExecutor.INST.init(config)
  -> ModuleConstraintExecutor.INST.addModule(module.getId(), module)
  -> new DefaultRuleUnitTestExecutorService(module)
  -> executeCaseSet(caseSet)
  -> RuleUnitExecutionResult
  -> ModuleConstraintExecutor.INST.fini()
```

`RuleUnitExecutionResult` 可以直接包装 `RuleUnitTestCaseSetReport`，也可以提供一层 RuleTrans 友好的失败信息。

---

## 6. 旧 JUnit 主链路删除清单

### 6.1 必须从主流程删除

| 位置 | 调整 |
| --- | --- |
| `RuleTransEngine` | 删除 `TestExecutionProcessor` 字段、构造器参数、`testExecutionProcessor()` 暴露方法 |
| `RuleTransEngine.translateWithRetry(...)` | 改为调用 `RuleTransPipeline` 或内部只走 RuleUnit 执行器 |
| `RuleSnippetAssembler` | 删除 `assembleExecutableTest(...)`、`executableTestSource(...)`、`appendTestMethod(...)` 等 JUnit 测试类生成逻辑 |
| `RuleSnippetAssembler` | 删除对 `RuleTransTestCase`、`RuleTransTestCaseSet`、JUnit、`ModuleScenarioTestBase` 的 source 层依赖 |
| `TestExecutionProcessor` | 从 `src/main/java` 删除，或移动到 `src/test/java` 作为历史测试工具 |
| `FailedTestCase` / `TestExecutionResult` | 若语义仍需复用，重命名为非 JUnit 的 `RuleTransVerificationFailure` / `RuleTransVerificationResult` |
| `RuleTestCaseGenerator.generate(...)` | 删除或标记弃用旧 `RuleTransTestCaseSet` 路径；主流程只调用 `generateBusinessCases(...)` |

### 6.2 可以暂时保留但不得被主流程引用

| 类型 | 处理建议 |
| --- | --- |
| 历史 scenario 测试中的 `RuleScenarioHarnessSupport` | P0 可先改为 test-scope 工具，后续逐步改成业务 JSON + RuleUnit |
| `RuleTransTestCase` / `RuleTransTestCaseSet` | 若测试还未迁移，可移到 `src/test/java` 或 `src/testFixtures`；source 主流程不得依赖 |
| JUnit 本身 | 继续作为 Maven 回归测试框架存在 |

### 6.3 删除后的静态约束

P0 完成后，以下命令不应在 `src/main/java/com/jmix/ruletrans` 下命中 JUnit 运行依赖：

```powershell
rg "org\\.junit\\.platform|LauncherFactory|DiscoverySelectors|SummaryGeneratingListener" src/main/java/com/jmix/ruletrans
```

允许 `src/test/java` 中继续使用 `org.junit.jupiter.api.Test` 编写项目回归测试。

---

## 7. 重试策略

### 7.1 尝试状态

每次尝试记录：

```java
public record RuleTransAttemptState(
        int attempt,
        String methodBody,
        CompilationResult compilationResult,
        BusinessRuleTestCaseSet businessCaseSet,
        RuleUnitTestCaseSetReport ruleUnitReport,
        RuleTransFailureKind failureKind) {
}
```

### 7.2 重试决策矩阵

| 失败类型 | 是否重试 | 修复对象 | Prompt 输入 |
| --- | --- | --- | --- |
| 编译失败 | 是 | Java 规则方法体 | 编译 diagnostics、上一版 methodBody、规则上下文 |
| 业务 JSON 解析失败 | 是 | 测试用例 JSON | JSON 解析错误、schema、自然语言、methodBody |
| serviceMethod 不支持 | 是 | 测试用例 JSON | 支持的方法枚举、错误字段、自然语言、methodBody |
| RuleUnit 执行基础设施失败 | 否 | 无 | 返回失败，人工处理 |
| RuleUnit 断言失败且像规则逻辑错误 | 是 | Java 规则方法体 | failed cases、actual、expect、methodBody |
| RuleUnit 断言失败且像用例期望错误 | 是 | 测试用例 JSON | failed cases、actual、expect、自然语言 |
| 重试耗尽 | 否 | 无 | 返回 `RETRY_EXHAUSTED` |

### 7.3 规则逻辑失败与用例错误的区分

P0 使用保守启发式：

1. 如果 Java 编译成功，RuleUnit 执行成功但期望不匹配，默认先认为是规则逻辑错误，修复 methodBody。
2. 如果失败原因是 JSON 字段缺失、serviceMethod 无法解析、part/parameter code 不存在，认为是用例生成错误，重生成业务 JSON。
3. 如果 `ModuleConstraintExecutor` 初始化、类加载、注解生成 Module 失败，认为是基础设施错误，不自动重试。

P1 可以加入更细的 LLM 判别 prompt，让模型判断“代码错还是测试错”。

---

## 8. 业务 JSON 生成与持久化

### 8.1 生成时机

建议在 Java 编译成功之后生成业务 JSON：

```text
methodBody generated
  -> compile success
  -> generate business cases using naturalLanguage + scenario + methodBody
```

理由：

1. 编译失败时无需浪费一次测试用例 LLM 调用。
2. 编译通过后的 methodBody 更稳定，测试用例生成 prompt 更准确。
3. 如果后续重试只修复 Java 代码，可以按策略选择复用旧用例或重新生成。

默认策略：

- 第一次编译成功后生成业务 JSON。
- 如果后续只是编译修复，编译通过后重新生成业务 JSON。
- 如果后续是规则逻辑修复，可复用同一套业务 JSON，除非执行报告表明用例引用了不存在的对象。

### 8.2 持久化策略

默认不自动写入源码目录。成功后若 `persistBusinessCases=true`，写入：

```text
src/test/resources/rule-unit-cases/<module-code>/<rule-code>.json
```

写入约束：

1. 只写通过 RuleUnit 执行的用例。
2. 文件名来自 `RuleMetadata.ruleCode()`，不可用时使用方法名。
3. 若文件已存在，默认覆盖策略必须由 options 指定；P0 推荐 `overwrite=false` 并返回冲突消息。

---

## 9. 与现有类的关系

### 9.1 保留复用

| 类 | 新职责 |
| --- | --- |
| `CategoryIdentifier` | module-level 自然语言规则的目标分类识别 |
| `RuleScenarioClassifier` | 判断规则大类、scope、POST/priority 等 |
| `RuleSnippetGenerator` | 生成 Java 方法体 |
| `RuleSnippetAssembler` | 只负责生成可编译规则类，不负责生成 JUnit 测试类 |
| `CompilationProcessor` | 编译临时规则类 |
| `RuleTestCaseGenerator.generateBusinessCases(...)` | 生成业务 JSON 用例 |
| `BusinessRuleTestCaseSet` | 业务用例事实来源 |
| `DefaultRuleUnitTestExecutorService` | 业务用例执行与比较 |
| `DefaultModuleConstraintExecutor4SingleRule` | 对主线 `ModuleConstraintExecutor` 的单规则 facade |

### 9.2 重命名或拆分建议

| 当前类 | 建议 |
| --- | --- |
| `RuleTransResult` | 保留兼容构造器，但新增或迁移到 `RuleTransPipelineResult` |
| `TestExecutionResult` | 若还被 RuleUnit 主流程使用，改名为 `RuleTransVerificationResult`，删除 JUnit 语义 |
| `FailedTestCase` | 改名为 `RuleTransVerificationFailure` |
| `TestExecutionProcessor` | 删除或移动到 test-scope，不再被 source 主流程依赖 |

---

## 10. 端到端接口示例

### 10.1 调用方

```java
RuleTransPipelineResult result = ruleTransPipeline.execute(
        new RuleTransRequest(
                "cpu 中 CoreNum=4 的部件不能和 drive 中 Speed=5400 的部件同时选择",
                RuleContextFactory.module(module),
                1,
                RuleTransPipelineOptions.defaults()));

if (!result.success()) {
    log.warn("RuleTrans failed: {}", result.messages());
}
```

### 10.2 返回结构

```json
{
  "success": true,
  "attempts": 1,
  "methodBody": "inCompatible(ruleCode, \"cpu:CoreNum=4\", \"drive:Speed=5400\");",
  "compilationResult": {
    "success": true
  },
  "businessCaseSet": {
    "cases": [
      {
        "id": "COMP-PART-001",
        "serviceMethod": "testCompatibility",
        "given": {
          "parts": [
            {"code": "cpu4", "isSelected": true},
            {"code": "drive5400", "isSelected": true}
          ]
        },
        "expect": {
          "compatible": false
        }
      }
    ]
  },
  "ruleUnitReport": {
    "passed": true
  }
}
```

---

## 11. 实现计划

| 阶段 | 任务 | 优先级 |
| --- | --- | --- |
| P0-1 | 新增 `RuleTransRequest`、`RuleTransPipelineOptions`、`RuleTransPipelineResult`、`RuleTransFailureKind` | P0 |
| P0-2 | 新增 `RuleTransPipeline` 编排类 | P0 |
| P0-3 | 新增 `RuleUnitCaseExecutionProcessor`，封装生成类加载、Module 构建、RuleUnit 执行和资源清理 | P0 |
| P0-4 | 调整 `RuleTransEngine.translateWithRetry(...)` 调用新 pipeline，移除 `TestExecutionProcessor` 构造器依赖 | P0 |
| P0-5 | 裁剪 `RuleSnippetAssembler`，只保留 compile unit 组装；删除 JUnit 测试类生成方法 | P0 |
| P0-6 | 主流程停止使用 `RuleTransTestCase` / `RuleTransTestCaseSet`，只使用 `BusinessRuleTestCaseSet` | P0 |
| P0-7 | 删除或 test-scope 移动 `TestExecutionProcessor` | P0 |
| P0-8 | 更新 RuleTrans 回归测试，使用 fake LLM + RuleUnit 报告断言，不再断言 JUnit Launcher 路径 | P0 |
| P0-9 | 增加静态约束测试，确保 `src/main/java/com/jmix/ruletrans` 不依赖 JUnit Launcher | P0 |
| P1-1 | 增加业务 JSON 持久化选项和文件冲突策略 | P1 |
| P1-2 | 增强“代码错 / 用例错”自动判别 prompt | P1 |

---

## 12. 验收标准

### AC-001: 自然语言到 RuleUnit 报告端到端通过

给定一条自然语言兼容规则：

```text
cpu 中 CoreNum=4 的部件不能和 drive 中 Speed=5400 的部件同时选择
```

当调用新主入口后：

1. 返回非空 `methodBody`。
2. `compilationResult.success=true`。
3. 返回非空 `businessCaseSet.cases`。
4. `ruleUnitReport.passed=true`。

### AC-002: 主流程不再直接运行 JUnit

执行：

```powershell
rg "LauncherFactory|DiscoverySelectors|SummaryGeneratingListener|org\\.junit\\.platform" src/main/java/com/jmix/ruletrans
```

结果应为空。

### AC-003: `RuleSnippetAssembler` 不再生成 JUnit 测试类

`RuleSnippetAssembler` 中不再存在：

- `assembleExecutableTest`
- `executableTestSource`
- `appendTestMethod`
- `ModuleScenarioTestBase`
- `org.junit.jupiter.api.Test`

### AC-004: 编译失败触发代码修复重试

使用 fake LLM 第一次返回无法编译的方法体、第二次返回可编译方法体：

1. pipeline 尝试次数为 2。
2. 第二次编译通过。
3. 若业务用例执行通过，则整体成功。

### AC-005: 业务 JSON schema 错误触发用例重生成

使用 fake LLM 第一次返回不支持的 `serviceMethod`，第二次返回合法 JSON：

1. 第一次失败分类为 `BUSINESS_CASE_SCHEMA_INVALID`。
2. pipeline 不修改 Java methodBody。
3. 第二次用例合法并继续 RuleUnit 执行。

### AC-006: RuleUnit 逻辑失败触发代码修复重试

给定业务 JSON 期望 `compatible=false`，第一次生成规则未添加互斥约束：

1. RuleUnit 报告失败。
2. 失败分类为 `RULE_LOGIC_FAILED`。
3. 下一次 prompt 包含失败 case 的 `given`、`expect`、`actual`。

### AC-007: JUnit 只作为项目回归测试框架存在

项目测试仍可使用 JUnit，例如：

```powershell
mvn test "-Dtest=RuleTransPipelineTest,RuleUnitBusinessCaseTest"
```

但被测的 RuleTrans 主流程不得创建 JUnit Launcher，不得生成 JUnit 测试源文件。

---

## 13. 测试计划

新增或调整测试：

| 测试类 | 覆盖点 |
| --- | --- |
| `RuleTransPipelineTest` | fake LLM 端到端成功、编译重试、RuleUnit 失败重试 |
| `RuleTransNoJUnitMainFlowTest` | 静态扫描 source 主流程不依赖 JUnit Launcher |
| `RuleSnippetAssemblerCompileUnitTest` | 只生成规则类，不生成测试类 |
| `RuleBusinessTestCaseGeneratorTest` | 业务 JSON schema、serviceMethod 归一化 |
| `RuleUnitBusinessCaseTest` | RFC-0013 已有执行能力继续通过 |
| `RuleTransModuleTest` | 改为验证 pipeline result，不再注入 `TestExecutionProcessor` |

不再保留或需迁移：

| 测试 | 处理 |
| --- | --- |
| `TestExecutionProcessorTest` | 删除，或移动为 test-scope 历史工具测试 |
| 依赖 `assembleExecutableTest(...)` 的 scenario harness | 改为编译规则类 + 业务 JSON + RuleUnit 执行 |

---

## 14. 风险与边界

### 14.1 编译通过不代表 Module 可用

生成 Java 编译通过后，`ModuleGenneratorByAnno.buildModule(...)` 仍可能因为注解、字段、规则元数据不完整而失败。该失败应归类为 `RULE_UNIT_INFRA_FAILED` 或更细的 `MODULE_BUILD_FAILED`，P0 不自动让 LLM 修复，除非错误明确来自方法体。

### 14.2 业务用例可能比规则代码更容易出错

LLM 生成业务 JSON 时可能引用不存在的 part 或 parameter。P0 通过 schema 校验和模型对象存在性校验兜住，失败时优先重生成业务 JSON。

### 14.3 ModuleConstraintExecutor 是单例

当前 `ModuleConstraintExecutor.INST` 是单例。`RuleUnitCaseExecutionProcessor` 必须在 `finally` 中调用 `fini()` 并恢复 classloader。并发执行不在 P0 范围内；P1 再考虑隔离执行器实例或加锁。

### 14.4 历史测试迁移成本

`RuleSnippetAssembler` 的旧 JUnit 生成路径在一些 scenario 测试中仍被使用。P0 可以先把这些测试改成 RuleUnit 路径；若迁移成本过高，可以临时把旧工具移动到 `src/test/java`，但不能留在 `src/main/java`。

---

## 15. 复用优先清单

优先复用：

- `CategoryIdentifier`
- `RuleScenarioClassifier`
- `RuleSnippetGenerator`
- `RuleSnippetAssembler.assembleCompileUnit(...)`
- `CompilationProcessor`
- `RuleTestCaseGenerator.generateBusinessCases(...)`
- `BusinessRuleTestCaseSet`
- `DefaultRuleUnitTestExecutorService`
- `DefaultModuleConstraintExecutor4SingleRule`
- `RuleUnitInputConverter`
- `RuleUnitResultComparator`
- `ModuleGenneratorByAnno`

不新增：

- 不新增第二套求解器。
- 不新增第二套 JUnit 生成器。
- 不新增业务 JSON 之外的测试用例事实模型。
- 不在主流程里手写 `InferParasReq` / `ModuleValidateReq`。
- 不让 RuleTrans 主流程依赖 `ModuleScenarioTestBase`。

可由上下文推导：

- `ruleCode` 可从 `RuleMetadata`、方法名或注解生成。
- `moduleId` 可从生成后的 Module 读取。
- part 所属 category 可从 Module 查询。
- `serviceMethod` 可由 `RuleScenario` + `BusinessRuleFamily` 推导；JSON 中缺省时由生成器归一化补齐。

---

## 16. 待确认问题

Q1. `TestExecutionProcessor` 是直接删除，还是移动到 `src/test/java` 作为历史测试迁移期工具？

Q2. 成功后的业务 JSON 是否默认写入 `src/test/resources/rule-unit-cases`？本 RFC 建议默认不写，只有显式 `persistBusinessCases=true` 时写入。

Q3. `translateWithRetry(...)` 是否保持原方法签名并返回增强后的 `RuleTransResult`，还是新增 `translateAndVerify(...)` / `RuleTransPipeline.execute(...)` 作为新入口，旧方法标记废弃？

---

## 17. 参考资料

- `doc/RFC-0011-RuleTrans-Module.md`
- `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`
- `src/main/java/com/jmix/ruletrans/RuleTransEngine.java`
- `src/main/java/com/jmix/ruletrans/assembler/RuleSnippetAssembler.java`
- `src/main/java/com/jmix/ruletrans/postprocessor/TestExecutionProcessor.java`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTestCaseGenerator.java`
- `src/main/java/com/jmix/ruleunit/DefaultRuleUnitTestExecutorService.java`
- `src/main/java/com/jmix/ruleunit/DefaultModuleConstraintExecutor4SingleRule.java`
