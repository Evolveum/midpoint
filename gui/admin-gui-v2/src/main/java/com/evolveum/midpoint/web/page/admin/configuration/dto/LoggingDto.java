/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class LoggingDto implements Serializable {

    public static final String LOGGER_PROFILING = "PROFILING";
    public static final String LOGGER_MIDPOINT_ROOT = "com.evolveum.midpoint";

    private PrismObject<SystemConfigurationType> oldConfiguration;

    private LoggingLevelType rootLevel;
    private String rootAppender;

    private LoggingLevelType midPointLevel;
    private String midPointAppender;

    private List<LoggerConfiguration> loggers = new ArrayList<LoggerConfiguration>();

    private ProfilingLevel profilingLevel;
    private String profilingAppender;

    private List<AppenderConfiguration> appenders = new ArrayList<AppenderConfiguration>();

    private boolean auditLog = false;
    private boolean auditDetails;
    private String auditAppender;

    private boolean advanced;

    public LoggingDto() {
        this(null, null);
    }

    public LoggingDto(PrismObject<SystemConfigurationType> oldConfiguration, LoggingConfigurationType config) {
        this.oldConfiguration = oldConfiguration;
        init(config);
    }

    private void init(LoggingConfigurationType config) {
        if (config == null) {
            return;
        }
        rootLevel = config.getRootLoggerLevel();
        rootAppender = config.getRootLoggerAppender();

        for (SubSystemLoggerConfigurationType logger : config.getSubSystemLogger()) {
            loggers.add(new FilterLogger(logger));
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
            } else if (LOGGER_MIDPOINT_ROOT.equals(logger.getPackage())) {
                setMidPointAppender(logger.getAppender() != null && logger.getAppender().size() > 0 ? logger.getAppender().get(0) : null);
                setMidPointLevel(logger.getLevel());
                continue;
            }

            loggers.add(new ClassLogger(logger));
        }

        Collections.sort(loggers, new LoggersComparator());

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

    public String getMidPointAppender() {
        return midPointAppender;
    }

    public void setMidPointAppender(String midPointAppender) {
        this.midPointAppender = midPointAppender;
    }

    public LoggingLevelType getMidPointLevel() {
        return midPointLevel;
    }

    public void setMidPointLevel(LoggingLevelType midPointLevel) {
        this.midPointLevel = midPointLevel;
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
}
