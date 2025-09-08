package com.jmix.configengine.inf;

import java.util.Map;

/**
 * 结果类
 * 用于封装操作结果
 */
public class Result<T> {
    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int NO_SOLUTION = 2;
    public int code = SUCCESS;
    public T data;
    public String message;
    public Map<String,Object> extAttrs;

    public static <X> Result<X> success(X data){
        Result<X> r = new Result<>();
        r.code = SUCCESS;
        r.data = data;
        return r;
    }
    
    public static <X> Result<X> failed(String msg){
        Result<X> r = new Result<>();
        r.code = FAILED;
        r.message = msg;
        return r;
    }
    
    public static <X> Result<X> noSolution(){
        Result<X> r = new Result<>();
        r.code = NO_SOLUTION;
        return r;
    }
}
