
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
 //rule1:
if(colorVar.value == Red && sizeVar.value == Small) {
    tShirt11Var.qty = 1;
}
else {
    tShirt11Var.qty = 3;
} 
```  