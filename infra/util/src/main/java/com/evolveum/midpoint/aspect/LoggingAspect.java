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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.core.annotation.Order;

/**
 * @author Igor Farinic
 * 
 */
@Aspect
@Order(value = 2)
public class LoggingAspect {
	@Around("repositoryService() || provisioningService() || modelService()")
	public Object logMethodExecution(final ProceedingJoinPoint pjp) throws Throwable {
		final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(pjp.getSignature()
				.getDeclaringType().getName());

		String name = null;
		if (logger.isTraceEnabled()) {
			final Object[] args = pjp.getArgs();
			final String[] names = ((CodeSignature) pjp.getSignature()).getParameterNames();
			@SuppressWarnings("unchecked")
			final Class<CodeSignature>[] types = ((CodeSignature) pjp.getSignature()).getParameterTypes();
			name = ((CodeSignature) pjp.getSignature()).getName();
			final StringBuffer methodCallInfo = new StringBuffer();
			methodCallInfo.append("Entering method: " + name + "(");

			for (int i = 0; i < args.length; i++) {
				methodCallInfo.append(types[i].getName() + " : " + names[i] + " = '" + args[i] + "'");
				if (args.length == i + 1) {
					methodCallInfo.append(" )");
				} else {
					methodCallInfo.append(",\n");
				}
			}
			if (args.length == 0) {
				methodCallInfo.append(")");
			}

			logger.trace(methodCallInfo.toString());
		}

		final Object tmp = pjp.proceed();
		if (logger.isTraceEnabled()) {
			logger.trace("Successfully executed method: " + name + "(). Returned was: " + tmp);
		}
		return tmp;
	}

	@Pointcut("execution(public * com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
	public void repositoryService() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
	public void provisioningService() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.model.api.ModelService.*(..))")
	public void modelService() {
	}

}
