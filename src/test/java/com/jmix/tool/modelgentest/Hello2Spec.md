
##userVariableModel
```java
@ParaAnno( 
    options = {"Red", "Black", "White"}
)
private ParaVar colorVar;

@ParaAnno( 
    options = {"Small", "Medium", "Big"}
)
private ParaVar sizeVar;

@PartAnno()
private PartVar tShirt11Var;
```

##userLogicByPseudocode
```java
 //rule1: 如果的颜色是红色且大小是小号是，则tShirt11的数量为1，否者为3 
```  