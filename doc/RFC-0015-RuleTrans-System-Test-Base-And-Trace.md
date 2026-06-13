# RFC-0014: RuleTransPipeline 系统测试基类与诊断打印

> 状态：草案（Draft）
> 日期：2026-06-13
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0011-RuleTrans-Module.md`, `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`, `doc/RFC-0014-RuleTrans-End-to-End-RuleUnit-Pipeline.md`

---

## 1. 摘要

当前代码已经新增 `RuleTransPipeline`，并把端到端主流程收束为：

```text
自然语言规则
  -> 构建 RuleContext
  -> 模块级分类识别
  -> 场景分类与 SDK profile
  -> LLM 生成 Java rule 方法体
  -> 组装临时规则类
  -> 编译
  -> 生成业务可读 BusinessRuleTestCaseSet
  -> RuleUnitCaseExecutionProcessor 执行业务用例
  -> 返回 RuleTransPipelineResult
```

本 RFC 不再重新设计主流程，而是在现有 `RuleTransPipeline.execute(...)` 之上新增系统级测试封装 `RuleTransPipelineTestBase`。目标是让测试方法保持 2-3 行，只表达自然语言输入、期望 Java 输出和打印结果，同时把 LLM 调用、编译、业务用例生成、RuleUnit 执行、最终方法体等关键信息用统一 `print(...)` 输出。

---

## 2. 动机

### 2.1 当前代码事实

截至 2026-06-13，相关代码事实如下：

| 能力 | 当前代码 |
| --- | --- |
| 端到端入口 | `src/main/java/com/jmix/ruletrans/RuleTransPipeline.java` |
| 请求对象 | `RuleTransRequest(naturalLanguage, context, maxRetries, options)` |
| 运行选项 | `RuleTransPipelineOptions.defaults()`、`compileOnly()` |
| 结果对象 | `RuleTransPipelineResult`，包含 `methodBody`、`attempts`、`compilationResult`、`businessCaseSet`、`ruleUnitReport`、`failureKind`、`messages`、`attemptStates` |
| 单次尝试快照 | `RuleTransAttemptState` |
| 失败分类 | `RuleTransFailureKind` |
| 业务用例执行 | `RuleUnitCaseExecutionProcessor` 调用 `DefaultRuleUnitTestExecutorService` |
| 低层翻译协作者 | `RuleTransEngine.translate(...)`，不再承担端到端重试执行 |
| 规则类组装 | `RuleSnippetAssembler.assembleCompileUnit(...)`，当前不再生成 JUnit harness |
| 现有端到端测试 | `RuleTransPipelineTest`、`RuleTransModuleTest` |

这说明旧版 RFC 中“当前没有 `RuleTransPipeline`，需要未来新增 trace-aware 主链路”的判断已经过时。新设计必须以现有 `RuleTransPipeline` 为中心。

### 2.2 具体诉求

期望系统测试接近以下形式：

```java
@Test
void testCpuAtMostOne() {
    RuleTransPipelineRunResult result = assertRuleTrans(CpuFacts.class, "cpu",
            "CPU最多只能配一个",
            expectJavaContains("addLessOrEqual"));
    print(result);
}
```

模块级规则：

```java
@Test
void testCpuDriveCompatibility() {
    RuleTransPipelineRunResult result = assertRuleTrans(CpuDriveFacts.class,
            "四核 CPU 不能兼容转速为 5400 转的硬盘",
            expectJavaContains("inCompatible"));
    print(result);
}
```

测试方法只写：

1. 输入：自然语言规则。
2. 预期输出：最终 Java 方法体应包含或等价于什么。
3. 打印：把整条链路关键结果打印出来。

### 2.3 为什么需要补封装

`RuleTransPipelineResult` 已经包含端到端结果，但直接写测试仍然需要重复：

- 从注解模型类构建 `Module` 和 `RuleContext`。
- 构造 `PromptBuilder`、`RuleSnippetGenerator`、`RuleSnippetAssembler`、`CompilationProcessor`、`RuleTestCaseGenerator`、`RuleUnitCaseExecutionProcessor`。
- 选择真实 LLM。
- 根据 `RuleTransPipelineResult` 手工拼打印。
- 为 LLM prompt/response 和耗时额外包一层采集器。

`RuleTransPipelineTestBase` 的价值是把这些重复工作封装掉，并把诊断输出做成稳定格式。

---

## 3. 设计方案

### 3.1 分层边界

本 RFC 只新增测试辅助与诊断打印，不改变 RuleTrans 生产主流程：

| 层次 | 职责 | 本 RFC 处理 |
| --- | --- | --- |
| 产品/模型声明 | `@ModuleAnno`、`@PartAnno`、`@ParaAnno`、`@DAttrAnnoN` 声明模型事实 | 复用，不新增 `@ModelAnno` |
| 上下文构建 | `ModuleGenneratorByAnno` + `RuleContextFactory` 构建 `RuleContext` | 测试基类封装 |
| 自然语言转换 | `RuleTransPipeline.execute(...)` 编排完整链路 | 复用现有主入口 |
| 业务用例验证 | `BusinessRuleTestCaseSet` + RuleUnit | 复用现有主链路 |
| 系统测试辅助 | 简化测试写法、断言最终 Java、打印诊断 | 本 RFC 新增 |
| 生产诊断模型 | 对外暴露服务级 trace 或持久化 | 不在 P0 范围 |

### 3.2 复用优先清单

- 优先复用：
  - `RuleTransPipeline`
  - `RuleTransRequest`
  - `RuleTransPipelineOptions`
  - `RuleTransPipelineResult`
  - `RuleTransAttemptState`
  - `RuleTransFailureKind`
  - `RuleContextFactory.fromAnnotatedClass(...)`
  - `ModuleGenneratorByAnno`
  - `RuleTransRealLlmSupport.realLlmInvoker()`
  - `RuleSnippetAssembler`
  - `CompilationProcessor`
  - `RuleTestCaseGenerator.generateBusinessCases(...)`
  - `RuleUnitCaseExecutionProcessor`
  - `BusinessRuleTestCaseSet`
  - `RuleUnitTestCaseSetReport`
- 不新增：
  - 不新增第二套 pipeline。
  - 不新增第二套模型注解。
  - 不在测试方法里重复手工拼 pipeline 依赖。
  - 不恢复 JUnit harness 生成路径。
  - 不新增与 `RuleTransPipelineResult` 平行的生产级 trace 返回对象。
- 可由上下文推导：
  - `Module` 由注解模型类推导。
  - `RuleContext` 由 `Module` 和可选 `categoryCode` 推导。
  - `serviceMethod` 可由 `RuleScenario` 和 `BusinessRuleFamily` 推导。
  - 编译次数、业务用例数量、RuleUnit 执行数量可由 `RuleTransPipelineResult` 和 `attemptStates` 推导。
- 相似测试来源：
  - `src/test/java/com/jmix/ruletrans/RuleTransPipelineTest.java`
  - `src/test/java/com/jmix/ruletrans/RuleTransModuleTest.java`
  - `src/test/java/com/jmix/ruletrans/rulescenario/RuleScenarioHarnessSupport.java`
  - `src/test/java/com/jmix/ruleunit/RuleUnitBusinessCaseTest.java`

### 3.3 新增测试基类

新增测试基类：

```text
src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java
```

核心接口：

```java
public abstract class RuleTransPipelineTestBase {

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String naturalLanguage,
            RuleTransJavaExpectation expectation) {
        return assertRuleTrans(annotatedModelClass, null, naturalLanguage, expectation);
    }

    protected RuleTransPipelineRunResult assertRuleTrans(
            Class<?> annotatedModelClass,
            String categoryCode,
            String naturalLanguage,
            RuleTransJavaExpectation expectation) {
        RuleTransPipelineRunResult result = runRuleTrans(
                annotatedModelClass,
                categoryCode,
                naturalLanguage,
                RuleTransPipelineOptions.defaults());
        expectation.assertMatches(result.methodBody());
        return result;
    }

    protected RuleTransPipelineRunResult runRuleTrans(
            Class<?> annotatedModelClass,
            String categoryCode,
            String naturalLanguage,
            RuleTransPipelineOptions options) {
        // Build module and context from annotations, execute RuleTransPipeline,
        // collect diagnostic LLM calls, return wrapper result.
    }

    protected RuleTransJavaExpectation expectJavaContains(String expectedSnippet) {
        return RuleTransJavaExpectation.contains(expectedSnippet);
    }

    protected RuleTransJavaExpectation expectJavaEqualsIgnoringWhitespace(String expectedMethodBody) {
        return RuleTransJavaExpectation.equalsIgnoringWhitespace(expectedMethodBody);
    }

    protected void print(RuleTransPipelineRunResult result) {
        RuleTransPipelineResultPrinter.print(result, System.out);
    }
}
```

### 3.4 运行结果包装

新增测试辅助结果对象：

```text
src/test/java/com/jmix/ruletrans/RuleTransPipelineRunResult.java
```

```java
public record RuleTransPipelineRunResult(
        String naturalLanguage,
        String contextSummary,
        RuleTransPipelineResult pipelineResult,
        RuleTransPipelineDiagnostics diagnostics) {

    public String methodBody() {
        return pipelineResult.methodBody();
    }

    public boolean success() {
        return pipelineResult.success();
    }
}
```

说明：

1. `RuleTransPipelineResult` 是生产代码已有结果，不复制字段。
2. `RuleTransPipelineDiagnostics` 是测试辅助诊断，主要保存 LLM 调用信息和打印文件路径。
3. 编译、业务用例、RuleUnit 执行统计优先从 `RuleTransPipelineResult` 和 `attemptStates` 推导。

### 3.5 LLM 调用诊断

当前 `RuleTransPipelineResult` 不包含 Prompt、Response 或 LLM 耗时。P0 在测试基类中用包装器实现，不侵入生产主流程：

```java
final class DiagnosticLlmInvoker implements LLMInvoker {

