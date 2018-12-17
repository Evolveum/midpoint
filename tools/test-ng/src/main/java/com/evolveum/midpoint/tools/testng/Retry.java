package com.evolveum.midpoint.tools.testng;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Retry implements IRetryAnalyzer {
    private int retryCount = 0;
    private String maxRetryCountEnv = System.getProperty("testsRetryCount");

    public boolean retry(ITestResult result) {
        if (maxRetryCountEnv == null)
            return false;

        int maxRetryCount;
        try {
            maxRetryCount = Integer.parseInt(maxRetryCountEnv);
        } catch (NumberFormatException e) {
            System.out.println("Retry for test: " + result.getMethod().getMethodName() + ", FAILED, cannot parse retry count: "+e.getMessage());
            return false;
        }

        if (retryCount < maxRetryCount) {
            retryCount++;
            System.out.println("Retry #" + retryCount + " for test: " + result.getMethod().getMethodName() + ", on thread: " + Thread.currentThread().getName());
            return true;
        }
        return false;
    }
}