/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.HashMap;
import java.util.Map;

/**
 *  @author shood
 * */
public class ProfilingConfigurationManager {

    private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationManager.class);

    private static final String REQUEST_FILTER_LOGGER_CLASS_NAME = "com.evolveum.midpoint.web.util.MidPointProfilingServletFilter";
    private static final String SUBSYSTEM_PROFILING_LOGGER = ProfilingDataManager.class.getName();
    private static final String APPENDER_IDM_PROFILE = "MIDPOINT_PROFILE_LOG";

    /**
     * In this method, we perform the check of systemConfiguration object, searching for any data
     * related to profilingConfiguration.
     *
     * @return The logging configuration, updated by profiling-related loggers (if profiling is enabled).
     */
    public static LoggingConfigurationType checkSystemProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism){
        if (systemConfigurationPrism == null) {
            return null;
        }
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();
        ProfilingConfigurationType profilingConfig = systemConfig.getProfilingConfiguration();
        boolean isSubsystemConfig;

        if (profilingConfig == null || !profilingConfig.isEnabled()) {
            return systemConfig.getLogging();
        } else {
            isSubsystemConfig = applySubsystemProfiling(systemConfig);
            return applyProfilingConfiguration(systemConfigurationPrism, profilingConfig, isSubsystemConfig);
        }
    }

    private static LoggingConfigurationType applyProfilingConfiguration(PrismObject<SystemConfigurationType> systemConfigurationPrism, ProfilingConfigurationType profilingConfig, boolean subsystemProfiling){
        SystemConfigurationType systemConfig = systemConfigurationPrism.asObjectable();

        LoggingConfigurationType loggingConfig = systemConfig.getLogging();

        if (loggingConfig != null) {
            if (loggingConfig.isImmutable()) {
                loggingConfig = loggingConfig.clone();
            }
            if (checkXsdBooleanValue(profilingConfig.isRequestFilter())) {
                ClassLoggerConfigurationType requestFilterLogger = new ClassLoggerConfigurationType();
                requestFilterLogger.setPackage(REQUEST_FILTER_LOGGER_CLASS_NAME);
                requestFilterLogger.setLevel(LoggingLevelType.TRACE);
                requestFilterLogger.getAppender().clear();
                requestFilterLogger.getAppender().add(APPENDER_IDM_PROFILE);

                loggingConfig.getClassLogger().add(requestFilterLogger);
            }
            if (subsystemProfiling) {
                ClassLoggerConfigurationType subsystemLogger = new ClassLoggerConfigurationType();
                subsystemLogger.setPackage(SUBSYSTEM_PROFILING_LOGGER);
                subsystemLogger.setLevel(LoggingLevelType.DEBUG);
                subsystemLogger.getAppender().clear();
                subsystemLogger.getAppender().add(APPENDER_IDM_PROFILE);

                loggingConfig.getClassLogger().add(subsystemLogger);
            }
        }

        LOGGER.info("Applying profiling configuration.");
        return loggingConfig;
    }

    private static boolean applySubsystemProfiling(SystemConfigurationType systemConfig) {
        ProfilingConfigurationType profilingConfig = systemConfig.getProfilingConfiguration();

        Map<ProfilingDataManager.Subsystem, Boolean> profiledSubsystems = new HashMap<>();
        int dumpInterval = 0;
        boolean subSystemProfiling = false;
        boolean performanceProfiling;
        boolean requestProfiling;

        profiledSubsystems.put(ProfilingDataManager.Subsystem.PROVISIONING, checkXsdBooleanValue(profilingConfig.isProvisioning()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, checkXsdBooleanValue(profilingConfig.isRepository()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.SYNCHRONIZATION_SERVICE, checkXsdBooleanValue(profilingConfig.isSynchronizationService()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.TASK_MANAGER, checkXsdBooleanValue(profilingConfig.isTaskManager()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.UCF, checkXsdBooleanValue(profilingConfig.isUcf()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.MODEL, checkXsdBooleanValue(profilingConfig.isModel()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.WORKFLOW, checkXsdBooleanValue(profilingConfig.isWorkflow()));
        profiledSubsystems.put(ProfilingDataManager.Subsystem.WEB, checkXsdBooleanValue(profilingConfig.isRequestFilter()));

        for(Boolean b: profiledSubsystems.values()){
            if(b != null && b){
                subSystemProfiling = true;
                break;
            }
        }

        //Check the dump interval
        if(profilingConfig.getDumpInterval() != null) {
            dumpInterval = profilingConfig.getDumpInterval();
        }

        performanceProfiling = checkXsdBooleanValue(profilingConfig.isPerformanceStatistics());
        requestProfiling = checkXsdBooleanValue(profilingConfig.isRequestFilter());

        if (subSystemProfiling || performanceProfiling || requestProfiling){
            ProfilingDataManager.getInstance().configureProfilingDataManager(profiledSubsystems, dumpInterval, subSystemProfiling, performanceProfiling, requestProfiling);
            return true;
        }
        else {
            ProfilingDataManager.getInstance().configureProfilingDataManager(profiledSubsystems, dumpInterval, subSystemProfiling, performanceProfiling, requestProfiling);
            return false;
        }
    }

    private static boolean checkXsdBooleanValue(Boolean value){
        return value != null && value;
    }
}
