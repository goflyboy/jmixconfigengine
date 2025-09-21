package com.jmix.executor.util;

/**
 * 简单的Pair类，用于替代Apache Commons的Pair
 */
public class Pair<L, R> {
    private final L first;
    private final R second;

    public Pair(L first, R second) {
        this.first = first;
        this.second = second;
    }

    public L getFirst() {
        return first;
    }

    public R getSecond() {
        return second;
    }

    public static <L, R> Pair<L, R> of(L first, R second) {
        return new Pair<>(first, second);
    }
}
