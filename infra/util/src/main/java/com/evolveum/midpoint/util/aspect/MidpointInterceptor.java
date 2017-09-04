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

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.evolveum.midpoint.util.PrettyPrinter;

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

    private static AtomicInteger idcounter = new AtomicInteger(0);

	// This logger provide profiling informations
    private static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger("PROFILING");

    private static boolean isProfilingActive = false;

	private static final String MDC_SUBSYSTEM_KEY = "subsystem";
    public static final String INDENT_STRING = " ";

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		return wrapSubsystem(methodInvocation);
	}

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

	private Object wrapSubsystem(MethodInvocation invocation) throws Throwable {

		ProfilingDataManager.Subsystem subsystem = getSubsystem(invocation);

	    Object retValue = null;
		String prev = null;
        int id = 0;
        int d = 1;
        boolean exc = false;
        String excName = null;
        long elapsed;
		// Profiling start
		long startTime = System.nanoTime();

        final StringBuilder infoLog = new StringBuilder("#### Entry: ");

		try {
			// Marking MDC->Subsystem with current one subsystem and mark previous
			prev = swapSubsystemMark(subsystem != null ? subsystem.name() : null);

            if (LOGGER_PROFILING.isDebugEnabled()) {
                id = idcounter.incrementAndGet();
                infoLog.append(id);
            }

            if (LOGGER_PROFILING.isTraceEnabled()) {
                String depth = MDC.get("depth");
                if (depth == null || depth.isEmpty()) {
                    d = 0;
                } else {
                    d = Integer.parseInt(depth);
                }
                d++;
                MDC.put("depth", Integer.toString(d));
                for (int i = 0; i < d; i++) {
                    infoLog.append(INDENT_STRING);
                }
            }

            // is profiling info is needed
            if (LOGGER_PROFILING.isDebugEnabled()) {
                infoLog.append(getClassName(invocation));
                LOGGER_PROFILING.debug("{}->{}", infoLog, invocation.getMethod().getName());

                // If debug enable get entry parameters and log them
                if (LOGGER_PROFILING.isTraceEnabled()) {
                    final Object[] args = invocation.getArguments();
                    final StringBuilder sb = new StringBuilder();
                    sb.append("###### args: ");
                    sb.append("(");
                    for (int i = 0; i < args.length; i++) {
                        sb.append(formatVal(args[i]));
                        if (args.length != i + 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append(")");
                    LOGGER_PROFILING.trace(sb.toString());
                }
            }

            //We dont need profiling on method start in current version
			// if profiling info is needed - start
            //if(isProfilingActive){
            //    LOGGER.info("Profiling is active: onStart");
			//    AspectProfilingFilters.applyGranularityFilterOnStart(pjp, subsystem);
            //}

			// Process original call
			try {
				retValue = invocation.proceed();
			} catch (Exception e) {
                excName = e.getClass().getName();
                exc = true;
				throw e;
			}
			return retValue;
		} finally {
            if (LOGGER_PROFILING.isTraceEnabled()) {
                d--;
                MDC.put("depth", Integer.toString(d));
            }

            // Restore previously marked subsystem executed before return
            if (LOGGER_PROFILING.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("##### Exit: ");
                if (LOGGER_PROFILING.isDebugEnabled()) {
                    sb.append(id);
                    sb.append(" ");
                }
                // sb.append("/");
                if (LOGGER_PROFILING.isTraceEnabled()) {
                    for (int i = 0; i < d + 1; i++) {
                        sb.append(INDENT_STRING);
                    }
                }
                sb.append(getClassName(invocation));
                sb.append("->");
                sb.append(invocation.getMethod().getName());

                if (LOGGER_PROFILING.isDebugEnabled()) {
                    sb.append(" etime: ");
                    // Mark end of processing
                    elapsed = System.nanoTime() - startTime;
                    sb.append((long) (elapsed / 1000000));
                    sb.append('.');
                    long mikros = (long) (elapsed / 1000) % 1000;
                    if (mikros < 100) {
                        sb.append('0');
                    }
                    if (mikros < 10) {
                        sb.append('0');
                    }
                    sb.append(mikros);
                    sb.append(" ms");
                }

                LOGGER_PROFILING.debug(sb.toString());
                if (LOGGER_PROFILING.isTraceEnabled()) {
                    if (exc) {
                        LOGGER_PROFILING.trace("###### return exception: {}", excName);
                    } else {
                        LOGGER_PROFILING.trace("###### retval: {}", formatVal(retValue));
                    }
                }
            }

            if (isProfilingActive) {
				Long processingStartTime = System.nanoTime();
				ProfilingDataManager.getInstance().applyGranularityFilterOnEnd(getClassName(invocation), invocation.getMethod().getName(), invocation.getArguments(), subsystem, startTime, processingStartTime);
            }

			// Restore MDC
			swapSubsystemMark(prev);
		}
	}

	private ProfilingDataManager.Subsystem getSubsystem(MethodInvocation invocation) {
		String className = getFullClassName(invocation);
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

	/**
     * Get joinPoint class name if available
     *
     */
    private String getClassName(MethodInvocation invocation) {
		String className = getFullClassName(invocation);
		return className != null ? className.replaceFirst("com.evolveum.midpoint", "..") : null;
	}

	private String getFullClassName(MethodInvocation invocation) {
		String className = null;
		if (invocation.getThis() != null) {
			className = invocation.getThis().getClass().getName();
		}
		return className;
	}

    /*
    *   Stores current depth value to MDC
    * */
	@Deprecated
	protected static void storeMDC(int d){
		MDC.put("depth", Integer.toString(d));
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

    private String formatVal(Object value) {
		if (value == null) {
			return ("null");
		}
		try {
			return PrettyPrinter.prettyPrint(value);
		} catch (Throwable t) {
            LOGGER_PROFILING.error("Internal error formatting a value: {}", value, t);
			return "###INTERNAL#ERROR### "+t.getClass().getName()+": "+t.getMessage()+" value="+value;
		}
	}

}
