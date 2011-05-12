/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;

import com.evolveum.midpoint.api.logging.Trace;

/**
 *
 * @author Vilo Repan
 */
public class LoggerMXBeanImpl implements LoggerMXBean {

    private static Trace TRACE = TraceManager.getTrace(LoggerMXBeanImpl.class);
    //constants
    private static final String DEFAULT_LOG_PATTERN = "%d{HH:mm:ss,SSS} %-5p [%c] - %m%n";
    private static final int DEFAULT_LOG_LEVEL = Level.WARN_INT;
    private static final String APPENDER_NAME = "LoggerMXBeanControllerAppender";
    //variables
    private int moduleLogLevel = DEFAULT_LOG_LEVEL;
    private String name;
    private String displayName;
    private String logPattern;
    private String rootPackage;
    private List<LogInfo> logInfoList;

    public LoggerMXBeanImpl(String name, String rootPackage) {
        this.name = name;
        this.rootPackage = rootPackage;
        TRACE.debug("Constructor::Initialising Logger MXBean instance: " + name);
        updateRootLogger();
    }

    @Override
    public List<LogInfo> getLogInfoList() {
        if (logInfoList == null) {
            logInfoList = new ArrayList<LogInfo>();
        }
        return logInfoList;
    }

    @Override
    public void setLogInfoList(List<LogInfo> logInfoList) {
    	if (!getLogInfoList().isEmpty()) {
    		for (LogInfo logInfo : getLogInfoList()) {
				logInfo.setLevel(getModuleLogLevel());
			}
    		updateLoggingSettings(getLogInfoList());
    	}
        this.logInfoList = logInfoList;

        updateLoggingSettings(logInfoList);
    }

    @Override
    public String getLogPattern() {
        if (logPattern == null) {
            logPattern = DEFAULT_LOG_PATTERN;
        }
        return logPattern;
    }

    @Override
    public void setLogPattern(String pattern) {
        if (pattern == null) {
            pattern = DEFAULT_LOG_PATTERN;
        }
        if (!pattern.equals(this.logPattern)) {
            TRACE.info("Updating log pattern '{}' for logger {}.", new Object[]{pattern, logPattern});

            this.logPattern = pattern;
            updateRootLogger();
        }
    }

    @Override
    public LogInfo getLogInfo(String packageName) {
        List<LogInfo> list = getLogInfoList();
        LogInfo logInfo = null;
        for (LogInfo info : list) {
            if (info.getPackageName().equals(packageName)) {
                logInfo = info;
            }
        }

        if (logInfo == null) {
            logInfo = new LogInfo(packageName, getLoggerLevel(packageName));
        }

        return logInfo;
    }

    @Override
    public void setLogInfo(LogInfo logInfo) {
        List<LogInfo> list = getLogInfoList();
        boolean found = false;
        for (LogInfo info : list) {
            if (logInfo.getPackageName().equals(info.getPackageName())) {
                info.setLevel(logInfo.getLevel());
                found = true;
                break;
            }
        }

        if (!found) {
            list.add(logInfo);
        }

        updateLoggingSettings(logInfo);
    }

    @Override
    public void setLogInfo(String packageName, int level) {
        setLogInfo(new LogInfo(packageName, level));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        if (displayName == null || displayName.isEmpty()) {
            return name;
        }
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    private void updateLoggingSettings(LogInfo info) {
        List<LogInfo> list = new ArrayList<LogInfo>();
        list.add(info);

        updateLoggingSettings(list);
    }

    private void updateLoggingSettings(List<LogInfo> infoList) {
        if (infoList == null) {
            return;
        }

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        for (LogInfo info : infoList) {
            if (info == null || info.getPackageName() == null) {
                continue;
            }

            org.apache.log4j.Logger pkgLogger = rootLogger.getLoggerRepository().
                    getLogger(info.getPackageName());
            if (pkgLogger == null) {
                TRACE.warn("Can't change logger level, reason: logger for package '" +
                        info.getPackageName() + "' is null.");
                continue;
            }
            TRACE.info("Log4J root logger: {} Module: {}. Updating logger '{}' to level: {}", new Object[]{
                        rootLogger, name, info.getPackageName(), Level.toLevel(info.getLevel())});
            pkgLogger.setLevel(Level.toLevel(info.getLevel()));
        }
    }

    private int getLoggerLevel(String packageName) {
        int level = Level.OFF_INT;
        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        org.apache.log4j.Logger pkgLogger = rootLogger.getLoggerRepository().getLogger(packageName);
        if (pkgLogger != null) {
            level = pkgLogger.getEffectiveLevel().toInt();
        }

        return level;
    }

    @Override
    public void setModuleLogLevel(int level) {
        if (moduleLogLevel != level) {
            moduleLogLevel = level;
            updateRootLogger();
        }
    }

    @Override
    public int getModuleLogLevel() {
        return moduleLogLevel;
    }

    public void setModuleLogLevel(String name) {
        if (name != null && levelExists(name)) {
            Level level = Level.toLevel(name);
            setModuleLogLevel(level.toInt());
        }
    }

    private boolean levelExists(String name) {
        try {
            Level level = Level.toLevel(name);
            if (level != null) {
                return true;
            }
        } catch (Exception ex) {
            TRACE.error("Unknown log level '" + name + "', reason: " + ex.getMessage());
        }

        return false;
    }

    private void updateRootLogger() {
        TRACE.info("Updating root logger for: {}, package: {}, level: {}", new Object[]{name,
                    rootPackage, Level.toLevel(moduleLogLevel)});

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
        updateAppender(rootLogger); //TODO: fix when appenders can be managed through webapp

        org.apache.log4j.Logger loggingLogger = rootLogger.getLoggerRepository().getLogger("com.evolveum.midpoint.logging");
        loggingLogger.setLevel(Level.INFO);

//        org.apache.log4j.Logger logger = rootLogger.getLoggerRepository().getLogger(rootPackage);
        org.apache.log4j.Logger logger = rootLogger.getLoggerRepository().getLogger("com.evolveum.midpoint");
        logger.setLevel(Level.toLevel(moduleLogLevel));
    }

    private void updateAppender(org.apache.log4j.Logger logger) {  	
//        Appender appender = logger.getAppender(APPENDER_NAME);
//        if (appender == null) {
//            appender = new ConsoleAppender(new PatternLayout(getLogPattern()));
//            appender.setName(APPENDER_NAME);
//            logger.addAppender(appender);
//        }
    }
}
