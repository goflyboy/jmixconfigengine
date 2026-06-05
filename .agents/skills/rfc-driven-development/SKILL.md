---
name: rfc-driven-development
description: RFC 驱动的开发工作流：读取 RFC 文档 → 代码开发 → 测试用例编写 → 测试验证 → 自动创建分支提交。Use when user mentions RFC, RFC-XXXX, or asks to develop based on RFC/design documents.
---

# RFC 驱动开发工作流

## 概述

当用户要求根据 RFC 文档进行开发时，执行以下标准化工作流。

## 工作流程

### 阶段 1：理解需求

1. 读取 RFC 文档（如 `doc/RFC-XXXX-*.md`）
2. 读取 `doc/ACCEPTANCE.md` 了解验收标准
3. 通读 RFC 的所有验收准则、边界条件和 TODO，不只实现示例代码中的主路径
4. 用 `rg` 查找相似 RFC、相似测试、现有 helper、现有 DSL 和相关实现入口
5. 分析设计要求，确定需要修改的文件和代码

#### RFC 实现前检查清单

- **分清层次**：确认功能属于模块基础数据生成、运行时求解、运行时校验、测试辅助工具中的哪一层，避免把规则定义、测试用例和销售/调用场景混在一起。
- **产品侧优先声明式**：如果 RFC 要求产品或模块通过注解、Schema、配置生成基础数据，测试模型也必须使用同一入口声明规则，不要在测试方法里临时拼装内部执行结构来代替产品声明。
- **接口语义对齐**：逐项核对 RFC 中列出的 public API。若 RFC 同时要求生成和校验，就必须同时覆盖原有生成接口（如 `inferParas` / `InfoParse` 风格入口）和校验接口（如 `validate`），不能只验证其中一条路径。
- **表达面向用户**：注解和 DSL 示例应尽量使用业务/界面友好的表达，例如 `expr1 = "cpu.CoreNum=8"`；只有内部实现真正需要时才暴露拆散的 IT 字段。
- **避免重复配置**：如果信息能从子规则、`parentRuleCode`、表达式或上下文推导，优先由解析器推导，不要求用户在注解域重复填写 pragma、维度、子规则列表等样板字段。
- **先发现接口再新增接口**：实现前必须查找现有 helper、DSL、builder、validator 和测试基类。若现有入口能表达，不新增局部封装。
- **RFC 与代码偏差审查**：RFC 可能滞后于当前代码。实现前核对 RFC 示例方法名、DTO 字段、测试写法是否仍符合当前代码库。

#### 实现前知识卡片

动手前先在思考中填完这张卡片；如果填不出来，继续侦察代码库，不要直接实现：

```text
相似 RFC：
相似测试：
现有 helper：
现有 DSL：
现有注解/生成规则：
需要新增的字段：
可以推导的字段：
需要新增的生产类：
需要新增的测试 helper：
可能误用的旧逻辑：
回归测试范围：
用户偏好检查：
```

#### 最小新增代码挑战

每个新增字段、DTO、helper 或封装都必须能回答：

- 这是业务输入，还是能由上下文稳定推导。
- 现有 DSL、测试基类或运行态模型是否已经能承载。
- 新增内容是否只是为了测试方便。
- 若不新增，会造成什么真实复杂度或重复。

能推导的 code 不新增字段；现有测试框架能表达的输入，不新增测试局部 helper。

### 阶段 2：代码开发

1. 根据 RFC 设计实现代码
2. 遵循项目代码规范（参见 `CLAUDE.md`）
3. 确保 Lombok 注解处理正确
4. Java 源码级别：21，最大行长度：120
5. 代码实现优先贴近现有模块边界，不为了 RFC 示例伪代码强行新增抽象

### 阶段 3：测试验证

1. 编写测试用例（参考 `src/test/java/com/jmix/scenario/`）
2. 使用注解驱动方式定义测试模块：
   - `@ModuleAnno`, `@ParaAnno`, `@PartAnno`
   - `@CodeRuleAnno`, `@CompatiableRuleAnno`, `@PriorityRuleAnno`
3. 优先复用测试基类已有封装，例如参数推理、推荐、校验、结果断言等 helper；只有基类没有合适抽象时才在测试类内新增辅助方法。
4. 单个测试方法保持短小，业务逻辑尽量表现为 1-3 行断言或调用；复杂构造应封装到测试基类或专用 helper。
5. 为 RFC 中每个枚举值、关系类型、正反场景和边界场景补测试。若一个测试类已经过大，应新增独立测试类覆盖其他场景。
6. 推荐/求解类测试默认调用 `printSimpleSolutions()`，方便调试看所有解。
7. 运行测试验证：
   ```bash
   mvn test -Dtest=YourTestClass
   ```
8. 运行相关回归测试，至少覆盖新测试类、被改动功能的既有测试类和注解/生成入口测试：
   ```bash
   mvn test "-Dtest=NewFeatureTest,RelatedExistingTest,AnnotationRuleTest"
   ```
9. 确保现有测试也能通过：
   ```bash
   mvn test
   ``` 

#### 项目测试惯例检查

在本项目中，RFC 测试尤其要检查：

- 先用 `rg inferRecommendModule src/test/java` 查找相似写法。
- 推荐/求解入口优先使用 `inferRecommendModule(...)`。
- 单分类输入使用 `category:...`。
- 跨分类或总体输入使用 `category1,category2:...`。
- 不新增 `singleReqs`、`crossTotal` 这类测试局部封装，除非基类无法表达。
- part code 尽量短且有语义；长度足够短时断言直接用 code/short code，不依赖无语义 `PTn`。
- 测试注解只保留本用例需要的信息；未使用的 `price` 等字段不要写。
- 如果字段名能推导 code，不重复写 `@PartAnno(code = "...")`。

### 阶段 4：自动提交

当测试全部通过后：

1. **创建新分支**：
   ```bash
   git checkout -b feature/rfc-XXXX-<short-description>
   ```

2. **提交代码**：
   ```bash
   git add .
   git commit -m "$(cat <<'EOF'
   feat(rfc): implement RFC-XXXX feature name
   
   - 根据 RFC-XXXX 文档实现功能
   - 添加测试用例
   - 通过验收测试
   
   EOF
   )"
   ```

3. **推送到远程**：
   ```bash
   git push -u origin HEAD
   ```

## 关键约束

- **日志语言**：所有 log.info() 必须使用英文
- **中文注释**：JavaDoc 和内联注释可使用中文
- **不修改原文档**：RFC 文档保持不变
- **向后兼容**：确保现有测试不受影响

## 常见 RFC 文件位置

- 设计文档：`doc/RFC-*.md`
- 验收标准：`doc/ACCEPTANCE.md`
- 核心设计：`doc/CORE-DESIGN.md`
- 测试目录：`src/test/java/com/jmix/scenario/`

## 提交消息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

Type 类型：
- `feat`：新功能
- `fix`：错误修复
- `refactor`：重构
- `test`：测试相关
- `docs`：文档更新
