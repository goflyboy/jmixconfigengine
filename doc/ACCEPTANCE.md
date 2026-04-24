# JMix Config Engine - 验收准则

> 版本：v0.2
> 生成日期：2026-04-24
> 状态：正式版

---

## 第一部分：验收准则（规则/原则）

### 1.1 核心原则

#### 1.1.1 测试用例设计原则

| 原则 | 说明 | 具体要求 |
|------|------|----------|
| **逻辑完备** | 每个功能的测试点要全面覆盖 | 正常路径 + 边界条件 + 异常路径 |
| **用例独立** | 避免用例之间重叠 | 一个用例只测一个功能点 |
| **数据极简** | 只保留当前功能关注的数据 | 无关的参数/部件/规则一律省略 |

#### 1.1.2 开发流程原则

| 原则 | 说明 | 具体要求 |
|------|------|----------|
| **复用参考** | 新增用例时复制参考测试而非从头创建 | 复制 → 修改 → 验证 |
| **大模型验证** | 复杂逻辑需用大模型交叉验证 | 自动生成 Prompt → LLM 验证 |
| **自动化回归** | 验证通过的用例需纳入回归测试集 | `mvn test` 可自动执行 |

#### 1.1.3 设计示例

**❌ 错误示例**：一个用例测多个功能，数据冗余

```java
// 错误：一个用例测了3个功能，数据复杂
@PartAnno(code = "cpu")    // 不相关
@PartAnno(code = "gpu")    // 不相关
@PartAnno(code = "memory")  // 不相关
@ParaAnno  // 不相关
private PartVar storage;   // 不相关

@Test
public void testRequiresAndIncompatibleAndPriority() {
    // 同时测试3种规则...
}
```

**✅ 正确示例**：一个用例只测一个功能，数据极简

```java
// ✅ 正确：只测试 Requires 约束，数据极简
@ParaAnno(options = {"i9", "i7", "i5"})
private ParaVar cpuVar;

@ParaAnno(options = {"air", "liquid"})
private ParaVar coolerVar;

@Test
public void testRequires_cpu_i9_needs_liquid() {
    // 只测试 Requires 规则
}

@Test
public void testRequires_cpu_i7_no_restriction() {
    // 只测试 i7 不受限制的情况
}
```

### 1.2 测试分类

| 测试类型 | 描述 | 参考示例 | 存放位置 |
|----------|------|----------|----------|
| **场景测试** | 复杂结构性测试，带完整业务数据 | `BaseOptiTest` | `opti/base/` |
| **逻辑测试** | 纯粹测试某个约束/规则逻辑 | `CalculateRuleIfThenTest` | `scenario/ruletest/` |
| **API测试** | 测试底层 API 功能正确性 | `FilterExpressionExecutorTest` | `tool/` |

---

## 第二部分：测试数据构建规范

### 2.1 数据构建核心原则

> **极简原则**：如果不是当前功能关注的，就不要构建。

| 构建项 | 关注点 | 非关注点（不要构建） |
|--------|--------|---------------------|
| PartCategory | 当前功能涉及的分类 | 其他无关分类 |
| Part | 当前功能涉及的部件 | 其他部件 |
| Para | 当前功能涉及的参数 | 其他参数 |
| Rule | 当前功能涉及的规则 | 其他规则 |
| 属性选项 | 当前功能需要的选项 | 其他选项 |

### 2.2 注解使用规范

参考 `BaseOptiTest.java` 中的注解定义模式：

```java
// ---------------模型定义结构----------------------------------------
// 1. 定义模块
@ModuleAnno(id = 123L)
static public class MyConstraint extends ConstraintAlgImplTestBase {

    // 2. 定义部件分类（父层）
    @PartAnno(code = "drive")
    @DAttrAnno2(code = "Speed", options = {"Speed_5400:5400:转", ...})
    @DAttrAnno3(code = "Capacity", options = {"Capacity_1T:1:T", ...})
    private PartCategoryVar drive;

    // 3. 定义部件分类（子层，继承父层属性）
    @PartAnno(code = "sd", fatherCode = "drive")
    @DAttrInherit(fatherCode = "driveVar")
    private PartCategoryVar sd;

    // 4. 定义具体部件实例
    @PartAnno(fatherCode = "sd", attrs = {"5400", "1"})
    private PartVar sd1;

    // 5. 定义参数
    @ParaAnno(fatherCode = "drive", type = ParaType.INTEGER, assignType = AssignType.INPUT)
    private ParaVar Sum_Capacity;

    // 6. 定义规则
    @CodeRuleAnno(fatherCode = "drive", normalNaturalCode = "业务规则描述")
    private void rule1() {
        // 约束实现
    }

    @PriorityRuleAnno(strategy = PriorityStrategy.MIN)
    private void rule2() {
        // 优化规则实现
    }
}
```

