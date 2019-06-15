/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.util.aspect;

import ch.qos.logback.classic.Level;
import com.evolveum.midpoint.util.PrettyPrinter;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides basically the functionality of MidpointInterceptor. However it was refactored to be callable also
 * outside of the context of AOP - manually by injecting appropriate code, mimicking MidpointInterceptor.invoke method.
 *
 * EXPERIMENTAL.
 */
public class MethodInvocationRecord {

	private static AtomicInteger idCounter = new AtomicInteger(0);

	private long startTime = System.nanoTime();
	private long elapsedTime;
	private int invocationId;
	private Object returnValue;
	private boolean gotException;
	private String exceptionName;

	private ProfilingDataManager.Subsystem subsystem;
	private String previousSubsystem;
	private String fullClassName;
	private String shortenedClassName;
	private String methodName;
	private int callDepth;

	private boolean debugEnabled;
	private boolean traceEnabled;

	private MethodInvocationRecord(String fullClassName, String methodName) {
		this.fullClassName = fullClassName;
		shortenedClassName = getClassName(fullClassName);
		subsystem = getSubsystem(fullClassName);
		this.methodName = methodName;
		Level localLevelOverride = MidpointInterceptor.threadLocalLevelOverride.get();
		if (MidpointInterceptor.globalLevelOverride == null && localLevelOverride == null) {
			debugEnabled = MidpointInterceptor.LOGGER_PROFILING.isDebugEnabled();
			traceEnabled = MidpointInterceptor.LOGGER_PROFILING.isTraceEnabled();
		} else {
			debugEnabled = isDebug(MidpointInterceptor.globalLevelOverride) || isDebug(localLevelOverride);
			traceEnabled = isTrace(MidpointInterceptor.globalLevelOverride) || isTrace(localLevelOverride);
		}
	}

	static MethodInvocationRecord create(MethodInvocation invocation) {
		MethodInvocationRecord ctx = new MethodInvocationRecord(getFullClassName(invocation), invocation.getMethod().getName() + "#");
		ctx.beforeCall(invocation.getArguments());
		return ctx;
	}

	public static MethodInvocationRecord create(String operationName, Object[] arguments) {
		int i = operationName.lastIndexOf('.');
		String className, methodName;
		if (i < 0) {
			className = operationName;
			methodName = "unknownMethod";
		} else {
			className = operationName.substring(0, i);
			methodName = operationName.substring(i+1);
		}
		MethodInvocationRecord ctx = new MethodInvocationRecord(className, methodName);
		ctx.beforeCall(arguments);
		return ctx;
	}

	private static String formatVal(Object value) {
		if (value == null) {
			return ("null");
		}
		try {
			return PrettyPrinter.prettyPrint(value);
		} catch (Throwable t) {
			MidpointInterceptor.LOGGER_PROFILING.error("Internal error formatting a value: {}", value, t);
			return "###INTERNAL#ERROR### " + t.getClass().getName() + ": " + t.getMessage() + " value=" + value;
		}
	}

	private boolean isDebug(Level level) {
		return level != null && Level.DEBUG.isGreaterOrEqual(level);
	}

	private boolean isTrace(Level level) {
		return level != null && Level.TRACE.isGreaterOrEqual(level);
	}

	private void beforeCall(Object[] arguments) {
		previousSubsystem = MidpointInterceptor.swapSubsystemMark(subsystem != null ? subsystem.name() : null);

		StringBuilder infoLog = new StringBuilder("#### Entry: ");

		if (debugEnabled) {
			invocationId = idCounter.incrementAndGet();
			infoLog.append(invocationId);

			if (traceEnabled) {
				String depthStringValue = MDC.get(MidpointInterceptor.MDC_DEPTH_KEY);
				if (depthStringValue == null || depthStringValue.isEmpty()) {
					callDepth = 1;
				} else {
					callDepth = Integer.parseInt(depthStringValue) + 1;
				}
				MDC.put(MidpointInterceptor.MDC_DEPTH_KEY, Integer.toString(callDepth));
				for (int i = 0; i < callDepth; i++) {
					infoLog.append(MidpointInterceptor.INDENT_STRING);
				}
			}

			infoLog.append(shortenedClassName);
			MidpointInterceptor.LOGGER_PROFILING.debug("{}->{}", infoLog, methodName);

			if (traceEnabled) {
				StringBuilder sb = new StringBuilder();
				sb.append("###### args: ");
				sb.append("(");
				if (arguments != null) {
					for (int i = 0; i < arguments.length; i++) {
						sb.append(formatVal(arguments[i]));
						if (arguments.length != i + 1) {
							sb.append(", ");
						}
					}
				}
				sb.append(")");
				MidpointInterceptor.LOGGER_PROFILING.trace(sb.toString());
			}
		}
	}

