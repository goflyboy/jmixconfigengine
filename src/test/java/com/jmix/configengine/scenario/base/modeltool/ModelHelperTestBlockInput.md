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
 
```

##userLogicByPseudocode
```java
// Logic 1: P1.visibilityModeVar and P2.visibilityModeVar are incompatible
// Logic 2: if P0.var in (0,1) then P1.visibilityModeVar=3 else P2.visibilityModeVar=3
// Logic3:  if P1.visibilityModeVar=3 then P1.var=0
// Logic4:  if P2.visibilityModeVar=3 then P1.var=0
// Logic5:   if P1.visibilityModeVar=0  才可以对P1.var手工修改（也就是作为inferParasByPara的入参)
// Logic6:   if P2.visibilityModeVar=0  才可以对P2.var手工修改（也就是作为inferParasByPara的入参)
 
``` 