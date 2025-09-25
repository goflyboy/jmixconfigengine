package com.jmix.coretest;

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
                    "Solution count mismatch, expected: %d, actual: null", expectedSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize != expectedSize) {
                throw new AssertionError(String.format(
                        "Solution count mismatch, expected: %d, actual: %d", expectedSize, actualSize));
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
                    "Solution count should be greater than: %d, actual: null", minSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize <= minSize) {
                throw new AssertionError(String.format(
                        "Solution count should be greater than: %d, actual: %d", minSize, actualSize));
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
                    "Solution count should be greater than or equal to: %d, actual: null", minSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize < minSize) {
                throw new AssertionError(String.format(
                        "Solution count should be greater than or equal to: %d, actual: %d", minSize, actualSize));
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
                    "Solution count should be less than: %d, actual: null", maxSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize >= maxSize) {
                throw new AssertionError(String.format(
                        "Solution count should be less than: %d, actual: %d", maxSize, actualSize));
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
                    "Solution count should be less than or equal to: %d, actual: null", maxSize));
        }

        if (actualResult.getData() instanceof Collection) {
            int actualSize = ((Collection<?>) actualResult.getData()).size();
            if (actualSize > maxSize) {
                throw new AssertionError(String.format(
                        "Solution count should be less than or equal to: %d, actual: %d", maxSize, actualSize));
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
                    "Result code mismatch, expected: %d, actual: %d", expectedCode, actualResult.getCode()));
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
                    "Result code should not equal: %d", unexpectedCode));
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
                    "Result message mismatch, expected: %s, actual: %s", expectedMessage, actualResult.getMessage()));
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
                    "Result message should contain: %s, actual: %s", expectedContent, actualResult.getMessage()));
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
                    "Result message should not contain: %s, actual: %s", unexpectedContent, actualResult.getMessage()));
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
                    "Expected execution success, actual result code: %d, message: %s",
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
                    "Expected execution failure, actual result code: %d, message: %s",
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
                    "Expected no solution, actual result code: %d, solution count: %s",
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