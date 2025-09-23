package com.jmix.configengine.scenario.base;

import com.jmix.executor.omodel.Result;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

/**
 * 结果断言类，提供链式API验证执行结果
 * 
 * @since 2025-09-23
 */
@Slf4j
public class ResultAssert {

    private final Result<?> actualResult;

    /**
     * 构造ResultAssert实例
     * 
     * @param actualResult 实际执行结果
     */
    public ResultAssert(Result<?> actualResult) {
        this.actualResult = actualResult;
    }

    /**
     * 断言解决方案数量
     * 
     * @param expectedSize 期望的解决方案数量
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSolutionSizeEqual(int expectedSize) {
        if (actualResult.getData() == null) {
            throw new AssertionError(String.format(
                    "解决方案数量不匹配，期望: %d，实际: null", expectedSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize != expectedSize) {
                throw new AssertionError(String.format(
                        "解决方案数量不匹配，期望: %d，实际: %d", expectedSize, actualSize));
            }
        } else {
            throw new AssertionError("Result data is not a collection type, cannot get count");
        }
        return this;
    }

    /**
     * 断言解决方案数量大于指定值
     * 
     * @param minSize 最小解决方案数量
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSolutionSizeGreaterThan(int minSize) {
        if (actualResult.getData() == null) {
            throw new AssertionError(String.format(
                    "解决方案数量应大于: %d，实际: null", minSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize <= minSize) {
                throw new AssertionError(String.format(
                        "解决方案数量应大于: %d，实际: %d", minSize, actualSize));
            }
        } else {
            throw new AssertionError("Result data is not a collection type, cannot get count");
        }
        return this;
    }

    /**
     * 断言解决方案数量大于等于指定值
     * 
     * @param minSize 最小解决方案数量
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSolutionSizeGreaterThanOrEqual(int minSize) {
        if (actualResult.getData() == null) {
            throw new AssertionError(String.format(
                    "解决方案数量应大于等于: %d，实际: null", minSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize < minSize) {
                throw new AssertionError(String.format(
                        "解决方案数量应大于等于: %d，实际: %d", minSize, actualSize));
            }
        } else {
            throw new AssertionError("Result data is not a collection type, cannot get count");
        }
        return this;
    }

    /**
     * 断言解决方案数量小于指定值
     * 
     * @param maxSize 最大解决方案数量
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSolutionSizeLessThan(int maxSize) {
        if (actualResult.getData() == null) {
            throw new AssertionError(String.format(
                    "解决方案数量应小于: %d，实际: null", maxSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize >= maxSize) {
                throw new AssertionError(String.format(
                        "解决方案数量应小于: %d，实际: %d", maxSize, actualSize));
            }
        } else {
            throw new AssertionError("Result data is not a collection type, cannot get count");
        }
        return this;
    }

    /**
     * 断言解决方案数量小于等于指定值
     * 
     * @param maxSize 最大解决方案数量
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSolutionSizeLessThanOrEqual(int maxSize) {
        if (actualResult.getData() == null) {
            throw new AssertionError(String.format(
                    "解决方案数量应小于等于: %d，实际: null", maxSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize > maxSize) {
                throw new AssertionError(String.format(
                        "解决方案数量应小于等于: %d，实际: %d", maxSize, actualSize));
            }
        } else {
            throw new AssertionError("Result data is not a collection type, cannot get count");
        }
        return this;
    }

    /**
     * 断言结果代码
     * 
     * @param expectedCode 期望的结果代码
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertCodeEqual(int expectedCode) {
        if (actualResult.getCode() != expectedCode) {
            throw new AssertionError(String.format(
                    "结果代码不匹配，期望: %d，实际: %d", expectedCode, actualResult.getCode()));
        }
        return this;
    }

    /**
     * 断言结果代码不等于指定值
     * 
     * @param unexpectedCode 不期望的结果代码
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertCodeNotEqual(int unexpectedCode) {
        if (actualResult.getCode() == unexpectedCode) {
            throw new AssertionError(String.format(
                    "结果代码不应等于: %d", unexpectedCode));
        }
        return this;
    }

    /**
     * 断言结果消息
     * 
     * @param expectedMessage 期望的结果消息
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertMessage(String expectedMessage) {
        if (!expectedMessage.equals(actualResult.getMessage())) {
            throw new AssertionError(String.format(
                    "结果消息不匹配，期望: %s，实际: %s", expectedMessage, actualResult.getMessage()));
        }
        return this;
    }

    /**
     * 断言结果消息包含指定内容
     * 
     * @param expectedContent 期望包含的内容
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertMessageContains(String expectedContent) {
        if (actualResult.getMessage() == null || !actualResult.getMessage().contains(expectedContent)) {
            throw new AssertionError(String.format(
                    "结果消息应包含: %s，实际: %s", expectedContent, actualResult.getMessage()));
        }
        return this;
    }

    /**
     * 断言结果消息不包含指定内容
     * 
     * @param unexpectedContent 不期望包含的内容
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertMessageNotContains(String unexpectedContent) {
        if (actualResult.getMessage() != null && actualResult.getMessage().contains(unexpectedContent)) {
            throw new AssertionError(String.format(
                    "结果消息不应包含: %s，实际: %s", unexpectedContent, actualResult.getMessage()));
        }
        return this;
    }

    /**
     * 断言执行成功
     * 
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertSuccess() {
        if (actualResult.getCode() != Result.SUCCESS) {
            throw new AssertionError(String.format(
                    "期望执行成功，实际结果代码: %d，消息: %s",
                    actualResult.getCode(), actualResult.getMessage()));
        }
        return this;
    }

    /**
     * 断言执行失败
     * 
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertFailure() {
        if (actualResult.getCode() == Result.SUCCESS) {
            throw new AssertionError(String.format(
                    "期望执行失败，实际结果代码: %d，消息: %s",
                    actualResult.getCode(), actualResult.getMessage()));
        }
        return this;
    }

    /**
     * 断言无解
     * 
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertNoSolution() {
        if (actualResult.getCode() != Result.SUCCESS
                || (actualResult.getData() instanceof Collection
                        && ((Collection<?>) actualResult.getData()).size() > 0)) {
            throw new AssertionError(String.format(
                    "期望无解，实际结果代码: %d，解数量: %s",
                    actualResult.getCode(),
                    actualResult.getData() instanceof Collection
                            ? String.valueOf(((Collection<?>) actualResult.getData()).size())
                            : "N/A"));
        }
        return this;
    }

    /**
     * 断言结果数据不为空
     * 
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertDataNotNull() {
        if (actualResult.getData() == null) {
            throw new AssertionError("Result data should not be null");
        }
        return this;
    }

    /**
     * 断言结果数据为空
     * 
     * @return 当前ResultAssert实例，支持链式调用
     */
    public ResultAssert assertDataNull() {
        if (actualResult.getData() != null) {
            throw new AssertionError("Result data should be null");
        }
        return this;
    }
}