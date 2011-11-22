package com.evolveum.midpoint.tools.testng;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * Created by IntelliJ IDEA.
 * User: mamut
 * Date: 22.11.2011
 * Time: 16:13
 * To change this template use File | Settings | File Templates.
 */
public class TestMethodLoggerListener implements IInvokedMethodListener {
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        System.out.println("-----------------------------------------------------------");
        System.out.println("TestNG running method: " + method.getTestMethod().toString());
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        System.out.println("TestNG finish method : " + method.getTestMethod().toString() + " Result: " + (testResult.isSuccess() ? "PASS" : "FAIL"));
        System.out.println("-----------------------------------------------------------");
    }
}
