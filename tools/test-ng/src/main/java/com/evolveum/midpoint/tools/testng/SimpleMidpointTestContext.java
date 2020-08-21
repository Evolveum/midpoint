package com.evolveum.midpoint.tools.testng;

import org.testng.ITestResult;

public class SimpleMidpointTestContext implements MidpointTestContext {

    private static final ThreadLocal<SimpleMidpointTestContext> TEST_CONTEXT_THREAD_LOCAL =
            new ThreadLocal<>();

    private final ITestResult testResult;
    private final String originalThreadName;

    public SimpleMidpointTestContext(ITestResult testResult) {
        this.testResult = testResult;
        this.originalThreadName = Thread.currentThread().getName();
    }

    @Override
    public Class<?> getTestClass() {
        return testResult.getMethod().getTestClass().getRealClass();
    }

    @Override
    public String getTestMethodName() {
        return testResult.getMethod().getMethodName();
    }

    public static SimpleMidpointTestContext create(ITestResult testResult) {
        SimpleMidpointTestContext ctx = new SimpleMidpointTestContext(testResult);
        Thread.currentThread().setName(ctx.getTestName());
        TEST_CONTEXT_THREAD_LOCAL.set(ctx);
        return ctx;
    }

    public static SimpleMidpointTestContext get() {
        return TEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static void destroy() {
        Thread.currentThread().setName(get().originalThreadName);
        TEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
