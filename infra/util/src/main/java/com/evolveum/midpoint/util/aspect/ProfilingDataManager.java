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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /* LOGGER */
    private Trace LOGGER = TraceManager.getTrace(ProfilingDataManager.class);

    /* ProfilingDataManager attributes */
    private long lastDumpTimestamp;
    private long nextDumpTimestamp;
    private int minuteDumpInterval = 30;

    private Map<String, ProfilingDataLog> profilingDataLogMap = new HashMap<String, ProfilingDataLog>();

    /* profilingDataLogMap keys for individual midPoint interfaces */



    /* ===BEHAVIOR=== */
    /*
    *   Retrieves instance of ProfilingDataManager
    * */
    public static ProfilingDataManager getInstance() {

        if(profilingDataManager == null){
            profilingDataManager = new ProfilingDataManager();
        }

        return profilingDataManager;
    }

    /*
    *   ProfilingDataManager instance private constructor - not accessible from outside of this class
    * */
    private ProfilingDataManager() {

    //TODO - continue here!

    }   //ProfilingDataManager

    /**
     *  Configures ProfilingDataManager - can be called from outside
     * */
    public void configureProfilingDataManager(Map<String, Boolean> profiledSubsystems, Integer dumpInterval, boolean subsystemProfilingActive){

        if(subsystemProfilingActive){
            MidpointAspect.activateSubsystemProfiling();
        }else {
            MidpointAspect.deactivateSubsystemProfiling();
        }

        AspectProfilingFilters.subsystemConfiguration(profiledSubsystems);

        profilingDataManager = new ProfilingDataManager();

    }   //configureProfilingDataManager

}
