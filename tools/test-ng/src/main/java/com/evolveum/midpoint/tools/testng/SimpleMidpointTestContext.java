package com.evolveum.midpoint.tools.testng;

import org.testng.ITestResult;

public class SimpleMidpointTestContext implements MidpointTestContext {

    private static final ThreadLocal<SimpleMidpointTestContext> TEST_CONTEXT_THREAD_LOCAL =
            new ThreadLocal<>();

    private final ITestResult testResult;

    public SimpleMidpointTestContext(ITestResult testResult) {
        this.testResult = testResult;
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
        TEST_CONTEXT_THREAD_LOCAL.set(ctx);
        return ctx;
    }

    public static SimpleMidpointTestContext get() {
        return TEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static void destroy() {
        TEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
