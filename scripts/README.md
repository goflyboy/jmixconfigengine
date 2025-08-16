# JMix Config Engine - 开发工具脚本

本目录包含用于开发和代码质量检查的各种工具脚本。

## 📋 工具列表

### 1. 中文日志检查器 (`check-chinese-logs.py`)

**功能**: 检查Java代码中的中文日志内容

**使用方法**:
```bash
# 在项目根目录执行
python scripts/check-chinese-logs.py

# 或者在scripts目录执行
cd scripts
python check-chinese-logs.py
```

**输出示例**:
```
🔍 JMix Config Engine - Chinese Log Checker
==================================================
Project root: /path/to/jmixconfigengine
Found 15 Java files

❌ src/main/java/com/jmix/configengine/SomeClass.java
   Line 25: 用户认证成功，用户名: admin

==================================================
❌ Found 1 issues in 1 files

📋 Summary:
   - All log messages must be in English
   - Chinese characters are only allowed in comments
   - Please fix these issues before committing
```

### 2. Checkstyle规则文件 (`checkstyle-rules.xml`)

**功能**: Maven Checkstyle插件的配置文件，用于代码质量检查

**集成到Maven**:
在`pom.xml`中添加以下配置：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <configLocation>scripts/checkstyle-rules.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
    </configuration>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**使用方法**:
```bash
# 检查代码风格
mvn checkstyle:check

# 生成报告
mvn checkstyle:checkstyle
```

## 🚀 快速开始

### 1. 安装Python依赖
```bash
# 确保Python 3.6+已安装
python --version

# 如果需要，安装依赖包
pip install pathlib
```

### 2. 运行检查
```bash
# 检查中文日志
python scripts/check-chinese-logs.py

# 如果发现问题，修复后重新检查
python scripts/check-chinese-logs.py
```

### 3. 集成到开发流程
```bash
# 在提交前检查
python scripts/check-chinese-logs.py

# 如果检查通过，继续提交
git add .
git commit -m "feat: add new feature"

# 如果检查失败，修复问题后重新检查
```

## 📊 检查规则说明

### 中文日志检查
- **规则**: 所有日志消息必须使用英文
- **检查范围**: `log.info()`, `log.warn()`, `log.error()`, `log.debug()`, `log.trace()`
- **允许**: 代码注释可以使用中文
- **禁止**: 日志内容使用中文

### 日志格式检查
- **规则**: 避免在日志中使用字符串拼接
- **推荐**: 使用占位符 `{}`
- **示例**:
  ```java
  // ✅ 正确
  log.info("Processing user: {}", username);
  
  // ❌ 错误
  log.info("Processing user: " + username);
  ```

### 日志级别检查
- **规则**: 合理使用日志级别
- **生产环境**: 避免过多DEBUG/TRACE日志
- **开发环境**: 可以使用DEBUG级别进行调试

## 🔧 自定义配置

### 修改检查规则
编辑`check-chinese-logs.py`文件中的`log_patterns`列表：

```python
log_patterns = [
    r'log\.info\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
    r'log\.warn\s*\(\s*["\']([^"\']*[\u4e00-\u9fff][^"\']*)["\']',
    # 添加更多模式...
]
```

### 修改Checkstyle规则
编辑`checkstyle-rules.xml`文件，调整检查规则和严重级别。

## 📝 最佳实践

### 1. 日志消息编写
```java
// ✅ 好的日志消息
log.info("User {} logged in successfully from IP {}", username, ipAddress);
log.warn("Database connection pool {}% full", poolUsage);
log.error("Failed to process request {}: {}", requestId, errorMessage);

// ❌ 不好的日志消息
log.info("用户登录成功");  // 中文
log.warn("数据库连接池满了");  // 中文
log.error("处理请求失败");  // 中文
```

### 2. 异常日志
```java
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("Failed to process user request: {}", requestId, e);
}
```

### 3. 调试日志
```java
log.debug("Entering method: {} with parameters: {}", methodName, parameters);
log.debug("Method execution completed in {} ms", executionTime);
```

## 🚨 常见问题

### Q: 为什么日志必须用英文？
A: 日志是给运维人员和开发者看的，使用英文便于国际团队协作和日志分析工具处理。

### Q: 注释可以用中文吗？
A: 可以！代码注释是给开发者看的，使用中文便于中文开发者理解。

### Q: 如何批量修复中文日志？
A: 使用IDE的查找替换功能，搜索中文日志并替换为对应的英文版本。

### Q: 检查工具可以集成到CI/CD吗？
A: 可以！Python脚本返回非零退出码，Maven插件会在构建失败时停止，便于CI/CD集成。

## 📞 支持

如果遇到问题或有改进建议，请：
1. 检查Python版本和依赖
2. 查看错误信息和日志
3. 提交Issue或Pull Request

---

**记住**: 日志国际化，注释本地化！ 🌍 