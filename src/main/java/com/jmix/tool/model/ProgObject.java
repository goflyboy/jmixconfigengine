package com.jmix.tool.model;

/**
 * 编程对象，用于解析代码中的对象引用
 * 
 * @since 2025-09-22
 */
public class ProgObject {
    /**
     * 对象代码，例如：Color.value的"Color"
     */
    private String objCode;

    /**
     * 对象字段，例如：Color.value的"value"
     */
    private String objField;

    /**
     * 构造函数
     * 
     * @param objCode  对象代码
     * @param objField 对象字段
     */
    public ProgObject(String objCode, String objField) {
        this.objCode = objCode;
        this.objField = objField;
    }

    /**
     * 获取对象代码
     * 
     * @return 对象代码
     */
    public String getObjCode() {
        return objCode;
    }

    /**
     * 设置对象代码
     * 
     * @param objCode 对象代码
     */
    public void setObjCode(String objCode) {
        this.objCode = objCode;
    }

    /**
     * 获取对象字段
     * 
     * @return 对象字段
     */
    public String getObjField() {
        return objField;
    }

    /**
     * 设置对象字段
     * 
     * @param objField 对象字段
     */
    public void setObjField(String objField) {
        this.objField = objField;
    }

    /**
     * 返回对象的字符串表示
     * 
     * @return 对象的字符串表示
     */
    @Override
    public String toString() {
        return "ProgObject{" +
                "objCode='" + objCode + '\'' +
                ", objField='" + objField + '\'' +
                '}';
    }
}