### 2.2 数据层级结构

```
Module (模块)
  └── PartCategory (部件分类)
        ├── PartCategory (子分类，继承父分类属性)
        │     └── Part (具体部件)
        └── Part (具体部件)
```

### 2.3 新增测试数据步骤

1. **复制参考文件**：从 `scenario/ruletest/` 或 `opti/base/` 复制参考测试
2. **修改类名**：使用 `XxxNewFeatureTest` 命名
3. **定义新的约束类**：使用 `XxxNewFeatureConstraint` 命名
4. **简化数据**：只包含当前功能需要的最小数据集
5. **编写测试用例**：每个测试用例对应一个验证点

---

## 第三部分：验收用例集

### 3.1 用例设计规范

#### 3.1.1 极简原则示例

**测试 Requires 约束时**：

```java
// ✅ 正确：极简数据，只测 Requires
@ParaAnno(options = {"A", "B"})
private ParaVar x;

@ParaAnno(options = {"C", "D"})
private ParaVar y;

@CompatRuleAnno(type = RuleType.REQUIRES)
private void rule() {
    requires("x:A", "y:D"); // A→D
}

// ❌ 错误：包含无关数据
@PartAnno // 不相关
@ParaAnno // 不相关
private ParaVar z;
```

**测试 If-Then 规则时**：

```java
// ✅ 正确：极简数据，只测 If-Then
@ParaAnno(options = {"T", "F"})
private ParaVar p1;

@ParaAnno(options = {"T", "F"})
private ParaVar p2;

@PartAnno
private PartVar pt;

// ✅ 正确：边界条件要覆盖
// 测试点1: if 条件满足 → 1个解
// 测试点2: else 条件满足 → 多个解
// 测试点3: 无解情况 → 0个解
```

---

### 3.2 逻辑测试用例（参考：CalculateRuleIfThenTest）

#### LOGIC-001：If-Then 条件分支

**目的**：验证 `if(p1==op11 && p2==op21) then qty=1 else qty=3` 逻辑

**测试数据（极简）**：
```java
@ParaAnno(options = {"op11", "op12", "op13"})
private ParaVar p1Var;

@ParaAnno(options = {"op21", "op22", "op23"})
private ParaVar p2Var;

@PartAnno
private PartVar pt1Var;
```

**测试用例（逻辑完备）**：

| ID | 输入 | 预期解数量 | 预期解内容 | 验证逻辑 |
|----|------|------------|------------|----------|
| LOGIC-001-1 | `pt1.qty = 1` | 1 | p1=op11, p2=op21 | if条件满足，1个解 |
| LOGIC-001-2 | `pt1.qty = 3` | 8 | 除(op11,op21)外所有组合 | else条件，3×3-1=8 |
| LOGIC-001-3 | `pt1.qty = 4` | 0 | 无解 | 不满足if也不满足else |

**大模型验证 Prompt**：
```
给定约束：如果 p1=op11 且 p2=op21，则 pt1.qty=1；否则 pt1.qty=3

问题1：当 pt1.qty=1 时，有几个可行解？
问题2：当 pt1.qty=3 时，有几个可行解？
问题3：当 pt1.qty=4 时，有解吗？为什么？
```

---

#### LOGIC-002：Requires 约束（A→B）

**目的**：验证 `A选中 → B必须选中` 约束

**测试数据（极简）**：
```java
@ParaAnno(options = {"i9", "i7"})
private ParaVar cpu;

@ParaAnno(options = {"air", "liquid"})
private ParaVar cooler;
```

**测试用例（逻辑完备）**：

