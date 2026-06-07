# RFC-0011: RuleTrans 模块 - 自然语言规则转换引擎

> 状态：草案（Draft）
> 日期：2026-06-07

---

## 1. 摘要

在 `com.jmix.tool` 包工程化演进过程中，将规则转换能力从 `ModelHelper` 中分离，孵化为独立的 `ruletrans` 模块。该模块以自然语言为输入，经 Prompt 构建、LLM 调用、代码片段生成、编译验证与迭代纠错，输出可用于约束引擎的 Java `rule` 方法代码片段，全程不侵入原有代码。

---

## 2. 动机

### 2.1 问题背景

当前 `ModelHelper` 承担了两类职责：

1. **模型生成**：`generatorModelFile` / `generatorRunModelFile` —— 输入变量模型 + 逻辑伪代码，生成含约束代码的完整测试类文件。
2. **代码注入**：`autoInjectConstraintCode` / `StructCodeInjector` —— 将结构化规则展开为约束实现代码。

两类职责混在同一个类中，导致：
- Prompt 模板与业务建模 Prompt 耦合，无法独立演进。
- 产品级规则（跨部件/跨分类）的 Prompt 构建逻辑散落在 `ModuleGenerator` 中。
- 生成-编译-测试-纠错的迭代循环未封装，外部调用方需要自行拼接。
- 自然语言驱动的规则代码生成能力缺失——当前需要用户手工写伪代码，未来期望直接输入自然语言。

### 2.2 具体场景

**场景 1：PartCategory 规则**
用户输入自然语言：`"CPU最多配置一块"`
上下文：CPU PartCategory 的规格信息（Para、Part、属性等），直接生成代码。

**场景 2：产品级规则**
用户输入自然语言：`"四核CPU不能兼容转速为5400转的硬盘"`
上下文：涉及 CPU 和 HDD 两个 PartCategory。处理分两阶段：
- Stage1：根据自然语言识别关联分类（CPU + HDD）
- Stage2：携带两个分类的规格信息生成约束代码

**场景 3：独立测试用例生成**
用户希望验证生成的规则代码是否正确。系统根据自然语言和上下文，生成独立测试用例（JSON 格式），包含输入、预期输出描述。

**场景 4：编译错误驱动的 Prompt 动态纠错**
生成的 Java 代码编译失败时，系统解析编译错误信息，动态构建纠错 Prompt（含错误详情），反馈给 LLM 重新生成，直到编译通过。

**场景 5：测试用例驱动的逻辑纠错**
编译通过后，加载 JSON 测试用例，执行测试。测试失败时分析错误数据，判断是否源于规则逻辑错误（LLM 误解自然语言），若是则触发纠错重试。

### 2.3 为什么需要改变

- **职责分离**：`ModelHelper` 保持不变，`ruletrans` 独立演进。
- **Prompt 独立**：新建 `ruletrans` 专用的 Prompt 模板资源，不修改现有 `cengine/` 目录下的模板。
- **工程化**：将"生成→编译→纠错→测试→纠错"的循环封装为可复用管线，提升 AI 生成代码的正确率。
- **上下文感知**：支持 PartCategory 级和产品级两种上下文，构建差异化 Prompt。
- **测试用例结构化**：独立生成 JSON 测试用例，支持未来流程自动化。

---

## 3. 设计方案

### 3.1 核心流程

```
自然语言输入
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│  RuleTransEngine（主入口）                                     │
│                                                              │
│  ① 接收自然语言 + RuleContext                                  │
│                                                              │
│  PartCategoryContext ──────────────────────────────┐        │
│           │                                             │        │
│  ProductContext ──▶ CategoryIdentifier ───────┐   │        │
│           │               │                   │   │        │
│           │               ▼                   │   │        │
│           │         识别的分类列表               │   │        │
│           │               │                   │   │        │
│           └───────────────┼───────────────────┘   │        │
│                          ▼                       │        │
│                  RuleSnippetGenerator ───────────┤        │
│                          │                      │        │
│                          ▼                      │        │
│                  RuleSnippetAssembler            │        │
│                          │                      │        │
│                          ▼                      │        │
│                  CompilationProcessor            │        │
│                          │                      │        │
│              ┌───────────┴───────────┐         │        │
│              ▼                       ▼         │        │
│         编译失败                   编译通过      │        │
│              │                       │         │        │
│              ▼                       ▼         │        │
│   构建纠错 Prompt          RuleTestCaseGenerator ──┤     │
│   (含编译错误详情)                │                     │
│              │                  ▼                     │
│   ┌──────────┴──────────────── TestExecutionProcessor │
│   │                              │                    │
│   │          ┌──────────────────┴────────┐          │
│   │          ▼                            ▼          │
│   │     测试通过                       测试失败       │
│   │          │                            │          │
│   │          ▼                            ▼          │
│   │      返回代码片段              分析错误原因        │
│   │                            │                   │
│   │            ┌────────────────┘                   │
│   │            ▼                                     │
│   │      是规则逻辑错误？                            │
│   │            │                                   │
│   │    ┌───────┴───────┐                          │
│   │    ▼               ▼                          │
│   │   是               否                          │
│   │    │               │                          │
│   │    ▼               ▼                          │
│   └────纠错 Prompt   返回代码片段+错误报告           │
│              │                                     │
│         重新生成                                    │
└──────────────────────────────────────────────────────────────┘
    │
    ▼
Java 代码片段（@CodeRuleAnno rule 方法，非完整测试类）
```

