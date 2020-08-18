/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.io.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LoggingSchemaUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.*;
import com.evolveum.midpoint.util.statistics.OperationExecutionLogger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class LoggingConfigurationManager {

    public static final String AUDIT_LOGGER_NAME = "com.evolveum.midpoint.audit.log";

    private static final Trace LOGGER = TraceManager.getTrace(LoggingConfigurationManager.class);

    private static final String REQUEST_FILTER_LOGGER_CLASS_NAME = "com.evolveum.midpoint.web.util.MidPointProfilingServletFilter";
    private static final String PROFILING_ASPECT_LOGGER = ProfilingDataManager.class.getName();
    private static final String IDM_PROFILE_APPENDER = "IDM_LOG";
    private static final String ALT_APPENDER_NAME = "ALT_LOG";
    private static final String TRACING_APPENDER_NAME = "TRACING_LOG";
    private static final String TRACING_APPENDER_CLASS_NAME = TracingAppender.class.getName();
    private static final LoggingLevelType DEFAULT_PROFILING_LEVEL = LoggingLevelType.INFO;

    private static String currentlyUsedVersion = null;

    public static void configure(LoggingConfigurationType config, String version,
            MidpointConfiguration midpointConfiguration, OperationResult result) throws SchemaException {

        OperationResult res = result.createSubresult(LoggingConfigurationManager.class.getName() + ".configure");

        if (InternalsConfig.isAvoidLoggingChange()) {
            LOGGER.info("IGNORING change of logging configuration (current config version: {}, new version {}) because avoidLoggingChange=true", currentlyUsedVersion, version);
            res.recordNotApplicableIfUnknown();
            return;
        }

        if (currentlyUsedVersion != null) {
            LOGGER.info("Applying logging configuration (currently applied version: {}, new version: {})", currentlyUsedVersion, version);
        } else {
            LOGGER.info("Applying logging configuration (version {})", version);
        }
        currentlyUsedVersion = version;

        // JUL Bridge initialization was here. (SLF4JBridgeHandler)
        // But it was moved to a later phase as suggested by http://jira.qos.ch/browse/LOGBACK-740

        // Initialize JUL bridge
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        //Get current log configuration
        LoggerContext lc = (LoggerContext) TraceManager.getILoggerFactory();

        //Prepare configurator in current context
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);

        //Generate configuration file as string
        String configXml = prepareConfiguration(config, midpointConfiguration);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("New logging configuration:");
            LOGGER.trace(configXml);
        }

        InputStream cis = new ByteArrayInputStream(configXml.getBytes());
        LOGGER.debug("Resetting current logging configuration");
        lc.getStatusManager().clear();
        //Set all loggers to error
        for (Logger l : lc.getLoggerList()) {
            LOGGER.trace("Disable logger: {}", l);
            l.setLevel(Level.ERROR);
        }
        // Reset configuration
        lc.reset();
        //Switch to new logging configuration
        lc.setName("MidPoint");
        try {
            configurator.doConfigure(cis);
            LOGGER.debug("New logging configuration applied");
        } catch (JoranException | NumberFormatException e) {
            System.out.println("Error during applying logging configuration: " + e.getMessage());
            LOGGER.error("Error during applying logging configuration: " + e.getMessage(), e);
            result.createSubresult("Applying logging configuration.").recordFatalError(e.getMessage(), e);
        }

        //Get messages if error occurred;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StatusPrinter.setPrintStream(new PrintStream(baos));
        StatusPrinter.print(lc);

        String internalLog = null;
        try {
            internalLog = baos.toString("UTF8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
            LOGGER.error("Whoops?", e);
        }

        if (!StringUtils.isEmpty(internalLog)) {
            //Parse internal log
            res.recordSuccess();
            for (String internalLogLine : internalLog.split("\n")) {
                if (internalLogLine.contains("|-ERROR")) {
                    res.recordPartialError(internalLogLine);
                }
                res.appendDetail(internalLogLine);
            }
            LOGGER.trace("LogBack internal log:\n{}", internalLog);
        } else {
            res.recordSuccess();
        }

        // Initialize JUL bridge
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static String prepareConfiguration(LoggingConfigurationType config, MidpointConfiguration midpointConfiguration)
            throws SchemaException {

        if (null == config) {
            throw new IllegalArgumentException("Configuration can't be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        boolean debug = Boolean.TRUE.equals(config.isDebug());
        sb.append("<configuration scan=\"false\" debug=\"").append(debug).append("\">\n");

        sb.append("\t<turboFilter class=\"").append(LevelOverrideTurboFilter.class.getName()).append("\"/>\n");

        //find and configure ALL logger and bring it to top of turbo stack
        for (SubSystemLoggerConfigurationType ss : config.getSubSystemLogger()) {
            if ("ALL".contentEquals(ss.getComponent().name())) {
                defineSubsystemTurboFilter(sb, ss);
            }
        }

        //Generate subsystem logging quickstep
        for (SubSystemLoggerConfigurationType ss : config.getSubSystemLogger()) {
            if (null == ss.getComponent() || null == ss.getLevel()) {
                LOGGER.error("Subsystem ({}) or level ({})is null", ss.getComponent(), ss.getLevel());
                continue;
            }

            //skip disabled subsystem logger
            if ("OFF".equals(ss.getLevel().name())) {
                continue;
            }

            //All ready defined above
            if ("ALL".contentEquals(ss.getComponent().name())) {
                continue;
            }
            defineSubsystemTurboFilter(sb, ss);
        }

        boolean rootAppenderDefined = StringUtils.isNotEmpty(config.getRootLoggerAppender());
        boolean altAppenderEnabled = isAltAppenderEnabled(midpointConfiguration);
        boolean altAppenderCreated = false;

        //Generate appenders configuration
        for (AppenderConfigurationType appender : config.getAppender()) {
            boolean isRoot = rootAppenderDefined && config.getRootLoggerAppender().equals(appender.getName());
            boolean createAlt = altAppenderEnabled && isRoot;
            prepareAppenderConfiguration(sb, appender, config, isRoot, createAlt, midpointConfiguration);
            altAppenderCreated = altAppenderCreated || createAlt;
        }

        //define root appender if defined
        if (rootAppenderDefined) {
            sb.append("\t<root level=\"");
            sb.append(config.getRootLoggerLevel());
            sb.append("\">\n");
            sb.append("\t\t<appender-ref ref=\"");
            sb.append(config.getRootLoggerAppender());
            sb.append("\" />\n");
            if (altAppenderCreated) {
                sb.append("\t\t<appender-ref ref=\"" + ALT_APPENDER_NAME + "\"/>\n");
            }
            sb.append("\t\t<appender-ref ref=\"" + TRACING_APPENDER_NAME + "\"/>\n");
            sb.append("\t</root>\n");
        }

        ProfilingMode profilingMode = midpointConfiguration.getProfilingMode();

        LoggingLevelType profilingLoggingLevelSet = null;

        //Generate class based loggers
        for (ClassLoggerConfigurationType logger : config.getClassLogger()) {
            sb.append("\t<logger name=\"");
            sb.append(logger.getPackage());
            sb.append("\" level=\"");
            LoggingLevelType level;
            if (MidPointConstants.PROFILING_LOGGER_NAME.equals(logger.getPackage())) {
                if (profilingMode == ProfilingMode.DYNAMIC) {
                    profilingLoggingLevelSet = logger.getLevel() != null ? logger.getLevel() : DEFAULT_PROFILING_LEVEL;
                    level = LoggingLevelType.TRACE;
                } else {
                    level = logger.getLevel();
                }
            } else {
                level = logger.getLevel();
            }
            sb.append(level);
            sb.append("\"");
            //if logger specific appender is defined
            if (null != logger.getAppender() && !logger.getAppender().isEmpty()) {
                sb.append(" additivity=\"false\">\n");
                for (String appenderName : logger.getAppender()) {
                    sb.append("\t\t<appender-ref ref=\"");
                    sb.append(appenderName);
                    sb.append("\"/>");
                }
                sb.append("\t</logger>\n");
            } else {
                sb.append("/>\n");
            }
        }

        if (profilingMode == ProfilingMode.DYNAMIC) {
            if (profilingLoggingLevelSet != null) {
                OperationExecutionLogger.setGlobalOperationInvocationLevelOverride(LoggingSchemaUtil.toLevel(profilingLoggingLevelSet));
            } else {
                OperationExecutionLogger.setGlobalOperationInvocationLevelOverride(LoggingSchemaUtil.toLevel(DEFAULT_PROFILING_LEVEL));
                sb.append("\t<logger name=\"");
                sb.append(MidPointConstants.PROFILING_LOGGER_NAME);
                sb.append("\" level=\"TRACE\"/>\n");
            }
        }

        generateAuditingLogConfig(config.getAuditing(), sb);

        if (null != config.getAdvanced()) {
            for (Object item : config.getAdvanced().getContent()) {
                sb.append(item.toString());
                sb.append("\n");
            }
        }

        // LevelChangePropagator to propagate log level changes to JUL
        // this keeps us from performance impact of disable JUL logging statements
        // WARNING: if deployed in Tomcat then this propagates only to the JUL loggers in current classloader.
        // It means that ICF connector loggers are not affected by this
        // MAGIC: moved to the end of the "file" as suggested in http://jira.qos.ch/browse/LOGBACK-740
        sb.append("\t<contextListener class=\"ch.qos.logback.classic.jul.LevelChangePropagator\">\n");
        sb.append("\t\t<resetJUL>true</resetJUL>\n");
        sb.append("\t</contextListener>\n");

        sb.append("</configuration>");
        return sb.toString();
    }

    private static void prepareAppenderConfiguration(StringBuilder sb, AppenderConfigurationType appender,
            LoggingConfigurationType config, boolean isRoot, boolean createAltForThisAppender,
            MidpointConfiguration midpointConfiguration) throws SchemaException {
        if (appender instanceof FileAppenderConfigurationType) {
            prepareFileAppenderConfiguration(sb, (FileAppenderConfigurationType) appender, config);
        } else if (appender instanceof SyslogAppenderConfigurationType) {
            prepareSyslogAppenderConfiguration(sb, (SyslogAppenderConfigurationType) appender, config);
        } else {
            throw new SchemaException("Unknown appender configuration " + appender);
        }
        if (createAltForThisAppender) {
            prepareAltAppenderConfiguration(sb, appender, midpointConfiguration);
        }
        if (isRoot) {
            prepareTracingAppenderConfiguration(sb, appender);
        }
    }

    private static void prepareAltAppenderConfiguration(StringBuilder sb, AppenderConfigurationType appender,
            MidpointConfiguration midpointConfiguration) {
        String altFilename = getAltAppenderFilename(midpointConfiguration);
        String altPrefix = getAltAppenderPrefix(midpointConfiguration);
        if (StringUtils.isNotEmpty(altFilename)) {
            sb.append("\t<appender name=\"" + ALT_APPENDER_NAME + "\" class=\"ch.qos.logback.core.FileAppender\">\n")
                    .append("\t\t\t\t<file>").append(altFilename).append("</file>\n")
                    .append("\t\t\t\t<layout class=\"ch.qos.logback.classic.PatternLayout\">\n")
                    .append("\t\t\t\t\t<pattern>").append(altPrefix).append(appender.getPattern()).append("</pattern>\n")
                    .append("\t\t\t\t</layout>\n")
                    .append("\t\t\t</appender>\n");
        } else {
            sb.append("\t<appender name=\"" + ALT_APPENDER_NAME + "\" class=\"ch.qos.logback.core.ConsoleAppender\">\n")
                    .append("\t\t\t\t<layout class=\"ch.qos.logback.classic.PatternLayout\">\n")
                    .append("\t\t\t\t\t<pattern>").append(altPrefix).append(appender.getPattern()).append("</pattern>\n")
                    .append("\t\t\t\t</layout>\n")
                    .append("\t\t\t</appender>\n");
        }
    }

    private static boolean isAltAppenderEnabled(MidpointConfiguration midpointConfiguration) {
        Configuration config = midpointConfiguration.getConfiguration();
        return config != null && "true".equals(config.getString(MidpointConfiguration.MIDPOINT_LOGGING_ALT_ENABLED_PROPERTY));
    }

    private static String getAltAppenderPrefix(MidpointConfiguration midpointConfiguration) {
        Configuration config = midpointConfiguration.getConfiguration();
        return config != null ? config.getString(MidpointConfiguration.MIDPOINT_LOGGING_ALT_PREFIX_PROPERTY, "") : "";
    }

    private static String getAltAppenderFilename(MidpointConfiguration midpointConfiguration) {
        Configuration config = midpointConfiguration.getConfiguration();
        return config != null ? config.getString(MidpointConfiguration.MIDPOINT_LOGGING_ALT_FILENAME_PROPERTY) : null;
    }

    private static void prepareFileAppenderConfiguration(StringBuilder sb, FileAppenderConfigurationType appender, LoggingConfigurationType config) {
        String fileName = appender.getFileName();
        String filePattern = appender.getFilePattern();
        boolean isPrudent = Boolean.TRUE.equals(appender.isPrudent());
        boolean isAppend = Boolean.TRUE.equals(appender.isAppend());
        boolean isRolling = filePattern != null ||
                appender.getMaxHistory() != null && appender.getMaxHistory() > 0 ||
                StringUtils.isNotEmpty(appender.getMaxFileSize());
        String appenderClass = isRolling ? "ch.qos.logback.core.rolling.RollingFileAppender" : "ch.qos.logback.core.FileAppender";

        prepareCommonAppenderHeader(sb, appender, config, appenderClass);

        if (!isPrudent) {
            appendProp(sb, "file", fileName);
            appendProp(sb, "append", isAppend);
        } else {
            appendProp(sb, "prudent", true);
        }

        if (isRolling) {
            sb.append("\t\t<rollingPolicy class=\"ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy\">\n");
            sb.append("\t\t\t<fileNamePattern>").append(filePattern).append("</fileNamePattern>\n");
            if (appender.getMaxHistory() != null && appender.getMaxHistory() > 0) {
                sb.append("\t\t\t<maxHistory>").append(appender.getMaxHistory()).append("</maxHistory>\n");
            }
            sb.append("\t\t\t<cleanHistoryOnStart>true</cleanHistoryOnStart>\n");
            if (StringUtils.isNotEmpty(appender.getMaxFileSize())) {
                sb.append("\t\t\t<maxFileSize>").append(appender.getMaxFileSize()).append("</maxFileSize>\n");
            }
            if (StringUtils.isNotEmpty(appender.getTotalSizeCap())) {
                sb.append("\t\t\t<totalSizeCap>").append(appender.getTotalSizeCap()).append("</totalSizeCap>\n");
            }
            sb.append("\t\t</rollingPolicy>\n");
        }

        prepareCommonAppenderFooter(sb, appender);
    }

    private static void prepareSyslogAppenderConfiguration(StringBuilder sb, SyslogAppenderConfigurationType appender, LoggingConfigurationType config) {

        prepareCommonAppenderHeader(sb, appender, config, "ch.qos.logback.classic.net.SyslogAppender");

        appendProp(sb, "syslogHost", appender.getSyslogHost());
        appendProp(sb, "port", appender.getPort());
        appendProp(sb, "facility", appender.getFacility());
        appendProp(sb, "suffixPattern", appender.getSuffixPattern());
        appendProp(sb, "stackTracePattern", appender.getStackTracePattern());
        appendProp(sb, "throwableExcluded", appender.isThrowableExcluded());

        prepareCommonAppenderFooter(sb, appender);
    }

    private static void appendProp(StringBuilder sb, String name, Object value) {
        if (value != null) {
            sb.append("\t\t<").append(name).append(">");
            sb.append(value);
            sb.append("</").append(name).append(">\n");
        }
    }

    private static void prepareTracingAppenderConfiguration(StringBuilder sb, AppenderConfigurationType appender) {
        sb.append("\t<appender name=\"" + TRACING_APPENDER_NAME + "\" class=\"").append(TRACING_APPENDER_CLASS_NAME).append("\">\n")
                .append("\t\t<layout class=\"ch.qos.logback.classic.PatternLayout\">\n")
                .append("\t\t\t<pattern>").append(appender.getPattern()).append("</pattern>\n")
                .append("\t\t</layout>\n")
                .append("\t</appender>\n");
    }

    private static void prepareCommonAppenderHeader(StringBuilder sb,
            AppenderConfigurationType appender, LoggingConfigurationType config, String appenderClass) {
        sb.append("\t<appender name=\"").append(appender.getName()).append("\" class=\"").append(appenderClass).append("\">\n");

        //Apply profiling appender filter if necessary
        if (IDM_PROFILE_APPENDER.equals(appender.getName())) {
            for (ClassLoggerConfigurationType cs : config.getClassLogger()) {
                if (REQUEST_FILTER_LOGGER_CLASS_NAME.equals(cs.getPackage()) || PROFILING_ASPECT_LOGGER.endsWith(cs.getPackage())) {
                    LOGGER.debug("Defining ProfilingLogbackFilter to {} appender.", appender.getName());
                    sb.append(defineProfilingLogbackFilter());
                }
            }
        }
    }

    private static void prepareCommonAppenderFooter(StringBuilder sb, AppenderConfigurationType appender) {
        sb.append("\t\t<encoder>\n");
        sb.append("\t\t\t<pattern>");
        sb.append(appender.getPattern());
        sb.append("</pattern>\n");
        sb.append("\t\t</encoder>\n");
        sb.append("\t</appender>\n");
    }

    private static void generateAuditingLogConfig(LoggingAuditingConfigurationType auditing, StringBuilder sb) {
        sb.append("\t<logger name=\"");
        sb.append(AUDIT_LOGGER_NAME);
        sb.append("\" level=\"");
        if (auditing != null && (auditing.isEnabled() == null || auditing.isEnabled())) {
            if (auditing.isDetails() != null && auditing.isDetails()) {
                sb.append("DEBUG");
            } else {
                sb.append("INFO");
            }
        } else {
            sb.append("OFF");
        }
        sb.append("\"");
        //if logger specific appender is defined
        if (auditing != null && auditing.getAppender() != null && !auditing.getAppender().isEmpty()) {
            sb.append(" additivity=\"false\">\n");
            for (String appenderName : auditing.getAppender()) {
                sb.append("\t\t<appender-ref ref=\"");
                sb.append(appenderName);
                sb.append("\"/>");
            }
            sb.append("\t</logger>\n");
        } else {
            sb.append("/>\n");
        }
    }

    // Not very useful these days, as we don't set system loggers in the sysconfig object any more.
    // But let's keep this just in case it's needed in the future.
    private static void defineSubsystemTurboFilter(StringBuilder sb, SubSystemLoggerConfigurationType ss) {
        sb.append("\t<turboFilter class=\"").append(MDCLevelTurboFilter.class.getName()).append("\">\n");
        sb.append("\t\t<MDCKey>subsystem</MDCKey>\n");
        sb.append("\t\t<MDCValue>");
        sb.append(ss.getComponent().name());
        sb.append("</MDCValue>\n");
        sb.append("\t\t<level>");
        sb.append(ss.getLevel().name());
        sb.append("</level>\n");
        sb.append("\t\t<OnMatch>ACCEPT</OnMatch>\n");
        sb.append("\t</turboFilter>\n");
    }

    private static String defineProfilingLogbackFilter() {
        return ("\t<filter class=\"" + ProfilingLogbackFilter.class.getName() + "\" />\n");
    }

    public static void dummy() {
    }

}
