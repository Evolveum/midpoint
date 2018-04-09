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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

    private static final int DEFAULT_DUMP_INTERVAL = 30;
    private static final int DEFAULT_PERF_DUMP_INTERVAL = 10;
    private static byte TOP_TEN_METHOD_NUMBER = 5;

    private static boolean profilingTest = false;

    public static enum Subsystem {
        REPOSITORY,
        TASK_MANAGER,
        PROVISIONING,
		SYNCHRONIZATION_SERVICE,
        MODEL,
        UCF,
        WORKFLOW,
        WEB
    }

    public static final List<Subsystem> subsystems = Arrays.asList(Subsystem.values());

    public static final String INDENT_STRING = " ";
    private static final String ARGS_NULL = "NULL";
    private static final String ARGS_EMPTY = "NO ARGS";

    private static boolean isRepositoryProfiled = false;
    private static boolean isTaskManagerProfiled = false;
    private static boolean isProvisioningProfiled = false;
    private static boolean isSynchronizationServiceProfiled = false;
    private static boolean isModelProfiled = false;
    private static boolean isUcfProfiled = false;
    private static boolean isWorkflowProfiled = false;
    private static boolean isWebProfiled = false;

    private static final ArrayComparator arrayComparator = new ArrayComparator();

    private static Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    private long lastDumpTimestamp;
    private long lastPerformanceDumpTimestamp;
    private int minuteDumpInterval = DEFAULT_DUMP_INTERVAL;

    private boolean isPerformanceProfiled = false;

    private Map<String, MethodUsageStatistics> performanceMap = new HashMap<>();

    private static final String PRINT_RIGHT_ARROW = "->";

    public static ProfilingDataManager getInstance() {

        if(profilingDataManager == null){
            profilingDataManager = new ProfilingDataManager(DEFAULT_DUMP_INTERVAL, false);
        }

        return profilingDataManager;
    }

    private ProfilingDataManager(int dumpInterval, boolean performance) {
        this.isPerformanceProfiled = performance;
        this.minuteDumpInterval = dumpInterval;
        lastDumpTimestamp = System.currentTimeMillis();
        lastPerformanceDumpTimestamp = System.currentTimeMillis();
    }

    public void configureProfilingDataManager(Map<Subsystem, Boolean> profiledSubsystems, Integer dumpInterval,
                                              boolean subsystemProfilingActive, boolean performance, boolean request){

        isPerformanceProfiled = performance;

        if(subsystemProfilingActive || isPerformanceProfiled || request){
            MidpointInterceptor.activateSubsystemProfiling();
        }else {
            MidpointInterceptor.deactivateSubsystemProfiling();
        }

        subsystemConfiguration(profiledSubsystems);

        //Configure the dump interval
        if(dumpInterval != null && dumpInterval > 0){
            minuteDumpInterval = dumpInterval;
        }

        profilingTest = false;
        profilingDataManager = new ProfilingDataManager(minuteDumpInterval, performance);
    }

    public void configureProfilingDataManagerForTest(Map<Subsystem, Boolean> subsystems, boolean performance){
        subsystemConfiguration(subsystems);

        TOP_TEN_METHOD_NUMBER = 10;
        profilingDataManager = new ProfilingDataManager(30, performance);
        profilingTest = true;
    }

    public void applyGranularityFilterOnEnd(String className, String methodName, Object[] args, Subsystem subsystem, long startTime, long processingStartTime){

        ProfilingDataLog profilingEvent;
        profilingEvent = prepareProfilingDataLog(className, methodName, startTime, args);
        String key = prepareKey(profilingEvent);

        if(isRepositoryProfiled && Subsystem.REPOSITORY.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key,  Subsystem.REPOSITORY);

        } else if(isModelProfiled && Subsystem.MODEL.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.MODEL);

        } else if (isProvisioningProfiled && Subsystem.PROVISIONING.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.PROVISIONING);

        } else if (isTaskManagerProfiled && Subsystem.TASK_MANAGER.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.TASK_MANAGER);

        } else if (isUcfProfiled && Subsystem.UCF.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.UCF);

        } else if(isSynchronizationServiceProfiled && Subsystem.SYNCHRONIZATION_SERVICE.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.SYNCHRONIZATION_SERVICE);

        } else if(isWorkflowProfiled && Subsystem.WORKFLOW.equals(subsystem)){
            updateOverallStatistics(performanceMap, profilingEvent, key, Subsystem.WORKFLOW);
        }

        Long processingEstTime = System.nanoTime() - processingStartTime;
        logEventProcessingDuration(key, processingEstTime);

        ProfilingDataManager.getInstance().dumpToLog();
    }

    private void logEventProcessingDuration(String key, long est){
        if(performanceMap.get(key) != null)
            performanceMap.get(key).updateProcessTimeList(est);
    }

    public void prepareRequestProfilingEvent(ProfilingDataLog requestEvent){
        String key = requestEvent.getClassName();
        updateOverallStatistics(performanceMap, requestEvent, key, Subsystem.WEB);
    }

    private String prepareKey(ProfilingDataLog log){
        String key = log.getClassName();
        key = key.concat(PRINT_RIGHT_ARROW);
        key = key.concat(log.getMethodName());

        return key;
    }

    public synchronized void dumpToLog(){
        if(profilingTest){
            return;
        }

        long currentTime = System.currentTimeMillis();

        if(currentTime >= (lastDumpTimestamp + minutesToMillis(minuteDumpInterval))){
            if(LOGGER.isDebugEnabled()){

                printEverything(false);

                //Set next dump cycle
                lastDumpTimestamp = System.currentTimeMillis();
                cleanEverything();
            }
        }

        //Print performance statistics if needed
        if(isPerformanceProfiled){
            if(currentTime >= (lastPerformanceDumpTimestamp + minutesToMillis(DEFAULT_PERF_DUMP_INTERVAL))){
                new PerformanceStatistics();
                lastPerformanceDumpTimestamp = System.currentTimeMillis();
            }
        }
    }

    public void printEverything(boolean afterTest){
        if(isModelProfiled){
            printMap(performanceMap, Subsystem.MODEL, afterTest);
        }
        if(isProvisioningProfiled) {
            printMap(performanceMap, Subsystem.PROVISIONING, afterTest);
        }
        if(isRepositoryProfiled)  {
            printMap(performanceMap, Subsystem.REPOSITORY, afterTest);
        }
        if(isTaskManagerProfiled) {
            printMap(performanceMap, Subsystem.TASK_MANAGER, afterTest);
        }
        if(isUcfProfiled) {
            printMap(performanceMap, Subsystem.UCF, afterTest);
        }
        if(isWorkflowProfiled){
            printMap(performanceMap, Subsystem.WORKFLOW, afterTest);
        }
        if(isSynchronizationServiceProfiled) {
            printMap(performanceMap, Subsystem.SYNCHRONIZATION_SERVICE, afterTest);
        }
        if(isWebProfiled){
            printMap(performanceMap, Subsystem.WEB, afterTest);
        }
    }

    private static long minutesToMillis(int minutes){
        return (long)(minutes*60*1000);
    }

    private synchronized void updateOverallStatistics(Map<String, MethodUsageStatistics> logMap, ProfilingDataLog eventLog, String key, Subsystem subsystem){
        if(!logMap.containsKey(key)){
            eventLog.setArgs(prepareArguments(eventLog.args));
            logMap.put(key, new MethodUsageStatistics(eventLog, subsystem));
        } else {
            logMap.get(key).update(eventLog);
        }

        if(logMap.get(key).getSlowestMethodList().size() < TOP_TEN_METHOD_NUMBER){
            eventLog.setArgs(prepareArguments(eventLog.args));
            logMap.get(key).getSlowestMethodList().add(eventLog);
            sort(logMap.get(key).getSlowestMethodList());
        } else {
            if(logMap.get(key).getSlowestMethodList().get(logMap.get(key).getSlowestMethodList().size()-1).getEstimatedTime() < eventLog.getEstimatedTime()){
                eventLog.setArgs(prepareArguments(eventLog.args));
                logMap.get(key).getSlowestMethodList().add(eventLog);
                sort(logMap.get(key).getSlowestMethodList());
                logMap.get(key).setCurrentTopTenMin(logMap.get(key).getSlowestMethodList().get(logMap.get(key).getSlowestMethodList().size()-1).getEstimatedTime());
            }
        }

        if(logMap.get(key).getSlowestMethodList().size() > TOP_TEN_METHOD_NUMBER){
            logMap.get(key).getSlowestMethodList().remove(logMap.get(key).getSlowestMethodList().size()-1);
        }
    }

    private static void printMap(Map<String, MethodUsageStatistics> logMap, Subsystem subsystem, boolean afterTest){
        for (Map.Entry<String, MethodUsageStatistics> usage : logMap.entrySet()){
            final MethodUsageStatistics value = usage.getValue();
            if(subsystem.equals(value.getSubsystem())){
                value.appendToLogger(afterTest);
            }
        }
    }

    public void subsystemConfiguration(Map<Subsystem, Boolean> subsystems){
        isModelProfiled = isSubsystemProfiled(Subsystem.MODEL, subsystems);
        isProvisioningProfiled = isSubsystemProfiled(Subsystem.PROVISIONING, subsystems);
        isRepositoryProfiled = isSubsystemProfiled(Subsystem.REPOSITORY, subsystems);
        isSynchronizationServiceProfiled = isSubsystemProfiled(Subsystem.SYNCHRONIZATION_SERVICE, subsystems);
        isTaskManagerProfiled = isSubsystemProfiled(Subsystem.TASK_MANAGER, subsystems);
        isUcfProfiled = isSubsystemProfiled(Subsystem.UCF, subsystems);
        isWorkflowProfiled = isSubsystemProfiled(Subsystem.WORKFLOW, subsystems);
        isWebProfiled = isSubsystemProfiled(Subsystem.WEB, subsystems);
    }

    private boolean isSubsystemProfiled(Subsystem subsystem, Map<Subsystem, Boolean> map){
        if(map.get(subsystem) != null && map.get(subsystem)){
            return true;
        }

        return false;
    }

    private void cleanEverything(){
        performanceMap.clear();
    }

    private ProfilingDataLog prepareProfilingDataLog(String className, String methodName, long startTime, Object[] args){
        long eTime = calculateTime(startTime);
        long timestamp = System.currentTimeMillis();

        return new ProfilingDataLog(className, methodName, eTime, timestamp, args);
    }

    private synchronized static List<ProfilingDataLog> sort(List<ProfilingDataLog> list){
        Collections.sort(list, arrayComparator);
        return list;
    }

    private static class ArrayComparator implements Comparator<ProfilingDataLog>{

        @Override
        public int compare(ProfilingDataLog o1, ProfilingDataLog o2) {
            return ((Long)o2.getEstimatedTime()).compareTo(o1.getEstimatedTime());
        }

    }

    private long calculateTime(long startTime){
        return (System.nanoTime() - startTime);
    }

    public Map<String, MethodUsageStatistics> getPerformanceMap() {
        return performanceMap;
    }

    public int getMinuteDumpInterval() {
        return minuteDumpInterval;
    }

    private String[] prepareArguments(Object[] args){

        if(args == null || args.length == 0)
            return new String[]{ARGS_EMPTY};

        StringBuilder sb = new StringBuilder();

        for(Object o: args){
            if(o == null)
                sb.append(ARGS_NULL);
            else
                sb.append(o.toString());

            sb.append(INDENT_STRING);
        }

        return new String[]{sb.toString()};
    }

    public void appendProfilingToTest(){
        MidpointInterceptor.activateSubsystemProfiling();
    }

    public void stopProfilingAfterTest(){
        MidpointInterceptor.deactivateSubsystemProfiling();
    }

    public void printMapAfterTest(){
        printEverything(true);
    }

    public Map<String, MethodUsageStatistics> getProfilingData(){
        return performanceMap;
    }
}
