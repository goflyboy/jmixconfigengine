# ModelHelperTool 使用指导

## 概述

`ModelHelperTool` 是一个基于AI大模型的约束建模辅助工具，它能够：
- 从Markdown配置文件读取约束模型定义
- 自动调用LLM（DeepSeek/Qwen）生成约束算法代码和测试用例
- 自动编译并运行生成的测试用例
- 支持约束代码的自动注入
- 提供完整的约束模型开发流程

## 快速开始

### 1. 环境准备

确保您的开发环境满足以下要求：
- Java 8 或更高版本
- Maven 或 Gradle 构建工具
- 配置了有效的LLM API密钥（DeepSeek或Qwen）

### 2. LLM API配置

#### 2.1 配置文件方式
在 `src/test/resources/llmmodel.properties` 中配置：

```properties
# DeepSeek配置
deepseek.api.key=your_deepseek_api_key_here
deepseek.api.base.url=https://api.deepseek.com
deepseek.model.name=deepseek-chat

# Qwen配置
qwen.api.key=your_qwen_api_key_here
qwen.api.base.url=https://dashscope.aliyuncs.com/compatible-mode/v1
qwen.model.name=qwen-plus

# 默认模型
default.model=deepseek
```

#### 2.2 环境变量方式
也可以通过环境变量配置：

```bash
# DeepSeek配置
export DEEPSEEK_API_KEY=your_deepseek_api_key_here
export DEEPSEEK_API_BASE_URL=https://api.deepseek.com
export DEEPSEEK_MODEL_NAME=deepseek-chat

# Qwen配置
export QWEN_API_KEY=your_qwen_api_key_here
export QWEN_API_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export QWEN_MODEL_NAME=qwen-plus
```

### 3. 配置文件设置

在 `ModelHelperTool.java` 同级目录下创建 `ModelHelperToolBlockInput.md` 文件：

```markdown
##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
MyTShirt
```

##userVariableModel
```java
@ParaAnno(
    options = {"Red", "Black", "White"}
)
private ParaVar colorVar;

@ParaAnno(
    options = {"Small", "Medium", "Big"}
)
private ParaVar sizeVar;

@PartAnno()
private PartVar tShirt11Var;
```

##userLogicByPseudocode
```java
if(colorVar.value == Red && sizeVar.value == Small) {
    tShirt11Var.qty = 1;
}
else {
    tShirt11Var.qty = 3;
}
```

##userTestCaseSpec
```java
// 用户自定义测试用例特殊规格（可选）
// 如果为空，系统将自动生成测试用例
```

### 4. 运行测试

```bash
cd src/test/java/com/jmix/configengine/scenario/base/modeltool
javac -cp ".:../../../resources" ModelHelperTool.java
java -cp ".:../../../resources" ModelHelperTool
```

## 详细配置说明

### packageName 配置

**用途**: 指定生成的测试类所在的包名
**格式**: 标准的Java包名格式
**示例**: 
```java
com.jmix.configengine.scenario.ruletest
com.jmix.configengine.scenario.custom
com.mycompany.constraints
```

**注意事项**:
- 包名必须符合Java命名规范
- 确保包路径在项目中存在或有权限创建
- 建议使用项目标准的包命名约定

### modelName 配置

**用途**: 指定约束模型的名称，生成的测试类将命名为 `{modelName}Test`
**格式**: 驼峰命名法，首字母大写
**示例**:
```java
MyTShirt
CalculateRule
ProductConstraint
UserPreference
```

**注意事项**:
- 模型名将直接影响生成的类名
- 避免使用Java关键字或特殊字符
- 建议使用描述性的名称

### userVariableModel 配置

**用途**: 定义约束模型的变量结构，包括参数和部件
**格式**: 使用注解定义的Java字段声明

#### 参数变量 (ParaVar)

```java
@ParaAnno(
    options = {"option1", "option2", "option3"}
)
private ParaVar parameterName;
```

**注解说明**:
- `@ParaAnno`: 标识这是一个参数变量
- `options`: 定义参数的可选值数组
- `parameterName`: 参数变量名，建议使用驼峰命名

#### 部件变量 (PartVar)

