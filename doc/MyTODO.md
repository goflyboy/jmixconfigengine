
 
2025-9-5：


工作量列表：
1、调整自动生成的代码？**promt  --商务
2、生成代码的逻辑换掉？ --- -==OK
2、其它几种约束算法 --搞定**	
--Requires的实现 --OK
--Codepent的实现 --OK 
--incompatible的 --OK



3、选择类型规则类型？ 看怎么定义？**
---
4

3、Part的不兼容规则***

3、支持part的选择算法
---是否要支持sqlite文件**
4、补充doselect的测试代码

3、全体几种算法，算法算法--是不是直接java实现等了？（如果约束相关，则使用约束）
1、支持多选参数
4、重构module.quantity的重构？--***
 
3、支持全局参数
4、HW的扩展
5、支持冲突的扩展定位
6、打印所以解的变量？方便定位（我们定义优先，其它的房后续，需要有简称，简单一点）
Incompatible
8、一个产品很多花，需要仅加载和他相关的参数，其它的不需要？ ——--
---mainpart?  --> 
---详细的场景看一下？


7、ModuleConstraintExecutor.Result<Void>  拆出来比较合适  ==OK 	
同步刷新: 扩展性功能实现总结.md
Extension

ExtensibleProcess --> Extension
public abstract class ExtensibleProcess  使用lombok

//
	public Result<Void> fini() {
		// 销毁所有扩展处理器
		for (ExtensibleProcess process : extensibleProcesses) {
			try {
				process.destroy();
			} catch (Exception e) {
				log.warn("Failed to destroy extensible process: {}", process.getProcessName(), e);
			}
		}
		extensibleProcesses.clear();


			extensibleProcesses.sort(Comparator.comparingInt(ExtensibleProcess::getPriority));

	private List<ModuleInst> executePostProcess(Module module, List<ModuleInst> solutions) {
		List<ModuleInst> result = solutions;


                DCParaInst dcParaInst = paraInstAdapter.adapt(paraInst);

	   extAttrs.put("season", "String");    --枚举类型？


新定义了规则类型，支持自动转化？

ModelExtension


LogicExtension


2025-9-10
1、testDoSelectWithExtAttrFilter 扩展属性仅支持String类型，后续支持其它类型
2、后续给addConstrainti增加注册，支持的调试等？
2025-9-11：
1、生成打包代码ModuleAlgArtifact(xl调用)，包括基础数据和算法的数据
2、生成性能测试代码
2025-9-20：
1、动态调度不行
2、打jar包不行
3、调一下数通的案例？**，下一步计划？**
3、抽取为tool没有
4、把所有的问题改为？ 

2025-9-20：
1、javaconvertor程序改玩
2、jm全部转完
3、jm格式基本OK 
4、文档是否要转

/check 错误信息？ 格式化的问题？


1、两个接口要提取出来
--Excutor

--impl

2、把类名改一下，能转出来？

3、
executor
  --impl
  --inf 
tool
  --impl
  
  
  類 'CompatiableRuleSchema' 看起來像是為擴展設計的（可以是子類），但方法 'getFromLeftProgObjs' 沒有javadoc，解釋瞭如何安全地執行。如果類不是為擴展而設計的，請考慮創建類 'CompatiableRuleSchema' final或使方法 'getFromLeftProgObjs' static/final/abstract/empty，或為方法添加允許的