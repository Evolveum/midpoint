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

import java.sql.Timestamp;
import java.util.*;

/**
 *  Notes:
 *  1. Default dump interval is set to 30 minutes
 *
 *  TODO - ADD descriptive description
 *
 *  This is a Singleton Class
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
    private static final byte TOP_TEN_METHOD_NUMBER = 10;

    /* LOGGER */
    private static Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    /* ProfilingDataManager attributes */
    private long lastDumpTimestamp;
    private long nextDumpTimestamp;
    private int minuteDumpInterval = DEFAULT_DUMP_INTERVAL;

    //Maps for profiling events

    /* profilingDataLogMap keys for individual midPoint interfaces */
    private Map<String, MethodUsageStatistics> repositoryLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> taskManagerLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> provisioningLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> resourceObjectChangeListenerLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> modelLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> ucfLogMap = new HashMap<String, MethodUsageStatistics>();
    private Map<String, MethodUsageStatistics> workflowLogMap = new HashMap<String, MethodUsageStatistics>();

    /* Another HashMaps, containing top ten worst performing method invocations for each subsystem */
    private Map<String, ArrayList<ProfilingDataLog>> repositoryTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> taskManagerTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> provisioningTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> resourceObjectChangeListenerTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> modelTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> ucfTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();
    private Map<String, ArrayList<ProfilingDataLog>> workflowTopTenMap = new HashMap<String, ArrayList<ProfilingDataLog>>();

    /* Some more print constants */
    private static final String PRINT_RIGHT_ARROW = "->";
    private static final String PRINT_TOP_TEN_HEADER = "TOP 10 slowest calls:\n";

    /* ===BEHAVIOR=== */
    /*
    *   Retrieves instance of ProfilingDataManager
    * */
    public static ProfilingDataManager getInstance() {

        if(profilingDataManager == null){
            profilingDataManager = new ProfilingDataManager(DEFAULT_DUMP_INTERVAL);
        }

        return profilingDataManager;
    }   //getInstance

    /*
    *   ProfilingDataManager instance private constructor - not accessible from outside of this class
    * */
    private ProfilingDataManager(int dumpInterval) {
        //Configure timestamps
        this.minuteDumpInterval = dumpInterval;
        //LOGGER.info("LAST: " + lastDumpTimestamp + " next: " + nextDumpTimestamp);
        //LOGGER.info("MINUTES: " + minuteDumpInterval);
        long secondDumpInterval = minutesToMillis(minuteDumpInterval);
        //LOGGER.info("DUMP INTERVAL: " + secondDumpInterval);
        lastDumpTimestamp = System.currentTimeMillis();
        nextDumpTimestamp = lastDumpTimestamp + secondDumpInterval;
        //LOGGER.info("LAST: " + lastDumpTimestamp + " next: " + nextDumpTimestamp);

    }   //ProfilingDataManager

    /**
     *  Configures ProfilingDataManager - can be called from outside
     * */
    public void configureProfilingDataManager(Map<String, Boolean> profiledSubsystems, Integer dumpInterval, boolean subsystemProfilingActive){

        //LOGGER.info("CONFIGURING SUBSYSTEM PROFILING");

        if(subsystemProfilingActive){
            MidpointAspect.activateSubsystemProfiling();
        }else {
            MidpointAspect.deactivateSubsystemProfiling();
        }

        AspectProfilingFilters.subsystemConfiguration(profiledSubsystems);

        //LOGGER.info("Current dump interval before: " + minuteDumpInterval);
        //Configure the dump interval
        if(dumpInterval != null && dumpInterval > 0){
            minuteDumpInterval = dumpInterval;
        }

        //LOGGER.info("Current dump interval after: " + minuteDumpInterval);

        profilingDataManager = new ProfilingDataManager(minuteDumpInterval);

    }   //configureProfilingDataManager

    /*
    *   Add eventLog to HashMap - repository subsystem
    * */
    public void addRepositoryLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(repositoryLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(repositoryTopTenMap, repositoryLogMap, eventLog, key);

    }   //addRepositoryLog

    /*
    *   Add eventLog to HashMap - task manager subsystem
    * */
    public void addTaskManagerLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(taskManagerLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(taskManagerTopTenMap, taskManagerLogMap, eventLog, key);

    }   //addTaskManagerLog

    /*
    *   Add eventLog to HashMap - provisioning subsystem
    * */
    public void addProvisioningLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(provisioningLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(provisioningTopTenMap, provisioningLogMap, eventLog, key);

    }   //addProvisioningLog

    /*
    *   Add eventLog to HashMap - resource object change listener subsystem
    * */
    public void addResourceObjectChangeListenerLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(resourceObjectChangeListenerLogMap, eventLog, key);


        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(resourceObjectChangeListenerTopTenMap, resourceObjectChangeListenerLogMap, eventLog, key);

    }   //addResourceObjectChangeListenerLog

    /*
    *   Add eventLog to HashMap - model subsystem
    * */
    public void addModelLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(modelLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(modelTopTenMap, modelLogMap, eventLog, key);

    }   //addModelLog

    /*
*   Add eventLog to HashMap - UCF subsystem
* */
    public void addUcfLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(ucfLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
        updateTopTenMap(ucfTopTenMap, ucfLogMap, eventLog, key);

    }   //addUcfLog

    /*
*   Add eventLog to HashMap - workflow subsystem
* */
    public void addWorkflowLog(String key, ProfilingDataLog eventLog){
        //First - we update the map containing overall usage statistics
        updateOverallStatistics(workflowLogMap, eventLog, key);

        //Then, we examine, if current method run is not too slow, if it is, we will put it into
        //current top 10 slowest runs
       updateTopTenMap(workflowTopTenMap, workflowLogMap, eventLog, key);

    }   //addWorkflowLog

    /*
    *   If the time is right, dump collected profiling information to log false
    *
    *   This method is synchronized
    * */
    public synchronized void dumpToLog(){

        //LOGGER.info("Current: " + System.currentTimeMillis() + ", NEXT: " + nextDumpTimestamp);
        long currentTime = System.currentTimeMillis();

        if(currentTime >= nextDumpTimestamp){
            if(LOGGER.isDebugEnabled()){

                //LOGGER.info("DUMP TO LOG!");

                //Print everything
                if(AspectProfilingFilters.isModelProfiled()){
                    //LOGGER.info("printing subsystem MODEL");
                    printMap(modelLogMap, modelTopTenMap);
                }
                if(AspectProfilingFilters.isProvisioningProfiled()) {
                    //LOGGER.info("printing subsystem PROVISIONING");
                    printMap(provisioningLogMap, provisioningTopTenMap);
                }
                if(AspectProfilingFilters.isRepositoryProfiled())  {
                    //LOGGER.info("printing subsystem REPOSITORY");
                    printMap(repositoryLogMap, repositoryTopTenMap);
                }
                if(AspectProfilingFilters.isTaskManagerProfiled()) {
                    //LOGGER.info("printing subsystem TASK MANAGER");
                    printMap(taskManagerLogMap, taskManagerTopTenMap);
                }
                if(AspectProfilingFilters.isUcfProfiled()) {
                    //LOGGER.info("printing subsystem UCF");
                    printMap(ucfLogMap, ucfTopTenMap);
                }
                if(AspectProfilingFilters.isWorkflowProfiled()){
                    //LOGGER.info("printing subsystem WORKFLOW");
                    printMap(workflowLogMap, workflowTopTenMap);
                }
                if(AspectProfilingFilters.isResourceObjectChangeListenerProfiled()) {
                    //LOGGER.info("printing subsystem ROCL");
                    printMap(resourceObjectChangeListenerLogMap, resourceObjectChangeListenerTopTenMap);
                }

                //Set next dump cycle
                lastDumpTimestamp = currentTime;
                nextDumpTimestamp = lastDumpTimestamp + minutesToMillis(minuteDumpInterval);
                cleanEverything();
                //LOGGER.info("Last: " + lastDumpTimestamp + " , NEXT: " + nextDumpTimestamp);
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
    private static void updateOverallStatistics(Map<String, MethodUsageStatistics> logMap, ProfilingDataLog eventLog, String key){
        if(!logMap.containsKey(key)){
            logMap.put(key, new MethodUsageStatistics(eventLog));
        } else {
            logMap.get(key).update(eventLog);
        }
    }   //updateOverallStatistics

    /*
    *   Updates top 10 list of slowest method calls
    * */
    private static void updateTopTenMap(Map<String, ArrayList<ProfilingDataLog>> topTenMap, Map<String, MethodUsageStatistics> logMap, ProfilingDataLog eventLog, String key){
        long previousMin = logMap.get(key).getCurrentTopTenMin();

        if(!topTenMap.containsKey(key)){
            ArrayList<ProfilingDataLog> helpList = new ArrayList<ProfilingDataLog>();
            helpList.add(eventLog);
            topTenMap.put(key, helpList);
        } else {
            if(topTenMap.get(key).size() < TOP_TEN_METHOD_NUMBER){
                topTenMap.get(key).add(eventLog);
            } else {
                //LOGGER.info("CURRENT: " + eventLog.getEstimatedTime() + " cMIN: " + previousMin + " MAX: " + logMap.get(key).getMax());
                if(previousMin < eventLog.getEstimatedTime()){
                    topTenMap.get(key).add(eventLog);
                }
            }

            if(previousMin < eventLog.getEstimatedTime())
                logMap.get(key).setCurrentTopTenMin(eventLog.getEstimatedTime());

            //If we have more than 10 top ten slow methods, we need to delete the fastest one
            if(topTenMap.get(key).size() >= TOP_TEN_METHOD_NUMBER){
                for(ProfilingDataLog event: topTenMap.get(key)){
                    if(event.getEstimatedTime() == previousMin){
                        topTenMap.get(key).remove(event);
                        break;
                    }
                }
            }
        }

    }   //updateTopTenMap

    /*
    *   prints provided map to log
    * */
    private static void printMap(Map<String, MethodUsageStatistics> logMap, Map<String, ArrayList<ProfilingDataLog>> topTenMap){
        //LOGGER.info("Print data log map!");

        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        for(String key: logMap.keySet()){
            if(logMap.get(key) != null){
                if(topTenMap.get(key) != null && !topTenMap.get(key).isEmpty()){
                    sb.append(topTenMap.get(key).get(0).getClassName());
                    sb.append(PRINT_RIGHT_ARROW);
                    sb.append(topTenMap.get(key).get(0).getMethodName());
                    sb.append(logMap.get(key).appendToLogger());

                    sb.append(printTopTenMap(topTenMap.get(key), key));
                }
            }
        }

        //LOGGER.info("PRINTING DEBUG!");
        LOGGER.debug(sb.toString());

    }   //printMap

    /*
    *   Print top ten arrayList of ProfilingData logs
    * */
    private static String printTopTenMap(List<ProfilingDataLog> topTenList, String key){
        //LOGGER.info("PRINT TOP TEN MAP");
        StringBuilder sb = new StringBuilder();

        sb.append(PRINT_TOP_TEN_HEADER);

        //First, we need to sort the list by
        Collections.sort(topTenList, new Comparator<ProfilingDataLog>() {
            @Override
            public int compare(ProfilingDataLog o1, ProfilingDataLog o2) {
                return ((Long)o2.getEstimatedTime()).compareTo(o1.getEstimatedTime());
            }
        });

        //LOGGER.info("TOPTENLIST SIZE: " + topTenList.size());
        int counter = 0;
        for(ProfilingDataLog log: topTenList){
            sb.append(log.appendToLogger());
            counter++;
            if(counter == 10)
                break;
        }

        return sb.toString();
    }   //printTopTenMap

    /*
    *   Cleans everything, all subsystem maps and top ten lists
    * */
    private void cleanEverything(){
        modelLogMap.clear();
        modelTopTenMap.clear();
        repositoryLogMap.clear();
        repositoryTopTenMap.clear();
        provisioningLogMap.clear();
        provisioningTopTenMap.clear();
        taskManagerLogMap.clear();
        taskManagerTopTenMap.clear();
        workflowTopTenMap.clear();
        workflowLogMap.clear();
        ucfLogMap.clear();
        ucfTopTenMap.clear();
        resourceObjectChangeListenerTopTenMap.clear();
        resourceObjectChangeListenerLogMap.clear();
    }   //cleanEverything



}
