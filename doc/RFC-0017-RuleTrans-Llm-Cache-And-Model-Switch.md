# RFC-0017: RuleTrans LLM 调用缓存与模型快速切换

> 状态：草案（Draft）
> 日期：2026-06-13
> 相关文档：`doc/CORE-DESIGN.md`, `doc/ACCEPTANCE.md`, `doc/RFC-0011-RuleTrans-Module.md`, `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`, `doc/RFC-0016-RuleTrans-TestCase-Output-And-Diagnostics-Switch.md`

---

## 1. 摘要

当前 RuleTrans 系统测试默认调用真实大模型。全量回归时，稳定用例会重复消耗 token 且执行速度慢；同时 `LLMInvokerImpl` 当前在构造时根据 `default.model` 固定选择 DeepSeek 或 Qwen，测试代码无法方便地按用例切换模型。

本 RFC 设计一层 `LLMInvoker` 装饰器能力：

1. 在 `LLMInvoker.generate(systemMessage, userMessage)` 外增加本地文件缓存，默认命中缓存时不再调用真实模型，缓存未命中时调用真实模型并写入缓存。
2. 在测试选项中提供强制刷新缓存的开关，用于怀疑缓存结果有问题时绕过旧缓存并重新调用指定模型。
3. 增加模型 profile / tag 机制，让测试代码可以通过 `enableModel("qwen")` 快速切换底层模型，业务 pipeline 仍只依赖 `LLMInvoker`。

---

## 2. 动机

### 2.1 问题背景

当前相关代码事实：

| 能力 | 当前代码 |
| --- | --- |
| LLM 统一接口 | `src/main/java/com/jmix/tool/impl/llm/LLMInvoker.java` |
| 默认实现 | `src/main/java/com/jmix/tool/impl/llm/LLMInvokerImpl.java` |
| 配置加载 | `src/main/java/com/jmix/tool/impl/llm/LLMInvokerBase.java` 读取 `cengine/llmmodel.properties` |
| 当前模型选择 | `default.model=deepseek`，`LLMInvokerImpl` 构造时选择 DeepSeek 或 Qwen |
| RuleTrans 测试入口 | `RuleTransPipelineTestBase.runRuleTrans(...)` 构造 pipeline 并注入 invoker |
| 诊断包装 | `DiagnosticLlmInvoker` 记录 prompt、response、耗时和文件路径 |
| 运行选项 | `RuleTransPipelineOptions(generateBusinessCases, executeBusinessCases, allowEmptyBusinessCases)` |

这说明项目已经有统一的 LLM 抽象，最小改造路径不是在 `RuleTransPipeline` 每个阶段分别加缓存，而是在 `LLMInvoker` 外围统一装饰。

### 2.2 具体场景

场景 1：稳定用例重复运行

```java
RuleTransPipelineRunResult result = runRuleTrans(
        SampleRuleTransConstraint.class,
        null,
        "四核 CPU 不能兼容转速为 5400 转的硬盘",
        RuleTransPipelineOptions.defaults());
```

期望：

- 第一次没有缓存，真实调用模型并保存结果。
- 后续相同业务输入、相同 prompt、相同模型 profile 命中缓存，不再调用真实模型。
- 系统测试仍可走完整编译、业务用例生成、RuleUnit 执行，只是不重复消耗 LLM token。

场景 2：怀疑缓存结果有问题

```java
RuleTransPipelineOptions options = RuleTransPipelineOptions.defaults()
        .forceRefreshLlmCache();
```

期望：

- 即使本地已有缓存，本次仍真实调用模型。
- 调用成功后用新响应替换或新增缓存 entry。
- 诊断输出明确标记 `cache=REFRESHED`。

场景 3：切换模型验证

```java
RuleTransPipelineOptions options = RuleTransPipelineOptions.defaults()
        .enableModel("qwen")
        .forceRefreshLlmCache();
```

期望：

- 测试基类根据 `"qwen"` 选择 qwen profile。
- 底层使用 Qwen 配置发起调用。
- 业务层仍只接收 `LLMInvoker`，不感知 DeepSeek、Qwen、GLM 的差异。

### 2.3 为什么需要改变

不加缓存会导致：

- 全量测试随着 RuleTrans 场景增加而线性增加 token 成本。
- 本地回归速度受网络和模型响应速度影响。
- 稳定结果被反复验证，降低调试效率。

不加模型 profile 会导致：

- 切换模型必须修改全局配置或环境变量，容易影响其他测试。
- 同一条业务规则很难快速比较多个模型的生成质量。
- 业务 pipeline 可能被迫知道具体 provider，破坏 `LLMInvoker` 的抽象边界。

---

## 3. 设计方案

### 3.1 核心思路

保持 `LLMInvoker` 作为唯一 LLM 调用接口不变，新增三类协作对象：

