
##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java 
DynSchedule
```
##userVariableModel
```java
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "4"
        )		
        private ParaVar P0;


        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "5"
        )
        private ParaVar P11; 
        
        @PartAnno(
			maxQuantity=3
		)
        private PartVar PT1;
      
        @ParaAnno(   
			options = {"op211", "op212", "op213", "op214"}
        )
        private ParaVar P21; 
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P22; 

        @PartAnno(
			maxQuantity=5
		)
        private PartVar PT2;  
 
```

##userLogicByPseudocode
```java
//P0->P11,P21 Rule01,Rule02
//P11,PT1为组1,有Rule11
//P21,PT2为组2,有Rule21

// Rule01: if P0.value > 1 then P11.value > P0.value+1 else P11.value < P0.value 
// Rule02: if P0.value != 2 then P21.value in ( op211,op212) else P21.value in ( op213,op214) 
// Rule11:  PT1.qty=P11.value 
// Rule21:   
	if  P21.value in ( op211,op212) then PT2.qty= 1*P22.value  else PT2.qty= 2*P22.value 
``` 

##userTestCaseSpec
```java
用例1：根据PT1.qty反推P11.value和P0.value，参与计算变量:P0,P11,PT1, 约束为：Rule01，Rule11 
用例2：根据PT2.qty反推P21.value和P0.value，参与计算变量:P0,P21,PT1, 约束为：Rule02，Rule21 


``` 