package com.jmix.executor.impl;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.algmodel.OtherVar;
import com.jmix.executor.cmodel.ModuleBaseInst;
import com.jmix.executor.cmodel.ModuleInst;
import com.jmix.executor.model.Result;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 解决方案工具类
 * 提供打印解决方案相关的静态方法
 * 
 * @since 2025-01-XX
 */
@Slf4j
public final class SolutionUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private SolutionUtils() {
        // 工具类，不允许实例化
    }

    /**
     * 打印短格式解信息
     * 
     * @param module    模块
     * @param solutions 解列表
     * @param isSimple  是否使用简单格式
     * @param result    结果对象，用于获取消息
     */
    public static void printSolutions(Module module, List<ModuleInst> solutions, boolean isSimple,
            Result<List<ModuleInst>> result) {
        if (solutions == null || solutions.isEmpty()) {
            log.info("No solutions found");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append("******************************************")
                .append(System.lineSeparator());
        // 打印缩语解释
        sb.append("1.Abbreviation explanation:").append(System.lineSeparator());

        // 1. shortCodes(P1:Size, P2:Color,PT1:part1)
        sb.append("1.1. ").append(module.getProgObjShortCodeMemo()).append(System.lineSeparator());
        // 2. Attrs(V:value, H:isHidden, Q:qty)
        sb.append("1.2. Attrs(V:value, H:isHidden, Q:qty,S:isSelected)").append(System.lineSeparator());
        // 3. Other Variable shortName
        sb.append("1.3. Other Variable shortName:").append(System.lineSeparator());
        printOthers(solutions, sb);

        // 4. Priority Attribute shortName
        sb.append("1.4. Priority Attribute shortName:").append(solutions.get(0).getPriorityAttrShortCodeStr())
                .append(System.lineSeparator());

        sb.append("2.Solutions:").append(System.lineSeparator());
        // 打印解
        printSolutionList(solutions, isSimple, sb);
        sb.append(System.lineSeparator()).append("******************************************")
                .append(System.lineSeparator());
        sb.append("3.Messages:").append(System.lineSeparator());
        if (result != null) {
            sb.append(result.getMessage()).append(System.lineSeparator());
        }
        log.info(sb.toString());
    }

    /**
     * 打印解决方案列表
     *
     * @param solutions 解决方案列表
     * @param isSimple  是否使用简单格式
     */
    public static String toSolutionString(List<ModuleInst> solutions) {
        return toSolutionString(solutions, true);
    }

    /**
     * 打印解决方案列表
     *
     * @param solutions 解决方案列表
     */
    public static String toSolutionString(List<ModuleInst> solutions, boolean isSimple) {
        StringBuilder sb = new StringBuilder();
        sb.append("Solutions-Total:").append(solutions.size()).append(System.lineSeparator());
        sb.append("Solutions-Detail:").append(System.lineSeparator());
        printSolutionList(solutions, isSimple, sb);
        return sb.toString();
    }

    private static void printSolutionList(List<ModuleInst> solutions, boolean isSimple, StringBuilder sb) {
        for (int i = 0; i < solutions.size(); i++) {
            sb.append(" S_").append(i + 1).append(": ").append(solutions.get(i).toShortString(isSimple))
                    .append(System.lineSeparator());
        }
    }

    /**
     * 打印其他变量的短名称信息
     *
     * @param solutions 解决方案列表
     * @param sb        字符串构建器
     */
    private static void printOthers(List<ModuleInst> solutions, StringBuilder sb) {
        if (!solutions.isEmpty()) {
            ModuleInst firstSolution = solutions.get(0);
            Object otherVarsMemo = firstSolution.getExtAttrs().get(ModuleBaseInst.OTHER_VARIABLES_MEMO_KEY);
            if (otherVarsMemo instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, OtherVar> otherVarMap = (Map<String, OtherVar>) otherVarsMemo;
                for (Map.Entry<String, OtherVar> entry : otherVarMap.entrySet()) {
                    OtherVar otherVar = entry.getValue();
                    sb.append(" ").append(otherVar.getShortCode()).append(":").append(otherVar.getCode())
                            .append(System.lineSeparator());
                }
            }
        }
    }
}