1. `LLMInvokerFactory`：根据模型 tag/profile 构造真实模型 invoker。
2. `CachedLLMInvoker`：实现 `LLMInvoker`，在调用真实模型前读写本地缓存。
3. `LlmRuntimeOptions`：挂到 `RuleTransPipelineOptions`，表达模型 tag、缓存模式和缓存目录。

调用链：

```text
RuleTransPipelineTestBase
  -> resolve LlmRuntimeOptions
  -> LLMInvokerFactory.create(modelTag)
  -> new CachedLLMInvoker(realInvoker, cacheStore, cacheOptions)
  -> optional DiagnosticLlmInvoker / diagnostics observer
  -> RuleTransPipeline.execute(...)
```

生产 `RuleTransPipeline` 不直接依赖缓存或具体模型。它继续通过构造函数接收 `LLMInvoker`。

### 3.2 分层边界

| 层次 | 职责 | 本 RFC 决策 |
| --- | --- | --- |
| `com.jmix.tool.impl.llm` | LLM 统一接口、真实模型 invoker、模型 profile、通用缓存装饰器 | 新增缓存与模型工厂能力 |
| `com.jmix.ruletrans` | 自然语言规则转换、编译、业务用例生成和 RuleUnit 执行 | 不修改 pipeline 主流程语义 |
| RuleTrans 测试辅助层 | 根据测试选项选择模型、缓存模式、诊断输出 | 扩展 `RuleTransPipelineOptions` 和 `RuleTransPipelineTestBase` |
| 本地缓存文件 | 保存 prompt 级业务输入与 LLM 输出 | 默认放在 `.ruletrans-cache/llm`，必须加入 `.gitignore` |
| 数据库缓存 | 后续可替换存储 | P0 不做，只保留接口边界 |

### 3.3 复用优先清单

- 优先复用：
  - `LLMInvoker`
  - `LLMInvokerImpl`
  - `LLMInvokerBase`
  - `RuleTransPipelineOptions`
  - `RuleTransPipelineTestBase`
  - `DiagnosticLlmInvoker`
  - `RuleTransPipelineDiagnostics`
  - `RuleTransLlmCallDiagnostic`
  - `PromptBuilder` 生成的稳定 prompt 文本
  - `jackson-databind`
- 不新增：
  - 不新增第二套 RuleTrans pipeline。
  - 不让业务规则生成器直接依赖 DeepSeek/Qwen/GLM 类名。
  - 不把 API key、base URL、provider 细节放进业务测试方法。
  - 不在测试方法里手工读写缓存文件。
  - 不把缓存文件提交到 git。
- 可由上下文推导：
  - cache key 由 system message、user message、模型 identity、缓存 schema version 推导。
  - stage 可由现有 `DiagnosticLlmInvoker.identifyStage(...)` 逻辑或后续 observer 推导。
  - provider/modelName 由模型 profile 和配置推导。
  - response 保存路径由 key 推导。
- 相似测试来源：
  - `src/test/java/com/jmix/ruletrans/RuleTransPipelineSystemTest.java`
  - `src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java`
  - `src/test/java/com/jmix/ruletrans/DiagnosticLlmInvoker.java`

### 3.4 数据模型扩展

#### 3.4.1 运行选项

扩展 `RuleTransPipelineOptions`，保留旧构造和静态方法兼容：

```java
public record RuleTransPipelineOptions(
        boolean generateBusinessCases,
        boolean executeBusinessCases,
        boolean allowEmptyBusinessCases,
        LlmRuntimeOptions llmRuntime) {

    public RuleTransPipelineOptions(
            boolean generateBusinessCases,
            boolean executeBusinessCases,
            boolean allowEmptyBusinessCases) {
        this(generateBusinessCases, executeBusinessCases, allowEmptyBusinessCases,
                LlmRuntimeOptions.defaults());
    }

    public RuleTransPipelineOptions enableModel(String modelTag) {
        return withLlmRuntime(llmRuntime.withModel(modelTag));
    }

    public RuleTransPipelineOptions forceRefreshLlmCache() {
        return withLlmRuntime(llmRuntime.withCacheMode(LlmCacheMode.REFRESH));
    }

    public RuleTransPipelineOptions disableLlmCache() {
        return withLlmRuntime(llmRuntime.withCacheMode(LlmCacheMode.DISABLED));
    }
}
```

新增：

```java
public record LlmRuntimeOptions(
        String modelTag,
        LlmCacheMode cacheMode,
        Path cacheDir) {

    public static LlmRuntimeOptions defaults() {
        return new LlmRuntimeOptions(
                "default",
                LlmCacheMode.READ_THROUGH,
                Path.of(".ruletrans-cache", "llm"));
    }
}
```

缓存模式：

```java
public enum LlmCacheMode {
    READ_THROUGH,
    REFRESH,
    DISABLED,
    CACHE_ONLY
}
```