### 3.2 包结构

```
com.jmix.ruletrans
├── RuleTransEngine.java               # 主入口，对外 API
├── RuleTransException.java            # 异常定义
│
├── context/
│   ├── RuleContext.java               # 规则上下文接口
│   ├── PartCategoryContext.java       # PartCategory 级上下文
│   └── ProductContext.java           # 产品级上下文
│
├── identifier/
│   └── CategoryIdentifier.java       # 分类识别器（产品级用）
│
├── generator/
│   ├── RuleSnippetGenerator.java     # 代码片段生成器
│   ├── RuleSnippetAssembler.java     # 代码片段组装为可编译单元
│   └── PromptBuilder.java           # Prompt 构建器（含纠错 Prompt）
│
├── testgen/
│   └── RuleTestCaseGenerator.java   # JSON 测试用例生成器
│
├── postprocessor/
│   ├── CompilationProcessor.java     # 编译验证
│   └── TestExecutionProcessor.java  # 测试执行与结果解析
│
└── template/
    └── PromptTemplateLoader.java   # Prompt 模板加载器（复用）

src/main/resources/ruletrans/
├── part_category_prompt.jtl         # PartCategory 级 Prompt 模板
├── product_stage1_prompt.jtl       # 产品级 Stage1 模板（分类识别）
├── product_stage2_prompt.jtl       # 产品级 Stage2 模板（代码生成）
├── correction_compilation_prompt.jtl # 编译纠错 Prompt 模板
└── correction_test_prompt.jtl      # 测试纠错 Prompt 模板
```

### 3.3 详细设计

#### 3.3.1 RuleContext（规则上下文接口）

```java
package com.jmix.ruletrans.context;

/**
 * 规则上下文接口
 * 作为所有上下文类型的统一入口
 */
public interface RuleContext {

    /**
     * 判断是否为产品级上下文（跨分类）
     */
    boolean isProductLevel();

    /**
     * 获取上下文摘要（用于日志和调试）
     */
    String summary();

    /**
     * 获取涉及的所有 PartCategory code
     */
    List<String> getCategoryCodes();
}
```

#### 3.3.2 数据模型总览

上下文数据模型完整对齐现有 PartCategory 领域模型继承链：

```
com.jmix.executor.bmodel.Onto
├── paras: List<Para>                   # 参数列表（可配置选项）
├── attrParas: List<AttrPara>          # 属性参数（汇总类型：Sum_MaxSpeed）
├── dynAttrSchemas: List<DynamicAttribute>  # 动态属性定义（Part 的属性：Speed、Capacity）
├── dynAttr: Map<String,String>         # 扩展属性值
└── rules: List<Rule>                  # 已有规则（不作为生成输入）

    └── com.jmix.executor.bmodel.ModuleBase
    ├── atomicParts: List<Part>         # 原子部件列表
    └── partCategorys: List<PartCategory> # 子分类列表

        └── com.jmix.executor.bmodel.PartCategory
        ├── partType: PartType          # 部件类型
        ├── supportMultiInst: boolean   # 是否支持多实例
        └── selectionPolicy            # 选择策略
```

规则代码生成时，LLM 需要用到：
1. **Para（参数）**：枚举类型参数的可选值
2. **Part（部件）**：部件的 maxQuantity（数量约束）
3. **DynamicAttribute（动态属性）**：Part 的属性字段（Speed、Capacity 等）
4. **AttrPara（属性参数）**：汇总类型（Sum_Quantity、Sum_Capacity）

#### 3.3.3 PartCategoryContext（PartCategory 级上下文）

