package com.evolveum.midpoint.aspect;

import org.apache.log4j.NDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class NdcAspect {

    @Around("entriesIntoRepository()")
    public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
        Object retValue = null;
        try {
        	NDC.push("repository");
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
    }

    @Around("entriesIntoTaskManager()")
    public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
        Object retValue = null;
        try {
        	NDC.push("task-manager");
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
    }

    @Around("entriesIntoProvisioning()")
    public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
        Object retValue = null;
        try {
        	NDC.push("provisioning");
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
    }

    @Around("entriesIntoModel()")
    public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
        Object retValue = null;
        try {
        	NDC.push("model");
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
    }
    
    @Around("entriesIntoWeb()")
    public Object processWebNdc(ProceedingJoinPoint pjp) throws Throwable {
        Object retValue = null;
        try {
        	NDC.push("web");
            retValue = pjp.proceed();
            return retValue;
        } finally {
            NDC.pop();
        }
    }
    
    @Pointcut("execution(public * com.evolveum.midpoint.repo..*(..))")
    public void entriesIntoRepository() {}

    @Pointcut("execution(public * com.evolveum.midpoint.task..*(..))")
    public void entriesIntoTaskManager() {}

    @Pointcut("execution(public * com.evolveum.midpoint.provisioning..*(..))")
    public void entriesIntoProvisioning() {}

    @Pointcut("execution(public * com.evolveum.midpoint.model..*(..))")
    public void entriesIntoModel() {}
    
    @Pointcut("execution(public * com.evolveum.midpoint.web..*(..))")
    public void entriesIntoWeb() {}
        
}