    private final LLMInvoker delegate;
    private final RuleTransPipelineDiagnostics diagnostics;

    @Override
    public String generate(String systemMessage, String userMessage) {
        long start = System.nanoTime();
        try {
            String response = delegate.generate(systemMessage, userMessage);
            diagnostics.addLlmCall(systemMessage, userMessage, response,
                    elapsedMillis(start), null);
            return response;
        } catch (RuntimeException e) {
            diagnostics.addLlmCall(systemMessage, userMessage, "",
                    elapsedMillis(start), e);
            throw e;
        }
    }
}
```

LLM 调用记录：

```java
public record RuleTransLlmCallDiagnostic(
        int index,
        String stage,
        String promptSummary,
        Path fullPromptFile,
        String responseSummary,
        Path fullResponseFile,
        long durationMillis,
        boolean success,
        String errorMessage) {
}
```

`stage` 可以 P0 做 best-effort 识别：

| 识别来源 | stage |
| --- | --- |
| Prompt 包含可用分类摘要 | `CATEGORY_IDENTIFICATION` |
| Prompt 是规则生成模板 | `RULE_GENERATION` |
| Prompt 包含 compiler errors | `COMPILATION_CORRECTION` |
| Prompt 包含 failed cases | `TEST_CORRECTION` |
| Prompt 生成业务 JSON 用例 | `BUSINESS_CASE_GENERATION` |
| 识别不出来 | `LLM_CALL` |

完整 prompt/response 默认写入：

```text
target/ruletrans-pipeline-system/<test-id>/llm-001.prompt.txt
target/ruletrans-pipeline-system/<test-id>/llm-001.response.txt
```

打印时只展示摘要和文件路径，避免大段 Prompt 淹没关键信息。

### 3.6 Java 输出断言

新增测试辅助断言：

```text
src/test/java/com/jmix/ruletrans/RuleTransJavaExpectation.java
```

P0 提供：

```java
expectJavaContains("inCompatible")
expectJavaContains("sum4Selected")
expectJavaEqualsIgnoringWhitespace("""
        model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
        """)