```java
package com.jmix.ruletrans.context;

/**
 * PartCategory 级上下文
 * 完整对齐 com.jmix.executor.bmodel.PartCategory 领域模型
 *
 * 继承链：
 *   Onto（paras, attrParas, dynAttrSchemas, dynAttr, rules）
 *     └── ModuleBase（atomicParts, partCategorys）
 *           └── PartCategory（partType, supportMultiInst, selectionPolicy）
 */
public class PartCategoryContext implements RuleContext {

    // === Onto 层 ===
    /** 参数列表（Para 及其选项） */
    private List<ParaSpec> paras;

    /** 属性参数列表（AttrPara：汇总类型，如 Sum_Quantity、Sum_Capacity） */
    private List<AttrParaSpec> attrParas;

    /** 动态属性列表（DynamicAttribute：Part 的属性，如 Speed、Capacity） */
    private List<DynamicAttrSpec> dynAttrSchemas;

    /** 扩展属性（键值对） */
    private Map<String, String> dynAttr;

    // === ModuleBase 层 ===
    /** 原子部件列表 */
    private List<PartSpec> atomicParts;

    /** 子分类列表 */
    private List<PartCategoryContext> partCategorys;

    // === PartCategory 层 ===
    /** 分类 code，如 "cpu" */
    private String categoryCode;

    /** 部件类型 */
    private String partType;

    /** 是否支持多实例 */
    private boolean supportMultiInst;

    // 构造方法、getters、toString...
}
```

#### 3.3.4 ProductContext（产品级上下文）

```java
package com.jmix.ruletrans.context;

/**
 * 产品级上下文
 * 包含多个 PartCategoryContext
 */
public class ProductContext implements RuleContext {

    private final String productCode;
    private final List<PartCategoryContext> categories;

    @Override
    public boolean isProductLevel() { return true; }

    @Override
    public List<String> getCategoryCodes() {
        return categories.stream()
                .map(PartCategoryContext::getCategoryCode)
                .toList();
    }

    @Override
    public String summary() {
        return String.format("Product[code=%s, categories=%s]",
                productCode, getCategoryCodes());
    }
}
```

#### 3.3.5 ParaSpec（参数规格）

```java
package com.jmix.ruletrans.context;

/**
 * 参数规格
 * 对齐 com.jmix.executor.bmodel.para.Para
 */
public class ParaSpec {

    private String code;           // 参数编码，如 "Color"
    private String name;           // 参数名称，如 "颜色"
    private String paraType;      // 类型：ENUM / INTEGER / RANGE / STRING 等（对齐 ParaType 枚举）

    // ENUM / MULTI_ENUM 类型
    private List<ParaOptionSpec> options;

    // RANGE / INTEGER / FLOAT / DOUBLE 类型
    private String minValue;
    private String maxValue;
}

/**
 * 参数选项规格
 * 对齐 com.jmix.executor.bmodel.attr.DynamicAttributerOption
 */
public class ParaOptionSpec {

    private int codeId;           // 选项编码 ID
    private String code;          // 选项编码（用于代码中引用）
    private String codeValue;     // 选项值（实际的值内容）
}
```

#### 3.3.6 AttrParaSpec（属性参数规格）

```java
package com.jmix.ruletrans.context;

/**
 * 属性参数规格
 * 对齐 com.jmix.executor.bmodel.AttrPara
 *
 * 用于汇总类约束，如：
 *   Sum_Quantity >= 2
 *   Sum_Capacity > 100
 *   Max_Speed == 5400
 */
public class AttrParaSpec {

    /** 属性编码，如 "Quantity"、"Capacity"、"Speed" */
    private String attrCode;

    /** 汇总类型：SUM / MAX / MIN / AVG */
    private String attrType;
}
```

#### 3.3.7 DynamicAttrSpec（动态属性规格）

```java
package com.jmix.ruletrans.context;

/**
 * 动态属性规格
 * 对齐 com.jmix.executor.bmodel.attr.DynamicAttribute
 *
 * 描述 Part 的属性字段（Speed、Capacity 等），用于 where 条件过滤
 */
public class DynamicAttrSpec {

    private String code;           // 属性编码，如 "Speed"、"Capacity"
    private String name;           // 属性名称
    private String dynAttrType;   // 属性类型：E_INT / E_STRING / B_INT 等（对齐 DynamicAttributeType）

    // 选项列表（非选项型属性时为空）
    private List<ParaOptionSpec> options;

    // 范围
    private String minValue;
    private String maxValue;
}
```

#### 3.3.8 PartSpec（部件规格）

```java
package com.jmix.ruletrans.context;

/**
 * 部件规格
 * 对齐 com.jmix.executor.bmodel.Part
 */
public class PartSpec {

    private String code;           // 部件编码，如 "cpu1"
    private String name;           // 部件名称
    private Integer maxQuantity;   // 最大数量，默认 20
    private Long price;           // 价格
}
```

#### 3.3.9 CategoryIdentifier（分类识别器）

