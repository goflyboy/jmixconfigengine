##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
ParaInteger
```
##userVariableModel
```java
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0"
        )
        private ParaVar P2;	
		

        @PartAnno(
			maxQuantity=3
		)
        private PartVar Part1;	
```

##userLogicByPseudocode
```java
//逻辑2：
Part1.quantity = P1.value+ P2.value 
``` 