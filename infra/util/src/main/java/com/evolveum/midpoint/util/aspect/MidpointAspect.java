/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 []
 * Portions Copyrighted 2011 Igor Farinic
 * Portions Copyrighted 2011 Peter Prochazka
 */
package com.evolveum.midpoint.util.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Aspect
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MidpointAspect {

	public static final String INDENT_STRING = " ";

	private static AtomicInteger idcounter = new AtomicInteger(0);
	private static AtomicInteger subidcounter = new AtomicInteger(0);

	// This logger provide profiling informations
	private static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger("PROFILING");

	private static final String MDC_SUBSYSTEM_KEY = "subsystem";
	
	public static final String SUBSYSTEM_REPOSITORY = "REPOSITORY";
	public static final String SUBSYSTEM_TASKMANAGER = "TASKMANAGER";
	public static final String SUBSYSTEM_PROVISIONING = "PROVISIONING";
	public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "RESOURCEOBJECTCHANGELISTENER";
	public static final String SUBSYSTEM_MODEL = "MODEL";
	public static final String SUBSYSTEM_WEB = "WEB";
	public static final String SUBSYSTEM_UCF = "UCF";
	
	public static final String[] SUBSYSTEMS = { SUBSYSTEM_REPOSITORY, SUBSYSTEM_TASKMANAGER, SUBSYSTEM_PROVISIONING, 
		SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER, SUBSYSTEM_MODEL, SUBSYSTEM_WEB, SUBSYSTEM_UCF };

	// FIXME: try to switch to spring injection. Note: infra components
	// shouldn't depend on spring
	// Formatters are statically initialized from class common's DebugUtil
	private static List<ObjectFormatter> formatters = new ArrayList<ObjectFormatter>();

	/**
	 * Register new formatter
	 *
	 * @param formatter
	 */
	public static void registerFormatter(ObjectFormatter formatter) {
		formatters.add(formatter);
	}

	@Around("entriesIntoRepository()")
	public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_REPOSITORY);
	}

	@Around("entriesIntoTaskManager()")
	public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_TASKMANAGER);
	}

	@Around("entriesIntoProvisioning()")
	public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_PROVISIONING);
	}

	@Around("entriesIntoResourceObjectChangeListener()")
	public Object processResourceObjectChangeListenerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
	}

	@Around("entriesIntoModel()")
	public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_MODEL);
	}

	@Around("entriesIntoWeb()")
	public Object processWebNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_WEB);
	}

	@Around("entriesIntoUcf()")
	public Object processUcfNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_UCF);
	}
	
	// This is made public to use in testing
	public static String swapSubsystemMark(String subsystemName) {
		String prev = (String) MDC.get(MDC_SUBSYSTEM_KEY);
		if (subsystemName == null) {
			MDC.remove(MDC_SUBSYSTEM_KEY);
		} else {
			MDC.put(MDC_SUBSYSTEM_KEY, subsystemName);
		}
		return prev;
	}

	private Object wrapSubsystem(ProceedingJoinPoint pjp, String subsystem) throws Throwable {
		Object retValue = null;
		String prev = null;
		int id = 0;
		int d = 1;
		boolean exc = false;
		String excName = null;
		// Profiling start
		long startTime = System.nanoTime();

		final StringBuilder infoLog = new StringBuilder("#### Entry: ");

		try {
			// Marking MDC->Subsystem with current one subsystem and mark
			// previous
			prev = swapSubsystemMark(subsystem);

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
				infoLog.append(getClassName(pjp));
				LOGGER_PROFILING.debug("{}->{}", infoLog, pjp.getSignature().getName());

				// If debug enable get entry parameters and log them
				if (LOGGER_PROFILING.isTraceEnabled()) {
					final Object[] args = pjp.getArgs();
					// final String[] names = ((CodeSignature)
					// pjp.getSignature()).getParameterNames();
					// @SuppressWarnings("unchecked")
					// final Class<CodeSignature>[] types = ((CodeSignature)
					// pjp.getSignature()).getParameterTypes();
					final StringBuffer sb = new StringBuffer();
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

			// Process original call
			try {
				retValue = pjp.proceed();

			} catch (Exception e) {
				excName = e.getClass().getName();
				exc = true;
				throw e;
			}
			// Return original response
			return retValue;

		} finally {
			// Depth -1
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
				sb.append(getClassName(pjp));
				sb.append("->");
				sb.append(pjp.getSignature().getName());

				if (LOGGER_PROFILING.isDebugEnabled()) {
					sb.append(" etime: ");
					// Mark end of processing
					long elapsed = System.nanoTime() - startTime;
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
			// Restore MDC
			swapSubsystemMark(prev);
		}
	}

	@Pointcut("execution(* com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
	public void entriesIntoRepository() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.task.api.TaskManager.*(..))")
	public void entriesIntoTaskManager() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
	public void entriesIntoProvisioning() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void entriesIntoResourceObjectChangeListener() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.model.api.ModelService.*(..))")
	public void entriesIntoModel() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.web.controller..*.*(..)) " +
            "&& !execution(public * com.evolveum.midpoint.web.controller..*.get*(..)) " +
            "&& !execution(public * com.evolveum.midpoint.web.controller..*.set*(..))" +
            "&& !execution(public * com.evolveum.midpoint.web.controller..*.is*(..))" +
            "&& !execution(* com.evolveum.midpoint.web.controller.Language..*.*(..))")
	public void entriesIntoWeb() {
	}

//	@Pointcut("execution(* com.evolveum.midpoint.web.model.impl..*.*(..))")
//	public void entriesIntoWeb() {
//	}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance.*(..)) " +
			"|| execution(* com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory.*(..))")
	public void entriesIntoUcf() {
	}

	/**
	 * Get joinpoint class name if available
	 *
	 * @param pjp
	 * @return
	 */
	private String getClassName(ProceedingJoinPoint pjp) {
		String className = null;
		if (pjp.getThis() != null) {
			className = pjp.getThis().getClass().getName();
			className = className.replaceFirst("com.evolveum.midpoint", "..");
		}
		return className;
	}

	/**
	 * Debug output formater
	 *
	 * @param value
	 * @return
	 */

	private String formatVal(Object value) {
		if (value == null) {
			return ("null");
		} else {
			String out = null;
			for (ObjectFormatter formatter : formatters) {
				out = formatter.format(value);
				if (out != null) {
					break;
				}
			}
			if (out == null) {
				return (value.toString());
			} else {
				return out;
			}
		}
	}
}