P0 必须实现 `READ_THROUGH`、`REFRESH`、`DISABLED`。`CACHE_ONLY` 可作为 P1，但枚举可以先预留；若 P0 预留但未实现，必须明确抛出 `UnsupportedOperationException`，不能静默当作其他模式。

#### 3.4.2 模型 profile

新增模型 profile：

```java
public record LlmModelProfile(
        String tag,
        String provider,
        String apiKeyEnvName,
        String baseUrl,
        String modelName,
        double temperature,
        int maxTokens,
        String completionsPath) {

    public String identity() {
        return provider + ":" + modelName + "@" + baseUrl;
    }
}
```

`LLMInvokerFactory`：

```java
public final class LLMInvokerFactory {

    public static LLMInvoker create(String modelTag) {
        LlmModelProfile profile = LlmModelRegistry.load().resolve(modelTag);
        return new LLMInvokerImpl(profile);
    }
}
```

兼容当前配置：

```properties
# Existing keys remain supported
default.model=deepseek
deepseek.api.base.url=https://api.deepseek.com
deepseek.model.name=deepseek-v4-pro
qwen.api.base.url=https://dashscope.aliyuncs.com/compatible-mode/v1
qwen.model.name=qwen-plus
```

历史配置中如果存在 `deepseek.api.key`、`qwen.api.key` 或 `llm.*.apiKey`，新实现必须忽略这些配置项，并输出英文 warning。SK/API key 只允许从环境变量读取。

新增通用 profile 配置建议：

```properties
llm.default.model=deepseek

llm.model.deepseek.provider=openai-compatible
llm.model.deepseek.apiKeyEnv=DEEPSEEK_API_KEY
llm.model.deepseek.baseUrl=https://api.deepseek.com
llm.model.deepseek.modelName=deepseek-v4-pro
llm.model.deepseek.temperature=0.3
llm.model.deepseek.maxTokens=4000
llm.model.deepseek.completionsPath=/chat/completions

llm.model.qwen.provider=openai-compatible
llm.model.qwen.apiKeyEnv=QWEN_API_KEY
llm.model.qwen.baseUrl=https://dashscope.aliyuncs.com/compatible-mode/v1
llm.model.qwen.modelName=qwen-plus
llm.model.qwen.temperature=0.7
llm.model.qwen.maxTokens=4000

llm.model.glm.provider=openai-compatible
llm.model.glm.apiKeyEnv=GLM_API_KEY
llm.model.glm.baseUrl=${GLM_API_BASE_URL}
llm.model.glm.modelName=${GLM_MODEL_NAME}
llm.model.glm.temperature=0.7
llm.model.glm.maxTokens=4000
```

说明：

- `default` 是逻辑 tag，解析时指向 `llm.default.model` 或旧 `default.model`。
- `enableModel(...)` 只接受显式模型 tag，例如 `deepseek`、`qwen`、`glm`；P0 不实现中文别名或大小写 alias 解析。
- `provider=openai-compatible` 表示继续复用 Spring AI OpenAI-compatible 调用方式。
- SK/API key 只允许通过 `apiKeyEnv` 指向的环境变量读取；配置文件不得保存真实 SK，也不得通过 `${ENV}` 占位符间接保存密钥值。
- API key 不写入 cache key，不写入诊断，不写入缓存文件。

#### 3.4.3 缓存 key

P0 使用 prompt 级业务输入作为缓存输入。虽然底层接口只看到 `systemMessage/userMessage`，但 RuleTrans 的 prompt 已经包含自然语言、上下文投影、SDK profile、业务阶段和方法体纠错信息，因此它是当前最稳定的业务输入载体。

缓存 key 的 canonical payload：

```json
{
  "schemaVersion": 1,
  "contract": "LLMInvoker.generate",
  "modelIdentity": "openai-compatible:qwen-plus@https://dashscope.aliyuncs.com/compatible-mode/v1",
  "systemMessage": "...",
  "userMessage": "..."
}
```

key 计算：

```text
sha256(canonicalJsonUtf8)
```

默认 key 包含 `modelIdentity`。这样 DeepSeek 与 Qwen 的结果不会互相复用，避免“换模型验证”时误用旧模型响应。

P1 可以增加 `shareAcrossModels` 之类的业务级共享模式，但 P0 不默认跨模型复用。

#### 3.4.4 本地文件结构

默认目录：

```text
.ruletrans-cache/
  llm/
    index.jsonl
    entries/
      ab/
        abcdef1234....json
```

必须更新 `.gitignore`：

```gitignore
.ruletrans-cache/
```

`index.jsonl` 每行一条简短索引，便于快速定位和人工审计：

