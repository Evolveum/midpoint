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

import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.List;
import java.util.Map;

/**
 *  IMPORTANT NOTES:
 *  3. Web subsystem filter was removed - this functionality is already covered with servlet filter
 *
 *  This class contains main filters for aspect events captured in MidpointAspect class. Here, we perform
 *  filtering based on subsystems and on subsystem profiling configuration (which is applied in this
 *  class as well). Some special filters are applied on events based on event method specification.
 *
 *  Currently, we only capture events after they're finished. This may be easily altered to capture events
 *  in the beginning of their execution, but this choice would double dump log size and that is not what
 *  we want.
 *
 *  After filtering, events are further processed by ProfilingDataManager singleton object
 *
 *  @author shood
 * */
public class AspectProfilingFilters {

    /* ATTRIBUTES - Class */

    private static Trace LOGGER = TraceManager.getTrace(AspectProfilingFilters.class);

    //Subsystems
    public static final String SUBSYSTEM_REPOSITORY = "REPOSITORY";
    public static final String SUBSYSTEM_TASKMANAGER = "TASKMANAGER";
    public static final String SUBSYSTEM_PROVISIONING = "PROVISIONING";
    public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "RESOURCEOBJECTCHANGELISTENER";
    public static final String SUBSYSTEM_MODEL = "MODEL";
    public static final String SUBSYSTEM_UCF = "UCF";
    public static final String SUBSYSTEM_WORKFLOW = "WORKFLOW";

    private static boolean isRepositoryProfiled = false;
    private static boolean isTaskManagerProfiled = false;
    private static boolean isProvisioningProfiled = false;
    private static boolean isResourceObjectChangeListenerProfiled = false;
    private static boolean isModelProfiled = false;
    private static boolean isUcfProfiled = false;
    private static boolean isWorkflowProfiled = false;

    private static final boolean GET_OBJECT_TYPE_REPOSITORY = true;
    private static final boolean GET_OBJECT_TYPE_TASK_MANAGER = true;
    private static final boolean GET_OBJECT_TYPE_PROVISIONING = true;
    private static final boolean GET_OBJECT_TYPE_SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = false;
    private static final boolean GET_OBJECT_TYPE_MODEL = true;
    private static final boolean GET_OBJECT_TYPE_UCF = false;
    private static final boolean GET_OBJECT_TYPE_WORKFLOW = true;

    private static final String MODEL_EXECUTE_CHANGES = "executeChanges";

    //Model subsystem constants
    public static final String DELTA_ADD = "ADD";
    public static final String DELTA_REPLACE = "REPLACE";
    public static final String DELTA_DELETE = "DELETE";

    /* GETTERS AND SETTERS */
    public static boolean isRepositoryProfiled() {
        return isRepositoryProfiled;
    }

    public static boolean isTaskManagerProfiled() {
        return isTaskManagerProfiled;
    }

    public static boolean isProvisioningProfiled() {
        return isProvisioningProfiled;
    }

    public static boolean isResourceObjectChangeListenerProfiled() {
        return isResourceObjectChangeListenerProfiled;
    }

    public static boolean isModelProfiled() {
        return isModelProfiled;
    }

    public static boolean isUcfProfiled() {
        return isUcfProfiled;
    }

    public static boolean isWorkflowProfiled() {
        return isWorkflowProfiled;
    }

    /* BEHAVIOR - STATIC */
    /*
    *   Here, we will decide, what filter will be applied (based on subsystem) on method entry
    */
    /*
    public static void applyGranularityFilterOnStart(ProceedingJoinPoint pjp, String subsystem){

        if(pjp == null)
            return;

        if(isRepositoryProfiled && SUBSYSTEM_REPOSITORY.equals(subsystem)){
            applyRepositoryFilterOnStart(pjp);
        } else if(isModelProfiled && SUBSYSTEM_MODEL.equals(subsystem)){
            applyModelFilterOnStart(pjp);
        } else if(isProvisioningProfiled && SUBSYSTEM_PROVISIONING.equals(subsystem)){
            applyProvisioningFilterOnStar(pjp);
        } else if(isTaskManagerProfiled && SUBSYSTEM_TASKMANAGER.equals(subsystem)){
            applyTaskManagerFilterOnStar(pjp);
        } else if(isUcfProfiled && SUBSYSTEM_UCF.equals(subsystem)){
            applyUcfFilterOnStar(pjp);
        } else if(isResourceObjectChangeListenerProfiled && SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER.equals(subsystem)){
            applyResourceObjectChangeListenerFilterOnStar(pjp);
        }
    }   //applyGranularityFilterOnStart
    */

    /*
    *   Here, we will decide, what filter will be applied (based on subsystem) on method end
    * */
    public static void applyGranularityFilterOnEnd(ProceedingJoinPoint pjp, String subsystem, long startTime){

        if(pjp == null)
            return;

        if(isRepositoryProfiled && SUBSYSTEM_REPOSITORY.equals(subsystem)){
            applyRepositoryFilterOnEnd(pjp, startTime);
        } else if(isModelProfiled && SUBSYSTEM_MODEL.equals(subsystem)){
            applyModelFilterOnEnd(pjp, startTime) ;
        } else if (isProvisioningProfiled && SUBSYSTEM_PROVISIONING.equals(subsystem)){
            applyProvisioningFilterOnEnd(pjp, startTime);
        } else if (isTaskManagerProfiled && SUBSYSTEM_TASKMANAGER.equals(subsystem)){
            applyTaskManagerFilterOnEnd(pjp, startTime);
        } else if (isUcfProfiled && SUBSYSTEM_UCF.equals(subsystem)){
            applyUcfFilterOnEnd(pjp, startTime);
        } else if(isResourceObjectChangeListenerProfiled && SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER.equals(subsystem)){
            applyResourceObjectChangeListenerFilterOnEnd(pjp, startTime);
        } else if(isWorkflowProfiled && SUBSYSTEM_WORKFLOW.equals(subsystem)){
            applyWorkflowFilterOnEnd(pjp, startTime);
        }

        ProfilingDataManager.getInstance().dumpToLog();

    }   //applyGranularityFilterOnEnd