| ID | 输入 | 预期 | 验证逻辑 |
|----|------|------|----------|
| LOGIC-002-1 | `cpu=i9` | cooler=liquid | i9必须配液冷 |
| LOGIC-002-2 | `cpu=i7` | cooler=air 或 liquid | i7无限制 |
| LOGIC-002-3 | `cooler=liquid` | cpu 任意 | 反向不约束 |

**大模型验证 Prompt**：
```
约束：REQUIRES(cpu=i9, cooler=liquid)

判断以下组合是否满足：
1. cpu=i9, cooler=liquid ✓
2. cpu=i9, cooler=air ✗
3. cpu=i7, cooler=liquid ✓
4. cpu=i7, cooler=air ✓
```

---

#### LOGIC-003：Incompatible 约束（¬A∧B）

**目的**：验证 `A和B不能同时选中` 约束

**测试数据（极简）**：
```java
@ParaAnno(options = {"yes", "no"})
private ParaVar a;

@ParaAnno(options = {"yes", "no"})
private ParaVar b;
```

**测试用例（逻辑完备）**：

| ID | a | b | 预期 | 验证逻辑 |
|----|---|---|------|----------|
| LOGIC-003-1 | yes | no | ✓ 有效 | 互斥满足 |
| LOGIC-003-2 | no | yes | ✓ 有效 | 互斥满足 |
| LOGIC-003-3 | yes | yes | ✗ 无解 | 违反互斥 |
| LOGIC-003-4 | no | no | ✓ 有效 | 都未选 |

---

#### LOGIC-004：CoDependent 约束（A↔B）

**目的**：验证 `A和B必须同选或同不选` 约束

**测试数据（极简）**：
```java
@ParaAnno(options = {"yes", "no"})
private ParaVar a;

@ParaAnno(options = {"yes", "no"})
private ParaVar b;
```

**测试用例（逻辑完备）**：

| ID | a | b | 预期 | 验证逻辑 |
|----|---|---|------|----------|
| LOGIC-004-1 | yes | yes | ✓ 有效 | 同选 |
| LOGIC-004-2 | no | no | ✓ 有效 | 同不选 |
| LOGIC-004-3 | yes | no | ✗ 无解 | 违反同选 |
| LOGIC-004-4 | no | yes | ✗ 无解 | 违反同选 |

---

### 3.3 场景测试用例（参考：BaseOptiTest）

#### SCENE-001：PartCategory 嵌套继承

**目的**：验证子分类继承父分类属性

**测试数据（极简）**：
```java
// 父分类
@PartAnno(code = "disk")
@DAttrAnno2(code = "Speed", options = {"100", "200"})
private PartCategoryVar disk;

// 子分类（继承Speed属性）
@PartAnno(code = "ssd", fatherCode = "disk")
@DAttrInherit(fatherCode = "disk")
private PartCategoryVar ssd;

// 具体部件
@PartAnno(fatherCode = "ssd", attrs = {"100"})
private PartVar ssd1;

@PartAnno(fatherCode = "ssd", attrs = {"200"})
private PartVar ssd2;
```

**测试用例（逻辑完备）**：

| ID | 输入 | 预期 | 验证逻辑 |
|----|------|------|----------|
| SCENE-001-1 | filter: Speed=100 | ssd1 | 子分类继承父属性 |
| SCENE-001-2 | filter: Speed=200 | ssd2 | 子分类继承父属性 |
| SCENE-001-3 | filter: Speed=300 | 无解 | 属性值不存在 |

---

#### SCENE-002：多实例支持

**目的**：验证 supportMultiInst=true 时的多实例处理

**测试数据（极简）**：
```java
@PartAnno(code = "gpu", supportMultiInst = true)
private PartCategoryVar gpu;

@PartAnno(fatherCode = "gpu", attrs = {"3090"})
private PartVar gpu1;

@PartAnno(fatherCode = "gpu", attrs = {"4090"})
private PartVar gpu2;
```

**测试用例（逻辑完备）**：

| ID | 输入 | 预期 | 验证逻辑 |
|----|------|------|----------|
| SCENE-002-1 | gpu:Qty >= 2 | gpu1+gpu2 | 多实例数量约束 |
| SCENE-002-2 | gpu:Qty == 1 | gpu1 或 gpu2 | 单实例 |

---

### 3.4 API 测试用例（参考：FilterExpressionExecutorTest）

