/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.testng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.testng.IConfigurationListener;
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
public class TestMethodLoggerListener implements IInvokedMethodListener, IConfigurationListener {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        System.out.println(DATE_FORMAT.format(new Date()));
        System.out.println("----[ TestNG running method: " + method.getTestMethod().toString() + " ("+testResult+") ]----");
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        System.out.println(DATE_FORMAT.format(new Date()));
        System.out.println("----[ TestNG finished method : " + method.getTestMethod().toString() + " Result: " + (testResult.isSuccess() ? "PASS" : "FAIL")+" ]----");
    }

    @Override
    public void onConfigurationFailure(ITestResult res) {
        System.out.println("--- TestNG configuration failure : " + res.getTestName());
    }

    @Override
    public void onConfigurationSkip(ITestResult res) {
        System.out.println("--- TestNG configuration skip : " + res.getTestName());
    }

    @Override
    public void onConfigurationSuccess(ITestResult res) {
        System.out.println("--- TestNG configuration success : " + res.getTestName());
    }
}
