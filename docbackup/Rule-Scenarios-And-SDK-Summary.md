# 规则场景与南向 SDK/API 汇总

> 状态：归档稿  
> 日期：2026-06-08  
> 口径：面向“生成规则时需要调用的南向接口”，不再按测试引擎能力做全量综述。

## 1. 范围说明

本文只总结原规则引擎的南向接口和可由规则生成器产出的业务规则场景。南向接口主要指产品算法在 `ModuleAlgBase` 中编写规则时可调用的稳定 API，包括 `model()`、变量 facade、聚合表达式、兼容关系 helper、POST 阶段实例 view 等。

本次不纳入以下内容：

- 自动规则生成/注解链路测试：`src/test/java/com/jmix/tool/autoruletest`
- 冲突诊断与历史调试：`src/test/java/com/jmix/tool/conflictdiagnosis`、`src/test/java/com/jmix/tool/confictdebug*`
- 性能与大规模样例：`src/test/java/com/jmix/scenario/perf20`
- 算法制品/规则信息生成：`src/test/java/com/jmix/tool/artifact`
- 北向执行入口、测试 helper、断言 DSL、Schema 序列化测试

注解如 `@CodeRuleAnno`、`@PartAnno`、`@ParaAnno`、`@PriorityRuleAnno`、`@CombinationStructRuleAnno` 只作为模型和代码生成辅助，不计入“API 依赖”。测试入口如 `inferRecommendModule`、`validData`、`validateData` 也不计入南向 API 依赖。

## 2. 场景因子

后续补测试或补规则生成能力时，建议先按以下因子定位场景。当前测试中缺失的组合在本文中标记为 `TODO`。


| 因子    | 取值                                 | 判定方式                                                                                 |
| ----- | ---------------------------------- | ------------------------------------------------------------------------------------ |
| 层级    | 模块级、部件分类级、部件级                      | 没有设置 `fatherCode` 为空时按模块级处理；`fatherCode` 指向分类时按部件分类级处理；直接绑定具体部件实例或具体 Part 特征时按部件级处理。 |
| 元素相关性 | 参数相关、部件相关、参数+部件混合                  | 看规则主要约束 `ParaVar`、`PartVar`/`PartCategoryVar`，还是两者共同参与。                              |
| 运行方式  | 基于约束模型、基于非约束模型                  | 基于约束模型指进入 CP-SAT/`model()` 构造变量、表达式和约束；基于非约束模型指在已有配置解或实例 view 上后置读取/写回。`CalcStage.POST`/非 POST 是当前运行阶段标记，不等同于运行方式，后续作为遗留任务梳理。 |
| 表达方式  | 结构化、非结构化                           | 结构化是组合/二元/三元等结构表达；非结构化是自由代码或代码生成后的 Java 规则。                                          |
| 文法类型  | if-else、for/遍历、父子层级、线性表达式、结构化二元/三元 | 看生成代码需要的控制结构或表达式形态。                                                                  |
| 业务场景  | 计算赋值类、兼容类、优先类            | 看业务语义，不按测试入口分类；已有用例中的计算、汇总、选择、隐藏统一归入计算赋值类。                                                                      |


## 3. 已有用例的场景矩阵


