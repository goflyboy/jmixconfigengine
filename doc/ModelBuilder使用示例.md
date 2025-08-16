# ModelBuilder 使用示例

`ModelBuilder` 是一个统一的模型构建器类，支持流式创建各种模型对象，减少类的数量，提供统一的构建接口。

## 🏗️ 类结构

```
ModelBuilder
├── ParaOptionBuilder    # 参数选项构建器
├── PartBuilder         # 部件构建器
└── ParaBuilder         # 参数构建器
```

## 📝 使用示例

### 1. ParaOption 构建示例

#### 基础构建
```java
ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .codeId(10)
    .code("Red")
    .description("红色选项")
    .sortNo(1)
    .build();
```

#### 使用便捷方法
```java
ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .asOption(10, "Red", "红色选项")
    .sortNo(1)
    .build();
```

#### 动态添加扩展属性
```java
ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .asOption(20, "Black", "黑色选项")
    .extAttr("hexCode", "#000000")
    .extAttr("popularity", "veryHigh")
    .extAttr("category", "primary")
    .extAttr("season", "all")
    .build();
```

#### 批量设置扩展属性
```java
Map<String, String> extAttrs = new HashMap<>();
extAttrs.put("hexCode", "#FF0000");
extAttrs.put("popularity", "high");
extAttrs.put("category", "primary");

ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .asOption(30, "White", "白色选项")
    .extAttrs(extAttrs)
    .build();
```

### 2. Part 构建示例

#### 基础构建
```java
Part part = ModelBuilder.PartBuilder.create()
    .code("TShirt11")
    .description("T恤衫主体部件")
    .sortNo(1)
    .type(PartType.ATOMIC)
    .price(1500L)
    .build();
```

#### 使用便捷方法
```java
Part part = ModelBuilder.PartBuilder.create()
    .asTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g")
    .build();
```

#### 动态添加扩展属性
```java
Part part = ModelBuilder.PartBuilder.create()
    .asTShirtPart("TShirt12", "T恤衫装饰部件", 2, 500L, "polyester", "50g")
    .extAttr("category", "decoration")
    .extAttr("position", "chest")
    .extAttr("size", "10x8cm")
    .build();
```

#### 添加规格属性
```java
Part part = ModelBuilder.PartBuilder.create()
    .asTShirtPart("TShirt11", "T恤衫主体部件", 1, 1500L, "cotton", "180g")
    .attr("fabric", "100%棉")
    .attr("thickness", "中等")
    .attr("elasticity", "适中")
    .build();
```

### 3. Para 构建示例

#### 基础构建
```java
Para para = ModelBuilder.ParaBuilder.create()
    .code("Color")
    .description("T恤衫颜色选择参数")
    .sortNo(1)
    .type(ParaType.ENUM)
    .build();
```

#### 使用便捷方法
```java
Para para = ModelBuilder.ParaBuilder.create()
    .asColorPara("Color", "T恤衫颜色选择参数", 1)
    .build();
```

#### 动态添加扩展属性
```java
Para para = ModelBuilder.ParaBuilder.create()
    .asColorPara("Size", "T恤衫尺寸选择参数", 2)
    .extAttr("measurement", "EU")
    .extAttr("fit", "regular")
    .extAttr("category", "dimension")
    .build();
```

## 🔧 高级用法

### 1. 链式调用
```java
ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .codeId(100)
    .code("Test")
    .description("测试选项")
    .sortNo(10)
    .extAttr("key1", "value1")
    .extAttr("key2", "value2")
    .extAttr("key3", "value3")
    .build();
```

### 2. 带默认值的构建
```java
ParaOption option = ModelBuilder.ParaOptionBuilder.create()
    .codeId(15)
    .code("Yellow")
    .description("黄色选项")
    .buildWithDefaults(); // 自动设置defaultValue和sortNo
```

### 3. 在测试数据中的使用
```java
List<ParaOption> colorOptions = Arrays.asList(
    ModelBuilder.ParaOptionBuilder.create()
        .asOption(10, "Red", "红色")
        .sortNo(1)
        .extAttr("hexCode", "#FF0000")
        .extAttr("popularity", "high")
        .build(),
    ModelBuilder.ParaOptionBuilder.create()
        .asOption(20, "Black", "黑色")
        .sortNo(2)
        .extAttr("hexCode", "#000000")
        .extAttr("popularity", "veryHigh")
        .build(),
    ModelBuilder.ParaOptionBuilder.create()
        .asOption(30, "White", "白色")
        .sortNo(3)
        .extAttr("hexCode", "#FFFFFF")
        .extAttr("popularity", "high")
        .build()
);
```

## 💡 设计优势

### 1. **统一接口**
- 所有构建器都在一个类中，减少类的数量
- 统一的创建方式：`ModelBuilder.XxxBuilder.create()`

### 2. **流式API**
- 支持链式调用，代码更简洁
- 每个方法都返回构建器实例

### 3. **动态扩展属性**
- 支持 `.extAttr("key", "value")` 的链式调用
- 可以动态添加任意数量的扩展属性

### 4. **便捷方法**
- 提供常用的构建模式
- 减少重复代码

### 5. **类型安全**
- 编译时类型检查
- IDE自动补全支持

## 🚀 最佳实践

### 1. **扩展属性命名**
```java
// ✅ 推荐：使用有意义的键名
.extAttr("hexCode", "#FF0000")
.extAttr("popularity", "high")
.extAttr("category", "primary")

// ❌ 避免：使用无意义的键名
.extAttr("attr1", "value1")
.extAttr("attr2", "value2")
```

### 2. **链式调用顺序**
```java
// ✅ 推荐：逻辑清晰的顺序
ModelBuilder.ParaOptionBuilder.create()
    .asOption(10, "Red", "红色")  // 基础信息
    .sortNo(1)                     // 排序信息
    .extAttr("hexCode", "#FF0000") // 扩展属性
    .extAttr("popularity", "high")
    .build();

// ❌ 避免：混乱的顺序
ModelBuilder.ParaOptionBuilder.create()
    .extAttr("popularity", "high")
    .codeId(10)
    .extAttr("hexCode", "#FF0000")
    .code("Red")
    .build();
```

### 3. **批量属性设置**
```java
// ✅ 推荐：使用Map批量设置
Map<String, String> extAttrs = new HashMap<>();
extAttrs.put("hexCode", "#FF0000");
extAttrs.put("popularity", "high");
extAttrs.put("category", "primary");

.extAttrs(extAttrs)

// ✅ 也推荐：链式设置（当属性较少时）
.extAttr("hexCode", "#FF0000")
.extAttr("popularity", "high")
.extAttr("category", "primary")
```

## 📚 相关类

- `ParaOption`: 参数选项模型
- `Part`: 部件模型
- `Para`: 参数模型
- `Extensible`: 可扩展基类
- `ProgramableObject`: 可编程对象基类

---

**记住**: ModelBuilder 提供了统一的构建接口，支持流式调用和动态扩展属性，让对象创建更加简洁和灵活！ 