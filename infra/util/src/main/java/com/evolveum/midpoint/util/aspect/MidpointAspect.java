/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.evolveum.midpoint.util.PrettyPrinter;

/**
 *  TODO - add class description
 *
 *
 *  @author shood
 * */

@Aspect
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MidpointAspect {

	// This logger provide profiling informations
    final static Trace LOGGER = TraceManager.getTrace(MidpointAspect.class);

    //Defines status of Aspect based profiling
    private static boolean isProfilingActive = false;

	private static final String MDC_SUBSYSTEM_KEY = "subsystem";

    //Subsystems
	public static final String SUBSYSTEM_REPOSITORY = "REPO";
	public static final String SUBSYSTEM_TASKMANAGER = "TASK";
	public static final String SUBSYSTEM_PROVISIONING = "PROV";
	public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "ROCL";
	public static final String SUBSYSTEM_MODEL = "MODE";
	public static final String SUBSYSTEM_UCF = "_UCF";
	
	public static final String[] SUBSYSTEMS = { SUBSYSTEM_REPOSITORY, SUBSYSTEM_TASKMANAGER, SUBSYSTEM_PROVISIONING, 
		SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER, SUBSYSTEM_MODEL, SUBSYSTEM_UCF };

	@Around("entriesIntoRepository()")
	public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_REPOSITORY);
	}

    @Around("entriesIntoModel()")
    public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, SUBSYSTEM_MODEL);
    }

    @Around("entriesIntoProvisioning()")
    public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, SUBSYSTEM_PROVISIONING);
    }

    @Around("entriesIntoTaskManager()")
	public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_TASKMANAGER);
	}

    @Around("entriesIntoUcf()")
    public Object processUcfNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, SUBSYSTEM_UCF);
    }

    @Around("entriesIntoResourceObjectChangeListener()")
	public Object processResourceObjectChangeListenerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
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
	    Object retValue;
		String prev = null;
		// Profiling start
		long startTime = System.nanoTime();

		try {
			// Marking MDC->Subsystem with current one subsystem and mark
			// previous
			prev = swapSubsystemMark(subsystem);

			// if profiling info is needed - start
            if(isProfilingActive){
			    AspectProfilingFilters.applyGranularityFilterOnStart(pjp, subsystem);
            }

			// Process original call
			try {
				retValue = pjp.proceed();

			} catch (Exception e) {
				throw e;
			}
			// Return original response
			return retValue;

		} finally {
			// Restore previously marked subsystem executed before return

            if(isProfilingActive){
                AspectProfilingFilters.applyGranularityFilterOnEnd(pjp, subsystem, startTime);
            }

			// Restore MDC
			swapSubsystemMark(prev);
		}
	}

	@Pointcut("execution(* com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
	public void entriesIntoRepository() {
	}

    @Pointcut("execution(* com.evolveum.midpoint.model.api.ModelService.*(..))")
    public void entriesIntoModel() {
    }

    @Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
    public void entriesIntoProvisioning() {
    }

	@Pointcut("execution(* com.evolveum.midpoint.task.api.TaskManager.*(..))")
	public void entriesIntoTaskManager() {
	}

    @Pointcut("execution(* com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance.*(..)) " +
            "|| execution(* com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory.*(..))")
    public void entriesIntoUcf() {
    }

	@Pointcut("execution(* com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener.*(..))")
	public void entriesIntoResourceObjectChangeListener() {
	}


	/**
	 * Get joinpoint class name if available
	 *
	 * @param pjp
	 * @return
	 */
    @Deprecated
	private String getClassName(ProceedingJoinPoint pjp) {
		String className = null;
		if (pjp.getThis() != null) {
			className = pjp.getThis().getClass().getName();
			className = className.replaceFirst("com.evolveum.midpoint", "..");
		}
		return className;
	}

    /*
    *   Stores current depth value to MDC
    * */
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

    @Deprecated
    private String formatVal(Object value) {
		if (value == null) {
			return ("null");
		}
		try {
			return PrettyPrinter.prettyPrint(value);
		} catch (Throwable t) {
            LOGGER.error("Internal error formatting a value: {}", value, t);
			return "###INTERNAL#ERROR### "+t.getClass().getName()+": "+t.getMessage()+" value="+value;
		}
	}
	
}
