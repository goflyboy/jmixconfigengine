# ModelHelperTest 使用指导

## 概述

`ModelHelperTest` 是一个强大的约束模型测试工具，它能够：
- 从Markdown配置文件读取约束模型定义
- 自动调用LLM生成测试代码
- 自动编译并运行生成的测试用例
- 支持完整的约束模型开发流程

## 快速开始

### 1. 环境准备

确保您的开发环境满足以下要求：
- Java 8 或更高版本
- Maven 或 Gradle 构建工具
- 配置了有效的LLM API密钥（DeepSeek或Qwen）

### 2. 配置文件设置

在 `ModelHelperTest.java` 同级目录下创建 `ModelHelperTestBlockInput.md` 文件：

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
private ParaVar ColorVar;

@ParaAnno(
    options = {"Small", "Medium", "Big"}
)
private ParaVar SizeVar;

@PartAnno()
private PartVar TShirt11Var;
```

##userLogicByPseudocode
```java
// "Red-10", "Black-20", "White-30"
// "Small-10", "Medium-20", "Big-30"
if(ColorVar.var == Red && SizeVar.var == Small) {
    TShirt11Var.var = 1;
}
else {
    TShirt11Var.var = 3;
}
```
```

### 3. 运行测试

```bash
cd src/test/java/com/jmix/configengine/scenario/base/modeltool
javac -cp ".:../../../resources" ModelHelperTest.java
java -cp ".:../../../resources" ModelHelperTest
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
private ParaVar ColorVar;

// 尺寸参数
@ParaAnno(
    options = {"Small", "Medium", "Large"}
)
private ParaVar SizeVar;

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
if(parameter1.var == option1 && parameter2.var == option2) {
    part1.var = value1;
}
else {
    part1.var = value2;
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
if(ColorVar.var == Red && SizeVar.var == Small) {
    ProductVar.var = 1;
}
// 否则产品数量为3
else {
    ProductVar.var = 3;
}
```

## 高级用法

### 1. 复杂约束逻辑

```java
// 多条件组合
if(ColorVar.var == Red && SizeVar.var == Small) {
    ProductVar.var = 1;
}
else if(ColorVar.var == Blue && SizeVar.var == Medium) {
    ProductVar.var = 2;
}
else if(ColorVar.var == Green && SizeVar.var == Large) {
    ProductVar.var = 3;
}
else {
    ProductVar.var = 5;
}
```

### 2. 多部件约束

```java
// 同时约束多个部件
if(ColorVar.var == Red) {
    ShirtVar.var = 1;
    PantsVar.var = 0;
}
else {
    ShirtVar.var = 0;
    PantsVar.var = 1;
}
```

### 3. 数值范围约束

```java
// 数值范围约束
if(QuantityVar.var >= 10 && QuantityVar.var <= 100) {
    DiscountVar.var = 0.1;
}
else {
    DiscountVar.var = 0.0;
}
```

### 4. 数量限制约束

```java
// 使用maxQuantity限制部件数量
@PartAnno(maxQuantity = 10)
private PartVar LimitedPartVar;

// 约束逻辑：如果数量超过限制，则设置为最大值
if(SomeCondition) {
    LimitedPartVar.var = Math.min(calculatedValue, 10);
}
else {
    LimitedPartVar.var = 0;
}
```

## 工作流程

### 1. 设计阶段

1. **分析需求**: 理解业务约束规则
2. **识别变量**: 确定参数和部件的类型和可选值
3. **设计逻辑**: 用伪代码描述约束关系
4. **验证逻辑**: 检查约束逻辑的完整性和正确性

### 2. 配置阶段

1. **创建配置文件**: 编写 `ModelHelperTestBlockInput.md`
2. **定义变量模型**: 使用注解定义参数和部件
3. **编写约束逻辑**: 用伪代码描述业务规则
4. **设置包名和模型名**: 确定代码组织方式

### 3. 生成阶段

1. **运行测试**: 执行 `ModelHelperTest`
2. **LLM生成**: 自动调用大模型生成测试代码
3. **代码编译**: 自动编译生成的Java代码
4. **测试运行**: 自动执行生成的测试用例

### 4. 验证阶段

1. **检查输出**: 查看生成的测试文件
2. **运行测试**: 验证约束逻辑的正确性
3. **调试优化**: 根据测试结果调整约束逻辑
4. **文档更新**: 更新配置文件和说明文档

## 最佳实践

### 1. 命名规范

- **模型名**: 使用描述性的英文名称，如 `ProductConstraint`、`UserPreference`
- **参数名**: 使用驼峰命名，如 `ColorVar`、`SizeVar`
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

### 4. 文档维护

- **及时更新**: 修改约束后及时更新配置文件
- **版本控制**: 使用Git管理配置文件的变更历史
- **注释说明**: 在配置文件中添加必要的注释说明
- **示例丰富**: 提供多种使用场景的示例

## 故障排除

### 常见问题

#### 1. 配置文件读取失败

**症状**: 控制台显示"读取Markdown文件失败"
**原因**: 文件路径错误或文件不存在
**解决方案**: 
- 检查 `ModelHelperTestBlockInput.md` 文件是否在正确位置
- 确认文件名拼写正确
- 检查文件权限

#### 2. LLM调用失败

**症状**: 显示"LLM调用失败"
**原因**: API密钥配置错误或网络问题
**解决方案**:
- 检查API密钥配置
- 确认网络连接正常
- 查看API服务状态

#### 3. 代码编译失败

**症状**: 显示"Java文件编译失败"
**原因**: 生成的代码存在语法错误
**解决方案**:
- 检查生成的Java代码
- 确认约束逻辑的正确性
- 查看编译错误详情

#### 4. 测试运行失败

**症状**: 测试方法执行失败
**原因**: 约束逻辑存在逻辑错误
**解决方案**:
- 分析测试失败的原因
- 检查约束逻辑的正确性
- 调整约束规则

### 调试技巧

#### 1. 启用详细日志

```java
// 在ModelHelperTest中添加更多日志输出
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

## 扩展功能

### 1. 自定义约束类型

您可以扩展 `ModelHelperTest` 支持更多类型的约束：

```java
// 支持数值范围约束
if(MinValue <= parameter.var && parameter.var <= MaxValue) {
    // 约束逻辑
}

// 支持集合约束
if(parameter.var in {"option1", "option2", "option3"}) {
    // 约束逻辑
}
```

### 2. 集成CI/CD

将 `ModelHelperTest` 集成到持续集成流程中：

```yaml
# GitHub Actions 示例
- name: Run Constraint Tests
  run: |
    cd src/test/java/com/jmix/configengine/scenario/base/modeltool
    javac -cp ".:../../../resources" ModelHelperTest.java
    java -cp ".:../../../resources" ModelHelperTest
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

## 总结

`ModelHelperTest` 提供了一个完整的约束模型开发和测试解决方案：

1. **配置驱动**: 通过Markdown文件管理约束模型配置
2. **自动化生成**: 使用LLM自动生成测试代码
3. **智能运行**: 自动编译和运行生成的测试用例
4. **灵活扩展**: 支持自定义约束类型和测试策略

通过遵循本指导文档，您可以：
- 快速上手约束模型开发
- 提高开发效率和代码质量
- 建立标准化的约束模型开发流程
- 支持团队协作和知识共享

如有问题或建议，请参考故障排除部分或联系开发团队。 