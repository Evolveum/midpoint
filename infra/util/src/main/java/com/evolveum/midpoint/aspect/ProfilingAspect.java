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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.util.StopWatch;

/**
 * @author Igor Farinic
 *
 */
@Aspect
public class ProfilingAspect {

    private static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger("LOGGER_PROFILING");

    @Around("repositoryService() || provisioningService() || modelService() || resourceObjectChangeListener() || taskManager()")
    public Object profile(final ProceedingJoinPoint pjp) throws Throwable {
    	
        if (LOGGER_PROFILING.isTraceEnabled()) {
            StringBuilder message = new StringBuilder();
            message.append("Entry: ");
            message.append(getClassName(pjp));
            message.append(" ");
            message.append(pjp.getSignature().getName());
            LOGGER_PROFILING.trace(message.toString());
        }

        StopWatch sw = new StopWatch(getClass().getSimpleName());
        Object retValue = null;
        try {
            sw.start(pjp.getSignature().getName());
            retValue = pjp.proceed();
            return retValue;
        } finally {
            sw.stop();

            if (LOGGER_PROFILING.isTraceEnabled()) {
                StringBuilder message = new StringBuilder();
                message.append("Exit: ");
                message.append(getClassName(pjp));
                message.append(" ");
                message.append(pjp.getSignature().getName());
                message.append(", time: ");
                message.append(sw.getTotalTimeMillis());
                message.append(" ms");

                LOGGER_PROFILING.trace(message.toString());
            }
        }
    }

    private String getClassName(ProceedingJoinPoint pjp) {
        if (pjp.getThis() != null) {
            return pjp.getThis().getClass().getName();
        }

        return null;
    }

    @Pointcut("execution(public * com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
	public void repositoryService() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
	public void provisioningService() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void resourceObjectChangeListener() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.model.api.ModelService.*(..))")
	public void modelService() {
	}

	@Pointcut("execution(public * com.evolveum.midpoint.task.api.TaskManager.*(..))")
	public void taskManager() {
	}
    
}