| 场景                | 层级        | 元素相关性   | 运行方式   | 表达方式       | 文法类型             | 业务场景    | 状态         | 南向 API 依赖                                                                                                                               | 代表测试                                                                  |
| ----------------- | --------- | ------- | ------ | ---------- | ---------------- | ------- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| 参数 if-else 反推部件数量 | 模块级       | 参数+部件混合 | 基于约束模型 | 非结构化       | if-else          | 计算赋值类     | 已覆盖        | `model().newBoolVar`、`addBoolAnd`、`addBoolOr`、`addEquality`、`onlyEnforceIf`、`ParaVar.option(...).selectedVar()`、`PartVar.quantityVar()` | `CalculateRuleSimpleTest`、`CalculateRuleIfThenTest`、`HelloConstraint` |
| 参数 Requires       | 模块级       | 参数相关    | 基于约束模型 | 非结构化       | 普通蕴含             | 兼容类     | 已覆盖        | `addCompatibleConstraintRequires`、或 `model().addImplication`、`ParaVar.option(...).selectedVar()`                                        | `CompatibleRuleRequireTest`                                           |
| 参数 Incompatible   | 模块级       | 参数相关    | 基于约束模型 | 非结构化       | 普通蕴含/互斥          | 兼容类     | 已覆盖        | `addCompatibleConstraintInCompatible`、或 `addBoolAnd`/`addImplication`、`ParaVar.option(...).selectedVar()`                               | `CompatibleRuleIncompatibleTest`                                      |
| 参数 CoDependent    | 模块级       | 参数相关    | 基于约束模型 | 非结构化       | 双向蕴含             | 兼容类     | 已覆盖        | `addCompatibleConstraintCoDependent`、或多组 `model().addImplication`                                                                       | `CompatibleRuleCodependentTest`                                       |
| 部件互斥表达式           | 模块级       | 部件相关    | 基于约束模型 | 非结构化       | 父子层级 + 过滤表达式     | 兼容类     | 已覆盖        | `inCompatible(ruleCode, leftExpr, rightExpr)`、`partVars(filterCondition)`、`PartVar.attr(...)`                                           | `MultiPCTest`、`OptionalPartCategoryWhitelistGuardTest`                |
| 单分类数量/属性汇总        | 部件分类级     | 部件相关    | 基于约束模型 | 非结构化       | 线性表达式            | 计算赋值类     | 已覆盖        | `model().sum4Quantity(attr, filter)`、`model().sum4Selected(filter)`、`addLessOrEqual`、`addGreaterOrEqual`                                | `BaseOptiTest`                                                        |
| 跨分类数量/属性汇总        | 模块级       | 部件相关    | 基于约束模型 | 非结构化       | 线性表达式            | 计算赋值类     | 已覆盖        | `model().sum4Quantity(partCategoryCodes, attr, filter)`、`model().sum4Selected(partCategoryCodes, attr, filter)`、`addGreaterOrEqual`     | `MultiPCTest`、`CrossPartCategoryTotalConstraintTest`                  |
| 多实例分类汇总           | 模块级       | 部件相关    | 基于约束模型 | 非结构化       | for/实例展开 + 线性表达式 | 计算赋值类     | 已覆盖        | `model().sum4Quantity("drive*", attr, filter)`、`model().sum4Selected("drive*", attr, filter)`、`PartCategoryVar.sumSumPara(attr)`        | `DynMultReq4MultiReqTest`、`EnumMultReq4MultiReqTest`                  |
| 单选/至多单选           | 部件分类级     | 部件相关    | 基于约束模型 | 非结构化       | 父子层级             | 计算赋值类     | 已覆盖        | `PartCategoryVar.parts()`、`PartVar.selectedVar()`、`model().addExactlyOne`、`model().addAtMostOne`                                        | `SearchStrategyTest`、`SearchStrategyMultiTest`、`MultiPCTest`          |
| 优先级目标函数           | 部件分类级/模块级 | 部件相关    | 基于约束模型 | 非结构化       | 线性表达式            | 优先类     | 已覆盖        | `model().newPartLinearExpr`、`PartAlgCPLinearExpr.addExpr`、`addConstant`、`model().setObjectExpr`、`updatePriorityObjectFuntion`           | `BaseOptiTest`、`MultiPCTest`、`DynMultReq4MultiReqTest`                |
| 整数参数计算            | 模块级       | 参数+部件混合 | 基于约束模型 | 非结构化       | 线性表达式            | 计算赋值类     | 已覆盖        | `model().newLinearExpr`、`AlgCPLinearExpr.addTerm`、`addEquality`、`ParaVar.valueVar()`、`PartVar.quantityVar()`                            | `ParaIntegerTest`                                                     |
| 参数隐藏逻辑            | 模块级       | 参数相关    | 基于约束模型 | 非结构化       | if-else + 布尔表达式  | 计算赋值类     | 已覆盖        | `ParaVar.hiddenVar()`、`PartVar.hiddenVar()`、`addBoolOr`、`addBoolAnd`、`addDifferent`、`addEquality`、`addVarAboutHiddenConstraints`        | `ParaIsHiddenTest`                                                    |
| CodeRule 校验型组合    | 模块级       | 部件相关    | 基于约束模型 | 非结构化       | 线性表达式            | 兼容类     | 已覆盖        | `model().sum4Selected(partCategoryCodes, attr, filter)`、`model().newPartLinearExpr`、`addLessOrEqual`                                    | `CodeRuleOnlyValidateTest`                                            |
| 结构化二元白名单          | 模块级       | 部件相关    | 基于约束模型 | 结构化        | 结构化二元            | 兼容类     | 已覆盖，生成器可后置 | 执行态由结构化展开为约束；生成代码时可映射到 `sum4Selected`、`newPartLinearExpr`、`addLessOrEqual`                                                              | `StructCombinationRuleTest`                                           |
| 结构化二元黑名单          | 模块级       | 部件相关    | 基于约束模型 | 结构化        | 结构化二元            | 兼容类     | 已覆盖，生成器可后置 | 执行态由结构化展开为约束；生成代码时可映射到 `sum4Selected`、`newPartLinearExpr`、`addLessOrEqual`                                                              | `StructCombinationOtherRuleTest`                                      |
| 结构化三元白/黑名单        | 模块级       | 部件相关    | 基于约束模型 | 结构化        | 结构化三元            | 兼容类     | 已覆盖，生成器可后置 | 执行态由结构化展开为约束；未来生成三元组合时需要支持 tuple 展开                                                                                                     | `StructCombinationRuleTest`、`StructCombinationOtherRuleTest`          |
| 结构化 + 非结构化混合      | 模块级       | 部件相关    | 基于约束模型 | 结构化 + 非结构化 | 结构化二元 + 线性表达式    | 兼容类     | 已覆盖，生成器可后置 | 结构化约束展开 + `model().sum4Selected`、`newPartLinearExpr`、`addLessOrEqual`                                                                   | `StructCodeRuleMixedValidateTest`                                     |
| POST 后置计算写回产品参数   | 模块级       | 参数+部件混合 | 基于非约束模型 | 非结构化       | 实例 view 访问       | 计算赋值类     | 已覆盖        | `partCategorySum(code)`、`sumDynAttr4Int`、`dynAttr`、`dynAttrs`、`parameter(code).setValue(...)`                                           | `PostCalcRuleTest`                                                    |
| POST 后置计算写回分类参数   | 部件分类级     | 参数+部件混合 | 基于非约束模型 | 非结构化       | 实例 view 访问       | 计算赋值类     | 已覆盖        | `partCategory(code).parameter(code).setValue(...)`、`partCategory(code).sumQuantity()`                                                   | `PostCalcRuleTest`                                                    |
| 部件级专属规则           | 部件级       | 部件相关    | 基于约束模型 | 非结构化       | 普通/父子层级          | 兼容类/计算赋值类 | TODO       | 预计依赖 `partVar(code)`、`PartVar.quantityVar()`、`selectedVar()`、`attr(...)`                                                                | 当前测试主要访问部件变量，但规则挂载层级多为模块级或部件分类级                                       |
| 结构化部件分类级规则        | 部件分类级     | 部件相关    | 基于约束模型 | 结构化        | 结构化二元/三元         | 兼容类     | TODO       | 预计复用结构化展开 + `sum4Selected`/`addLessOrEqual`                                                                                             | 当前结构化组合样例主要是模块级跨分类组合                                                  |
| 结构化 POST 规则       | 模块级/部件分类级 | 参数+部件混合 | 基于非约束模型 | 结构化        | 结构化计算            | 计算赋值类     | TODO       | 预计映射到 POST view：`partCategorySum`、`parameter().setValue`                                                                                | 当前 POST 样例为非结构化代码                                                     |
| 显式 for 循环生成规则     | 模块级/部件分类级 | 部件相关    | 基于约束模型 | 非结构化       | for/遍历           | 计算赋值类/兼容类 | TODO       | 预计依赖 `partVars(filter)`、`PartCategoryVar.parts(filter)`、循环内 `selectedVar`/`quantityVar`                                                 | 当前多实例多由聚合 API 或手写实例名表达                                                |


