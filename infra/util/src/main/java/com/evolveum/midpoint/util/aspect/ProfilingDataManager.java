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

import java.sql.Timestamp;
import java.util.*;

/**
 *  IMPORTANT NOTES:
 *  1. Default dump interval is set to 30 minutes
 *
 *  This is a Singleton Class
 *
 *  ProfilingDataManager serves as a head of profiling data manipulation, configuration and dumping to log.
 *  Some of processes in this class are synchronized for obvious reasons.
 *
 *  @author shood
 * */
public class ProfilingDataManager {

    /*
    *   private instance of ProfilingDataManager
    * */
    private static ProfilingDataManager profilingDataManager = null;

    /* CONSTANTS */
    private static final int DEFAULT_DUMP_INTERVAL = 30;
    private static final int DEFAULT_PERF_DUMP_INTERVAL = 10;
    private static final byte TOP_TEN_METHOD_NUMBER = 5;

    //Subsystems
    public static final String SUBSYSTEM_REPOSITORY = "REPOSITORY";
    public static final String SUBSYSTEM_TASKMANAGER = "TASKMANAGER";
    public static final String SUBSYSTEM_PROVISIONING = "PROVISIONING";
    public static final String SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = "RESOURCEOBJECTCHANGELISTENER";
    public static final String SUBSYSTEM_MODEL = "MODEL";
    public static final String SUBSYSTEM_UCF = "UCF";
    public static final String SUBSYSTEM_WORKFLOW = "WORKFLOW";
    public static final String SUBSYSTEM_WEB = "WEB";

    private static final boolean GET_OBJECT_TYPE_REPOSITORY = true;
    private static final boolean GET_OBJECT_TYPE_TASK_MANAGER = true;
    private static final boolean GET_OBJECT_TYPE_PROVISIONING = true;
    private static final boolean GET_OBJECT_TYPE_SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER = false;
    private static final boolean GET_OBJECT_TYPE_MODEL = true;
    private static final boolean GET_OBJECT_TYPE_UCF = false;
    private static final boolean GET_OBJECT_TYPE_WORKFLOW = true;

    private static boolean isRepositoryProfiled = false;
    private static boolean isTaskManagerProfiled = false;
    private static boolean isProvisioningProfiled = false;
    private static boolean isResourceObjectChangeListenerProfiled = false;
    private static boolean isModelProfiled = false;
    private static boolean isUcfProfiled = false;
    private static boolean isWorkflowProfiled = false;
    private static boolean isWebProfiled = false;

    private static final String MODEL_EXECUTE_CHANGES = "executeChanges";

    //Model subsystem constants
    public static final String DELTA_ADD = "ADD";
    public static final String DELTA_REPLACE = "REPLACE";
    public static final String DELTA_DELETE = "DELETE";

    /* COMPARATOR */
    private static final ArrayComparator arrayComparator = new ArrayComparator();

    /* LOGGER */
    private static Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    /* ProfilingDataManager attributes */
    private long lastDumpTimestamp;
    private long lastPerformanceDumpTimestamp;
    private int minuteDumpInterval = DEFAULT_DUMP_INTERVAL;

    /* boolean triggers for profiling */
    private boolean isPerformanceProfiled = false;

    //Maps for profiling events

    /* profilingDataLogMap keys for individual midPoint interfaces */
    private Map<String, MethodUsageStatistics> performanceMap = new HashMap<String, MethodUsageStatistics>();



    /* Some more print constants */
    private static final String PRINT_RIGHT_ARROW = "->";

    /* ===BEHAVIOR=== */
    /*
    *   Retrieves instance of ProfilingDataManager
    * */
    public static ProfilingDataManager getInstance() {

        if(profilingDataManager == null){
            profilingDataManager = new ProfilingDataManager(DEFAULT_DUMP_INTERVAL, false);
        }

        return profilingDataManager;
    }   //getInstance

    /*
    *   ProfilingDataManager instance private constructor - not accessible from outside of this class
    * */
    private ProfilingDataManager(int dumpInterval, boolean performance) {
        //Configure timestamps
        this.isPerformanceProfiled = performance;
        this.minuteDumpInterval = dumpInterval;
        lastDumpTimestamp = System.currentTimeMillis();
        lastPerformanceDumpTimestamp = System.currentTimeMillis();

    }   //ProfilingDataManager

