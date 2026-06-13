# RFC-0016: RuleTrans 系统测试用例输出与诊断开关

> 状态：已批准（Approved）
> 日期：2026-06-13
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0013-Business-Readable-Rule-Test-Cases.md`, `doc/RFC-0014-RuleTrans-End-to-End-RuleUnit-Pipeline.md`, `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`

---

## 1. 摘要

RFC-0015 已经为 `RuleTransPipeline` 增加系统测试基类和诊断打印，但当前实现仍有两个缺口：

1. 打印内容只突出翻译后的 Java 方法体，对生成的 `BusinessRuleTestCaseSet` 只输出数量和 id，不足以定位业务测试输入输出是否正确。
2. 测试基类每次运行都会包装 LLM 并写入 prompt/response 文件，即使调用方不需要诊断输出，也会产生额外 I/O 和内存开销。

本 RFC 在不改变生产 `RuleTransPipeline` API 的前提下，扩展测试辅助层：

- `print(result)` 在诊断开启时同时输出最终 Java 方法体和业务测试用例的 `given/expect`。
- `assertRuleTrans(...)` 增加业务测试用例断言能力，用测试期 expectation 比较生成用例的预期输入和预期输出。
- `RuleTransPipelineTestBase` 增加诊断开关，默认关闭；只有显式开启时才采集 LLM 诊断文件并打印详细信息。

---

## 2. 动机

### 2.1 输出内容不足

RuleTrans 的端到端质量不仅取决于 LLM 生成的 Java 规则方法体，还取决于随后生成的业务测试用例是否能覆盖规则语义。当前 `RuleTransPipelineResultPrinter` 只打印：

```text
businessCases: count=1 ids=[COMP-PART-001]
```

这无法直接看到：

- 用例的业务输入是什么，例如 `given.parts`、`given.parameters`。
- 用例的预期输出是什么，例如 `expect.compatible`、`expect.parts`、`expect.solutions`。
- RuleUnit 实际输出和期望不一致时，失败来源是 Java 规则、测试用例，还是执行适配。

### 2.2 诊断开销需要可控

RFC-0015 的诊断包装器会记录 LLM prompt/response、耗时和文件路径。该能力适合系统级排查，但不应成为普通测试默认成本。

默认关闭诊断可以避免：

- 每次 LLM 调用都写 `target/ruletrans-pipeline-system/.../llm-*.txt`。
- `print(result)` 在普通回归中刷出大量日志。
- 因诊断输出过长影响测试性能和可读性。

---

## 3. 设计方案

### 3.1 分层边界

本 RFC 只修改 `src/test/java/com/jmix/ruletrans` 下的测试辅助层：

| 层次 | 本 RFC 决策 |
| --- | --- |
| 生产 pipeline | 不改 `RuleTransPipeline`、`RuleTransPipelineResult` 字段 |
| 业务用例模型 | 复用 `BusinessRuleTestCaseSet`、`BusinessRuleTestCase`、`BusinessCaseGiven`、`BusinessCaseExpect` |
| RuleUnit 执行 | 复用 `RuleUnitTestCaseSetReport` 和现有比较逻辑 |
| 系统测试 helper | 新增业务用例 expectation，扩展 printer，增加诊断开关 |

### 3.2 复用优先清单

- 优先复用：
  - `RuleTransPipelineTestBase`
  - `RuleTransPipelineRunResult`
  - `RuleTransPipelineDiagnostics`
  - `RuleTransPipelineResultPrinter`
  - `DiagnosticLlmInvoker`
  - `BusinessRuleTestCaseSet`
  - `BusinessRuleTestCase`
  - `BusinessCaseGiven`
  - `BusinessCaseExpect`
  - `RuleUnitTestCaseSetReport`
- 不新增：
  - 不新增生产级 diagnostics DTO。
  - 不新增第二套业务测试用例模型。
  - 不把测试用例断言下沉到生产 pipeline。
- 可由上下文推导：
  - 业务测试用例列表来自 `RuleTransPipelineResult.businessCaseSet()`。
  - RuleUnit 实际结果来自 `RuleTransPipelineResult.ruleUnitReport()`。
  - 诊断是否采集由测试基类开关决定。
- 相似测试来源：
  - `src/test/java/com/jmix/ruletrans/RuleTransPipelineSystemTest.java`
  - `src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java`
  - `src/test/java/com/jmix/ruleunit/RuleUnitBusinessCaseTest.java`

### 3.3 业务测试用例输出

`RuleTransPipelineResultPrinter` 在诊断开启时输出每个业务用例：

```text
[Business Test Cases]
#1 id=COMP-PART-001 family=COMPATIBILITY serviceMethod=testCompatibility
title=<title>
scenario=<scenario>
given:
  {
    "parts": [
      {"code":"cpu4","isSelected":true},
      {"code":"drive5400","isSelected":true}
    ]
  }
expect:
  {
    "compatible": false
  }
actual:
  {
    "compatible": false
  }
passed=true
```

要求：

- `given` 表示该业务测试用例的预期输入。
- `expect` 表示该业务测试用例的预期输出。
- 如果已执行 RuleUnit，则打印同 id 的 `actual` 和 `passed/failures`。
- 如果 compileOnly 或未执行业务用例，则 `actual` 标记为 skipped。

### 3.4 业务测试用例断言

新增测试辅助 expectation：


`assertRuleTrans(...)` 增加重载：

```java
protected RuleTransPipelineRunResult assertRuleTrans(
        Class<?> annotatedModelClass,
        String categoryCode,
        String naturalLanguage,
        RuleTransJavaExpectation javaExpectation,
        RuleTransBusinessCaseExpectation caseExpectation)
