/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * EXPERIMENTAL.
 *
 * To be used, it must be registered e.g. by using the following annotation:
 *
 * Listeners({ com.evolveum.midpoint.tools.testng.CurrentTestResultHolder.class })
 *
 * By default we use it e.g. on all tests derived from AbstractIntegrationTest.
 */
public class CurrentTestResultHolder implements IInvokedMethodListener {

	// assumes we run single-threaded tests
	private static ITestResult currentTestResult;

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		currentTestResult = testResult;
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
	}

	// assumes we run single-threaded tests
	public static ITestResult getCurrentTestResult() {
		return currentTestResult;
	}

	// assumes that we run in a single thread
	public static Class<?> getCurrentTestClass() {
		return currentTestResult != null && currentTestResult.getTestClass() != null ?
				currentTestResult.getTestClass().getRealClass() : null;
	}

	// assumes that we run in a single thread
	public static boolean isTestClassSimpleName(String simpleName) {
		Class<?> tc = getCurrentTestClass();
		return tc != null && simpleName.equals(tc.getSimpleName());
	}

}