```java
@PartAnno()
private PartVar partName;

// 或者指定最大数量限制
@PartAnno(maxQuantity = 100)
private PartVar limitedPartName;
```

**注解说明**:
- `@PartAnno`: 标识这是一个部件变量
- `partName`: 部件变量名
- `maxQuantity`: 最大数量限制（可选，默认为Integer.MAX_VALUE）

#### 完整示例

```java
// 颜色参数
@ParaAnno(
    options = {"Red", "Blue", "Green"}
)
private ParaVar colorVar;

// 尺寸参数
@ParaAnno(
    options = {"Small", "Medium", "Large"}
)
private ParaVar sizeVar;

// 产品部件
@PartAnno()
private PartVar ProductVar;

// 限量部件（最大数量为50）
@PartAnno(maxQuantity = 50)
private PartVar LimitedProductVar;
```

### userLogicByPseudocode 配置

**用途**: 定义约束逻辑的伪代码，描述参数和部件之间的关系
**格式**: 类似Java的if-else语句，但使用参数的可选值

#### 基本语法

```java
if(parameter1.value == option1 && parameter2.value == option2) {
    part1.qty = value1;
}
else {
    part1.qty = value2;
}
```

#### 逻辑运算符

- `&&`: 逻辑与
- `||`: 逻辑或
- `==`: 相等比较
- `!=`: 不等比较

#### 完整示例

```java
// 如果颜色是红色且尺寸是小号，则产品数量为1
if(colorVar.value == Red && sizeVar.value == Small) {
    ProductVar.qty = 1;
}
// 否则产品数量为3
else {
    ProductVar.qty = 3;
}
```

### userTestCaseSpec 配置

**用途**: 指定用户自定义的测试用例特殊规格
**格式**: 自然语言描述或伪代码
**示例**:
```java
// 测试用例特殊规格示例
// 1. 测试边界条件：当数量为0时的行为
// 2. 测试异常情况：当参数组合不合法时的处理
// 3. 测试性能：大量参数组合的求解时间
```

**注意事项**:
- 如果为空，系统将根据约束逻辑自动生成测试用例
- 支持自然语言描述，系统会理解并生成相应的测试代码
- 可以指定特定的测试场景和验证点

## 高级用法

### 1. 复杂约束逻辑

```java
// 多条件组合
if(colorVar.value == Red && sizeVar.value == Small) {
    ProductVar.qty = 1;
}
else if(colorVar.value == Blue && sizeVar.value == Medium) {
    ProductVar.qty = 2;
}
else if(colorVar.value == Green && sizeVar.value == Large) {
    ProductVar.qty = 3;
}
else {
    ProductVar.qty = 5;
}
```

### 2. 多部件约束

```java
// 同时约束多个部件
if(colorVar.value == Red) {
    ShirtVar.qty = 1;
    PantsVar.qty = 0;
}
else {
    ShirtVar.qty = 0;
    PantsVar.qty = 1;
}
```

### 3. 数值范围约束

```java
// 数值范围约束
if(QuantityVar.value >= 10 && QuantityVar.value <= 100) {
    DiscountVar.qty = 0.1;
}
else {
    DiscountVar.qty = 0.0;
}
```

### 4. 兼容性规则

```java
// 兼容性规则示例
// 如果两个参数是兼容的，则它们的隐藏状态应该一致
if(CompatibilityRule(colorVar, sizeVar)) {
    colorVar.isHidden == sizeVar.isHidden;
}
```

## 工作流程

### 1. 设计阶段

1. **分析需求**: 理解业务约束规则
2. **识别变量**: 确定参数和部件的类型和可选值
3. **设计逻辑**: 用伪代码描述约束关系
4. **验证逻辑**: 检查约束逻辑的完整性和正确性

### 2. 配置阶段

1. **创建配置文件**: 编写 `ModelHelperToolBlockInput.md`
2. **定义变量模型**: 使用注解定义参数和部件
3. **编写约束逻辑**: 用伪代码描述业务规则
4. **设置包名和模型名**: 确定代码组织方式
5. **配置LLM API**: 设置API密钥和模型参数

### 3. 生成阶段