## 4. 抽象规则场景矩阵

从业务抽象看，规则场景先按三大类归并。原来按测试样例拆开的“汇总、选择、隐藏”等不再作为一级业务分类，而是落到计算赋值类内部。

| 规则大类 | 场景定义 | 典型子场景 | 主要运行方式 | 备注 |
| ------- | -------- | ---------- | ------------ | ---- |
| 计算赋值类（Assignment） | 根据参数、部件、分类、实例或聚合结果计算并赋值，逻辑形态接近传统编程中的 `if...then...`、循环和表达式计算。 | if-else 逻辑及 for 循环；if-else 反推部件数量；单分类数量/属性汇总；跨分类数量/属性汇总；多实例分类汇总；单选与多选逻辑（本质上是对 `select` 或 `is_selected` 赋值）；整数参数计算；参数隐藏逻辑；POST 后置计算写回产品参数；POST 后置计算写回分类参数。 | 基于约束模型为主；POST 写回类基于非约束模型。 | 参数隐藏逻辑未来可能不再需要，但当前仍按计算赋值类归档。跨分类数量/属性汇总的业务语义属于计算赋值类，跨分类调度覆盖后续放到集成测试。 |
| 兼容类 | 描述参数、部件、组合之间允许或禁止同时成立的关系，既可以是 helper 形式，也可以是结构化白/黑名单或校验型组合。 | 参数 Requires；参数 Incompatible；参数 CoDependent；CodeRule 校验型组合；结构化二元白名单；结构化二元黑名单；结构化三元白/黑名单；二级分类约束。 | 基于约束模型。 | 部件互斥表达式暂不作为算法分类主轴，更多与集成调度方式相关，后续在集成测试中覆盖。 |
| 优先类 | 在满足约束的候选解之间表达偏好或目标函数。 | 优先级目标函数。 | 基于约束模型。 | 当前已有用例主要围绕部件聚合目标函数，后续可补模块级参数优先类样例。 |

