# T恤衫模块样例数据说明

## 概述

本文档描述了T恤衫配置模块的样例数据结构，该样例数据完全符合约束规则生成系统的设计规范。

## 数据结构说明

### 1. 模块基本信息 (Module)

| 字段 | 类型 | 说明 | 示例值 |
|------|------|------|--------|
| code | String | 模块编码 | "TShirt" |
| id | Long | 模块ID | 123123 |
| version | String | 版本号 | "V1.0" |
| type | String | 模块类型 | "General" |
| defaultValue | Integer | 默认值 | 1 |
| description | String | 描述信息 | "T恤衫配置模块，支持颜色、尺寸选择和部件数量约束" |
| sortNo | Integer | 排序号 | 1 |
| extSchema | String | 扩展属性Schema | "TShirtModuleSchema" |
| extAttrs | Map | 扩展属性 | {"category": "clothing", "season": "all"} |

### 2. 参数定义 (Para)

每个参数包含以下主要字段：

- **基础信息**: code, fatherCode, type, defaultValue, description, sortNo
- **扩展信息**: extSchema, extAttrs
- **选项列表**: options (仅枚举类型参数)

#### 参数类型 (ParaType)

- **EnumType**: 枚举类型，如颜色、尺寸
- **Boolean**: 布尔类型
- **Integer**: 整数类型
- **Float**: 浮点类型
- **Double**: 双精度类型
- **String**: 字符串类型
- **Range**: 范围类型
- **Date**: 日期类型
- **MultiEnum**: 多枚举类型
- **Group**: 分组类型

### 3. 部件定义 (Part)

每个部件包含以下主要字段：

- **基础信息**: code, fatherCode, type, defaultValue, description, sortNo
- **扩展信息**: extSchema, extAttrs
- **业务属性**: price (价格), attrs (规格属性)

#### 部件类型 (PartType)

- **AtomicPart**: 原子部件
- **PartCategory**: 部件分类
- **Bundle**: 部件包
- **Group**: 部件分组

### 4. 规则定义 (Rule)

每个规则包含以下主要字段：

- **基础信息**: code, name, progObjType, progObjCode, progObjField
- **规则内容**: normalNaturalCode, rawCode, ruleSchema
- **扩展信息**: extSchema, extAttrs

#### 规则类型 (RuleSchema)

- **CDSL.V5.Struct.CompatiableRule**: 兼容性规则
- **CDSL.V5.Struct.CalculateRule**: 计算规则
- **CDSL.V5.Struct.SelectRule**: 选择规则

## 样例数据特点

### 1. 完整性
- 包含了所有必需的基础字段
- 提供了完整的扩展属性示例
- 展示了多种规则类型的应用

### 2. 实用性
- 颜色和尺寸的兼容性约束
- 部件数量的计算关系
- 参数选择的限制规则

### 3. 扩展性
- 支持自定义扩展属性
- 灵活的Schema定义
- 可配置的规则优先级

## 使用说明

### 1. 数据验证
确保JSON数据符合以下要求：
- 所有必需字段都已填写
- 字段类型正确
- 引用关系一致

### 2. 规则解析
系统会根据ruleSchema字段自动识别规则类型：
- 兼容性规则：处理参数间的依赖关系
- 计算规则：处理数值计算关系
- 选择规则：处理选项选择逻辑

### 3. 代码生成
使用ModuleAlgArtifactGenerator可以：
- 解析Module对象
- 生成对应的约束算法代码
- 输出可运行的Java类文件

## 扩展建议

### 1. 新增参数类型
可以在extAttrs中添加业务相关的属性，如：
- 库存状态
- 价格区间
- 适用季节

### 2. 复杂规则组合
可以组合多种规则类型，实现复杂的业务逻辑：
- 多条件兼容性检查
- 复杂的计算表达式
- 动态选择逻辑

### 3. 性能优化
通过extAttrs添加性能相关的配置：
- 缓存策略
- 计算优先级
- 验证级别

## 注意事项

1. **字段命名**: 遵循驼峰命名规范
2. **数据类型**: 确保与Java代码中的类型定义一致
3. **引用关系**: 注意fatherCode的正确性
4. **规则表达式**: rawCode中的表达式语法需要符合系统要求
5. **扩展属性**: 避免与系统保留字段冲突 