```java
package com.jmix.ruletrans.identifier;

/**
 * 分类识别器
 * 产品级规则专用：根据自然语言识别涉及的 PartCategory
 */
public class CategoryIdentifier {

    private final LLMInvoker llmInvoker;

    /**
     * 从自然语言识别涉及的 PartCategory
     */
    public List<String> identify(String naturalLanguage, List<String> availableCategories);

    /**
     * 构建分类识别 Prompt（加载 product_stage1_prompt.jtl）
     */
    public String buildIdentifyPrompt(String naturalLanguage, List<String> availableCategories);
}
```

#### 3.3.10 PromptBuilder（Prompt 构建器）

```java
package com.jmix.ruletrans.generator;

/**
 * Prompt 构建器
 * 负责加载模板、注入上下文、构建纠错 Prompt
 */
public class PromptBuilder {

    /**
     * 构建 PartCategory 级代码生成 Prompt
     */
    public String buildPartCategoryPrompt(PartCategoryContext ctx, String naturalLanguage);

    /**
     * 构建产品级代码生成 Prompt（Stage2）
     */
    public String buildProductPrompt(ProductContext ctx, String naturalLanguage);

    /**
     * 构建编译错误纠错 Prompt
     */
    public String buildCorrectionPrompt(
            String previousCode, List<String> compilationErrors,
            String naturalLanguage, RuleContext ctx);

    /**
     * 构建测试失败纠错 Prompt
     */
    public String buildCorrectionPrompt(
            String previousCode, List<FailedTestCase> failedTestCases,
            String naturalLanguage, RuleContext ctx);
}
```

#### 3.3.11 RuleSnippetGenerator（代码片段生成器）

```java
package com.jmix.ruletrans.generator;

/**
 * 规则代码片段生成器
 * 调用 LLM 生成 Java rule 方法代码片段
 */
public class RuleSnippetGenerator {

    private final LLMInvoker llmInvoker;
    private final PromptBuilder promptBuilder;

    /**
     * 生成 PartCategory 级规则代码片段
     */
    public String generate(PartCategoryContext ctx, String naturalLanguage);

    /**
     * 生成产品级规则代码片段（Stage2）
     */
    public String generate(ProductContext ctx, String naturalLanguage);

    /**
     * 基于编译错误反馈生成纠错后的代码片段
     */
    public String generateWithCompilationCorrection(
            String previousCode, List<String> compilationErrors,
            String naturalLanguage, RuleContext ctx);

    /**
     * 基于测试失败反馈生成纠错后的代码片段
     */
    public String generateWithTestCorrection(
            String previousCode, List<FailedTestCase> failedTestCases,
            String naturalLanguage, RuleContext ctx);
}
```

#### 3.3.12 RuleSnippetAssembler（代码片段组装器）

```java
package com.jmix.ruletrans.generator;

/**
 * 代码片段组装器
 * 将 rule 方法代码组装为可编译的 Java 类，供编译验证使用
 */
public class RuleSnippetAssembler {

    /**
     * 将 rule 方法代码组装为完整类
     */
    public String assemble(String ruleCode, String ruleMethodName, RuleContext ctx);

    /**
     * 拆解完整类代码，提取 rule 方法代码片段
     */
    public String extractRuleCode(String fullClassCode);
}
```

#### 3.3.13 RuleTestCaseGenerator（JSON 测试用例生成器）

```java
package com.jmix.ruletrans.testgen;

/**
 * 规则测试用例生成器
 * 根据自然语言和上下文生成结构化测试用例（JSON 格式）
 */
public class RuleTestCaseGenerator {

    private final LLMInvoker llmInvoker;

    /**
     * 生成测试用例
     */
    public String generateTestCases(String naturalLanguage, RuleContext ctx, String generatedCode);
}
```

JSON 结构示例：

```json
{
  "ruleMethod": "rule1",
  "testCases": [
    {
      "id": "TC-001",
      "description": "正常场景：CPU配置一块",
      "input": { "cpu1.quantity": 1 },
      "expected": { "result": "SUCCESS", "constraints": ["cpu1.quantity == 1"] }
    },
    {
      "id": "TC-002",
      "description": "异常场景：CPU配置两块应被拒绝",
      "input": { "cpu1.quantity": 2 },
      "expected": { "result": "FAILURE", "reason": "CPU最多配置一块" }
    }
  ]
}
```

#### 3.3.14 CompilationProcessor（编译验证器）

```java
package com.jmix.ruletrans.postprocessor;

/**
 * 编译验证器
 * 复用 ModuleCompiler，不修改原类
 */
public class CompilationProcessor {

    public CompilationResult compile(String fullClassCode);

    public record CompilationResult(boolean success, List<String> errors) {}
}
```