```

断言语义：

- Java expectation 比较最终 `methodBody`。
- Business case expectation 根据 case id 查找生成用例。
- 对 `given` 和 `expect` 做 JSON 结构比较，不依赖字段顺序和空白。
- 断言失败时输出实际 generated case set 的 JSON，便于定位。

### 3.5 诊断开关

`RuleTransPipelineTestBase` 增加：

```java
protected boolean diagnosticsEnabled() {
    return false;
}
```

默认行为：

- 不使用 `DiagnosticLlmInvoker` 包装真实 LLM。
- 不写 prompt/response 诊断文件。
- `print(result)` 直接 no-op。
- `RuleTransPipelineDiagnostics.llmCalls()` 为空。

需要诊断的系统测试显式开启：

```java
@Override
protected boolean diagnosticsEnabled() {
    return true;
}
```

### 3.6 接口矩阵

| 入口 | 行为 | 验收 |
| --- | --- | --- |
| `assertRuleTrans(..., javaExpectation)` | 保持兼容，只断言 Java | 旧测试不改也可编译 |
| `assertRuleTrans(..., javaExpectation, caseExpectation)` | 同时断言 Java 和业务测试用例 | 新增系统测试覆盖 |
| `runRuleTrans(...)` | 按开关决定是否采集诊断 | 开关关闭时无 LLM 诊断文件 |
| `print(result)` | 开关关闭 no-op，开启时打印完整报告 | 两种路径均测试 |
| `RuleTransPipelineResultPrinter.print(...)` | 直接调用仍可打印完整报告 | 诊断开启测试覆盖业务用例明细 |

---

## 4. 验收准则

### AC-001：打印生成的业务测试用例输入输出

给定 pipeline 生成 `BusinessRuleTestCaseSet`，诊断开启并调用 `print(result)` 时：

- 输出包含 `[Business Test Cases]`。
- 每个 case 输出 id、businessFamily、serviceMethod。
- 每个 case 输出 `given` JSON，作为预期输入。
- 每个 case 输出 `expect` JSON，作为预期输出。
- 若 RuleUnit 已执行，则输出同 id 的 `actual` 和 `passed/failures`。

### AC-002：assert 方法能比较业务测试用例的预期输入和预期输出

系统测试可写成：

```java
RuleTransPipelineRunResult result = assertRuleTrans(
        CpuDriveFacts.class,
        "四核 CPU 不能兼容转速为 5400 转的硬盘",
        expectJavaContains("inCompatible"),
        expectBusinessCase("COMP-PART-001", givenJson, expectJson));
```

预期：

- Java 规则方法体断言通过。
- 生成业务测试用例中 id 为 `COMP-PART-001` 的 case 存在。
- `given` 与 `givenJson` 结构一致。
- `expect` 与 `expectJson` 结构一致。

### AC-003：诊断默认关闭

直接继承 `RuleTransPipelineTestBase` 且不覆写开关时：

- `diagnosticsEnabled()` 为 false。
- `runRuleTrans(...)` 不写 LLM prompt/response 文件。
- `result.diagnostics().llmCalls()` 为空。
- `print(result)` 不输出详细诊断信息。

### AC-004：诊断开启后保持 RFC-0015 行为

显式覆写 `diagnosticsEnabled()` 返回 true 时：

- LLM 调用被记录。
- prompt/response 文件写入 `target/ruletrans-pipeline-system/...`。
- `print(result)` 输出输入、LLM 调用、attempt、业务测试用例、摘要和最终 Java 方法体。

### AC-005：旧测试兼容

以下旧用法继续可编译：

```java
assertRuleTrans(CpuFacts.class, "cpu", naturalLanguage, expectJavaContains("addLessOrEqual"));
runRuleTrans(CpuFacts.class, "cpu", naturalLanguage, RuleTransPipelineOptions.compileOnly());
```

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 |
| --- | --- | --- |
| P0-1 | 新增 `RuleTransBusinessCaseExpectation` | P0 |
| P0-2 | 为 `RuleTransPipelineTestBase` 增加业务用例 expectation 重载 | P0 |
| P0-3 | 扩展 `RuleTransPipelineResultPrinter` 打印 `given/expect/actual` | P0 |
| P0-4 | 为 `RuleTransPipelineDiagnostics` 增加 enabled 状态 | P0 |
| P0-5 | `RuleTransPipelineTestBase` 增加 `diagnosticsEnabled()` 开关，默认关闭 | P0 |
| P0-6 | 更新 `RuleTransPipelineSystemTest` 覆盖开启、关闭、业务用例断言 | P0 |
| P0-7 | 运行 RuleTrans 系统测试和相关回归测试 | P0 |

---

## 6. 风险与边界

### 6.1 业务测试用例断言过脆

风险：真实 LLM 生成的 case id 或 title 可能变化。

策略：

- P0 的精确 `given/expect` 断言主要用于 fake LLM 或稳定录制响应。
- 真实 LLM 冒烟测试继续优先断言 Java 关键片段和 pipeline 成功。

### 6.2 诊断关闭后排查信息不足

风险：默认关闭时失败日志较少。

策略：

- 需要定位时覆写 `diagnosticsEnabled()` 或在专门系统测试类中开启。
- 生产结果中的 `messages`、`failureKind`、`attemptStates` 仍然保留。

### 6.3 打印 JSON 过长

风险：业务测试用例较多时打印输出变长。

策略：

- 仅在诊断开启时打印完整 `given/expect/actual`。
- 默认关闭诊断，普通回归不打印。

---

## 7. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineResultPrinter.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineSystemTest.java`
- `src/main/java/com/jmix/ruletrans/testgen/business/BusinessRuleTestCaseSet.java`
- `src/main/java/com/jmix/ruletrans/testgen/business/BusinessRuleTestCase.java`
- `src/main/java/com/jmix/ruleunit/RuleUnitTestReport.java`
