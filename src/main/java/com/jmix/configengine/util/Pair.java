package com.jmix.configengine.util;

/**
 * 简单的Pair类，用于替代Apache Commons的Pair
 */
public class Pair<L, R> {
    private final L left;
    private final R right;
    
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }
    
    public L getLeft() { return left; }
    public R getRight() { return right; }
    
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }
}
