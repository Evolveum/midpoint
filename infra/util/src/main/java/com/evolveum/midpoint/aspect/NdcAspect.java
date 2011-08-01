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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Igor Farinic
 *
 */
@Aspect
@Order(value=Ordered.HIGHEST_PRECEDENCE)
public class NdcAspect {
	
    @Around("entriesIntoRepository()")
    public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
        return markSubsystem(pjp, "REPOSITORY");
    }
    
    @Around("entriesIntoTaskManager()")
    public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "TASKMANAGER");
    }

    @Around("entriesIntoProvisioning()")
    public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "PROVISIONING");
    }

    @Around("entriesIntoResourceObjectChangeListener()")
    public Object processResourceObjectChangeListenerNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "RESOURCEOBJECTCHANGELISTENER");
    }
    
    @Around("entriesIntoModel()")
    public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "MODEL");
    }
    
    @Around("entriesIntoWeb()")
    public Object processWebNdc(ProceedingJoinPoint pjp) throws Throwable {
    	return markSubsystem(pjp, "WEB");
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
	
    @Pointcut("execution(* com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
    public void entriesIntoRepository() {}

    @Pointcut("execution(* com.evolveum.midpoint.task.api.TaskManager.*(..))")
    public void entriesIntoTaskManager() {}

    @Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
    public void entriesIntoProvisioning() {}

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void entriesIntoResourceObjectChangeListener() {
	}
	
	@Pointcut("execution(* com.evolveum.midpoint.model.api.ModelService.*(..))")
    public void entriesIntoModel() {}
    
    @Pointcut("execution(* com.evolveum.midpoint.web..*(..))")
    public void entriesIntoWeb() {}
        
}
