# JMix Config Engine (配置引擎)

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 📖 项目简介

JMix Config Engine 是一个基于Java的约束规则生成系统，专门用于根据配置数据自动生成约束算法代码。该系统使用OR-Tools约束求解器，通过FreeMarker模板引擎生成可运行的Java代码，适用于产品配置、规则引擎等场景。

## ✨ 主要特性

- 🔧 **智能代码生成**: 根据配置数据自动生成约束算法代码
- 🎯 **多种规则支持**: 支持兼容性规则、计算规则、选择规则等
- 📊 **灵活数据模型**: 支持模块、参数、部件、规则等复杂数据结构
- 🚀 **高性能求解**: 集成Google OR-Tools约束求解器
- 🎨 **模板化生成**: 使用FreeMarker模板引擎，支持自定义代码模板
- 🔌 **可扩展架构**: 支持扩展属性和自定义Schema

## 🏗️ 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   配置数据      │ -> │   解析引擎      │ -> │   代码生成器    │
│   (JSON/对象)   │    │   (数据验证)    │    │   (模板渲染)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │   约束求解器    │    │   生成的代码    │
                       │   (OR-Tools)    │    │   (Java类)      │
                       └─────────────────┘    └─────────────────┘
```

## 🛠️ 技术栈

- **Java 8+**: 核心开发语言
- **Maven**: 项目构建和依赖管理
- **OR-Tools**: Google约束求解器
- **FreeMarker**: 模板引擎
- **Lombok**: 代码简化工具
- **Jackson**: JSON数据处理
- **Guava**: Google核心库
- **JUnit**: 单元测试框架

## 📦 项目结构

```
jmixconfigengine/
├── src/main/java/com/jmix/configengine/
│   ├── artifact/          # 代码生成相关类
│   ├── model/             # 数据模型类
│   ├── schema/            # 规则Schema定义
│   ├── util/              # 工具类
│   └── Main.java          # 主程序入口
├── src/main/resources/
│   └── template/          # FreeMarker模板文件
├── src/test/java/         # 测试代码
├── doc/                   # 项目文档
├── pom.xml               # Maven配置文件
└── README.md             # 项目说明文档
```

## 🚀 快速开始

### 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/yourusername/jmixconfigengine.git
   cd jmixconfigengine
   ```

2. **编译项目**
   ```bash
   mvn clean compile
   ```

3. **运行示例**
   ```bash
   mvn exec:java -Dexec.mainClass="com.jmix.configengine.Main"
   ```

4. **运行测试**
   ```bash
   mvn test
   ```

## 📚 使用说明

### 1. 定义配置模块

```java
Module module = new Module();
module.setCode("TShirt");
module.setType(ModuleType.GENERAL);
module.setDescription("T恤配置模块");

// 添加参数
Para colorPara = new Para();
colorPara.setCode("Color");
colorPara.setType(ParaType.ENUM);
// ... 设置其他属性

module.getParas().add(colorPara);
```

### 2. 生成约束代码

```java
ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
String outputPath = "generated/Constraint.java";
generator.buildConstraintRule(module, outputPath);
```

### 3. 使用生成的代码

```java
// 生成的约束类会自动包含验证逻辑
TShirtConstraint constraint = new TShirtConstraint();
boolean isValid = constraint.validateConfiguration(config);
```

## 📋 核心概念

### 模块 (Module)
配置系统的基本单位，包含参数、部件和规则。

### 参数 (Para)
配置项的基本属性，支持多种数据类型：
- 枚举类型 (ENUM)
- 布尔类型 (BOOLEAN)
- 数值类型 (INTEGER, FLOAT, DOUBLE)
- 范围类型 (RANGE)
- 字符串类型 (STRING)

### 部件 (Part)
产品的组成部分，可以是原子部件或部件组合。

### 规则 (Rule)
约束逻辑的定义，支持：
- **兼容性规则**: 参数间的依赖关系
- **计算规则**: 数值计算关系
- **选择规则**: 选项选择逻辑

## 🔧 配置示例

项目包含完整的T恤配置示例，展示了如何定义：
- 颜色和尺寸参数
- 部件数量约束
- 参数兼容性规则
- 计算关系规则

详细示例请参考 `doc/T恤衫模块样例数据.json` 文件。

## 📖 API文档

### 主要类说明

- `Module`: 模块定义类
- `Para`: 参数定义类
- `Part`: 部件定义类
- `Rule`: 规则定义类
- `ModuleAlgArtifactGenerator`: 代码生成器

### 核心方法

- `Module.init()`: 初始化模块映射关系
- `ModuleAlgArtifactGenerator.buildConstraintRule()`: 生成约束代码
- `FilterExpressionExecutor.execute()`: 执行过滤表达式

## 🧪 测试

项目包含完整的单元测试，覆盖核心功能：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=FilterExpressionExecutorTest
```

## 📝 开发指南

### 添加新的参数类型

1. 在 `ParaType` 枚举中添加新类型
2. 在 `Para` 类中添加相应的属性
3. 更新模板文件以支持新类型
4. 添加相应的测试用例

### 自定义代码模板

1. 修改 `src/main/resources/template/` 目录下的模板文件
2. 在 `ModuleAlgArtifactGenerator` 中添加模板处理逻辑
3. 测试生成的代码

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系方式

- 项目主页: [https://github.com/yourusername/jmixconfigengine](https://github.com/yourusername/jmixconfigengine)
- 问题反馈: [Issues](https://github.com/yourusername/jmixconfigengine/issues)
- 讨论交流: [Discussions](https://github.com/yourusername/jmixconfigengine/discussions)

## 🙏 致谢

- [Google OR-Tools](https://developers.google.com/optimization) - 约束求解器
- [FreeMarker](https://freemarker.apache.org/) - 模板引擎
- [Lombok](https://projectlombok.org/) - 代码简化工具

---

⭐ 如果这个项目对您有帮助，请给我们一个星标！ 