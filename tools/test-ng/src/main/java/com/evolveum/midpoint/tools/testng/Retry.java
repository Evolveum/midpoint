/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class Retry implements IRetryAnalyzer {
    private int retryCount = 0;
    private int maxRetryCount = 0;
    private String maxRetryCountEnv = System.getProperty("testsRetryCount");

    private boolean initMaxRetry() {
        if (maxRetryCountEnv == null) {
            return false;
        }

        try {
            maxRetryCount = Integer.parseInt(maxRetryCountEnv);
        } catch (NumberFormatException e) {
            System.out.println("Test retry FAILED, cannot parse retry count: "+e.getMessage());
            return false;
        }
        return true;
    }

    public boolean retry(ITestResult result) {
        if (isOneMoreRetryAvailable()) {
            retryCount++;
            System.out.println("Retry #" + retryCount + " for test: " + result.getMethod().getMethodName() + ", on thread: " + Thread.currentThread().getName());
            return true;
        }
        return false;
    }

    public boolean isOneMoreRetryAvailable() {
        if (!initMaxRetry()) {
            return false;
        }

        return retryCount < maxRetryCount;
    }
}
