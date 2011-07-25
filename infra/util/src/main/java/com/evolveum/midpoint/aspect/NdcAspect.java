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

import org.apache.log4j.NDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author Igor Farinic
 *
 */
@Aspect
public class NdcAspect {
	
    @Around("entriesIntoRepository()")
    public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
        return markSubsystem(pjp, "repository");
    }

    @Around("entriesIntoTaskManager()")
    public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "taskManager");
    }

    @Around("entriesIntoProvisioning()")
    public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "provisioning");
    }

    @Around("entriesIntoResourceObjectChangeListener()")
    public Object processResourceObjectChangeListenerNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "resourceObjectChangeListener");
    }
    
    @Around("entriesIntoModel()")
    public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "model");
    }
    
    @Around("entriesIntoWeb()")
    public Object processWebNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "web");
    }
    
	private Object markSubsystem(ProceedingJoinPoint pjp, String subsystem) throws Throwable {
		Object retValue = null;
        try {
        	NDC.push(subsystem);
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
	}
	
    @Pointcut("execution(public * com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
    public void entriesIntoRepository() {}

    @Pointcut("execution(public * com.evolveum.midpoint.task.api.TaskManager.*(..))")
    public void entriesIntoTaskManager() {}

    @Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
    public void entriesIntoProvisioning() {}

	@Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void entriesIntoResourceObjectChangeListener() {
	}
    @Pointcut("execution(public * com.evolveum.midpoint.model.api.ModelService.*(..))")
    public void entriesIntoModel() {}
    
    @Pointcut("execution(public * com.evolveum.midpoint.web..*(..))")
    public void entriesIntoWeb() {}
        
}
