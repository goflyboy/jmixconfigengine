package com.jmix.configengine.constant;

/**
 * 可见性模式常量
 * 定义visibilityMode的四个状态
 */
public class VisibilityModeConstants {
    
    /**
     * 可见，可改（默认值）
     */
    public static final int VISIBLE_EDITABLE = 0;
    
    /**
     * 可见，不可改
     */
    public static final int VISIBLE_READONLY = 1;
    
    /**
     * 不可见，可改（不存在）
     */
    // public static final int HIDDEN_EDITABLE = 2;
    
    /**
     * 不可见，不可改
     */
    public static final int HIDDEN_READONLY = 3;
    
    public static int[] getVisibleModeValues() {
        return new int[] {VISIBLE_EDITABLE,VISIBLE_READONLY,VISIBLE_READONLY};
    }

    /**
     * 获取可见性模式的描述
     */
    public static String getDescription(int visibilityMode) {
        switch (visibilityMode) {
            case VISIBLE_EDITABLE:
                return "Visible, Editable";
            case VISIBLE_READONLY:
                return "Visible, Read-only";
            case HIDDEN_READONLY:
                return "Hidden, Read-only";
            default:
                return "Unknown";
        }
    }
    
    /**
     * 检查是否为可见状态
     */
    public static boolean isVisible(int visibilityMode) {
        return visibilityMode == VISIBLE_EDITABLE || visibilityMode == VISIBLE_READONLY;
    }
    
    /**
     * 检查是否为可编辑状态
     */
    public static boolean isEditable(int visibilityMode) {
        return visibilityMode == VISIBLE_EDITABLE;
    }
} 