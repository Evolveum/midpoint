/*
 * Copyright (c) 2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.tools.testng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

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
		StringBuilder sb = new StringBuilder();
		sb.append(DATE_FORMAT.format(new Date()));
		sb.append(PREFIX);
		sb.append(message);
		sb.append(SUFFIX);
        System.out.println(sb.toString());
	}

}