## 5. 南向 SDK/API 分类

### 5.1 基于约束模型的 API（当前非 POST 阶段）

这些接口用于 CP-SAT 求解阶段，生成代码会用它们构造变量、表达式和约束。


| 类别        | API                                                                                                                                      | 用途                                               |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| 算法基类      | `ModuleAlgBase.model()`                                                                                                                  | 进入基于约束模型的规则运行方式。                                   |
| 变量访问      | `para(code)`、`partVar(code)`、`partCategoryVar(code)`、`partVars(filter)`                                                                  | 获取参数、部件、部件分类或过滤后的部件变量。                           |
| 参数变量      | `ParaVar.valueVar()`、`value()`、`hiddenVar()`、`hidden()`、`option(code)`、`inputValue()`、`hasInput()`                                       | 参数取值、隐藏状态、枚举选项、输入值读取。                            |
| 参数选项变量    | `ParaOptionVar.selectedVar()`、`selected()`                                                                                               | 枚举参数某个选项是否选中。                                    |
| 部件变量      | `PartVar.quantityVar()`、`quantity()`、`selectedVar()`、`selected()`、`hiddenVar()`、`hidden()`、`fatherCode()`、`attr(code)`、`attrAsInt(code)` | 部件数量、选中状态、隐藏状态和属性读取。                             |
| 部件分类变量    | `PartCategoryVar.parts()`、`parts(filter)`、`sumPara(attr)`、`sumSumPara(attr)`                                                             | 分类下部件集合和分类/多实例聚合参数。                              |
| CP 变量     | `model().newIntVar`、`newIntVarFromDomain`、`newBoolVar`                                                                                   | 创建求解变量。                                          |
| 表达式       | `model().newLinearExpr`、`newPartLinearExpr`、`AlgCPLinearExpr.addTerm`、`PartAlgCPLinearExpr.addExpr`、`addConstant`、`name`                 | 构造普通线性表达式或部件聚合表达式。                               |
| 布尔约束      | `addBoolAnd`、`addBoolOr`、`addImplication`                                                                                                | if-else、兼容、隐藏等布尔关系。                              |
| 基数约束      | `addExactlyOne`、`addAtMostOne`                                                                                                           | 单选、至多单选。                                         |
| 比较约束      | `addEquality`、`addDifferent`、`addLessThan`、`addLessOrEqual`、`addGreaterThan`、`addGreaterOrEqual`                                         | 数值、数量、容量、互斥等约束。                                  |
| 条件约束      | `AlgCPConstraint.onlyEnforceIf`                                                                                                          | if-else 分支约束。                                    |
| 聚合 API    | `sum4Quantity(filter)`、`sum4Quantity(attr, filter)`、`sum4Quantity(partCategoryCodes, attr, filter)`                                      | 按数量或属性数量汇总。                                      |
| 聚合 API    | `sum4Selected(filter)`、`sum4Selected(attr, filter)`、`sum4Selected(partCategoryCodes, attr, filter)`                                      | 按选中状态或属性选中状态汇总。                                  |
| 兼容 helper | `addCompatibleConstraintRequires`、`addCompatibleConstraintInCompatible`、`addCompatibleConstraintCoDependent`、`inCompatible`              | 快速生成 Requires/Incompatible/CoDependent 或部件表达式互斥。 |
| 优先级       | `model().minimize`、`model().maximize`、`model().setObjectExpr`、`updatePriorityObjectFuntion`                                              | 优先级/目标函数。                                        |
| 隐藏辅助      | `addVarAboutHiddenConstraints`                                                                                                           | 将隐藏变量纳入隐藏约束处理。                                   |


