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
 * Portions Copyrighted 2011 Igor Farinic
 */
package com.evolveum.midpoint.aspect;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.NDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;

/**
 * @author Igor Farinic
 * 
 */
@Aspect
public class LoggingAspect {

	private static final org.slf4j.Logger LOGGER_ENTRIES_PARAMS = org.slf4j.LoggerFactory
			.getLogger("DEV_LOGGER_ENTRIES_PARAMS");
	private static final org.slf4j.Logger LOGGER_ENTRIES = org.slf4j.LoggerFactory
			.getLogger("DEV_LOGGER_ENTRIES");

	// FIXME: try to switch to spring injection. Note: infra components
	// shouldn't depend on spring
	// Formatters are statically initialized from class common's DebugUtil
	private static List<ObjectFormatter> formatters = new ArrayList<ObjectFormatter>();

	public static void registerFormatter(ObjectFormatter formatter) {
		formatters.add(formatter);
	}

	private static final String LOG_MESSAGE_PREFIX = "###";
	private static final String LOG_MESSAGE_ENTER = "ENTER";
	private static final String LOG_MESSAGE_EXIT = "EXIT";

	@Around("repositoryService()")
	public Object logRepoExecution(final ProceedingJoinPoint pjp) throws Throwable {
		return logMethodExecution(pjp);
	}

	@Around("provisioningService()")
	public Object logProvisioningExecution(final ProceedingJoinPoint pjp) throws Throwable {
		return logMethodExecution(pjp);
	}

	@Around("modelService()")
	public Object logModelExecution(final ProceedingJoinPoint pjp) throws Throwable {
		return logMethodExecution(pjp);
	}

	@Around("resourceObjectChangeListener()")
	public Object logResourceObjectChangeListenerExecution(final ProceedingJoinPoint pjp) throws Throwable {
		return logMethodExecution(pjp);
	}

	@Around("taskManager()")
	public Object logTaskManagerExecution(final ProceedingJoinPoint pjp) throws Throwable {
		return logMethodExecution(pjp);
	}

	private Object logMethodExecution(final ProceedingJoinPoint pjp) throws Throwable {

		String name = null;
		if (LOGGER_ENTRIES_PARAMS.isTraceEnabled() || LOGGER_ENTRIES.isInfoEnabled()) {
			final Object[] args = pjp.getArgs();
			final String[] names = ((CodeSignature) pjp.getSignature()).getParameterNames();
			@SuppressWarnings("unchecked")
			final Class<CodeSignature>[] types = ((CodeSignature) pjp.getSignature()).getParameterTypes();
			name = ((CodeSignature) pjp.getSignature()).getName();
			final StringBuffer methodCallInfo = new StringBuffer();
			methodCallInfo.append(LOG_MESSAGE_PREFIX + " " + LOG_MESSAGE_ENTER + " " + NDC.peek() + " "
					+ name + "(");

			if (LOGGER_ENTRIES_PARAMS.isTraceEnabled()) {
				for (int i = 0; i < args.length; i++) {
					methodCallInfo.append(formatVal(args[i]));

					if (args.length == i + 1) {
						methodCallInfo.append(")");
					} else {
						methodCallInfo.append(", ");
					}
				}
			}
			if (LOGGER_ENTRIES.isInfoEnabled()) {
				methodCallInfo.append("..");
			}
			
			if ((args.length == 0) || LOGGER_ENTRIES.isInfoEnabled() ) {
				methodCallInfo.append(")");
			}

			LOGGER_ENTRIES_PARAMS.trace(methodCallInfo.toString());
			LOGGER_ENTRIES.info(methodCallInfo.toString());
		}

		final Object tmp = pjp.proceed();
		if (LOGGER_ENTRIES_PARAMS.isTraceEnabled()) {
			LOGGER_ENTRIES_PARAMS.trace("{} {} {} {}(..): {}", new Object[]{LOG_MESSAGE_PREFIX, LOG_MESSAGE_EXIT, NDC.peek(), name, formatVal(tmp)});
		}
		if (LOGGER_ENTRIES.isInfoEnabled()) {
			LOGGER_ENTRIES.info("{} {} {} {}(..): ..", new Object[]{LOG_MESSAGE_PREFIX, LOG_MESSAGE_EXIT, NDC.peek(), name});
		}
		
		return tmp;
	}

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

	@Pointcut("execution(* com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
	public void repositoryService() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
	public void provisioningService() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void resourceObjectChangeListener() {
	}

	//@Pointcut("execution(* com.evolveum.midpoint.model.api.ModelService.*(..))")
	@Pointcut("execution(* com.evolveum.midpoint.model.controller.ModelController.*(..))")
	public void modelService() {
	}

	@Pointcut("execution(* com.evolveum.midpoint.task.api.TaskManager.*(..))")
	public void taskManager() {
	}

}