#### 3.3.15 TestExecutionProcessor（测试执行器）

```java
package com.jmix.ruletrans.postprocessor;

/**
 * 测试执行器
 * 加载 JSON 测试用例，通过测试框架执行，返回测试结果
 */
public class TestExecutionProcessor {

    public TestResult execute(String fullClassCode, String testCasesJson, RuleContext ctx);

    /**
     * 分析测试失败原因，判断是否源于规则逻辑错误（LLM 误解自然语言）
     */
    public boolean isRuleLogicError(List<FailedTestCase> failedCases);

    public record TestResult(boolean success, List<FailedTestCase> failedCases) {}

    public record FailedTestCase(
            String id, String description,
            String input, String expected, String actual, String reason) {}
}
```

#### 3.3.16 RuleTransEngine（主入口）

```java
package com.jmix.ruletrans;

/**
 * 规则转换引擎主入口
 * 编排生成-编译-测试-纠错全流程
 */
public class RuleTransEngine {

    private final CategoryIdentifier identifier;
    private final RuleSnippetGenerator generator;
    private final RuleSnippetAssembler assembler;
    private final RuleTestCaseGenerator testCaseGenerator;
    private final CompilationProcessor compilationProcessor;
    private final TestExecutionProcessor testProcessor;

    /**
     * 从自然语言生成规则代码（无纠错版）
     */
    public String translate(String naturalLanguage, RuleContext ctx);

    /**
     * 从自然语言生成规则代码（带迭代纠错）
     *
     * - Step 1: 生成代码片段
     * - Step 2: 组装为完整类，编译验证
     * - Step 3: 编译失败 → 解析错误 → 构建纠错 Prompt → 重新生成（循环 Step 2）
     * - Step 4: 编译通过 → 生成 JSON 测试用例
     * - Step 5: 执行测试
     * - Step 6: 测试失败 → 分析原因 → 若为规则逻辑错误则构建纠错 Prompt → 回到 Step 2
     */
    public String translateWithRetry(String naturalLanguage, RuleContext ctx, int maxRetries);
}
```

### 3.4 Prompt 模板详细设计

#### 3.4.1 PartCategory 级模板（part_category_prompt.jtl）

```
## 任务
将自然语言规则转换为 Java 约束代码片段，作用域为单个 PartCategory。

## PartCategory 信息
- 分类代码: ${categoryCode}

## 1. 参数（Para）规格
### 对齐 com.jmix.executor.bmodel.para.Para
参数是可配置的选项，供用户选择。
${parasSpec}
### 字段说明
- code: 参数编码
- name: 参数名称
- paraType: 参数类型（ENUM / INTEGER / RANGE / STRING / MULTI_ENUM 等）
- options: 选项列表（仅 ENUM/MULTI_ENUM 类型有值，每个选项含 codeId、code、codeValue）
- minValue / maxValue: 仅 RANGE / INTEGER / FLOAT / DOUBLE 类型使用

## 2. 属性参数（AttrPara）规格
### 对齐 com.jmix.executor.bmodel.AttrPara
用于汇总类约束，支持聚合函数。
${attrParasSpec}
### 字段说明
- attrCode: 属性编码（如 "Quantity"、"Capacity"）
- attrType: 汇总类型（SUM / MAX / MIN / AVG）
### 使用示例
- Sum_Quantity: 对部件数量求和
- Sum_Capacity: 对容量属性求和
- Max_Speed: 取速度最大值

## 3. 动态属性（DynamicAttribute）规格
### 对齐 com.jmix.executor.bmodel.attr.DynamicAttribute
描述 Part 的属性字段（Speed、Capacity 等），用于 where 条件过滤。
${dynAttrSchemasSpec}
### 字段说明
- code: 属性编码（如 "Speed"、"Capacity"）
- name: 属性名称
- dynAttrType: 属性类型（E_INT / E_STRING / B_INT 等）
- options: 选项列表（选项型属性有值）
- minValue / maxValue: 范围

## 4. 部件（Part）规格
### 对齐 com.jmix.executor.bmodel.Part
${partsSpec}
### 字段说明
- code: 部件编码
- maxQuantity: 最大数量（默认 20）

## 5. 自然语言规则
${naturalLanguage}

## 代码片段格式要求
仅输出 Java 代码片段，不得包含 package 声明、import 语句和类声明。
格式如下：
```java
@CodeRuleAnno(normalNaturalCode = "${naturalLanguage}")
private void ${ruleMethodName}() {
    // 约束实现代码
    // 参数引用：this.paraVar.getParaOptionByCode("codeValue").getIsSelectedVar()
    // 数量引用：this.partVar.qty
    // 动态属性过滤：使用 Sum_Capacity、Max_Speed 等 AttrPara 类型
    // 日志使用英文
}
```

## 技术约束
- 使用 Or-Tools CP-SAT 约束求解 API
- rule 方法体直接使用上下文中的规格信息
- 参数引用格式：变量名使用 Para.code（首字母小写）
- 选项引用格式：使用 option.codeValue
- 支持 AttrPara 类型汇总约束（Sum_Quantity、Max_Speed 等）
```

