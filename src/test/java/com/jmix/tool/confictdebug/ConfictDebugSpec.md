
##userVariableModel
```java
@PartAnno(maxQuantity = 50)
private PartVar x;

@PartAnno(maxQuantity = 50) 
private PartVar y;
  
```

##userLogicByPseudocode
```java
 //rule1:
 x.qty + y.qty < 20

 //rule2:
 x.qty -y.qty > 10

 //rule3:
 y.qty > 45
 
 //rule3:
 x.qty+ 2y.qty > 10

```  

##userTestCaseSpec
```java
//testcase1: 
 debugByRelaxationVar设置为false,     inferParasByPara(); 预期结果为：success, assertSolutionNum为0
 
//testcase2:
 debugByRelaxationVar设置为true,      inferParasByPara(); 预期结果为：no_solution, message的冲突规则为rule3

```  