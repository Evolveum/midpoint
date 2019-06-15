/*
 * Copyright (c) 2010-2017 Evolveum
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
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 *  In this class, we define some Pointcuts in AOP meaning that will provide join points for most common
 *  methods used in main midPoint subsystems. We wrap these methods with profiling wrappers.
 *
 *  This class also serves another purpose - it is used for basic Method Entry/Exit or args profiling,
 *  results from which are dumped to idm.log (by default)
 *
 *  @author shood
 * */

@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MidpointInterceptor implements MethodInterceptor {

	public static final String PROFILING_LOGGER_NAME = "PROFILING";

	// This logger provide profiling information
	static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger(PROFILING_LOGGER_NAME);

    static boolean isProfilingActive = false;

    static Level globalLevelOverride = null;
    static final ThreadLocal<Level> threadLocalLevelOverride = new ThreadLocal<>();

	private static final String MDC_SUBSYSTEM_KEY = "subsystem";
	static final String MDC_DEPTH_KEY = "depth";

    public static final String INDENT_STRING = " ";

	// This is made public to use in testing
	public static String swapSubsystemMark(String subsystemName) {
		String prev = MDC.get(MDC_SUBSYSTEM_KEY);
		if (subsystemName == null) {
			MDC.remove(MDC_SUBSYSTEM_KEY);
		} else {
			MDC.put(MDC_SUBSYSTEM_KEY, subsystemName);
		}
		return prev;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		MethodInvocationRecord ctx = MethodInvocationRecord.create(invocation);
		try {
			return ctx.processReturnValue(invocation.proceed());
		} catch (Throwable e) {
			throw ctx.processException(e);
		} finally {
			ctx.afterCall(invocation);
		}
	}

	static void formatExecutionTime(StringBuilder sb, long elapsed) {
		sb.append(elapsed / 1000000);
		sb.append('.');
		long micros = (elapsed / 1000) % 1000;
		if (micros < 100) {
		    sb.append('0');
		}
		if (micros < 10) {
		    sb.append('0');
		}
		sb.append(micros);
	}



    /*
    *   Activates aspect based subsystem profiling
    * */
    public static void activateSubsystemProfiling(){
        isProfilingActive = true;
    }

    /*
    *   Deactivates aspect based subsystem profiling
    * */
    public static void deactivateSubsystemProfiling(){
        isProfilingActive = false;
    }

	public static Level getGlobalMethodInvocationLevelOverride() {
		return globalLevelOverride;
	}

	public static void setGlobalMethodInvocationLevelOverride(Level value) {
		globalLevelOverride = value;
	}

	public static Level getLocalMethodInvocationLevelOverride() {
		return threadLocalLevelOverride.get();
	}

	public static void setLocalMethodInvocationLevelOverride(Level value) {
		threadLocalLevelOverride.set(value);
	}
}
