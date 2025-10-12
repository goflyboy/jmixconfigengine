
##userVariableModel
```java
@PartAnno(maxQuantity = 50)
private PartVar x;

@PartAnno(maxQuantity = 50) 
private PartVar y;

@PartAnno(maxQuantity = 50) 
private PartVar z;
  
```

##userLogicByPseudocode
```java
//rule1:
if(x.qty > 10) then y.qty > 7

//rule2:
x.qty + y.qty > 10

//rule3:
y.qty > 45

//rule4:
if(x.qty > 5 and y.qty > 5) then z.qty < 10

//rule5:
y.qty < 10
 
```  

##userTestCaseSpec
```java
//testcase1: 
 debugByRelaxationVar设置为false,     inferParasByPara(); 预期结果为：success, assertSolutionNum为0
 
//testcase2:
 debugByRelaxationVar设置为true,      inferParasByPara(); 预期结果为：no_solution, message的冲突规则为rule4

```  