    /**
     *  Configures ProfilingDataManager - can be called from outside
     * */
    public void configureProfilingDataManager(Map<String, Boolean> profiledSubsystems, Integer dumpInterval, boolean subsystemProfilingActive, boolean performance, boolean request){

        isPerformanceProfiled = performance;

        if(subsystemProfilingActive || isPerformanceProfiled || request){
            MidpointAspect.activateSubsystemProfiling();
        }else {
            MidpointAspect.deactivateSubsystemProfiling();
        }

        subsystemConfiguration(profiledSubsystems);

        //Configure the dump interval
        if(dumpInterval != null && dumpInterval > 0){
            minuteDumpInterval = dumpInterval;
        }

        profilingDataManager = new ProfilingDataManager(minuteDumpInterval, performance);

    }   //configureProfilingDataManager

    /*
    *   Here, we will decide, what filter will be applied (based on subsystem) on method end
    * */
    public void applyGranularityFilterOnEnd(ProceedingJoinPoint pjp, String subsystem, long startTime){

        if(pjp == null)
            return;

        ProfilingDataLog profilingEvent;

        if(isRepositoryProfiled && SUBSYSTEM_REPOSITORY.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_REPOSITORY, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_REPOSITORY);

        } else if(isModelProfiled && SUBSYSTEM_MODEL.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_MODEL, startTime);