```json
{"key":"abcdef...","stage":"RULE_GENERATION","modelTag":"qwen","modelName":"qwen-plus","createdAt":"2026-06-13T10:15:30Z","entryFile":"entries/ab/abcdef....json"}
```

entry 文件：

```json
{
  "schemaVersion": 1,
  "key": "abcdef...",
  "stage": "RULE_GENERATION",
  "modelTag": "qwen",
  "modelIdentity": "openai-compatible:qwen-plus@https://dashscope.aliyuncs.com/compatible-mode/v1",
  "createdAt": "2026-06-13T10:15:30Z",
  "updatedAt": "2026-06-13T10:15:30Z",
  "hitCount": 0,
  "request": {
    "systemMessage": "...",
    "userMessage": "..."
  },
  "response": {
    "text": "model().addLessOrEqual(...);"
  },
  "diagnostics": {
    "durationMillis": 1832,
    "sourceConfigInfo": "LLM Model: qwen-plus, Provider: QWen"
  }
}
```

### 3.5 缓存调用流程

`READ_THROUGH`：

```text
generate(system, user)
  -> build cache key
  -> if entry exists and readable:
       return cached response
  -> call delegate.generate(system, user)
  -> write entry
  -> return response
```

`REFRESH`：

```text
generate(system, user)
  -> build cache key
  -> ignore existing entry
  -> call delegate.generate(system, user)
  -> overwrite entry atomically
  -> return response
```

`DISABLED`：

```text
generate(system, user)
  -> call delegate.generate(system, user)
  -> do not read or write cache
  -> return response
```

`CACHE_ONLY`：

```text
generate(system, user)
  -> build cache key
  -> if entry exists: return cached response
  -> fail with LlmCacheMissException
```

P0 如不实现 `CACHE_ONLY`，测试必须证明调用该模式会明确失败。

### 3.6 强制失效语义

“强制让 LM Cache 失效”在 P0 定义为单次运行的 `REFRESH`，不是删除整个缓存目录。

原因：

1. 可以只刷新当前测试实际触发的 prompt，不影响其他稳定用例。
2. 刷新失败时旧缓存仍可保留，方便人工比较。
3. 诊断中能看到本次是 `REFRESHED`，而不是普通 miss。

可选扩展：

```java
RuleTransPipelineOptions.defaults()
        .forceRefreshLlmCache();
```

如果后续需要物理删除缓存，可新增独立工具或命令，例如：

```powershell
mvn test -Dtest=RuleTransPipelineSystemTest -Druletrans.llm.cache.clear=true
```

P0 不要求实现全局删除命令。

### 3.7 测试代码使用方式

默认读穿缓存：

```java
RuleTransPipelineRunResult result = runRuleTrans(
        RuleTransTestFixtures.SampleRuleTransConstraint.class,
        null,
        "四核 CPU 不能兼容转速为 5400 转的硬盘",
        RuleTransPipelineOptions.defaults());
```

切换 Qwen 并强制刷新：

```java
RuleTransPipelineRunResult result = runRuleTrans(
        RuleTransTestFixtures.SampleRuleTransConstraint.class,
        null,
        "四核 CPU 不能兼容转速为 5400 转的硬盘",
        RuleTransPipelineOptions.defaults()
                .enableModel("qwen")
                .forceRefreshLlmCache());
```

临时禁用缓存：

```java
RuleTransPipelineOptions options = RuleTransPipelineOptions.defaults()
        .disableLlmCache();
```

compileOnly 仍可组合：

```java
RuleTransPipelineOptions options = RuleTransPipelineOptions.compileOnly()
        .enableModel("deepseek")
        .forceRefreshLlmCache();
```

### 3.8 测试基类改造

`RuleTransPipelineTestBase` 增加带 options 的 invoker hook：

```java
protected LLMInvoker llmInvoker(RuleTransPipelineOptions options) {
    LlmRuntimeOptions runtime = options.llmRuntime();
    LLMInvoker realInvoker = LLMInvokerFactory.create(runtime.modelTag());
    if (runtime.cacheMode() == LlmCacheMode.DISABLED) {
        return realInvoker;
    }
    return new CachedLLMInvoker(
            realInvoker,
            new FileLLMCacheStore(runtime.cacheDir()),
            runtime);
}
```

`RuleTransPipelineTestBase` 不再鼓励子类覆写 `llmInvoker()` 注入 fake LLM。P0 实现时，应把当前依赖 `QueueInvoker`、`runWithFakeLlm(...)` 的系统级测试迁移为真实 LLM + 本地缓存：

- 稳定回归默认走 `READ_THROUGH`，命中缓存时不调用真实模型。
- 需要重新验证模型能力时使用 `forceRefreshLlmCache()`。
- 组件级缓存单元测试可以使用 `CountingInvoker` 这类极小 stub 验证缓存命中/未命中行为，但 RuleTrans pipeline 系统测试不再以 fake LLM 响应队列作为业务验收方式。