```

由于真实 LLM 可能生成等价但格式不同的 Java，系统测试默认推荐 `contains`。精确断言只用于低层组件测试。

### 3.7 打印函数

新增测试辅助 printer：

```text
src/test/java/com/jmix/ruletrans/RuleTransPipelineResultPrinter.java
```

默认输出：

```text
========== RuleTransPipeline ==========
[Input]
Natural language: CPU最多只能配一个
Context: module=CpuFacts, scope=PART_CATEGORY, targetCategories=cpu
Options: generateBusinessCases=true, executeBusinessCases=true, allowEmptyBusinessCases=false

[LLM Calls]
#1 stage=RULE_GENERATION duration=1832ms success=true
Prompt summary:
  naturalLanguage=CPU最多只能配一个
  targetCategories=cpu
  sdkProfile=CONSTRAINT
Full prompt: target/ruletrans-pipeline-system/cpu-at-most-one/llm-001.prompt.txt
Response summary:
  model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
Full response: target/ruletrans-pipeline-system/cpu-at-most-one/llm-001.response.txt

[Attempts]
#1 failureKind=NONE
methodBody:
  model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
compile: success=true errors=0 source=target/ruletrans-test/...
businessCases: count=2 ids=[CASE-001, CASE-002]
ruleUnit: passed=true total=2 passed=2 failed=0