1. **运行测试**: 执行 `ModelHelperTool.main()`
2. **LLM生成**: 自动调用大模型生成约束算法代码和测试用例
3. **代码编译**: 自动编译生成的Java代码
4. **约束注入**: 自动检测并注入兼容性规则代码
5. **测试运行**: 自动执行生成的测试用例

### 4. 验证阶段

1. **检查输出**: 查看生成的测试文件
2. **运行测试**: 验证约束逻辑的正确性
3. **调试优化**: 根据测试结果调整约束逻辑
4. **文档更新**: 更新配置文件和说明文档

## 核心组件说明

### ModelHelperTool

**职责**: 配置解析和流程控制
- 解析Markdown配置文件
- 协调整个代码生成和测试流程
- 提供默认配置和错误处理机制

**主要方法**:
- `generatorModelFile()`: 生成并运行模型文件
- `injectConstraintCode()`: 注入约束代码
- `readFromMarkdown()`: 从Markdown文件读取配置

### ModelHelper

**职责**: 代码生成和文件管理
- 协调LLM调用和代码生成过程
- 管理生成文件的创建、编译和运行
- 处理约束代码的自动注入

**主要方法**:
- `generatorModelFile()`: 生成模型文件
- `generatorRunModelFile()`: 生成并运行模型文件
- `autoInjectConstraintCode()`: 自动注入约束代码
- `compileJavaFile()`: 编译Java文件
- `runTestFile()`: 运行测试文件

### LLMInvoker

**职责**: 大模型调用和响应处理
- 封装多种LLM模型的调用接口
- 处理提示词构建和响应解析
- 提供统一的代码生成接口

**支持的模型**:
- DeepSeek: 支持深度思考模式，适合复杂约束逻辑
- Qwen: 支持快速响应，适合简单约束场景

**主要方法**:
- `generatorModelCode()`: 生成模型代码
- `cleanGeneratedCode()`: 清理生成的代码
- `ensureProperEncoding()`: 确保编码正确

### PromptTemplateLoader

**职责**: 提示词模板管理
- 加载和渲染JTL格式的模板文件
- 支持变量替换和模板渲染
- 提供便捷的模板调用方法

**主要方法**:
- `loadTemplate()`: 加载模板文件
- `renderTemplate()`: 渲染模板
- `renderJavaCodeTemplate()`: 渲染Java代码生成模板

## 最佳实践

### 1. 命名规范

- **模型名**: 使用描述性的英文名称，如 `ProductConstraint`、`UserPreference`
- **参数名**: 使用驼峰命名，如 `colorVar`、`sizeVar`
- **选项值**: 使用有意义的标识符，如 `Red`、`Small`、`Premium`

### 2. 约束设计

- **逻辑清晰**: 约束逻辑应该简单明了，易于理解
- **覆盖完整**: 确保所有可能的组合都有明确的约束
- **避免冲突**: 检查约束之间是否存在逻辑冲突
- **性能考虑**: 复杂的约束可能影响求解性能

### 3. 测试策略

- **边界测试**: 测试边界条件和异常情况
- **组合测试**: 测试不同参数组合的约束效果
- **回归测试**: 修改约束后重新运行所有测试
- **性能测试**: 测试约束求解的性能表现

### 4. 配置管理

- **及时更新**: 修改约束后及时更新配置文件
- **版本控制**: 使用Git管理配置文件的变更历史
- **注释说明**: 在配置文件中添加必要的注释说明
- **示例丰富**: 提供多种使用场景的示例

### 5. LLM使用

- **模型选择**: 根据约束复杂度选择合适的LLM模型
- **提示词优化**: 在constraint_generate_prompt.jtl中优化提示词模板
- **参数调优**: 根据生成质量调整temperature等参数
- **错误处理**: 处理LLM调用失败和生成错误的情况

## 故障排除

### 常见问题

#### 1. 配置文件读取失败

**症状**: 控制台显示"读取Markdown文件失败"
**原因**: 文件路径错误或文件不存在
**解决方案**: 
- 检查 `ModelHelperToolBlockInput.md` 文件是否在正确位置
- 确认文件名拼写正确
- 检查文件权限

