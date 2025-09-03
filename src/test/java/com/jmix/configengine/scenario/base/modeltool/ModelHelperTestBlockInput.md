##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
ParaIsHidden
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
// Logic 1: P1.isHiddenVar and P2.isHiddenVar are incompatible
// Logic 2: if P0.var in (0,1) then P1.isHiddenVar=1 else P2.isHiddenVar=1
// Logic3:  if P1.isHiddenVar=1 then P1.var=0
// Logic4:  if P2.isHiddenVar=1 then P1.var=0
// Logic5:   if P1.isHiddenVar=0  才可以对P1.var手工修改（也就是作为inferParasByPara的入参)
// Logic6:   if P2.isHiddenVar=0  才可以对P2.var手工修改（也就是作为inferParasByPara的入参)
 
``` 