    /*
    *   Workflow Filter - on method exit
    * */
    private static void applyWorkflowFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_WORKFLOW, startTime);
        ProfilingDataManager.getInstance().addWorkflowLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyWorkflowFilterOnEnd

    /*
    *   ResourceObjectChangeListener Filter - on method exit
    * */
    private static void applyResourceObjectChangeListenerFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER, startTime);
        ProfilingDataManager.getInstance().addResourceObjectChangeListenerLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyapplyResourceObjectChangeListenerFilterOnStarFilterOnEnd

    /*
    *   UCF Filter - on method exit
    * */
    private static void applyUcfFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_UCF, startTime);
        ProfilingDataManager.getInstance().addUcfLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyUcfFilterOnEnd

    /*
    *   Task Manager Filter - on method exit
    * */
    private static void applyTaskManagerFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_TASK_MANAGER, startTime);
        ProfilingDataManager.getInstance().addTaskManagerLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyTaskManagerFilterOnEnd

    /*
    *   Provisioning Filter - on method exit
    * */
    private static void applyProvisioningFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_PROVISIONING, startTime);
        ProfilingDataManager.getInstance().addProvisioningLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyProvisioningFilterOnEnd

    /*
    *   Model filter - on method exit
    * */
    private static void applyModelFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_MODEL, startTime);

        if(MODEL_EXECUTE_CHANGES.equals(profilingEvent.getMethodName())){
            profilingEvent.setObjectType(getDeltaType(pjp));
        }

        ProfilingDataManager.getInstance().addModelLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyModelFilterOnEnd

    /*
     *   Repository Filter - exit
     */
    private static void applyRepositoryFilterOnEnd(ProceedingJoinPoint pjp, long startTime){
        ProfilingDataLog profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_REPOSITORY, startTime);
        ProfilingDataManager.getInstance().addRepositoryLog(profilingEvent.getMethodName(), profilingEvent);
    }   //applyRepositoryFilterOnEnd


    /*===== SOME HELP METHODS =====*/
    /*
     *  Returns ObjectType with what operation operates
     */
    private static String getOperationType(ProceedingJoinPoint pjp){
        Object[] args = pjp.getArgs();

        if(args.length == 0)
            return "NO_ARGS";

        try{
            String[] splitType = PrettyPrinter.prettyPrint(args[0]).split("\\.");

            if(splitType.length < 1)
                return "null";
            else
                return splitType[splitType.length -1];

        } catch (Throwable t){
            LOGGER.error("Internal error formatting a value: {}", args[0], t);
            return "###INTERNAL#ERROR### "+t.getClass().getName()+": "+t.getMessage()+" value="+args[0];
        }
    }   //getOperationType

    /*
     *  Calculates estimated time on method exit
     */
    private static long calculateTime(long startTime){
        return (System.nanoTime() - startTime);
    }   //calculateTime

    /*
     *  Return type of delta
     */
    private static String getDeltaType(ProceedingJoinPoint pjp){
        String param = getOperationType(pjp);

        if(param.contains(DELTA_ADD))
            return DELTA_ADD;
        else if (param.contains(DELTA_DELETE))
            return DELTA_DELETE;
        else if (param.contains(DELTA_REPLACE))
            return DELTA_REPLACE;

        return "";
    }   //getDeltaType

    /**
     * Get joinpoint class name if available
     *
     */
    private static String getClassName(ProceedingJoinPoint pjp) {
        String className = null;
        if (pjp.getThis() != null) {
            className = pjp.getThis().getClass().getName();
            className = className.replaceFirst("com.evolveum.midpoint", "..");
        }
        return className;
    }   //getClassName

    /*
    *   Retrieves method name from pjp object
    * */
    private static String getMethodName(ProceedingJoinPoint pjp){
        return pjp.getSignature().getName();
    }   //getMethodName

    /*
    *   Prepares ProfilingDataLog object from provided ProceedingJoinPoint object
    *
    *   Based on entry boolean getObjectType - method adds working objectType to ProfilingDataLog object
    * */
    private static ProfilingDataLog prepareProfilingDataLog(ProceedingJoinPoint pjp, boolean getObjectType, long startTime){
        long eTime = calculateTime(startTime);
        long timestamp = System.currentTimeMillis();
        String className = getClassName(pjp);
        String method = getMethodName(pjp);

        ProfilingDataLog profilingEvent = new ProfilingDataLog(className, method, eTime, timestamp);

        if(getObjectType){
            String type = getOperationType(pjp);
            profilingEvent.setObjectType(type);
        }

        return profilingEvent;
    }   //prepareProfilingDataLog

    /*
    *   Configure profiled subsystems
    * */
    public static void subsystemConfiguration(Map<String, Boolean> subsystems){

        isModelProfiled = subsystems.get(SUBSYSTEM_MODEL);
        isProvisioningProfiled = subsystems.get(SUBSYSTEM_PROVISIONING);
        isRepositoryProfiled = subsystems.get(SUBSYSTEM_REPOSITORY);
        isResourceObjectChangeListenerProfiled = subsystems.get(SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
        isTaskManagerProfiled = subsystems.get(SUBSYSTEM_TASKMANAGER);
        isUcfProfiled = subsystems.get(SUBSYSTEM_UCF);
        isWorkflowProfiled = subsystems.get(SUBSYSTEM_WORKFLOW);

    }   //subsystemConfiguration

}