	Object processReturnValue(Object returnValue) {
		this.returnValue = returnValue;
		return returnValue;
	}

	public <T extends Throwable> T processException(T e) {
		exceptionName = e.getClass().getName();
		gotException = true;
		return e;
	}

	public void afterCall() {
		afterCall(null);
	}

	void afterCall(MethodInvocation invocation) {
		elapsedTime = System.nanoTime() - startTime;

		MethodsPerformanceMonitorImpl.INSTANCE.registerInvocationCompletion(this);

		if (traceEnabled) {
			MDC.put(MidpointInterceptor.MDC_DEPTH_KEY, Integer.toString(--callDepth));
		}

		// Restore previously marked subsystem executed before return
		if (debugEnabled) {
			StringBuilder sb = new StringBuilder();
			sb.append("##### Exit: ");
			sb.append(invocationId);
			sb.append(" ");
			if (traceEnabled) {
				for (int i = 0; i < callDepth + 1; i++) {
					sb.append(MidpointInterceptor.INDENT_STRING);
				}
			}
			sb.append(shortenedClassName);
			sb.append("->");
			sb.append(methodName);

			sb.append(" etime: ");
			MidpointInterceptor.formatExecutionTime(sb, elapsedTime);
			sb.append(" ms");

			MidpointInterceptor.LOGGER_PROFILING.debug(sb.toString());
			if (traceEnabled) {
				if (gotException) {
					MidpointInterceptor.LOGGER_PROFILING.trace("###### return exception: {}", exceptionName);
				} else {
					MidpointInterceptor.LOGGER_PROFILING.trace("###### retval: {}", formatVal(returnValue));
				}
			}
		}

		if (invocation != null && MidpointInterceptor.isProfilingActive) {
			long processingStartTime = System.nanoTime();
			ProfilingDataManager
					.getInstance().applyGranularityFilterOnEnd(shortenedClassName, invocation.getMethod().getName(),
					invocation.getArguments(), subsystem, startTime, processingStartTime);
		}

		MidpointInterceptor.swapSubsystemMark(previousSubsystem);
	}

	private ProfilingDataManager.Subsystem getSubsystem(String className) {
		if (className == null) {
			return null;
		}
		if (className.startsWith("com.evolveum.midpoint.repo")) {
			return ProfilingDataManager.Subsystem.REPOSITORY;
		} else if (className.startsWith("com.evolveum.midpoint.model.impl.sync")) {
			return ProfilingDataManager.Subsystem.SYNCHRONIZATION_SERVICE;
		} else if (className.startsWith("com.evolveum.midpoint.model")) {
			return ProfilingDataManager.Subsystem.MODEL;
		} else if (className.startsWith("com.evolveum.midpoint.provisioning")) {
			return ProfilingDataManager.Subsystem.PROVISIONING;
		} else if (className.startsWith("com.evolveum.midpoint.task")) {
			return ProfilingDataManager.Subsystem.TASK_MANAGER;
		} else if (className.startsWith("com.evolveum.midpoint.wf")) {
			return ProfilingDataManager.Subsystem.WORKFLOW;
		} else {
			return null;
		}
	}

	private String getClassName(String fullClassName) {
		return fullClassName != null ? fullClassName.replace("com.evolveum.midpoint", "..") : null;
	}

	private static String getFullClassName(MethodInvocation invocation) {
		if (invocation.getThis() != null) {
			return invocation.getThis().getClass().getName();
		} else {
			return null;
		}
	}

	public String getFullClassName() {
		return fullClassName;
	}

	public String getMethodName() {
		return methodName;
	}

	public long getElapsedTimeMicros() {
		return elapsedTime / 1000;
	}
}
