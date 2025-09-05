package com.jmix.configengine.scenario.base;

/**
 * 编程对象，用于解析代码中的对象引用
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
    
    public ProgObject(String objCode, String objField) {
        this.objCode = objCode;
        this.objField = objField;
    }
    
    public String getObjCode() {
        return objCode;
    }
    
    public void setObjCode(String objCode) {
        this.objCode = objCode;
    }
    
    public String getObjField() {
        return objField;
    }
    
    public void setObjField(String objField) {
        this.objField = objField;
    }
    
    @Override
    public String toString() {
        return "ProgObject{" +
                "objCode='" + objCode + '\'' +
                ", objField='" + objField + '\'' +
                '}';
    }
} 