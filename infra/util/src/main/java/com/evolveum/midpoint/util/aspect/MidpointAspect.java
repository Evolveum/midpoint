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

import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

@Aspect
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MidpointAspect {

    private static AtomicInteger idcounter = new AtomicInteger(0);

	// This logger provide profiling informations
    private static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger("PROFILING");
    //private static Trace LOGGER = TraceManager.getTrace(MidpointAspect.class);

    //Defines status of Aspect based profiling
    private static boolean isProfilingActive = false;

	private static final String MDC_SUBSYSTEM_KEY = "subsystem";
    public static final String INDENT_STRING = " ";

	@Around("entriesIntoRepository()")
	public Object processRepositoryNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.REPOSITORY);
	}

    @Around("entriesIntoModel()")
    public Object processModelNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.MODEL);
    }

    @Around("entriesIntoProvisioning()")
    public Object processProvisioningNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.PROVISIONING);
    }

    @Around("entriesIntoTaskManager()")
	public Object processTaskManagerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.TASK_MANAGER);
	}

    @Around("entriesIntoUcf()")
    public Object processUcfNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.UCF);
    }

    @Around("entriesIntoResourceObjectChangeListener()")
	public Object processResourceObjectChangeListenerNdc(ProceedingJoinPoint pjp) throws Throwable {
		return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.RESOURCE_OBJECT_CHANGE_LISTENER);
	}

    @Around("entriesIntoWorkflow()")
    public Object proccessWorkflowNdc(ProceedingJoinPoint pjp) throws Throwable {
        return wrapSubsystem(pjp, ProfilingDataManager.Subsystem.WORKFLOW);
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

	private Object wrapSubsystem(ProceedingJoinPoint pjp, ProfilingDataManager.Subsystem subsystem) throws Throwable {
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
			// Marking MDC->Subsystem with current one subsystem and mark
			// previous
			prev = swapSubsystemMark(subsystem.name());

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

            //We dont need profiling on method start in current version
			// if profiling info is needed - start
            //if(isProfilingActive){
            //    LOGGER.info("Profiling is active: onStart");
			//    AspectProfilingFilters.applyGranularityFilterOnStart(pjp, subsystem);
            //}

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

            if(isProfilingActive){

                if(pjp != null){

                    Long processingStartTime = System.nanoTime();
                    ProfilingDataManager.getInstance().applyGranularityFilterOnEnd(getClassName(pjp), getMethodName(pjp) ,pjp.getArgs(), subsystem, startTime, processingStartTime);
                }
            }

			// Restore MDC
			swapSubsystemMark(prev);
		}
	}

    /**
     * Get joinPoint class name if available
     *
     */
    private String getClassName(ProceedingJoinPoint pjp) {
        String className = null;
        if (pjp.getThis() != null) {
            className = pjp.getThis().getClass().getName();
            className = className.replaceFirst("com.evolveum.midpoint", "..");
        }
        return className;
    }

    /*
    *   Retrieves method name from pjp object
    * */
    private String getMethodName(ProceedingJoinPoint pjp){
        return pjp.getSignature().getName();
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

    @Pointcut("execution(* com.evolveum.midpoint.wf.api.WorkflowService.*(..))")
    public void entriesIntoWorkflow(){

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
