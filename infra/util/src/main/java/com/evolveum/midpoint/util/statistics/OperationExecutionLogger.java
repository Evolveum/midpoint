/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.statistics;

import ch.qos.logback.classic.Level;

/**
 *
 */
public class OperationExecutionLogger {

    public static final String PROFILING_LOGGER_NAME = "PROFILING";
    static final org.slf4j.Logger LOGGER_PROFILING = org.slf4j.LoggerFactory.getLogger(PROFILING_LOGGER_NAME);

    public static final String INDENT_STRING = " ";
    static final String MDC_DEPTH_KEY = "depth";
    static final String MDC_SUBSYSTEM_KEY = "subsystem";

    static Level globalLevelOverride = null;
    static final ThreadLocal<Level> threadLocalLevelOverride = new ThreadLocal<>();

    static boolean isProfilingActive = false;       // TODO decide what to do with this one

    @SuppressWarnings("unused")
    public static Level getGlobalOperationInvocationLevelOverride() {
        return globalLevelOverride;
    }

    public static void setGlobalOperationInvocationLevelOverride(Level value) {
        globalLevelOverride = value;
    }

    public static Level getLocalOperationInvocationLevelOverride() {
        return threadLocalLevelOverride.get();
    }

    public static void setLocalOperationInvocationLevelOverride(Level value) {
        threadLocalLevelOverride.set(value);
    }

    /**
     *   Activates aspect based subsystem profiling
     */
    public static void activateSubsystemProfiling(){
        isProfilingActive = true;
    }

    /**
     *   Deactivates aspect based subsystem profiling
     */
    public static void deactivateSubsystemProfiling(){
        isProfilingActive = false;
    }
}
