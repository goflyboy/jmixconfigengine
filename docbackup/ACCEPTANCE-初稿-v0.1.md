# JMix Config Engine - 验收准则 (初稿 v0.1)

> 生成时间：2026-04-24
> 状态：初稿，待讨论

## 1. 概述

本文档定义了 JMix Config Engine 项目的验收准则，用于确保项目满足预期的功能、性能和质量要求。

**目标读者**：后端开发人员、项目经理、测试工程师

**验收级别**：
- ✅ **L1 (必须通过)**：核心功能验收
- ✅ **L2 (建议通过)**：高级功能验收
- ✅ **L3 (可选)**：扩展功能验收

---

## 2. 功能验收标准

### 2.1 核心能力验收

| ID | 验收项 | 验收条件 | 测试用例 | 级别 |
|----|--------|----------|----------|------|
| F-001 | 约束求解 | 给定有效的约束集，求解器能返回至少一个可行解或正确报告无解 | `MyTShirtTest` | L1 |
| F-002 | 参数推理 | 输入部分参数，能正确推导出其他参数的合理值 | `MyTShirtTest` | L1 |
| F-003 | 无解检测 | 当约束无解时，返回明确的无解状态和诊断信息 | `MyTShirtTest` | L1 |
| F-004 | 多解枚举 | 支持枚举所有可行解 | `inferParas( enumerateAllSolution=true )` | L2 |

### 2.2 规则类型验收

| ID | 规则类型 | 验收条件 | 测试用例 | 级别 |
|----|----------|----------|----------|------|
| F-101 | Requires | A→B 约束：A选中时B必须选中 | `CompatibleRuleRequireTest` | L1 |
| F-102 | Incompatible | ¬(A∧B) 约束：A和B不能同时选中 | `CompatibleRuleIncompatibleTest` | L1 |
| F-103 | CoDependent | A↔B 约束：A和B必须同选或同不选 | `CompatibleRuleCodependentTest` | L1 |
| F-104 | Calculate-IfThen | if-then 计算规则正确执行 | `CalculateRuleIfThenTest` | L1 |
| F-105 | Calculate-Simple | 简单计算规则正确执行 | `CalculateRuleSimpleTest` | L1 |
| F-106 | Priority-MAX | 最大化目标函数 | `DynMultReq4MultiReqTest` | L2 |
| F-107 | Priority-MIN | 最小化目标函数 | `DynMultReq4MultiReqTest` | L2 |

### 2.3 多实例支持验收

| ID | 验收项 | 验收条件 | 测试用例 | 级别 |
|----|--------|----------|----------|------|
| F-201 | 单实例分类 | 单实例 PartCategory 正确处理 | `ParaIntegerTest` | L1 |
| F-202 | 多实例分类 | supportMultiInst=true 时正确处理多实例 | `DynMultReq4MultiReqTest` | L1 |
| F-203 | 动态实例数 | 实例数量根据输入动态确定 | `DynMultReq4MultiReqTest` | L1 |
| F-204 | 跨实例约束 | 约束可跨多个实例进行计算 | `DynMultReq4MultiReqTest` | L2 |

### 2.4 冲突诊断验收

| ID | 验收项 | 验收条件 | 测试用例 | 级别 |
|----|--------|----------|----------|------|
| F-301 | 冲突检测 | 正确识别冲突规则 | `ConfictDebugTest` | L2 |
| F-302 | 冲突定位 | 报告冲突发生的规则位置 | `ConfictDebugTest` | L2 |
| F-303 | IfThen冲突 | 正确处理 if-then 结构的冲突 | `ConfictDebugIfThenTest` | L2 |

---

## 3. 性能验收标准

### 3.1 求解性能

| ID | 指标 | 验收条件 | 测试用例 | 级别 |
|----|------|----------|----------|------|
| P-001 | 小规模求解时间 | 变量数 < 100 的问题，求解时间 < 1s | 基准测试 | L1 |
| P-002 | 中规模求解时间 | 变量数 100-1000 的问题，求解时间 < 60s | 基准测试 | L2 |
| P-003 | 超时控制 | 支持用户配置求解超时时间 | 配置项 | L1 |

### 3.2 内存使用

