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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.aspect.MethodUsageStatistics;
import com.evolveum.midpoint.util.aspect.PerformanceStatistics;
import com.evolveum.midpoint.util.aspect.ProfilingDataLog;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 *
 *  @author shood
 * */
public class ProfilingTest {

    public static final Integer TEST_MINUTE_DUMP_INTERVAL = 5;
    public static final String REQUEST_FILTER_TEST_URI = "URI";

    public static final long SLOWEST_METHOD_EST = 25000;
    public static final long MIDDLE_METHOD_EST = 10000;
    public static final long FASTEST_METHOD_EST = 5000;

    @Test
    public void prof_01_performanceStatisticsUsage(){

        //WHEN
        PerformanceStatistics stats = new PerformanceStatistics();

        //These numbers should not be 0 - compiler is using some system resources
        assertNotNull(stats);
        assertNotSame(0, stats.getUsedHeapMemory());
        assertNotSame(0, stats.getCommittedNonHeapMemory());
        assertNotSame(0, stats.getThreadCount());
    }

    @Test
    public void prof_02_profManagerConfigurationTest(){
        Map<ProfilingDataManager.Subsystem, Boolean> profMap = new HashMap<>();
        profMap.put(ProfilingDataManager.Subsystem.MODEL, true);
        profMap.put(ProfilingDataManager.Subsystem.PROVISIONING, false);
        profMap.put(ProfilingDataManager.Subsystem.REPOSITORY, false);
        profMap.put(ProfilingDataManager.Subsystem.SYNCHRONIZATION_SERVICE, false);
        profMap.put(ProfilingDataManager.Subsystem.TASK_MANAGER, false);
        profMap.put(ProfilingDataManager.Subsystem.UCF, false);
        profMap.put(ProfilingDataManager.Subsystem.WORKFLOW, false);
        profMap.put(ProfilingDataManager.Subsystem.WEB, false);

        ProfilingDataManager.getInstance().configureProfilingDataManager(profMap, TEST_MINUTE_DUMP_INTERVAL, true, false, false);

        ProfilingDataManager profManager = ProfilingDataManager.getInstance();
        assertSame(profManager.getMinuteDumpInterval(), TEST_MINUTE_DUMP_INTERVAL);
    }

    @Test
    public void prof_03_profManagerRequestTest(){

        ProfilingDataManager manager = ProfilingDataManager.getInstance();
        ProfilingDataLog event1 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", FASTEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event2 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", MIDDLE_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event3 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());

        //WHEN - adding 3 custom events
        manager.prepareRequestProfilingEvent(event1);
        manager.prepareRequestProfilingEvent(event2);
        manager.prepareRequestProfilingEvent(event3);

        Map<String, MethodUsageStatistics> perfMap =  manager.getPerformanceMap();

        //THEN
        assertSame(1, perfMap.keySet().size());
        assertSame(3, perfMap.get(REQUEST_FILTER_TEST_URI).getSlowestMethodList().size());

        //Here, we are testing, if ProfilingEventList - slowestMethodMap is ordered correctly - we must use this
        //strange approach, because testNG somehow thinks, that long 5000 != long 5000
        boolean first = false;
        boolean second = false;
        boolean third = false;

        if(SLOWEST_METHOD_EST == perfMap.get(REQUEST_FILTER_TEST_URI).getSlowestMethodList().get(0).getEstimatedTime())
            first = true;
        if(MIDDLE_METHOD_EST == perfMap.get(REQUEST_FILTER_TEST_URI).getSlowestMethodList().get(1).getEstimatedTime())
            second = true;
        if(FASTEST_METHOD_EST == perfMap.get(REQUEST_FILTER_TEST_URI).getSlowestMethodList().get(2).getEstimatedTime())
            third = true;

        assertSame(true, first);
        assertSame(true, second);
        assertSame(true, third);

        boolean max = false;
        boolean min = false;
        boolean mean = false;

        if(SLOWEST_METHOD_EST == perfMap.get(REQUEST_FILTER_TEST_URI).getMax())
            max = true;
        if(FASTEST_METHOD_EST == perfMap.get(REQUEST_FILTER_TEST_URI).getMin())
            min = true;

        long meanVal = (FASTEST_METHOD_EST+MIDDLE_METHOD_EST+SLOWEST_METHOD_EST)/3;
        if(meanVal == perfMap.get(REQUEST_FILTER_TEST_URI).getMean())
            mean = true;

        assertSame(true, max);
        assertSame(true, min);
        assertSame(true, mean);
    }

    @Test
    public void prof_04_testMaxEventsInList(){

        //Here, we create 7 events
        ProfilingDataManager manager = ProfilingDataManager.getInstance();
        ProfilingDataLog event1 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", FASTEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event2 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", MIDDLE_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event3 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event4 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event5 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event6 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());
        ProfilingDataLog event7 = new ProfilingDataLog("GET", REQUEST_FILTER_TEST_URI, "sessionID", SLOWEST_METHOD_EST, System.currentTimeMillis());

        manager.prepareRequestProfilingEvent(event1);
        manager.prepareRequestProfilingEvent(event2);
        manager.prepareRequestProfilingEvent(event3);
        manager.prepareRequestProfilingEvent(event4);
        manager.prepareRequestProfilingEvent(event5);
        manager.prepareRequestProfilingEvent(event6);
        manager.prepareRequestProfilingEvent(event7);

        //Only 5 slowest requests should be saved in methodList
        assertNotSame(7, manager.getPerformanceMap().get(REQUEST_FILTER_TEST_URI).getSlowestMethodList().size());
    }

    @Test
    public void prof_05_subsystemProfilingTest(){
        Map<ProfilingDataManager.Subsystem, Boolean> confMap = new HashMap<>();
        confMap.put(ProfilingDataManager.Subsystem.MODEL, true);
        confMap.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        confMap.put(ProfilingDataManager.Subsystem.SYNCHRONIZATION_SERVICE, false);
        confMap.put(ProfilingDataManager.Subsystem.WEB, false);
        confMap.put(ProfilingDataManager.Subsystem.WORKFLOW, false);
        confMap.put(ProfilingDataManager.Subsystem.PROVISIONING, false);
        confMap.put(ProfilingDataManager.Subsystem.UCF, false);
        confMap.put(ProfilingDataManager.Subsystem.TASK_MANAGER, false);

        ProfilingDataManager.getInstance().configureProfilingDataManager(confMap, 10, true, false, false);

        ProfilingDataManager manager = ProfilingDataManager.getInstance();


        //6 prof. events are created, there should be 2 keys in HashMap
        Long startTime = System.nanoTime();
        manager.applyGranularityFilterOnEnd("class", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.MODEL, System.currentTimeMillis(),startTime);
        manager.applyGranularityFilterOnEnd("class", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.MODEL, System.currentTimeMillis(),startTime);
        manager.applyGranularityFilterOnEnd("class", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.MODEL, System.currentTimeMillis(),startTime);
        manager.applyGranularityFilterOnEnd("class2", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.REPOSITORY, System.currentTimeMillis(),startTime);
        manager.applyGranularityFilterOnEnd("class2", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.REPOSITORY, System.currentTimeMillis(),startTime);
        manager.applyGranularityFilterOnEnd("class2", "method", new String[]{"1","2","3"}, ProfilingDataManager.Subsystem.REPOSITORY, System.currentTimeMillis(),startTime);

        Map<String, MethodUsageStatistics> perfMap = manager.getPerformanceMap();

        //Now we test the results
        assertSame(2, perfMap.keySet().size());
    }
}