### 5.2 基于非约束模型的 API（当前 POST 阶段）

POST 规则不再构造 CP 约束，而是在已有配置解上读取实例 view 并写回参数或实例属性。


| 类别     | API                                                                  | 用途                     |
| ------ | -------------------------------------------------------------------- | ---------------------- |
| 当前解入口  | `currentInst()`、`moduleId()`、`instanceConfigId()`、`quantity()`       | 访问当前 `ModuleInstView`。 |
| 产品参数   | `parameter(code).value()`、`parameter(code).setValue(value)`          | 读取/写回模块级参数。            |
| 产品部件   | `part(code)`、`PartInstView.quantity()`、`setQuantity(q)`、`selected()` | 读取/调整模块级部件实例。          |
| 分类实例   | `partCategory(code)`、`partCategory(code, instId)`                    | 访问单个部件分类实例。            |
| 分类参数   | `partCategory(code).parameter(code).value()`、`setValue(value)`       | 读取/写回部件分类级参数。          |
| 分类部件   | `partCategory(code).part(code)`、`partCategory(code).parts()`         | 访问分类实例内的部件。            |
| 分类数量   | `partCategory(code).sumQuantity()`                                   | 读取单分类实例数量汇总。           |
| 多实例汇总  | `partCategorySum(code).sumSumQuantity()`、`inst(id)`、`insts()`        | 访问多实例分类汇总和各实例。         |
| 动态属性汇总 | `sumDynAttr`、`sumDynAttr4Int`、`dynAttrs`、`dynAttrs4Int`              | 汇总读取分类/部件动态属性。         |
| 本体属性   | `extAttr`、`extAttr4Int`、`dynAttr`、`dynAttr4Int`、`setDynAttr`         | 读取静态扩展属性、读取或写入实例动态属性。  |