#### API-001：doSelect 过滤

**目的**：验证过滤表达式执行器

**测试数据（极简）**：
```java
List<TestPart> parts = Arrays.asList(
    createPart("p1", "100", "1"),
    createPart("p2", "100", "2"),
    createPart("p3", "200", "1")
);
```

**测试用例（逻辑完备）**：

| ID | 表达式 | 预期结果 | 验证逻辑 |
|----|--------|----------|----------|
| API-001-1 | `speed=100` | [p1, p2] | 等值过滤 |
| API-001-2 | `speed=100 and cap=1` | [p1] | 多条件AND |
| API-001-3 | `speed=100 or speed=200` | [p1, p2, p3] | 多条件OR |
| API-001-4 | `speed=300` | [] | 无匹配 |

---

#### API-002：doDeduct 扣除

**目的**：验证扣除表达式执行器

**测试用例（逻辑完备）**：

| ID | 输入 | 预期结果 | 验证逻辑 |
|----|------|----------|----------|
| API-002-1 | `doDeduct(speed=100)` | 移除p1,p2 | 排除匹配项 |
| API-002-2 | `doDeduct(cap>1)` | 移除p2 | 排除cap>1 |

---

### 3.5 用例汇总矩阵

| 用例ID | 类型 | 功能点 | 数据复杂度 | 验证方式 |
|--------|------|--------|------------|----------|
| LOGIC-001 | 逻辑 | If-Then | ⭐ 极简 | 大模型 |
| LOGIC-002 | 逻辑 | Requires | ⭐ 极简 | 大模型 |
| LOGIC-003 | 逻辑 | Incompatible | ⭐ 极简 | 自动化 |
| LOGIC-004 | 逻辑 | CoDependent | ⭐ 极简 | 自动化 |
| SCENE-001 | 场景 | 嵌套继承 | ⭐⭐ 简单 | 自动化 |
| SCENE-002 | 场景 | 多实例 | ⭐⭐ 简单 | 自动化 |
| API-001 | API | doSelect | ⭐ 极简 | 自动化 |
| API-002 | API | doDeduct | ⭐ 极简 | 自动化 |

---

## 第四部分：大模型验证流程

### 4.1 Prompt 生成器规范

对于每个验收用例，系统需要能自动生成验证 Prompt：

```java
/**
 * 生成大模型验证 Prompt
 *
 * @param testCase 验收用例
 * @param testData 测试数据
 * @param programOutput 程序输出
 * @return 发送给大模型的 Prompt
 */
public String generateVerificationPrompt(AcceptanceTestCase testCase,
                                         TestData testData,
                                         ProgramOutput programOutput) {
    return String.format("""
        ## 任务：验证约束求解器输出正确性

        ### 1. 业务背景
        %s

        ### 2. 测试数据
        %s

        ### 3. 业务规则
        %s

        ### 4. 程序输入
        %s

        ### 5. 程序输出
        %s

        ### 6. 验证要求
        - 请人工计算验证程序输出是否正确
        - 检查是否满足所有业务规则
        - 检查是否有遗漏的可行解
        - 如有不正确，请指出具体问题

        ### 7. 回答格式
        ```
        验证结果：[正确/错误]
        详细说明：[...]
        ```
        """,
        testCase.getPurpose(),
        testData.toPromptString(),
        testCase.getRulesDescription(),
        testCase.getInput(),
        programOutput.toPromptString()
    );
}
```

### 4.2 验证流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    大模型验证流程                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 构建测试数据                                                  │
│     └── 使用注解定义测试场景                                      │
│                                                                 │
│  2. 运行程序获取输出                                              │
│     └── 执行测试用例，获取求解结果                                │
│                                                                 │
│  3. 生成验证 Prompt                                              │
│     └── 自动生成包含数据、规则、输入输出的 Prompt                 │
│                                                                 │
│  4. 发送给大模型验证                                              │
│     └── 调用外部 LLM API                                         │
│                                                                 │
│  5. 获取验证结果                                                  │
│     └── 解析大模型返回的验证结论                                  │
│                                                                 │
│  6. 决策                                                        │
│     ├── 如果正确 → 纳入回归测试集                                │
│     └── 如果错误 → 修复程序，重新验证                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 验证示例

