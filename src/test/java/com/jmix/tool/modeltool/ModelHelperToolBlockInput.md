
##packageName
```java 
com.jmix.configengine.perf
```

##modelName
```java 
Perf20Paras
```
##userVariableModel
```java
//请构建20个参数，每个参数有10个可选值，例如：
        @ParaAnno(   
			options =  {
                "op00101", "op00102", "op00103", "op00104", "op00105",
                "op00106", "op00107", "op00108", "op00109", "op00110"
            }
        )
        private ParaVar P001; 

//构建5个部件(Part),例如 
        @PartAnno(
			maxQuantity=10
		)
        private PartVar PT001;
       

    
```

##userLogicByPseudocode
```java
//参数Para到参数Para见规则，自动生成20条，类似P001到P002，P002到P003，规则就是简单if-else等类型，类似： 
//Rule01: if P001.value== op00102  then P002.value != op00202

//参数Para到部件Part的规则，最后10个参数(P011,...P020)到部件的，生成5条，类似P011,P012到PT001,规则就是简单if-else等类型数量关系,类似
 //Rule31： if(P011.value== op01101 && P012.value== op01201) then {PT001.qty=1 }
 //          else if(P011.value== op01102 && P012.value== op01202) {PT001.qty=2}
 //          else {PT001.qty=3} 



``` 

##userTestCaseSpec
```java
//本测试目的是为了测试性能，不需要按逻辑完备生成用例
//每个用例需要执行10次，取平均时间
//用例1：  inferParas("PT001", 2); 时间小于300毫秒
//用例2：  inferParas("PT003", 4); 时间小于300毫秒

``` 