[Summary]
success=true
attempts=1
failureKind=NONE
messages=[]

[Final Method Body]
model().addLessOrEqual(model().sum4Selected("cpu", "", ""), 1);
=======================================
```

打印标签使用英文；自然语言原文和 Java 方法体保持原样。

### 3.8 打印统计来源

| 打印项 | 数据来源 |
| --- | --- |
| LLM 核心输入、输出、耗时 | `DiagnosticLlmInvoker` |
| 编译成功次数、失败次数 | `RuleTransPipelineResult.attemptStates().compilationResult` |
| 编译错误 | `CompilationResult.errors()` / `diagnostics()` |
| 测试用例生成数量 | `BusinessRuleTestCaseSet.cases().size()` |
| 测试用例 id | `BusinessRuleTestCase.id()` |
| 执行用例数量 | `RuleUnitTestCaseSetReport.caseReports().size()` |
| 执行成功/失败 | `RuleUnitTestReport.passed()` |
| 执行失败原因 | `RuleUnitTestReport.failures()` |
| 最终结果 | `RuleTransPipelineResult.success()`、`failureKind()`、`messages()`、`methodBody()` |

### 3.9 和现有 `RuleScenarioHarnessSupport` 的关系

`RuleScenarioHarnessSupport` 已经是规则族场景测试工具，适合覆盖复杂规则族矩阵。`RuleTransPipelineTestBase` 面向更外层的系统级冒烟和定位：

| 基类 | 关注点 |
| --- | --- |
| `RuleScenarioHarnessSupport` | 规则族覆盖、场景矩阵、手工业务用例期望 |
| `RuleTransPipelineTestBase` | 用户视角输入/输出、真实 pipeline 默认路径、统一诊断打印 |

二者可以共享底层 helper，但不强制合并。

---

## 4. 验收准则

### AC-001：测试方法保持 2-3 行

示例：

```java
class RuleTransPipelineSystemTest extends RuleTransPipelineTestBase {

    @Test
    void testCpuAtMostOne() {
        RuleTransPipelineRunResult result = assertRuleTrans(CpuFacts.class, "cpu",
                "CPU最多只能配一个",
                expectJavaContains("addLessOrEqual"));
        print(result);
    }
}
```

预期：

- 测试方法不出现 `new RuleTransPipeline(...)`。
- 测试方法不出现 `new PromptBuilder(...)`、`new CompilationProcessor(...)`。
- 测试默认真实调用 LLM。
- 测试断言最终 `RuleTransPipelineResult.methodBody()`。

### AC-002：支持 `@ModuleAnno` 注解模型体系

测试数据：

```java
@ModuleAnno(id = 814001L)
public static class CpuFacts extends ModuleAlgBase {

    @PartAnno(code = "cpu")
    @DAttrAnno1(code = "CoreNum", options = {"Core_4:4", "Core_8:8"})
    private PartCategoryVar cpu;

    @PartAnno(fatherCode = "cpu", attrs = {"4"})
    private PartVar cpu4;