### 5.3 辅助与版本 API

这些接口服务于算法描述、版本边界或代码生成便利，不直接表达业务约束。


| 类别       | API                                                                  | 用途                      |
| -------- | -------------------------------------------------------------------- | ----------------------- |
| 描述信息     | `ModuleAlgBase.descriptor()`、`AlgorithmDescriptor`                   | 标识算法类、算法版本、南向 API 版本。   |
| 运行时绑定    | `bindSouthboundRuntime`、`clearSouthboundRuntime`、`southboundRuntime` | 引擎桥接层绑定运行时；生成规则通常不直接调用。 |
| 简单辅助     | `listOf(...)`                                                        | 兼容 helper 等场景中构造选项列表。   |
| API 版本标记 | `SouthApiSince`、`SouthApiVersion`                                    | 标记正式南向 API 的版本范围。       |


## 6. 按 API 维度的下载列表


| API/能力       | 推荐下载文件                                                                                                                                                                      | 用途                                                                 |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| 南向基类与模型入口    | `src/main/java/com/jmix/executor/southinf/ModuleAlgBase.java`、`ModuleCPModel.java`、`PartCategoryCPModel.java`                                                               | 规则生成代码的继承基类和 `model()` 能力入口。                                       |
| CP 基础类型      | `src/main/java/com/jmix/executor/southinf/cp/*.java`                                                                                                                        | 布尔变量、整数变量、线性表达式、约束引用、`onlyEnforceIf`。                              |
| 变量 facade    | `src/main/java/com/jmix/executor/southinf/var/*.java`                                                                                                                       | `ParaVar`、`ParaOptionVar`、`PartVar`、`PartCategoryVar`。             |
| POST 实例 view | `src/main/java/com/jmix/executor/southinf/view/*.java`、`src/test/java/com/jmix/scenario/ruletest/PostCalcRuleTest.java`                                                     | POST 规则读取当前配置解并写回参数。                                               |
| if-else 规则   | `src/test/java/com/jmix/scenario/hello/HelloConstraint.java`、`src/test/java/com/jmix/scenario/ruletest/CalculateRuleSimpleTest.java`、`CalculateRuleIfThenTest.java`         | `newBoolVar`、`addBoolAnd`、`addBoolOr`、`onlyEnforceIf`。             |
| 参数兼容规则       | `src/test/java/com/jmix/scenario/ruletest/CompatibleRuleRequireTest.java`、`CompatibleRuleIncompatibleTest.java`、`CompatibleRuleCodependentTest.java`                        | Requires/Incompatible/CoDependent 的南向写法。                           |
| 部件互斥与过滤表达式   | `src/test/java/com/jmix/opti/base/MultiPCTest.java`、`src/test/java/com/jmix/scenario/ruletest/OptionalPartCategoryWhitelistGuardTest.java`                                  | `inCompatible`、`partVars(filter)`、部件属性过滤。                          |
| 单分类聚合        | `src/test/java/com/jmix/opti/base/BaseOptiTest.java`                                                                                                                        | `sum4Quantity`、`sum4Selected`、分类内数量/属性汇总。                          |
| 跨分类聚合        | `src/test/java/com/jmix/opti/base/MultiPCTest.java`、`src/test/java/com/jmix/scenario/ruletest/CrossPartCategoryTotalConstraintTest.java`                                    | `sum4Quantity(partCategoryCodes, attr, filter)`。                   |
| 多实例聚合        | `src/test/java/com/jmix/opti/multireq/DynMultReq4MultiReqTest.java`、`EnumMultReq4MultiReqTest.java`、`src/test/java/com/jmix/scenario/ruletest/SearchStrategyMultiTest.java` | `drive*`、`sumSumPara`、多实例分类聚合。                                     |
| 优先级目标函数      | `src/test/java/com/jmix/opti/base/BaseOptiTest.java`、`src/test/java/com/jmix/opti/base/MultiPCTest.java`                                                                    | `newPartLinearExpr`、`setObjectExpr`、`updatePriorityObjectFuntion`。 |
| 整数参数计算       | `src/test/java/com/jmix/scenario/ruletest/ParaIntegerTest.java`                                                                                                             | `newLinearExpr`、`addTerm`、`addEquality`。                           |
| 隐藏规则         | `src/test/java/com/jmix/scenario/ruletest/ParaIsHiddenTest.java`                                                                                                            | `hiddenVar`、隐藏值归零、隐藏变量约束。                                          |
| 单选/至多单选      | `src/test/java/com/jmix/scenario/ruletest/SearchStrategyTest.java`、`SearchStrategyMultiTest.java`                                                                           | `addExactlyOne`、`addAtMostOne`。                                    |
| 结构化二元/三元组合   | `src/test/java/com/jmix/scenario/ruletest/StructCombinationRuleTest.java`、`StructCombinationOtherRuleTest.java`、`StructCodeRuleMixedValidateTest.java`                      | 结构化表达方式样例；生成器支持可后置推进。                                              |
| 南向版本边界       | `src/test/java/com/jmix/scenario/ruletest/MigratedSouthboundConstraint.java`、`SouthboundApiDecouplingTest.java`                                                             | 验证算法迁移到 `southinf` API，不依赖旧测试基类。                                   |


