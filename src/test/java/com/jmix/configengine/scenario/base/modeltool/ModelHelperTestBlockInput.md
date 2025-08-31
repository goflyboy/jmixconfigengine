##packageName
```java
com.jmix.configengine.scenario.ruletest
```

##modelName
```java
MyTShirt
```
##userVariableModel
```java
@ParaAnno( 
    options = {"Red", "Black", "White"}
)
private ParaVar ColorVar;

@ParaAnno( 
    options = {"Small", "Medium", "Big"}
)
private ParaVar SizeVar;

@PartAnno(maxQuantity = 100)
private PartVar TShirt11Var;
```

##userLogicByPseudocode
```java
// "Red-10", "Black-20", "White-30"
// "Small-10", "Medium-20", "Big-30"
if(ColorVar.var == Red && SizeVar.var == Small) {
    TShirt11Var.var = 1;
}
else {
    TShirt11Var.var = 3;
}
``` 