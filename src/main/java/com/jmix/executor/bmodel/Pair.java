package com.jmix.executor.bmodel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 简单的Pair类，用于替代Apache Commons的Pair
 * 
 * @since 2025-09-22
 */
@Data
@AllArgsConstructor
public class Pair<L, R> {
    private final L first;

    private final R second;

    /**
     * 创建Pair实例
     * 
     * @param first  第一个元素
     * @param second 第二个元素
     * @return Pair实例
     */
    public static <L, R> Pair<L, R> of(L first, R second) {
        return new Pair<>(first, second);
    }
}