**场景**：If-Then 规则验证

**自动生成的 Prompt**：
```
## 任务：验证约束求解器输出正确性

### 1. 业务背景
验证 If-Then 条件分支规则：如果 p1=op11 且 p2=op21，则 pt1.qty=1；否则 pt1.qty=3

### 2. 测试数据
- p1 选项：[op11, op12, op13]
- p2 选项：[op21, op22, op23]
- pt1 数量：可取 1, 3 等值

### 3. 程序输入
inferParas("pt1", 1)  // 要求 pt1.qty = 1

### 4. 程序输出
解1: p1=op11, p2=op21, pt1.qty=1

### 5. 验证要求
- 请人工计算验证程序输出是否正确
- 当 pt1.qty=1 时，只有 p1=op11 且 p2=op21 才满足 if 条件
- 验证输出是否满足约束

### 6. 回答格式
验证结果：[正确/错误]
详细说明：[...]
```

---

## 第五部分：自动化测试规范

### 5.1 测试文件命名规范

| 测试类型 | 命名模式 | 示例 |
|----------|----------|------|
| 场景测试 | `XxxScenarioTest.java` | `ComputerScenarioTest.java` |
| 逻辑测试 | `XxxLogicTest.java` | `IfThenLogicTest.java` |
| API测试 | `XxxApiTest.java` 或 `XxxTest.java` | `FilterExpressionTest.java` |

### 5.2 测试方法命名规范

```java
// 场景测试：testCase{编号}_{功能点}_{输入条件}
@Test
public void testCase001_CapacityGreaterEqual6_Speed5400() { ... }

// 逻辑测试：test{规则类型}_{条件}
@Test
public void testIfThen_ConditionOp11Op21() { ... }

// API测试：test{方法名}_{场景}
@Test
public void testDoSelect_WithCodeFilter() { ... }
```

### 5.3 断言规范

```java
// 场景测试：使用高级断言
assertSoluContain(1, "sd1(Q:2,H:0,S:1)");
assertSoluContain("md1(Q:3,H:0,S:1),sd1(Q:1,H:0,S:1)");

// 逻辑测试：使用结果断言
resultAssert()
    .assertSuccess()
    .assertSolutionSizeEqual(8);

// API测试：使用标准断言
assertEquals(1, result.size());
assertEquals("para31", result.get(0).getCode());
```

### 5.4 回归测试集

验证通过的用例需添加到回归测试集：

```java
/**
 * 回归测试套件
 * 所有通过大模型验证的用例都应纳入此套件
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // 场景测试
    BaseOptiTest.class,
    MultiPCTest.class,

    // 逻辑测试
    CalculateRuleIfThenTest.class,
    CompatibleRuleRequireTest.class,
    CompatibleRuleIncompatibleTest.class,

    // API测试
    FilterExpressionExecutorTest.class,
})
public class RegressionTestSuite {
}
```

---

## 附录

### A. 术语表

| 术语 | 定义 |
|------|------|
| Requires 约束 | 如果A选中则B必须选中：A→B |
| Incompatible 约束 | A和B不能同时选中：¬(A∧B) |
| CoDependent 约束 | A和B必须同选或同不选：A↔B |
| PriorityRule | 优先级优化规则（MAX/MIN） |
| If-Then 规则 | 条件分支计算规则 |
| CP-SAT | Google OR-Tools 约束编程求解器 |

### B. 参考测试文件

| 文件 | 路径 | 用途 |
|------|------|------|
| `BaseOptiTest.java` | `opti/base/` | 复杂场景测试参考 |
| `MultiPCTest.java` | `opti/base/` | 多电脑配置测试参考 |
| `CalculateRuleIfThenTest.java` | `scenario/ruletest/` | 逻辑测试参考 |
| `CompatibleRuleRequireTest.java` | `scenario/ruletest/` | Requires约束测试参考 |
| `FilterExpressionExecutorTest.java` | `tool/` | API测试参考 |

### C. 验收检查清单

- [ ] 测试数据简洁，只测试当前功能点
- [ ] 每个测试用例有明确的输入输出
- [ ] 复杂逻辑已通过大模型验证
- [ ] 测试用例已加入回归测试集
- [ ] 测试可通过 `mvn test` 自动化执行
