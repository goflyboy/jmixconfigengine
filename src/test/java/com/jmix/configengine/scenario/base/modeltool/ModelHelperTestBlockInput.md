##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
VisibilityModePara
```
##userVariableModel
```java
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P2;	
 
```

##userLogicByPseudocode
```java
//逻辑2：
P1.visibilityModeVar 和 P1.visibilityModeVar是不兼容的
``` 