### 3.9 诊断输出扩展

扩展 `RuleTransLlmCallDiagnostic` 或新增并行 cache diagnostic：

```java
public enum LlmCacheStatus {
    DISABLED,
    HIT,
    MISS,
    REFRESHED,
    WRITE_FAILED
}
```

建议字段：

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
        String errorMessage,
        String modelTag,
        String modelIdentity,
        LlmCacheStatus cacheStatus,
        String cacheKey,
        Path cacheEntryFile) {
}
```

打印示例：

```text
[LLM Calls]
#1 stage=RULE_GENERATION model=qwen/qwen-plus cache=HIT duration=4ms success=true
cacheKey=abcdef... entry=.ruletrans-cache/llm/entries/ab/abcdef....json
```

日志语言必须保持英文，例如：

```text
LLM cache hit: key=abcdef stage=RULE_GENERATION model=qwen
LLM cache refresh: key=abcdef stage=RULE_GENERATION model=qwen
LLM cache miss: key=abcdef stage=RULE_GENERATION model=deepseek
```

### 3.10 接口矩阵

| 入口 | 变更 | 验收 |
| --- | --- | --- |
| `LLMInvoker.generate(...)` | 接口不变 | 旧真实实现继续可编译；RuleTrans 系统测试迁移掉 fake invoker 路径 |
| `LLMInvokerImpl()` | 保留默认构造 | 继续按当前配置选择默认模型 |
| `LLMInvokerImpl(LlmModelProfile)` | 新增 | 可显式选择 qwen/deepseek/glm profile |
| `LLMInvokerFactory.create(tag)` | 新增 | 支持显式 tag 和默认模型 |
| `CachedLLMInvoker.generate(...)` | 新增装饰器 | 覆盖 hit/miss/refresh/disabled |
| `RuleTransPipelineOptions.defaults()` | 行为扩展 | 默认 `READ_THROUGH`，旧调用方无需改代码 |
| `RuleTransPipelineOptions.compileOnly()` | 行为扩展 | 可组合模型选择和缓存模式 |
| `RuleTransPipelineTestBase.runRuleTrans(...)` | 使用 options 创建真实模型 + 缓存 invoker | 系统测试不再依赖 fake LLM |
| `RuleTransPipelineResultPrinter.print(...)` | 打印 cache/model 信息 | 诊断开启时可定位命中来源 |

### 3.11 正反语义矩阵

| 场景 | 命中条件 | 预期行为 |
| --- | --- | --- |
| `READ_THROUGH` + entry 存在 | key 完全一致，entry 可读，response 非空 | 返回缓存，不调用 delegate |
| `READ_THROUGH` + entry 不存在 | key 不存在 | 调用 delegate，成功后写缓存 |
| `REFRESH` + entry 存在 | key 存在 | 忽略旧 entry，调用 delegate，成功后更新 entry |
| `REFRESH` + delegate 失败 | 模型调用异常 | 抛出异常；旧 entry 不被破坏 |
| `DISABLED` | 任意 | 不读写缓存，直接调用 delegate |
| 切换模型 | `modelIdentity` 不同 | 默认生成不同 key，不复用旧模型输出 |
| prompt 模板变化 | `userMessage` 或 `systemMessage` 不同 | key 变化，自动 miss |
| 缓存文件损坏 | entry JSON 无法解析 | 记录 warning，把文件移动到 quarantine 或忽略后重新调用 |
| response 为空 | delegate 返回 blank | 不写缓存，沿用现有 `LLMInvokerImpl` 空响应失败语义 |

### 3.12 可推导字段审查

| 字段 | 是否需要用户填写 | 来源 |
| --- | --- | --- |
| `modelTag` | 可选 | 默认从 `default.model` 或 `llm.default.model` 推导 |
| `provider` | 否 | profile 配置 |
| `modelName` | 否 | profile 配置 |
| `modelIdentity` | 否 | `provider + modelName + baseUrl` |
| `cacheKey` | 否 | canonical payload hash |
| `stage` | 否 | prompt 内容识别或 observer |
| `cacheDir` | 可选 | 默认 `.ruletrans-cache/llm`，可用 system property 覆盖 |
| SK/API key | 否 | 只能来自 `apiKeyEnv` 指向的环境变量；不进入业务测试、配置文件和缓存 |

---

## 4. 验收准则

### AC-001：缓存命中不调用真实模型

测试示例：

```java
@Test
public void testReadThroughCacheHitDoesNotCallDelegate() throws Exception {
    CountingInvoker delegate = new CountingInvoker("cached-response");
    FileLLMCacheStore store = tempStoreWithEntry("system", "user", "qwen", "cached-response");
    CachedLLMInvoker invoker = new CachedLLMInvoker(
            delegate,
            store,
            runtime("qwen", LlmCacheMode.READ_THROUGH));

    String response = invoker.generate("system", "user");

    assertEquals("cached-response", response);
    assertEquals(0, delegate.callCount());
}
```

预期：

- 返回缓存中的 response。
- delegate 未被调用。
- diagnostics 标记 `cache=HIT`。

### AC-002：缓存未命中时调用模型并写入本地文件

测试示例：

```java
@Test
public void testReadThroughCacheMissWritesEntry() throws Exception {
    CountingInvoker delegate = new CountingInvoker("fresh-response");
    FileLLMCacheStore store = new FileLLMCacheStore(tempDir);
    CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.READ_THROUGH);

    String response = invoker.generate("system", "user");

    assertEquals("fresh-response", response);
    assertEquals(1, delegate.callCount());
    assertTrue(store.find(keyFor("system", "user")).isPresent());
}
```

预期：

- delegate 调用 1 次。
- `.ruletrans-cache/llm/index.jsonl` 和 entry 文件写入成功。
- diagnostics 标记 `cache=MISS`。

### AC-003：强制刷新缓存

测试示例：

```java
@Test
public void testForceRefreshIgnoresExistingEntry() throws Exception {
    CountingInvoker delegate = new CountingInvoker("new-response");
    FileLLMCacheStore store = tempStoreWithEntry("system", "user", "qwen", "old-response");
    CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.REFRESH);

    String response = invoker.generate("system", "user");

    assertEquals("new-response", response);
    assertEquals(1, delegate.callCount());
    assertEquals("new-response", store.get(keyFor("system", "user")).response().text());
}
```

预期：

- 不返回旧缓存。
- 新 response 替换旧 entry。
- diagnostics 标记 `cache=REFRESHED`。

### AC-004：禁用缓存

测试示例：

```java
@Test
public void testDisableCacheDoesNotReadOrWrite() throws Exception {
    CountingInvoker delegate = new CountingInvoker("real-response");
    FileLLMCacheStore store = tempStoreWithEntry("system", "user", "qwen", "cached-response");
    CachedLLMInvoker invoker = cached(delegate, store, LlmCacheMode.DISABLED);

    String response = invoker.generate("system", "user");

    assertEquals("real-response", response);
    assertEquals(1, delegate.callCount());
    assertEquals("cached-response", store.get(keyFor("system", "user")).response().text());
}
```

预期：

- 直接调用 delegate。
- 不覆盖已有缓存。
- diagnostics 标记 `cache=DISABLED`。

### AC-005：模型切换使用不同缓存 key

测试示例：

```java
@Test
public void testDifferentModelIdentityUsesDifferentCacheKey() {
    String deepseekKey = keyBuilder.key("system", "user", deepseekProfile);
    String qwenKey = keyBuilder.key("system", "user", qwenProfile);

    assertNotEquals(deepseekKey, qwenKey);
}
```

预期：

- 同一 prompt 在 DeepSeek 和 Qwen 下默认不共用缓存。
- 切换模型验证时不会误用另一个模型的 response。

### AC-006：模型 tag 能解析，alias 不在 P0 范围

测试示例：

```java
@Test
public void testResolveQwenTag() {
    LlmModelProfile profile = registry.resolve("qwen");

    assertEquals("qwen", profile.tag());
    assertEquals("qwen-plus", profile.modelName());
}
```

预期：

- `"qwen"` 解析到 qwen profile，`"deepseek"` 解析到 deepseek profile，`"glm"` 在配置存在时解析到 glm profile。
- `"default"` 解析到当前默认模型。
- `"千问"`、`"通义千问"`、大小写 alias 等不作为 P0 验收范围；如果传入未知 tag，应明确失败并列出可用 tag。

### AC-007：RuleTrans 系统测试可通过 options 切换模型

测试示例：

```java
@Test
public void testModuleLevelRuleWithQwenRefreshCache() {
    RuleTransPipelineRunResult result = runRuleTrans(
            RuleTransTestFixtures.SampleRuleTransConstraint.class,
            null,
            "四核 CPU 不能兼容转速为 5400 转的硬盘",
            RuleTransPipelineOptions.defaults()
                    .enableModel("qwen")
                    .forceRefreshLlmCache());

    assertTrue(result.success(), result.pipelineResult().messages().toString());
    assertTrue(result.methodBody().contains("cpu"), result.methodBody());
    assertTrue(result.methodBody().contains("drive"), result.methodBody());
}
```

预期：

- 本次真实调用 qwen profile。
- 诊断输出包含 `model=qwen` 和 `cache=REFRESHED`。
- 业务 pipeline 不出现 Qwen 专用分支。

### AC-008：RuleTrans 系统测试移除 fake LLM 响应队列

迁移前的测试形态：

```java
RuleTransPipelineRunResult result = runWithFakeLlm(
        "CPU最多只能配一个",
        "cpu",
        RuleTransPipelineOptions.compileOnly(),
        RuleTransTestFixtures.cpuAtMostOneMethodBody());
