package com.jmix.configengine.extensibleDemo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestEditorFormater {

    // <option name="FOR_BRACE_FORCE" value="3" /> 是IntelliJ
    // IDEA代码风格配置中的一个选项，用于控制for循环的大括号（花括号）使用规则。
    // FOR_BRACE_FORCE 选项说明
    // 这个选项控制for循环的大括号是否强制使用，有以下值：
    // value="0" - 不强制使用大括号（允许单行for循环不使用大括号）
    // value="1" - 总是使用大括号
    // value="2" - 当有多行语句时使用大括号
    // value="3" - 总是使用大括号（与value="1"相同）

    // 6. 快捷键格式化
    // 格式化整个文件：Shift + Alt + F
    // 格式化选中代码：Ctrl + K, Ctrl + F
    // 7. 检查当前配置
    // 你可以通过以下方式检查当前的Java格式化配置：
    // 打开命令面板：Ctrl + Shift + P
    // 输入 "Java: Show Formatter Settings"
    // 查看当前使用的格式化配置
    // 这样配置后，你的Java代码就会按照指定的风格进行格式化，保持代码的一致性和可读性。

    public void formateText() {
        // 正确的for循环格式 - 总是使用大括号
        for (int i = 0; i < 10; i++) {
            log.info("{}", i);
        }

        // 多行时仍然需要大括号
        for (int i = 0; i < 10; i++) {
            log.info("{}", i);
            log.info("Next");
        }
    }

}
