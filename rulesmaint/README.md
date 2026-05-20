# Rules Maintenance Design Pack

本目录是规则管理维护功能的独立设计包，参考 `doc/RFC-0008-Rule-Model-IR-Refactor.md` 的规则 IR 模型生成。

## 文件

| 文件 | 用途 |
| --- | --- |
| `RFC-0010-Rules-Maintenance-Architecture.md` | 总体架构设计初稿，包含后端、前端、规则编写页、测试闭环和验收准则 |
| `prototype.html` | 规则编写页低保真原型，可直接用浏览器打开 |
| `mock-business-data.json` | 原型和设计文档使用的模拟业务数据 |

## 原型入口

直接打开：

```text
rulesmaint/prototype.html
```

原型重点覆盖两个规则编写场景：

1. 结构化编辑生成 DSL，并自动生成测试用例和运行结果。
2. 自然语言编辑转换 DSL，支持 DSL 反向同步中文描述，并进入同一测试闭环。

## 当前状态

状态：草案
日期：2026-05-20