            if(MODEL_EXECUTE_CHANGES.equals(profilingEvent.getMethodName())){
                profilingEvent.setObjectType(getDeltaType(pjp));
            }

            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_MODEL);

        } else if (isProvisioningProfiled && SUBSYSTEM_PROVISIONING.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_PROVISIONING, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_PROVISIONING);

        } else if (isTaskManagerProfiled && SUBSYSTEM_TASKMANAGER.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_TASK_MANAGER, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_TASKMANAGER);

        } else if (isUcfProfiled && SUBSYSTEM_UCF.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_UCF, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_UCF);

        } else if(isResourceObjectChangeListenerProfiled && SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);

        } else if(isWorkflowProfiled && SUBSYSTEM_WORKFLOW.equals(subsystem)){
            profilingEvent = prepareProfilingDataLog(pjp, GET_OBJECT_TYPE_WORKFLOW, startTime);
            String key = prepareKey(profilingEvent);
            updateOverallStatistics(performanceMap, profilingEvent, key, SUBSYSTEM_WORKFLOW);
        }

        ProfilingDataManager.getInstance().dumpToLog();

    }   //applyGranularityFilterOnEnd

    /*
    *   Creates profiling event from captured servlet request
    * */
    public void prepareRequestProfilingEvent(ProfilingDataLog requestEvent){
        String key = requestEvent.getClassName();
        updateOverallStatistics(performanceMap, requestEvent, key, SUBSYSTEM_WEB);
    }   //prepareRequestProfilingEvent

    /*
    *   Prepares key to performance HashMap
    * */
    private String prepareKey(ProfilingDataLog log){
        String key = log.getClassName();
        key = key.concat(PRINT_RIGHT_ARROW);
        key = key.concat(log.getMethodName());

        return key;
    }   //prepareKey

    /*
    *   If the time is right, dump collected profiling information to log false
    *
    *   This method is synchronized
    * */
    public synchronized void dumpToLog(){

        long currentTime = System.currentTimeMillis();

        if(currentTime >= (lastDumpTimestamp + minutesToMillis(minuteDumpInterval))){
            if(LOGGER.isDebugEnabled()){

                //Print everything
                if(isModelProfiled){
                    printMap(performanceMap, SUBSYSTEM_MODEL);
                }
                if(isProvisioningProfiled) {
                    printMap(performanceMap, SUBSYSTEM_PROVISIONING);
                }
                if(isRepositoryProfiled)  {
                    printMap(performanceMap, SUBSYSTEM_REPOSITORY);
                }
                if(isTaskManagerProfiled) {
                    printMap(performanceMap, SUBSYSTEM_TASKMANAGER);
                }
                if(isUcfProfiled) {
                    printMap(performanceMap, SUBSYSTEM_UCF);
                }
                if(isWorkflowProfiled){
                    printMap(performanceMap, SUBSYSTEM_WORKFLOW);
                }
                if(isResourceObjectChangeListenerProfiled) {
                    printMap(performanceMap, SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
                }
                if(isWebProfiled){
                    printMap(performanceMap, SUBSYSTEM_WEB);
                }

                //Set next dump cycle
                lastDumpTimestamp = System.currentTimeMillis();
                cleanEverything();
            }
        }

        //Print performance statistics if needed
        if(isPerformanceProfiled){
            if(currentTime >= (lastPerformanceDumpTimestamp + minutesToMillis(DEFAULT_PERF_DUMP_INTERVAL))){
                new PerformanceStatistics(LOGGER);
                lastPerformanceDumpTimestamp = System.currentTimeMillis();
            }
        }
    }   //dumpToLog



    /* =====STATIC HELPER METHODS===== */
    /*
    *   Minutes to millis - transfer
    * */
    private static long minutesToMillis(int minutes){
        return (long)(minutes*60*1000);
    }   //minutesToMillis

    /*
    *   Updates overall statistics
    * */
    private static void updateOverallStatistics(Map<String, MethodUsageStatistics> logMap, ProfilingDataLog eventLog, String key, String subsystem){
        if(!logMap.containsKey(key)){
            logMap.put(key, new MethodUsageStatistics(eventLog, subsystem));
        } else {
            logMap.get(key).update(eventLog);
        }

        if(logMap.get(key).getSlowestMethodList().size() < TOP_TEN_METHOD_NUMBER){
            logMap.get(key).getSlowestMethodList().add(eventLog);
            sort(logMap.get(key).getSlowestMethodList());
        } else {
            if(logMap.get(key).getSlowestMethodList().get(logMap.get(key).getSlowestMethodList().size()-1).getEstimatedTime() < eventLog.getEstimatedTime()){
                logMap.get(key).getSlowestMethodList().add(eventLog);
                sort(logMap.get(key).getSlowestMethodList());
                logMap.get(key).setCurrentTopTenMin(logMap.get(key).getSlowestMethodList().get(logMap.get(key).getSlowestMethodList().size()-1).getEstimatedTime());
            }
        }

        if(logMap.get(key).getSlowestMethodList().size() > TOP_TEN_METHOD_NUMBER){
            logMap.get(key).getSlowestMethodList().remove(logMap.get(key).getSlowestMethodList().size()-1);
        }
    }   //updateOverallStatistics

    /*
    *   prints provided map to log
    * */
    private static void printMap(Map<String, MethodUsageStatistics> logMap, String subsystem){

        for(String key: logMap.keySet()){
            if(logMap.get(key) != null && subsystem.equals(logMap.get(key).getSubsystem())){
                logMap.get(key).appendToLogger(LOGGER);
            }
        }
    }   //printMap

    /*
    *   Configure profiled subsystems
    * */
    public void subsystemConfiguration(Map<String, Boolean> subsystems){

        isModelProfiled = subsystems.get(SUBSYSTEM_MODEL);
        isProvisioningProfiled = subsystems.get(SUBSYSTEM_PROVISIONING);
        isRepositoryProfiled = subsystems.get(SUBSYSTEM_REPOSITORY);
        isResourceObjectChangeListenerProfiled = subsystems.get(SUBSYSTEM_RESOURCEOBJECTCHANGELISTENER);
        isTaskManagerProfiled = subsystems.get(SUBSYSTEM_TASKMANAGER);
        isUcfProfiled = subsystems.get(SUBSYSTEM_UCF);
        isWorkflowProfiled = subsystems.get(SUBSYSTEM_WORKFLOW);
        isWebProfiled = subsystems.get(SUBSYSTEM_WEB);

    }   //subsystemConfiguration

    /*
    *   Cleans everything, all subsystem maps and top ten lists
    * */
    private void cleanEverything(){
        performanceMap.clear();
    }   //cleanEverything

    /*
    *   Prepares ProfilingDataLog object from provided ProceedingJoinPoint object
    *
    *   Based on entry boolean getObjectType - method adds working objectType to ProfilingDataLog object
    * */
    private ProfilingDataLog prepareProfilingDataLog(ProceedingJoinPoint pjp, boolean getObjectType, long startTime){
        long eTime = calculateTime(startTime);
        long timestamp = System.currentTimeMillis();
        String className = getClassName(pjp);
        String method = getMethodName(pjp);

        ProfilingDataLog profilingEvent = new ProfilingDataLog(className, method, eTime, timestamp, pjp);

        if(getObjectType){
            String type = getOperationType(pjp);
            profilingEvent.setObjectType(type);
        }

        return profilingEvent;
    }   //prepareProfilingDataLog

    /*
     *  Sorts ArrayList provided as the parameter
     */
    private static List<ProfilingDataLog> sort(List<ProfilingDataLog> list){
        Collections.sort(list, arrayComparator);
        return list;
    }

    /*
    *   Inner class ArrayComparator
    *   Compares two ProfilingDataLogs based on estimatedTime parameter
    * */
    private static class ArrayComparator implements Comparator<ProfilingDataLog>{

        @Override
        public int compare(ProfilingDataLog o1, ProfilingDataLog o2) {
            return ((Long)o2.getEstimatedTime()).compareTo(o1.getEstimatedTime());
        }

    }   //ArrayComparator inner-class

    /* =====STATIC HELPET METHODS=====*/
    /*
    *  Calculates estimated time on method exit
    */
    private static long calculateTime(long startTime){
        return (System.nanoTime() - startTime);
    }   //calculateTime

    /**
     * Get joinPoint class name if available
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

}