#### 3.4.2 产品级 Stage1 模板（product_stage1_prompt.jtl）

```
## 任务
从自然语言中识别涉及的 PartCategory。

## 可用的 PartCategory 列表
${availableCategories}

## 自然语言规则
${naturalLanguage}

## 输出格式
输出一个 JSON 数组，包含涉及的 category code：
```json
["cpu", "hdd"]
```

如果无法识别任何分类，输出：
```json
[]
```
```

#### 3.4.3 产品级 Stage2 模板（product_stage2_prompt.jtl）

```
## 任务
将自然语言规则转换为 Java 约束代码片段，作用域为多个 PartCategory。

## PartCategory 规格列表
${categorySpecs}

每个 PartCategory 包含：
- 分类代码、部件列表
- 参数（Para）规格
- 属性参数（AttrPara）规格
- 动态属性（DynamicAttribute）规格

## 自然语言规则
${naturalLanguage}

## 代码片段格式要求
仅输出 Java 代码片段，不得包含 package 声明、import 语句和类声明。
```java
@CodeRuleAnno(normalNaturalCode = "${naturalLanguage}")
private void ${ruleMethodName}() {
    // 跨 PartCategory 约束实现代码
    // 使用 coDependent / compatibility 方法建立分类间约束
    // 支持 AttrPara 汇总约束和动态属性过滤
}
```

## 技术约束
- 使用 Or-Tools CP-SAT 约束求解 API
- 跨 PartCategory 约束使用 coDependent / compatibility 方法
- 支持 Sum_Quantity、Sum_Capacity、Max_Speed 等汇总约束
- 日志使用英文
```

#### 3.4.4 编译错误纠错模板（correction_compilation_prompt.jtl）

```
## 任务
修正以下 Java 代码片段中的编译错误。

## 上一次生成的代码
```java
${previousCode}
```

## 编译错误详情
${compilationErrors}

## 自然语言规则（参考）
${naturalLanguage}

## 修正要求
1. 仔细分析每一条编译错误
2. 修正代码中的语法错误、类型错误、缺少 import 等问题
3. 不得改变业务逻辑，仅修复错误
4. 仅输出修正后的 Java 代码片段

## 输出格式
```java
@CodeRuleAnno(normalNaturalCode = "${naturalLanguage}")
private void ${ruleMethodName}() {
    // 修正后的约束实现代码
}
```
```

#### 3.4.5 测试失败纠错模板（correction_test_prompt.jtl）

```
## 任务
修正以下规则代码，使其通过所有测试用例。

## 自然语言规则
${naturalLanguage}

## 上一次生成的代码
```java
${previousCode}
```

## 测试失败详情
${testFailureDetails}

## 修正要求
1. 分析每个失败测试用例的输入、预期输出和实际输出
2. 判断是否因为 LLM 对自然语言规则的理解有偏差导致测试失败
3. 如果是规则逻辑错误，修正代码使其符合自然语言语义
4. 如果不是规则逻辑错误，保留原代码并在注释中说明

## 输出格式
```java
@CodeRuleAnno(normalNaturalCode = "${naturalLanguage}")
private void ${ruleMethodName}() {
    // 修正后的约束实现代码
}
```
```

### 3.5 关键设计决策

#### 决策 1：LLM 调用完全复用

`LLMInvoker` / `LLMInvokerImpl` 不做任何修改。`ruletrans` 各组件通过组合复用。

#### 决策 2：Prompt 模板独立存放

新建 `src/main/resources/ruletrans/` 目录，五个模板文件互不干扰。不修改 `cengine/` 目录。

#### 决策 3：上下文数据模型对齐领域模型

`PartCategoryContext` 完整对齐 PartCategory 继承链（Onto → ModuleBase → PartCategory），包含：
- `paras`（`List<ParaSpec>`）— 参数规格
- `attrParas`（`List<AttrParaSpec>`）— 属性参数规格（汇总类型）
- `dynAttrSchemas`（`List<DynamicAttrSpec>`）— 动态属性规格（Part 属性）
- `dynAttr`（`Map<String, String>`）— 扩展属性值
- `atomicParts`（`List<PartSpec>`）— 部件规格
- `partCategorys`（`List<PartCategoryContext>`）— 子分类

#### 决策 4：纠错 Prompt 动态构建

