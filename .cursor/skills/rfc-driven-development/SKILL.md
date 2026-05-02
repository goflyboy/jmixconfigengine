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
3. 分析设计要求，确定需要修改的文件和代码

### 阶段 2：代码开发

1. 根据 RFC 设计实现代码
2. 遵循项目代码规范（参见 `CLAUDE.md`）
3. 确保 Lombok 注解处理正确
4. Java 源码级别：21，最大行长度：120

### 阶段 3：测试验证

1. 编写测试用例（参考 `src/test/java/com/jmix/scenario/`）
2. 使用注解驱动方式定义测试模块：
   - `@ModuleAnno`, `@ParaAnno`, `@PartAnno`
   - `@CodeRuleAnno`, `@CompatiableRuleAnno`, `@PriorityRuleAnno`
3. 运行测试验证：
   ```bash
   mvn test -Dtest=YourTestClass
   ```
4. 确保现有测试也能通过：
   ```bash
   mvn test
   ```

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
