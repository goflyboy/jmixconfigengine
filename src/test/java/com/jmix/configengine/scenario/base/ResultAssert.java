package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor;
import lombok.extern.slf4j.Slf4j;
import java.util.Collection;

/**
 * 结果断言类，提供链式API验证执行结果
 */
@Slf4j
public class ResultAssert {
    
    private final ModuleConstraintExecutor.Result<?> actualResult;
    
    public ResultAssert(ModuleConstraintExecutor.Result<?> actualResult) {
        this.actualResult = actualResult;
    }
    
    /**
     * 断言解决方案数量
     */
    public ResultAssert assertSolutionSizeEqual(int expectedSize) {
        if (actualResult.data == null) {
            throw new AssertionError(String.format(
                "解决方案数量不匹配，期望: %d，实际: null", expectedSize));
        }
        
        if (actualResult.data instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.data).size();
            if (actualSize != expectedSize) {
                throw new AssertionError(String.format(
                    "解决方案数量不匹配，期望: %d，实际: %d", expectedSize, actualSize));
            }
        } else {
            throw new AssertionError("结果数据不是集合类型，无法获取数量");
        }
        return this;
    }
    
    /**
     * 断言解决方案数量大于指定值
     */
    public ResultAssert assertSolutionSizeGreaterThan(int minSize) {
        if (actualResult.data == null) {
            throw new AssertionError(String.format(
                "解决方案数量应大于: %d，实际: null", minSize));
        }
        
        if (actualResult.data instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.data).size();
            if (actualSize <= minSize) {
                throw new AssertionError(String.format(
                    "解决方案数量应大于: %d，实际: %d", minSize, actualSize));
            }
        } else {
            throw new AssertionError("结果数据不是集合类型，无法获取数量");
        }
        return this;
    }
    
    /**
     * 断言解决方案数量大于等于指定值
     */
    public ResultAssert assertSolutionSizeGreaterThanOrEqual(int minSize) {
        if (actualResult.data == null) {
            throw new AssertionError(String.format(
                "解决方案数量应大于等于: %d，实际: null", minSize));
        }
        
        if (actualResult.data instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.data).size();
            if (actualSize < minSize) {
                throw new AssertionError(String.format(
                    "解决方案数量应大于等于: %d，实际: %d", minSize, actualSize));
            }
        } else {
            throw new AssertionError("结果数据不是集合类型，无法获取数量");
        }
        return this;
    }
    
    /**
     * 断言解决方案数量小于指定值
     */
    public ResultAssert assertSolutionSizeLessThan(int maxSize) {
        if (actualResult.data == null) {
            throw new AssertionError(String.format(
                "解决方案数量应小于: %d，实际: null", maxSize));
        }
        
        if (actualResult.data instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.data).size();
            if (actualSize >= maxSize) {
                throw new AssertionError(String.format(
                    "解决方案数量应小于: %d，实际: %d", maxSize, actualSize));
            }
        } else {
            throw new AssertionError("结果数据不是集合类型，无法获取数量");
        }
        return this;
    }
    
    /**
     * 断言解决方案数量小于等于指定值
     */
    public ResultAssert assertSolutionSizeLessThanOrEqual(int maxSize) {
        if (actualResult.data == null) {
            throw new AssertionError(String.format(
                "解决方案数量应小于等于: %d，实际: null", maxSize));
        }
        
        if (actualResult.data instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.data).size();
            if (actualSize > maxSize) {
                throw new AssertionError(String.format(
                    "解决方案数量应小于等于: %d，实际: %d", maxSize, actualSize));
            }
        } else {
            throw new AssertionError("结果数据不是集合类型，无法获取数量");
        }
        return this;
    }
    
    /**
     * 断言结果代码
     */
    public ResultAssert assertCodeEqual(int expectedCode) {
        if (actualResult.code != expectedCode) {
            throw new AssertionError(String.format(
                "结果代码不匹配，期望: %d，实际: %d", expectedCode, actualResult.code));
        }
        return this;
    }
    
    /**
     * 断言结果代码不等于指定值
     */
    public ResultAssert assertCodeNotEqual(int unexpectedCode) {
        if (actualResult.code == unexpectedCode) {
            throw new AssertionError(String.format(
                "结果代码不应等于: %d", unexpectedCode));
        }
        return this;
    }
    
    /**
     * 断言结果消息
     */
    public ResultAssert assertMessage(String expectedMessage) {
        if (!expectedMessage.equals(actualResult.message)) {
            throw new AssertionError(String.format(
                "结果消息不匹配，期望: %s，实际: %s", expectedMessage, actualResult.message));
        }
        return this;
    }
    
    /**
     * 断言结果消息包含指定内容
     */
    public ResultAssert assertMessageContains(String expectedContent) {
        if (actualResult.message == null || !actualResult.message.contains(expectedContent)) {
            throw new AssertionError(String.format(
                "结果消息应包含: %s，实际: %s", expectedContent, actualResult.message));
        }
        return this;
    }
    
    /**
     * 断言结果消息不包含指定内容
     */
    public ResultAssert assertMessageNotContains(String unexpectedContent) {
        if (actualResult.message != null && actualResult.message.contains(unexpectedContent)) {
            throw new AssertionError(String.format(
                "结果消息不应包含: %s，实际: %s", unexpectedContent, actualResult.message));
        }
        return this;
    }
    
    /**
     * 断言执行成功
     */
    public ResultAssert assertSuccess() {
        if (actualResult.code != ModuleConstraintExecutor.Result.SUCCESS) {
            throw new AssertionError(String.format(
                "期望执行成功，实际结果代码: %d，消息: %s", 
                actualResult.code, actualResult.message));
        }
        return this;
    }
    
    /**
     * 断言执行失败
     */
    public ResultAssert assertFailure() {
        if (actualResult.code == ModuleConstraintExecutor.Result.SUCCESS) {
            throw new AssertionError(String.format(
                "期望执行失败，实际结果代码: %d，消息: %s", 
                actualResult.code, actualResult.message));
        }
        return this;
    }
    
    /**
     * 断言结果数据不为空
     */
    public ResultAssert assertDataNotNull() {
        if (actualResult.data == null) {
            throw new AssertionError("结果数据不应为空");
        }
        return this;
    }
    
    /**
     * 断言结果数据为空
     */
    public ResultAssert assertDataNull() {
        if (actualResult.data != null) {
            throw new AssertionError("结果数据应为空");
        }
        return this;
    }
} 