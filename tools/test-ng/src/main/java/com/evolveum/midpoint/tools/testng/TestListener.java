/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestListener implements ITestListener {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private static final String PREFIX = " ####[ ";
    private static final String SUFFIX = " ]####";

    @Override
    public void onFinish(ITestContext tc) {
        print("Finished "+tc);
    }

    @Override
    public void onStart(ITestContext tc) {
        print("Starting "+tc);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult tr) {
        print("FailedButWithinSuccessPercentage "+tr);
    }

    @Override
    public void onTestFailure(ITestResult tr) {
        /*
        if (tr.getMethod().getRetryAnalyzer() != null) {
            Retry retryAnalyzer = (Retry) tr.getMethod().getRetryAnalyzer();
            if (retryAnalyzer.isOneMoreRetryAvailable()) {
                print("Skipping test for retry "+tr);
                tr.setStatus(ITestResult.SKIP);
                return;
            }
        }
        */
        print("Failed test "+tr);

    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        print("Skipped test "+tr);
    }

    @Override
    public void onTestStart(ITestResult tr) {
        print("Started test "+tr);
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        print("Successful test "+tr);
    }

    private void print(String message) {
        System.out.println(DATE_FORMAT.format(new Date()) + PREFIX + message + SUFFIX);
    }
}