    @PartAnno(fatherCode = "cpu", attrs = {"8"})
    private PartVar cpu8;
}
```

预期：

- `RuleTransPipelineTestBase` 使用 `RuleContextFactory.fromAnnotatedClass(...)` 或等价路径构建 `Module`。
- 不新增 `@ModelAnno`。
- `categoryCode` 为空时构建 module-level context；非空时构建 part-category context。

### AC-003：打印 LLM 调用核心输入、输出和耗时

预期：

- `print(result)` 包含 `[LLM Calls]`。
- 每次调用包含 `stage`、`durationMillis`、`success`。
- 打印 prompt/response 摘要和完整文件路径。
- LLM 调用失败时也记录失败耗时和错误信息。

### AC-004：打印编译成功次数和失败详情

使用 `RuleTransPipelineTest` 中类似的 fake LLM 场景：

1. 第一次返回 `missingSymbol();`。
2. 第二次返回可编译方法体。

预期：

- `attempts=2`。
- attempts 中至少一条 `failureKind=COMPILATION_FAILED`。
- 打印编译失败 errors。
- 最终成功时打印最终方法体。

### AC-005：打印业务用例生成数量和 RuleUnit 执行结果

端到端默认选项：

```java
RuleTransPipelineOptions.defaults()
```

预期：

- 打印 `businessCases: count=N ids=[...]`。
- 打印 `ruleUnit: passed=true/false total=N passed=X failed=Y`。
- 失败时打印每个失败 case 的 `caseId` 和 `failures`。

### AC-006：支持只编译不执行业务用例

当使用：

```java
RuleTransPipelineOptions.compileOnly()
```

预期：

- 仍打印 LLM 调用和编译结果。
- `businessCases` 标记为 skipped 或 count=0。
- `ruleUnit` 标记为 skipped。
- 最终结果以 `RuleTransPipelineResult.success()` 为准。

### AC-007：模块级规则走真实分类识别

示例：

```java
RuleTransPipelineRunResult result = assertRuleTrans(CpuDriveFacts.class,
        "四核 CPU 不能兼容转速为 5400 转的硬盘",
        expectJavaContains("inCompatible"));
