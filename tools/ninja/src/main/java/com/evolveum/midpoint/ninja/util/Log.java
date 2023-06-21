/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.util;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.ninja.impl.LogTarget;

/**
 * TODO get rid of logback magic in here, this is hell. How could I do this. OMG.
 *
 * Created by Viliam Repan (lazyman).
 */
public class Log {

    public enum LogLevel {

        SILENT,

        DEFAULT,

        VERBOSE
    }

    private LogTarget target;

    private LogLevel level;

    private org.slf4j.Logger info = LoggerFactory.getLogger(Log.class);
    private org.slf4j.Logger error = LoggerFactory.getLogger(Log.class);

    public Log(@NotNull LogTarget target, @NotNull LogLevel level) {
        this.target = target;
        this.level = level;
    }

    private boolean isVerbose() {
        return level == LogLevel.VERBOSE;
    }

    private boolean isSilent() {
        return level == LogLevel.SILENT;
    }

    public void error(String message, Object... args) {
        error(message, null, args);
    }

    public void error(String message, Exception ex, Object... args) {
        error.error(message, args);

        if (isVerbose()) {
            error.error("Exception details", ex);
        }
    }

    public void debug(String message, Object... args) {
        info.debug(message, args);
    }

    public void info(String message, Object... args) {
        info.info(message, args);
    }
}