```

预期：

- `RuleTransPipelineSystemTest` 不再保留 `runWithFakeLlm(...)`、`fakeLlmBase(...)`、`QueueInvoker` 等 fake LLM helper。
- 系统级 RuleTrans 测试统一通过 `RuleTransPipelineTestBase` 创建真实模型 invoker，并由 `CachedLLMInvoker` 控制是否实际调用模型。
- 编译重试、业务用例生成重试这类需要稳定响应序列的边界测试，应迁移为缓存 fixture 或更低层组件测试，不再用 fake LLM 作为系统测试主路径。

### AC-009：缓存目录不进入 git

验收：

```powershell
rg "\.ruletrans-cache" .gitignore
```

预期：

- `.gitignore` 包含 `.ruletrans-cache/`。
- `git status --short` 不显示本地缓存 entry。

### AC-010：诊断打印显示 cache/model 状态

诊断开启时，打印输出应包含：

```text
model=qwen
cache=HIT
cacheKey=
entry=
```

或在刷新时包含：

```text
cache=REFRESHED
```

### AC-011：SK 只从环境变量读取

测试示例：

```java
@Test
public void testApiKeyIsReadFromEnvironmentOnly() {
    LlmModelProfile profile = registry.resolve("qwen");

    assertEquals("QWEN_API_KEY", profile.apiKeyEnvName());
    assertFalse(profileProperties.containsKey("llm.model.qwen.apiKey"));
}
```

预期：

- `llm.model.<tag>.apiKeyEnv` 只保存环境变量名，例如 `QWEN_API_KEY`。
- 配置文件不得保存 `apiKey`、`sk`、`secretKey` 等真实密钥字段。
- `LLMInvokerFactory` 创建 invoker 时从 `System.getenv(profile.apiKeyEnvName())` 读取 SK。
- 如果环境变量不存在或为空，调用前明确失败，错误信息只包含环境变量名，不包含密钥值。
- cache entry、diagnostics、`getConfigInfo()` 均不包含 SK。

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
| --- | --- | --- | --- |
| P0-1 | 新增 `LlmModelProfile`、`LlmModelRegistry`、`LLMInvokerFactory` | P0 | 待开始 |
| P0-2 | 为 `LLMInvokerImpl` 增加 profile 构造器，保留默认构造兼容 | P0 | 待开始 |
| P0-3 | 新增 `LlmRuntimeOptions`、`LlmCacheMode` 并扩展 `RuleTransPipelineOptions` | P0 | 待开始 |
| P0-4 | 新增 `LlmCacheKeyBuilder`、`LlmCacheEntry`、`LLMCacheStore`、`FileLLMCacheStore` | P0 | 待开始 |
| P0-5 | 新增 `CachedLLMInvoker`，实现 `READ_THROUGH`、`REFRESH`、`DISABLED` | P0 | 待开始 |
| P0-6 | 改造 `RuleTransPipelineTestBase`，根据 options 创建模型和缓存 invoker | P0 | 待开始 |
| P0-7 | 扩展 diagnostics / printer，显示 modelTag、cacheStatus、cacheKey、entry path | P0 | 待开始 |
| P0-8 | 更新 `.gitignore`，忽略 `.ruletrans-cache/` | P0 | 待开始 |
| P0-9 | 增加 SK 只从环境变量读取的配置安全测试 | P0 | 待开始 |
| P0-10 | 增加缓存单元测试、模型 tag 测试、RuleTrans options 集成测试 | P0 | 待开始 |
| P0-11 | 迁移或删除 RuleTrans 系统测试中的 fake LLM helper 和 QueueInvoker 路径 | P0 | 待开始 |
| P0-12 | 回归运行 RuleTrans pipeline 相关测试 | P0 | 待开始 |
| P1-1 | 实现 `CACHE_ONLY`，支持离线回归验证缓存完整性 | P1 | 待开始 |
| P1-2 | 增加数据库缓存实现 | P1 | 待开始 |
| P1-3 | 增加缓存 entry 审计、过期和批量清理工具 | P1 | 待开始 |
| P1-4 | 评估跨模型共享业务级缓存模式 | P1 | 待开始 |

---

## 6. 风险与边界

### 6.1 缓存掩盖模型能力退化

风险：默认命中缓存后，不会发现当前模型能力下降或 prompt 改动之外的模型行为变化。

策略：

- prompt 文本变化会自然导致 key 变化。
- 需要验证当前模型时使用 `forceRefreshLlmCache()`。
- 切换模型时 key 默认包含 `modelIdentity`，不会复用旧模型结果。
- 后续可给真实 LLM 验证测试加 JUnit tag 或 Maven profile。

### 6.2 缓存文件包含业务上下文

风险：prompt/response 可能包含业务规则、部件信息或生成代码。

策略：

- 默认缓存目录加入 `.gitignore`。
- 不写 SK/API key；缓存、诊断和日志不得输出密钥值。
- 文档明确缓存是本地开发数据，不作为源码资产。
- 如需共享缓存，后续必须设计脱敏和审核流程。

### 6.3 配置文件误存 SK

风险：开发者可能把千问、DeepSeek 或 GLM 的 SK 写入 `llmmodel.properties`，导致密钥进入源码或日志。

策略：

- profile 配置只保存 `apiKeyEnv`，例如 qwen 使用 `QWEN_API_KEY`。
- `LLMInvokerFactory` 只从环境变量读取 SK。
- 若配置文件存在 `apiKey`、`sk`、`secretKey` 等字段，P0 实现应忽略并输出英文 warning。
- 错误信息只报告缺失的环境变量名，不打印环境变量值。

### 6.4 模型配置碎片化

风险：旧 `deepseek.*` / `qwen.*` 配置与新 `llm.model.*` 配置并存，容易混淆。

策略：

- P0 保持旧配置兼容。
- 新 registry 加载时优先读取 `llm.model.<tag>.*`，缺失时回退旧 key。
- `getConfigInfo()` 输出 resolved profile，便于诊断。

### 6.5 缓存写入并发

风险：并行测试可能同时写同一个 entry 或 index。

策略：

- entry 写入使用临时文件 + atomic move。
- index 追加失败不影响 entry 主文件读取；必要时可重建 index。
- P0 不承诺跨进程强一致，只保证单 JVM 常规并行测试不破坏 entry。

### 6.6 历史 fake LLM 测试迁移成本

风险：当前 `RuleTransPipelineSystemTest` 中仍存在 `QueueInvoker` 和 `runWithFakeLlm(...)` 这类受控响应测试。缓存能力落地后，系统测试应改为真实 LLM + 缓存，这会带来一次性迁移成本。

策略：

- 系统级 RuleTrans 测试迁移到 `CachedLLMInvoker`。
- 稳定响应依赖由本地缓存文件承担，而不是由 fake 队列承担。
- 需要纯单元地验证缓存状态机时，允许使用最小 `CountingInvoker`，但它只服务缓存组件测试，不作为 RuleTrans pipeline 的业务验收方式。
- AC-008 必须覆盖系统测试 fake helper 的删除或迁移。

---

## 7. 参考资料

- `doc/CORE-DESIGN.md`
- `doc/ACCEPTANCE.md`
- `doc/RFC-0011-RuleTrans-Module.md`
- `doc/RFC-0015-RuleTrans-System-Test-Base-And-Trace.md`
- `doc/RFC-0016-RuleTrans-TestCase-Output-And-Diagnostics-Switch.md`
- `src/main/java/com/jmix/tool/impl/llm/LLMInvoker.java`
- `src/main/java/com/jmix/tool/impl/llm/LLMInvokerImpl.java`
- `src/main/java/com/jmix/tool/impl/llm/LLMInvokerBase.java`
- `src/main/java/com/jmix/ruletrans/RuleTransPipelineOptions.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineTestBase.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineSystemTest.java`
- `src/test/java/com/jmix/ruletrans/DiagnosticLlmInvoker.java`
- `src/test/java/com/jmix/ruletrans/RuleTransPipelineDiagnostics.java`
- `src/main/resources/cengine/llmmodel.properties`

---

## 8. 用户确认结论

2026-06-13 本轮用户已确认以下设计结论，后续实现不再按开放问题处理：

1. 默认缓存目录接受 `.ruletrans-cache/llm`，并需要加入 `.gitignore`。该目录不随 `mvn clean` 删除，以保留长期本地缓存收益。
2. P0 不实现 `CACHE_ONLY`。P0 只实现 `READ_THROUGH`、`REFRESH`、`DISABLED`；`CACHE_ONLY` 作为 P1 离线回归能力。
3. 切换模型时默认不复用其他模型缓存。cache key 必须包含 `modelIdentity`，例如 qwen 不使用 deepseek 的旧响应。
4. P0 不实现模型 alias。`AC-006` 只验证显式 tag，例如 `qwen`、`deepseek`、`glm`；`千问`、`通义千问` 这类中文别名不在范围内。
5. 缓存落地后，RuleTrans 系统测试不再使用 fake LLM 响应队列。当前 `runWithFakeLlm(...)`、`QueueInvoker` 等路径应迁移或删除，系统测试统一走真实模型 + 本地缓存。
6. 模型 SK/API key 只能通过环境变量读取，不写入配置文件。当前环境已配置千问 SK，因此 qwen profile 默认读取 `QWEN_API_KEY`。