| ID | 指标 | 验收条件 | 级别 |
|----|------|----------|------|
| P-101 | 峰值内存 | 单次求解峰值内存 < 512MB | L1 |
| P-102 | 内存泄漏 | 连续 1000 次求解无内存泄漏 | L2 |

---

## 4. 接口验收标准

### 4.1 北向接口

| ID | 接口 | 验收条件 | 级别 |
|----|------|----------|------|
| I-001 | `ModuleConstraintExecutor.init()` | 正确初始化执行器 | L1 |
| I-002 | `ModuleConstraintExecutor.addModule()` | 正确注册模块 | L1 |
| I-003 | `ModuleConstraintExecutor.inferParas()` | 正确执行推理请求 | L1 |
| I-004 | `ModuleConstraintExecutor.removeModule()` | 正确移除模块 | L2 |

### 4.2 配置接口

| ID | 配置项 | 验收条件 | 级别 |
|----|--------|----------|------|
| I-101 | `ConstraintConfig.loadType` | 支持全量加载和差量加载 | L1 |
| I-102 | `ConstraintConfig.debugByRelaxVar` | 启用冲突诊断模式 | L2 |
| I-103 | `ConstraintConfig.timeLimit` | 支持超时配置 | L1 |

---

## 5. 集成验收标准

### 5.1 依赖组件

| ID | 组件 | 验收条件 | 级别 |
|----|------|----------|------|
| INT-001 | Google OR-Tools | CP-SAT 求解器正确集成 | L1 |
| INT-002 | FreeMarker | 模板引擎正常工作 | L2 |
| INT-003 | Maven | 项目可正常构建 | L1 |

### 5.2 构建验收

| ID | 验收项 | 验收条件 | 级别 |
|----|--------|----------|------|
| INT-101 | 编译通过 | `mvn clean compile` 成功 | L1 |
| INT-102 | 测试通过 | `mvn test` 所有测试通过 | L1 |
| INT-103 | 打包成功 | `mvn package` 生成 jar | L1 |

---

## 6. 代码生成验收

| ID | 验收项 | 验收条件 | 测试用例 | 级别 |
|----|--------|----------|----------|------|
| CG-001 | 单文件生成 | 生成的 Java 代码可编译执行 | `SingleFilePackerTest` | L2 |
| CG-002 | 多文件生成 | 多文件打包正确 | `MultiFilePackerTest` | L2 |
| CG-003 | 代码正确性 | 生成的代码逻辑与手写代码等价 | 对比测试 | L2 |

---

## 7. 文档验收标准

| ID | 验收项 | 验收条件 | 级别 |
|----|--------|----------|------|
| DOC-001 | API 文档 | 核心接口有 Javadoc | L1 |
| DOC-002 | 使用指南 | README 包含快速开始指南 | L1 |
| DOC-003 | 架构文档 | CORE-DESIGN.md 完整描述架构 | L2 |

---

## 8. 验收测试矩阵

### 8.1 测试覆盖

| 模块 | 单元测试 | 集成测试 | 场景测试 |
|------|----------|----------|----------|
| bmodel | ✅ | - | - |
| impl | ✅ | ✅ | - |
| algmodel | ✅ | ✅ | - |
| cmodel | ✅ | - | - |
| scenario | - | ✅ | ✅ |

### 8.2 场景覆盖

| 场景 | 测试用例 | 状态 |
|------|----------|------|
| T恤配置 | `MyTShirtTest` | ✅ |
| 硬盘配置 | `DynMultReq4MultiReqTest` | ✅ |
| 冲突调试 | `ConfictDebugTest` | ✅ |
| 代码生成 | `ModuleAlgArtifactGeneratorTest` | ✅ |

---

## 9. 验收流程

1. **构建验证**：执行 `mvn clean test`
2. **功能验证**：逐项检查功能验收标准
3. **性能验证**：运行基准测试
4. **文档验证**：检查必要文档完整性
5. **问题记录**：记录未通过项并制定修复计划

---

## 10. 术语表

| 术语 | 定义 |
|------|------|
| L1 | 必须通过的核心验收项 |
| L2 | 建议通过的高级验收项 |
| L3 | 可选的扩展验收项 |
| CP-SAT | Constrained Programming - Satisfiability |
| Module | 约束模型的根容器 |
| PartCategory | 部件分类，支持嵌套和多实例 |