## 7. TODO 覆盖清单


| TODO           | 缺失因子                  | 建议补充                                                                          |
| -------------- | --------------------- | ----------------------------------------------------------------------------- |
| 补部件级规则样例       | 层级=部件级                | 增加明确绑定到具体 Part/Feature Code 的兼容、计算或隐藏规则。                                      |
| 补结构化部件分类级样例    | 层级=部件分类级，表达方式=结构化     | 将二元/三元结构化规则限定在一个部件分类内，验证与模块级跨分类组合的差异。                                         |
| 补结构化 POST 样例   | 运行方式=基于非约束模型，表达方式=结构化    | 结构化计算规则映射到 `partCategorySum`、`parameter().setValue` 等 POST view。              |
| 补显式 for/遍历生成样例 | 文法类型=for/遍历           | 对 `partVars(filter)` 或 `PartCategoryVar.parts(filter)` 生成循环式约束，而不是全部依赖聚合 API。 |
| 补模块级参数优先类样例    | 层级=模块级，元素=参数相关，业务=优先类 | 当前优先级多围绕部件聚合，缺少以参数取值为目标函数或偏好的样例。                                              |
| 补部件级 POST 写回样例 | 层级=部件级，运行方式=基于非约束模型      | 使用 `part(code).setQuantity` 或 `setDynAttr` 写回具体部件实例。                          |

## 8. 遗留任务

| 遗留任务 | 背景 | 当前处理 |
| -------- | ---- | -------- |
| 梳理 POST/非 POST 阶段标记与运行方式命名 | 现有实现中使用 `CalcStage.POST` 等阶段标记区分运行阶段，但运行阶段不等同于“基于约束模型/基于非约束模型”的运行方式。 | 本文档先按运行方式归类；代码和现有 POST/非 POST 标记暂不修改，后续作为独立遗留任务处理。 |

## 9. 维护原则

1. 场景归类优先使用六个因子，不再按测试包名或测试 helper 归类。
2. API 依赖只列 `com.jmix.executor.southinf` 及其 facade 暴露的规则编写接口。
3. 结构化规则不单独拆成专项章节，而是作为“表达方式”融入全场景矩阵。
4. 新增规则生成能力时，先补矩阵中的 `TODO`，再更新“按 API 维度的下载列表”。