#### 2. LLM调用失败

**症状**: 显示"LLM调用失败"
**原因**: API密钥配置错误或网络问题
**解决方案**:
- 检查API密钥配置
- 确认网络连接正常
- 查看API服务状态
- 检查配置文件格式

#### 3. 代码编译失败

**症状**: 显示"Java文件编译失败"
**原因**: 生成的代码存在语法错误
**解决方案**:
- 检查生成的Java代码
- 确认约束逻辑的正确性
- 查看编译错误详情
- 检查依赖包是否正确

#### 4. 测试运行失败

**症状**: 测试方法执行失败
**原因**: 约束逻辑存在逻辑错误
**解决方案**:
- 分析测试失败的原因
- 检查约束逻辑的正确性
- 调整约束规则
- 查看详细的错误日志

#### 5. 约束注入失败

**症状**: 显示"自动注入约束代码失败"
**原因**: 约束类结构不符合注入要求
**解决方案**:
- 检查约束类的注解配置
- 确认规则类型是否正确
- 查看注入过程的详细日志

### 调试技巧

#### 1. 启用详细日志

```java
// 在ModelHelperTool中添加更多日志输出
System.out.println("正在读取配置: " + section);
System.out.println("读取到的内容: " + content);
```

#### 2. 分步验证

```java
// 先测试配置读取
String config = readFromMarkdown("userVariableModel");
System.out.println("配置内容: " + config);

// 再测试代码生成
// 最后测试代码运行
```

#### 3. 使用默认值

```java
// 如果Markdown读取失败，使用硬编码的默认值进行测试
String userVariableModel = getDefaultVariableModel();
String userLogicByPseudocode = getDefaultLogicPseudocode();
```

#### 4. 检查LLM配置

```java
// 检查LLM配置信息
LLMInvoker invoker = new LLMInvoker();
System.out.println(invoker.getConfigInfo());
```

## 扩展功能

### 1. 自定义约束类型

您可以扩展 `ModelHelperTool` 支持更多类型的约束：

```java
// 支持数值范围约束
if(MinValue <= parameter.value && parameter.value <= MaxValue) {
    // 约束逻辑
}

// 支持集合约束
if(parameter.value in {"option1", "option2", "option3"}) {
    // 约束逻辑
}
```

### 2. 集成CI/CD

将 `ModelHelperTool` 集成到持续集成流程中：

```yaml
# GitHub Actions 示例
- name: Run Constraint Tests
  run: |
    cd src/test/java/com/jmix/configengine/scenario/base/modeltool
    javac -cp ".:../../../resources" ModelHelperTool.java
    java -cp ".:../../../resources" ModelHelperTool
```

### 3. 批量测试

支持批量运行多个约束模型：

```java
// 支持从目录读取多个配置文件
File[] configFiles = new File("configs").listFiles();
for (File configFile : configFiles) {
    if (configFile.getName().endsWith(".md")) {
        runModelTest(configFile);
    }
}
```

### 4. 自定义提示词模板

修改 `constraint_generate_prompt.jtl` 文件来自定义代码生成规则：

```jtl
# 自定义提示词模板
## 基本信息
- **包名**: ${packageName}
- **模型名称**: ${modelName}
- **变量模型描述**: ${userVariableModel}
- **逻辑伪代码**: ${userLogicByPseudocode}
- **用户测试用例特殊规格**: ${userTestCaseSpec}

## 自定义要求
// 添加您的自定义要求
```

## 总结

`ModelHelperTool` 提供了一个完整的基于AI的约束建模开发和测试解决方案：

1. **配置驱动**: 通过Markdown文件管理约束模型配置
2. **AI生成**: 使用LLM自动生成约束算法代码和测试用例
3. **自动化运行**: 自动编译和运行生成的测试用例
4. **智能注入**: 自动检测并注入兼容性规则代码
5. **灵活扩展**: 支持自定义约束类型和测试策略

通过遵循本指导文档，您可以：
- 快速上手基于AI的约束建模开发
- 提高开发效率和代码质量
- 建立标准化的约束模型开发流程
- 支持团队协作和知识共享

如有问题或建议，请参考故障排除部分或联系开发团队。