- 编译失败：解析 javac 输出，构建 `correction_compilation_prompt.jtl`
- 测试失败：`TestExecutionProcessor` 分析原因，构建 `correction_test_prompt.jtl`

#### 决策 5：输出内容边界

`ruletrans` 输出**纯 Java rule 方法代码片段**。组装为完整类的工作由 `RuleSnippetAssembler` 负责。

---

## 4. 验收准则

### 4.1 功能验收用例

#### AC-001：PartCategory 级规则翻译

```java
@Test
public void testPartCategoryTranslate() {
    // Para: Color = {Red, Black}
    List<ParaOptionSpec> colorOptions = List.of(
        new ParaOptionSpec(1, "Red", "Red"),
        new ParaOptionSpec(2, "Black", "Black")
    );
    ParaSpec colorPara = new ParaSpec("Color", "颜色", "ENUM", colorOptions, null, null);

    // Part: cpu1, maxQuantity = 1
    PartSpec cpu1 = new PartSpec("cpu1", "CPU", 1, 0L);

    PartCategoryContext ctx = new PartCategoryContext();
    ctx.setCategoryCode("cpu");
    ctx.setParas(List.of(colorPara));
    ctx.setAtomicParts(List.of(cpu1));

    RuleTransEngine engine = new RuleTransEngine();
    String code = engine.translate("CPU最多配置一块", ctx);

    assertNotNull(code);
    assertTrue(code.contains("@CodeRuleAnno"));
    assertTrue(code.contains("qty"));
}
```

#### AC-002：含动态属性的规则翻译

```java
@Test
public void testDynAttrTranslate() {
    // DynamicAttribute: Speed = {5400, 7200}
    List<ParaOptionSpec> speedOptions = List.of(
        new ParaOptionSpec(1, "5400转", "5400"),
        new ParaOptionSpec(2, "7200转", "7200")
    );
    DynamicAttrSpec speedAttr = new DynamicAttrSpec("Speed", "转速", "E_INT", speedOptions, null, null);

    PartCategoryContext ctx = new PartCategoryContext();
    ctx.setCategoryCode("hdd");
    ctx.setDynAttrSchemas(List.of(speedAttr));

    RuleTransEngine engine = new RuleTransEngine();
    String code = engine.translate("硬盘转速不能是5400转", ctx);

    assertNotNull(code);
    assertTrue(code.contains("Speed"));
}
```

#### AC-003：编译验证通过

```java
@Test
public void testGeneratedCodeCompiles() {
    RuleTransEngine engine = new RuleTransEngine();
    String snippet = engine.translate("CPU最多配置一块", partCategoryCtx);

    RuleSnippetAssembler assembler = new RuleSnippetAssembler();
    String fullClass = assembler.assemble(snippet, "rule1", partCategoryCtx);

    CompilationProcessor processor = new CompilationProcessor();
    CompilationResult result = processor.compile(fullClass);

    assertTrue(result.success(), String.join("\n", result.errors()));
}
```

#### AC-004：JSON 测试用例生成

```java
@Test
public void testTestCaseGeneration() {
    RuleTestCaseGenerator gen = new RuleTestCaseGenerator();
    String json = gen.generateTestCases("CPU最多配置一块", ctx, ruleCode);

    JsonNode node = new ObjectMapper().readTree(json);
    assertTrue(node.has("ruleMethod"));
    assertTrue(node.has("testCases"));
    assertTrue(node.get("testCases").isArray());

    for (JsonNode tc : node.get("testCases")) {
        assertTrue(tc.has("id"));
        assertTrue(tc.has("input"));
        assertTrue(tc.has("expected"));
    }
}
```

#### AC-005：原有 ModelHelper 不受影响

```java
@Test
public void testModelHelperUnchanged() {
    String source = Files.readString(Path.of("src/main/java/com/jmix/tool/ModelHelper.java"));
    assertFalse(source.contains("ruletrans"));
    assertFalse(source.contains("RuleTransEngine"));
}
```

### 4.2 边界条件

| 条件 | 输入 | 预期行为 |
|------|------|----------|
| 自然语言为空 | `""` | 抛出 `IllegalArgumentException` |
| RuleContext 为空 | `null` | 抛出 `IllegalArgumentException` |
| Stage1 未识别任何分类 | 产品级，LLM 返回空 | 抛出 `CategoryNotFoundException` |
| 重试超过上限 | 连续失败超过 3 次 | 抛出 `RuleTransException`，附最终错误信息 |
| LLM API 调用失败 | 网络错误/API Key 无效 | 抛出 `RuleTransException`，不修改任何文件 |

### 4.3 回归测试