```

预期：

- LLM calls 至少包含一次 `CATEGORY_IDENTIFICATION` 或等价分类识别调用。
- 最终方法体包含 `cpu`、`drive` 或等价 SDK 表达。
- RuleUnit 报告通过。

### AC-008：旧测试兼容

以下测试不需要因为新增测试基类而失效：

- `RuleTransPipelineTest`
- `RuleTransModuleTest`
- `CategoryIdentifierTest`
- `RuleBusinessTestCaseGeneratorTest`
- `com.jmix.ruletrans.rulescenario` 下现有场景测试
- `com.jmix.ruleunit` 下现有 RuleUnit 测试

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 当前状态 |
| --- | --- | --- | --- |
| P0-1 | 以当前代码事实更新 RFC，删除“Pipeline 不存在”的旧表述 | P0 | 已完成 |
| P0-2 | 新增 `RuleTransPipelineTestBase` | P0 | 待开始 |
| P0-3 | 新增 `RuleTransPipelineRunResult` 测试辅助包装 | P0 | 待开始 |
| P0-4 | 新增 `DiagnosticLlmInvoker`，记录 prompt/response/耗时并写入 target 文件 | P0 | 待开始 |
| P0-5 | 新增 `RuleTransPipelineDiagnostics` 与 `RuleTransLlmCallDiagnostic` 测试辅助模型 | P0 | 待开始 |
| P0-6 | 新增 `RuleTransJavaExpectation` | P0 | 待开始 |
| P0-7 | 新增 `RuleTransPipelineResultPrinter.print(...)` | P0 | 待开始 |
| P0-8 | 新增 `RuleTransPipelineSystemTest`，覆盖 part-category 规则、module-level 规则、compileOnly | P0 | 待开始 |
| P0-9 | 回归运行 RuleTrans pipeline 和 RuleUnit 相关测试 | P0 | 待开始 |
| P1-1 | 如果需要服务级诊断，再考虑把 diagnostics 下沉到 `RuleTransPipelineResult` | P1 | 待开始 |
| P1-2 | 增加 JavaParser / AST 等价断言 | P1 | 待开始 |

---

## 6. 风险与边界

### 6.1 Prompt 输出过长

风险：完整 Prompt、context JSON、业务用例 JSON 很长，直接打印会淹没关键信息。

策略：

- 默认打印摘要。
- 完整 prompt/response 写入 `target/ruletrans-pipeline-system/<test-id>/`。
- 打印文件路径，方便需要时定位。

### 6.2 真实 LLM 系统测试不稳定

风险：真实模型、网络、API key 和模型版本会影响系统测试。

策略：

- `RuleTransPipelineTestBase` 默认真实调用 LLM，符合用户确认。
- 重试策略仍由 `RuleTransPipeline` 控制。
- 低层重试矩阵测试可继续使用 fake LLM，例如 `RuleTransPipelineTest`。
- CI 如需隔离，可后续用 JUnit tag 或 Maven profile，但不改变本 RFC 的默认真实策略。

### 6.3 诊断数据与生产结果分离

风险：如果把测试诊断设计成生产结果字段，可能扩大主流程 API。

策略：

- P0 的 LLM prompt/response/duration 只由测试辅助 `DiagnosticLlmInvoker` 采集。
- 生产 `RuleTransPipelineResult` 仍作为权威业务结果。
- 未来确有服务侧诊断需求时，再在 P1 设计 production diagnostics。

### 6.4 精确 Java 输出断言易脆弱

风险：真实 LLM 可能生成等价但字符串不同的 Java。

策略：

- 系统测试优先使用 `expectJavaContains(...)`。
- 对稳定低层组件才使用 `expectJavaEqualsIgnoringWhitespace(...)`。
- P1 再考虑 AST 级断言。

---

## 7. 代码对齐检查

| 检查项 | 结论 |
| --- | --- |
| `RuleTransPipeline` 是否存在 | 已存在，本文档已改为围绕它设计 |
| 是否仍应设计 `RuleTransEngine.translateWithRetry(...)` | 不应设计；当前 `RuleTransEngine` 是低层 translate 协作者 |
| 是否还需要 `RuleTransTrace` 生产模型 | P0 不需要；现有 result + 测试诊断包装足够 |
| 是否还走 JUnit harness | 当前主流程已走 RuleUnit；本文档不恢复 JUnit harness |
| 业务用例事实来源 | `BusinessRuleTestCaseSet` |
| 执行报告来源 | `RuleUnitTestCaseSetReport` |
| 编译和尝试诊断来源 | `CompilationResult` + `RuleTransAttemptState` |
| LLM 调用耗时来源 | 新增测试辅助 `DiagnosticLlmInvoker` |

---

## 8. 用户确认结论

2026-06-13 本轮用户已确认以下设计结论，后续实现不再按开放问题处理：

1. 测试基类名称确定为 `RuleTransPipelineTestBase`。
2. `ModelAnno` 按当前项目的 `@ModuleAnno` 注解模型体系处理，由 `ModuleGenneratorByAnno` / `RuleContextFactory.fromAnnotatedClass(...)` 构建 `Module`。
3. 系统级测试默认真实调用 LLM；普通单元测试仍可使用受控响应或录制响应覆盖边界行为。
4. 最新代码已经包含 `RuleTransPipeline`，本文档以最新 `RuleTransPipeline.execute(...)` 代码事实为准。

---

## 9. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0011-RuleTrans-Module.md`
- `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`
- `doc/RFC-0014-RuleTrans-End-to-End-RuleUnit-Pipeline.md`
- `src/main/java/com/jmix/ruletrans/RuleTransPipeline.java`
- `src/main/java/com/jmix/ruletrans/RuleTransRequest.java`
- `src/main/java/com/jmix/ruletrans/RuleTransPipelineOptions.java`
- `src/main/java/com/jmix/ruletrans/RuleTransPipelineResult.java`
- `src/main/java/com/jmix/ruletrans/RuleTransAttemptState.java`
- `src/main/java/com/jmix/ruletrans/RuleTransFailureKind.java`
- `src/main/java/com/jmix/ruletrans/RuleTransEngine.java`
- `src/main/java/com/jmix/ruletrans/assembler/RuleSnippetAssembler.java`
- `src/main/java/com/jmix/ruletrans/postprocessor/RuleUnitCaseExecutionProcessor.java`
- `src/main/java/com/jmix/ruletrans/testgen/RuleTestCaseGenerator.java`
- `src/main/java/com/jmix/ruletrans/testgen/business/BusinessRuleTestCaseSet.java`
- `src/main/java/com/jmix/ruleunit/DefaultRuleUnitTestExecutorService.java`
- `src/main/java/com/jmix/ruleunit/RuleUnitTestCaseSetReport.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineTest.java`
- `src/test/java/com/jmix/ruletrans/RuleTransModuleTest.java`
- `src/test/java/com/jmix/ruletrans/rulescenario/RuleScenarioHarnessSupport.java`
- `src/test/java/com/jmix/ruleunit/RuleUnitBusinessCaseTest.java`
