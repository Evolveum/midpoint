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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class LoggingDto implements Serializable {

    public static String F_ROOT_LEVEL = "rootLevel";
    public static String F_ROOT_APPENDER = "rootAppender";
    public static String F_APPENDERS = "appenders";

    public static final String LOGGER_PROFILING = "PROFILING";
    public static final Map<String, LoggingComponentType> componentMap = new HashMap<>();

    static {
        componentMap.put("com.evolveum.midpoint", LoggingComponentType.ALL);
        componentMap.put("com.evolveum.midpoint.model", LoggingComponentType.MODEL);
        componentMap.put("com.evolveum.midpoint.provisioning", LoggingComponentType.PROVISIONING);
        componentMap.put("com.evolveum.midpoint.repo", LoggingComponentType.REPOSITORY);
        componentMap.put("com.evolveum.midpoint.web", LoggingComponentType.GUI);
        componentMap.put("com.evolveum.midpoint.task", LoggingComponentType.TASKMANAGER);
        componentMap.put("com.evolveum.midpoint.model.sync", LoggingComponentType.RESOURCEOBJECTCHANGELISTENER);
        componentMap.put("com.evolveum.midpoint.wf", LoggingComponentType.WORKFLOWS);
        componentMap.put("com.evolveum.midpoint.notifications", LoggingComponentType.NOTIFICATIONS);
    }

    private PrismObject<SystemConfigurationType> oldConfiguration;

    private LoggingLevelType rootLevel;
    private String rootAppender;

    private List<LoggerConfiguration> loggers = new ArrayList<>();
    private List<FilterConfiguration> filters = new ArrayList<>();

    private ProfilingLevel profilingLevel;
    private String profilingAppender;

    private List<AppenderConfiguration> appenders = new ArrayList<>();

    private boolean auditLog = false;
    private boolean auditDetails;
    private String auditAppender;

    private boolean advanced;

    private boolean profilingEnabled;
    private boolean requestFilter;
    private boolean performanceStatistics;
    private boolean subsystemModel;
    private boolean subsystemRepository;
    private boolean subsystemProvisioning;
    private boolean subsystemUcf;
    private boolean subsystemResourceObjectChangeListener;
    private boolean subsystemTaskManager;
    private boolean subsystemWorkflow;
    private Integer dumpInterval;


    public LoggingDto() {
        this(null, null);
    }

    public LoggingDto(PrismObject<SystemConfigurationType> oldConfiguration, LoggingConfigurationType config) {
        this.oldConfiguration = oldConfiguration;
        init(config);
        initProfiling(oldConfiguration);
    }

    private void initProfiling(PrismObject<SystemConfigurationType> sysConfig){
        SystemConfigurationType systemConfig = sysConfig.asObjectable();
        ProfilingConfigurationType profilingConfiguration = systemConfig.getProfilingConfiguration();

        if(profilingConfiguration != null && profilingConfiguration.isEnabled()){
            profilingEnabled = true;

            requestFilter = checkXsdBooleanValue(profilingConfiguration.isRequestFilter());
            performanceStatistics = checkXsdBooleanValue(profilingConfiguration.isPerformanceStatistics());
            subsystemModel = checkXsdBooleanValue(profilingConfiguration.isModel());
            subsystemProvisioning = checkXsdBooleanValue(profilingConfiguration.isProvisioning());
            subsystemRepository = checkXsdBooleanValue(profilingConfiguration.isRepository());
            subsystemResourceObjectChangeListener = checkXsdBooleanValue(profilingConfiguration.isResourceObjectChangeListener());
            subsystemTaskManager = checkXsdBooleanValue(profilingConfiguration.isTaskManager());
            subsystemUcf = checkXsdBooleanValue(profilingConfiguration.isUcf());
            subsystemWorkflow = checkXsdBooleanValue(profilingConfiguration.isWorkflow());

            if(profilingConfiguration.getDumpInterval() != null)
                dumpInterval = profilingConfiguration.getDumpInterval();
        }
    }

    private static boolean checkXsdBooleanValue(Boolean value){
        if(value == null || !value)
            return false;
        else
            return true;
    }

    private void init(LoggingConfigurationType config) {
        if (config == null) {
            return;
        }
        rootLevel = config.getRootLoggerLevel();
        rootAppender = config.getRootLoggerAppender();

        for (SubSystemLoggerConfigurationType logger : config.getSubSystemLogger()) {
            filters.add(new FilterConfiguration(logger));
        }

        AuditingConfigurationType auditing = config.getAuditing();
        if (auditing != null) {
            setAuditLog(auditing.isEnabled());
            setAuditDetails(auditing.isDetails());
            setAuditAppender(auditing.getAppender() != null && auditing.getAppender().size() > 0 ? auditing.getAppender().get(0) : null);
        }

        for (ClassLoggerConfigurationType logger : config.getClassLogger()) {
            if (LOGGER_PROFILING.equals(logger.getPackage())) {
                setProfilingAppender(logger.getAppender() != null && logger.getAppender().size() > 0 ? logger.getAppender().get(0) : null);
                setProfilingLevel(ProfilingLevel.fromLoggerLevelType(logger.getLevel()));
                continue;
            }

            if (componentMap.containsKey(logger.getPackage())) {
                loggers.add(new ComponentLogger(logger));
            } else if(StandardLogger.isStandardLogger(logger.getPackage())){
                loggers.add(new StandardLogger(logger));
            }else {
                loggers.add(new ClassLogger(logger));
            }
        }

        Collections.sort(loggers, new Comparator<LoggerConfiguration>() {

            @Override
            public int compare(LoggerConfiguration l1, LoggerConfiguration l2) {
                return String.CASE_INSENSITIVE_ORDER.compare(l1.getName(), l2.getName());
            }
        });
        Collections.sort(filters, new Comparator<FilterConfiguration>() {

            @Override
            public int compare(FilterConfiguration f1, FilterConfiguration f2) {
                return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
            }
        });

        for (AppenderConfigurationType appender : config.getAppender()) {
            if (appender instanceof FileAppenderConfigurationType) {
                appenders.add(new FileAppenderConfig((FileAppenderConfigurationType) appender));
            } else {
                appenders.add(new AppenderConfiguration(appender));
            }
        }
        Collections.sort(appenders);
    }

    public PrismObject<SystemConfigurationType> getOldConfiguration() {
        return oldConfiguration;
    }

    public List<LoggerConfiguration> getLoggers() {
        return loggers;
    }

    public List<FilterConfiguration> getFilters() {
        return filters;
    }

    public String getRootAppender() {
        return rootAppender;
    }

    public void setRootAppender(String rootAppender) {
        this.rootAppender = rootAppender;
    }

    public LoggingLevelType getRootLevel() {
        return rootLevel;
    }

    public void setRootLevel(LoggingLevelType rootLevel) {
        this.rootLevel = rootLevel;
    }

    public String getAuditAppender() {
        return auditAppender;
    }

    public void setAuditAppender(String auditAppender) {
        this.auditAppender = auditAppender;
    }

    public boolean isAuditDetails() {
        return auditDetails;
    }

    public void setAuditDetails(boolean auditDetails) {
        this.auditDetails = auditDetails;
    }

    public boolean isAuditLog() {
        return auditLog;
    }

    public void setAuditLog(boolean auditLog) {
        this.auditLog = auditLog;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(boolean advanced) {
        this.advanced = advanced;
    }

    public String getProfilingAppender() {
        return profilingAppender;
    }

    public void setProfilingAppender(String profilingAppender) {
        this.profilingAppender = profilingAppender;
    }

    public ProfilingLevel getProfilingLevel() {
        return profilingLevel;
    }

    public void setProfilingLevel(ProfilingLevel profilingLevel) {
        this.profilingLevel = profilingLevel;
    }

    public List<AppenderConfiguration> getAppenders() {
        return appenders;
    }

    public boolean isRequestFilter() {
        return requestFilter;
    }

    public void setRequestFilter(boolean requestFilter) {
        this.requestFilter = requestFilter;
    }

    public boolean isPerformanceStatistics() {
        return performanceStatistics;
    }

    public void setPerformanceStatistics(boolean performanceStatistics) {
        this.performanceStatistics = performanceStatistics;
    }

    public boolean isSubsystemModel() {
        return subsystemModel;
    }

    public void setSubsystemModel(boolean subsystemModel) {
        this.subsystemModel = subsystemModel;
    }

    public boolean isSubsystemRepository() {
        return subsystemRepository;
    }

    public void setSubsystemRepository(boolean subsystemRepository) {
        this.subsystemRepository = subsystemRepository;
    }

    public boolean isSubsystemProvisioning() {
        return subsystemProvisioning;
    }

    public void setSubsystemProvisioning(boolean subsystemProvisioning) {
        this.subsystemProvisioning = subsystemProvisioning;
    }

    public boolean isSubsystemUcf() {
        return subsystemUcf;
    }

    public void setSubsystemUcf(boolean subsystemUcf) {
        this.subsystemUcf = subsystemUcf;
    }

    public boolean isSubsystemResourceObjectChangeListener() {
        return subsystemResourceObjectChangeListener;
    }

    public void setSubsystemResourceObjectChangeListener(boolean subsystemResourceObjectChangeListener) {
        this.subsystemResourceObjectChangeListener = subsystemResourceObjectChangeListener;
    }

    public boolean isSubsystemTaskManager() {
        return subsystemTaskManager;
    }

    public void setSubsystemTaskManager(boolean subsystemTaskManager) {
        this.subsystemTaskManager = subsystemTaskManager;
    }

    public boolean isSubsystemWorkflow() {
        return subsystemWorkflow;
    }

    public void setSubsystemWorkflow(boolean subsystemWorkflow) {
        this.subsystemWorkflow = subsystemWorkflow;
    }

    public Integer getDumpInterval() {
        return dumpInterval;
    }

    public void setDumpInterval(Integer dumpInterval) {
        this.dumpInterval = dumpInterval;
    }

    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }

    public void setProfilingEnabled(boolean profilingEnabled) {
        this.profilingEnabled = profilingEnabled;
    }
}