- [ ] `ModelHelper` 现有测试全部通过
- [ ] `ModuleGenerator` 现有测试全部通过
- [ ] `LLMInvoker` 接口契约不变
- [ ] `ModuleCompiler` / `ModuleRunner` 行为不变

---

## 5. 实现计划

| 阶段 | 任务 | 优先级 | 状态 |
|------|------|--------|------|
| 1 | 创建 `com.jmix.ruletrans` 包骨架 | P0 | 待开始 |
| 2 | 实现 `RuleContext` 及上下文数据类（PartCategoryContext / ProductContext / ParaSpec / AttrParaSpec / DynamicAttrSpec / PartSpec） | P0 | 待开始 |
| 3 | 创建 `ruletrans/` 目录及五个 Prompt 模板文件 | P0 | 待开始 |
| 4 | 实现 `PromptBuilder`（含纠错 Prompt 构建） | P0 | 待开始 |
| 5 | 实现 `RuleSnippetGenerator`（复用 `LLMInvoker`） | P0 | 待开始 |
| 6 | 实现 `RuleSnippetAssembler`（代码片段组装） | P0 | 待开始 |
| 7 | 实现 `CompilationProcessor`（复用 `ModuleCompiler`） | P0 | 待开始 |
| 8 | 实现 `CategoryIdentifier`（产品级 Stage1） | P0 | 待开始 |
| 9 | 实现 `RuleTransEngine` 主入口（无纠错版） | P0 | 待开始 |
| 10 | 实现 `translateWithRetry`（编译纠错 + 测试纠错循环） | P1 | 待开始 |
| 11 | 实现 `RuleTestCaseGenerator`（JSON 测试用例） | P1 | 待开始 |
| 12 | 实现 `TestExecutionProcessor` | P1 | 待开始 |
| 13 | 编写单元测试（AC-001 ~ AC-005） | P1 | 待开始 |
| 14 | 集成测试：端到端自然语言翻译 | P2 | 待开始 |

---

## 6. 参考资料

- [RFC-0004: Hybrid Calculation Pre-Mid-Post](doc/RFC-0004-Hybrid-Calculation-Pre-Mid-Post.md)
- [RFC-0007: Struct Combination Rule Schema](doc/RFC-0007-Struct-Combination-Rule-Schema.md)
- [约束生成 Prompt 模板](../src/main/resources/cengine/constraint_generate_prompt.jtl)
- [ModelHelper](../src/main/java/com/jmix/tool/ModelHelper.java)
- [PartCategory 领域模型](../src/main/java/com/jmix/executor/bmodel/PartCategory.java)
- [Onto 领域模型](../src/main/java/com/jmix/executor/bmodel/Onto.java)
- [Para 领域模型](../src/main/java/com/jmix/executor/bmodel/para/Para.java)
- [DynamicAttribute 领域模型](../src/main/java/com/jmix/executor/bmodel/attr/DynamicAttribute.java)
- [AttrPara 领域模型](../src/main/java/com/jmix/executor/bmodel/AttrPara.java)
- [LLMInvoker](../src/main/java/com/jmix/tool/impl/llm/LLMInvoker.java)
- [ModuleScenarioTestBase](../src/test/java/com/jmix/coretest/ModuleScenarioTestBase.java)
- [约束引擎架构](../CLAUDE.md)

---

## 附录 A：复用优先清单

### 优先复用

- `com.jmix.tool.impl.llm.LLMInvoker` / `LLMInvokerImpl`：LLM 调用能力
- `com.jmix.tool.impl.PromptTemplateLoader`：`renderTemplate` 方法
- `com.jmix.tool.impl.ModuleCompiler`：编译验证逻辑
- `com.jmix.executor.bmodel.PartCategory`：领域模型（上下文类与其结构对齐）
- `com.jmix.executor.bmodel.para.Para`：参数领域模型
- `com.jmix.executor.bmodel.attr.DynamicAttribute`：动态属性领域模型
- `com.jmix.executor.bmodel.AttrPara`：属性参数领域模型
- `com.jmix.executor.bmodel.Part`：部件领域模型

### 不新增

- 不新建 LLM 调用封装类，直接复用 `LLMInvoker`
- 不新建编译工具类，复用 `ModuleCompiler`
- 不修改现有 `cengine/` 目录下的 Prompt 模板
- 不修改现有 `com.jmix.tool` 包下的任何类

### 可由领域模型推导

- `ParaSpec` 来自 `Para.getOptions()`（`List<DynamicAttributerOption>`）
- `DynamicAttrSpec` 来自 `DynamicAttribute`
- `AttrParaSpec` 来自 `AttrPara`
- 未来可通过 `ModuleGenneratorByAnno` 从注解自动构造 `PartCategoryContext`
