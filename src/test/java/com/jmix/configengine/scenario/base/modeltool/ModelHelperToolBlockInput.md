##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
ParaIsHiddenStand
```
##userVariableModel
```java
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "3"
        )
        private ParaVar P0;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "2"
        )
        private ParaVar P2;
        
        @PartAnno(
			maxQuantity=3
		)
        private PartVar Part1;
        
 
```

##userLogicByPseudocode
```java
// Rule1: P1.isHiddenVar and P2.isHiddenVar are incompatible
// Rule2: if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1
// Rule3:  if P1.isHiddenVar=1 then P1.var=0
// Rule4:  if P2.isHiddenVar=1 then P1.var=0
// Rule5:   if P1.isHiddenVar=0  才可以对P1.var手工修改（也就是作为inferParasByPara的入参)
// Rule6:   if P2.isHiddenVar=0  才可以对P2.var手工修改（也就是作为inferParasByPara的入参)
 
``` 