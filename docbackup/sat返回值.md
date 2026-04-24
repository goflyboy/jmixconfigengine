UNKNOWN (0)
原英文：The status of the model is still unknown. A search limit has been reached before any of the statuses below could be determined.
中文翻译：模型的状态仍然未知。在确定以下任何状态之前，已达到搜索限制。
MODEL_INVALID (1)
原英文：The given CpModelProto didn't pass the validation step. You can get a detailed error by calling ValidateCpModel(model_proto).
中文翻译：给定的 CpModelProto 未通过验证步骤。您可以通过调用 ValidateCpModel(model_proto) 获取详细错误。
FEASIBLE (2)
原英文：A feasible solution has been found. But the search was stopped before we could prove optimality or before we enumerated all solutions of a feasibility problem (if asked).
中文翻译：已找到可行解。但在证明最优性之前或在枚举可行性问题的所有解之前（如果要求），搜索已停止。
INFEASIBLE (3)
原英文：The problem has been proven infeasible.
中文翻译：问题已被证明不可行。
OPTIMAL (4)
原英文：An optimal feasible solution has been found. More generally, this status represent a success. So we also return OPTIMAL if we find a solution for a pure feasibility problem or if a gap limit has been specified and we return a solution within this limit. In the case where we need to return all the feasible solution, this status will only be returned if we enumerated all of them; If we stopped before, we will return FEASIBLE.
中文翻译：已找到最优可行解。更一般地说，此状态表示成功。因此，如果我们为纯可行性问题找到解，或者指定了间隙限制且我们在限制内返回解，我们也会返回 OPTIMAL。在需要返回所有可行解的情况下，仅当我们枚举了所有解时才会返回此状态；如果在此之前停止，我们将返回 FEASIBLE。
这些翻译准确传达了每个求解器状态的含义，有助于中文开发者更好地理解 CP-SAT 求解器的运行结果。


对Ortools-java 中SAT， 没有min/max 目标函数，solverStatus返回值是OPTIMAL，为什么？

类型A：可行性问题
没有目标函数，只要求找到任意可行解

只要找到满足所有约束的解决方案，状态就是OPTIMAL

这种情况下，任何可行解都是最优的