/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.impl;

import java.io.PrintStream;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Log {

    private final LogVerbosity level;

    private final PrintStream stream;

    public Log(@NotNull LogVerbosity level, @NotNull PrintStream stream) {
        this.level = level;
        this.stream = stream;
    }

    public void error(String message, Object... args) {
        error(message, null, args);
    }

    public void error(String message, Exception ex, Object... args) {
        if (ex != null) {
            message = message + ". Reason: " + ex.getMessage();
        }

        log(LogLevel.ERROR, message, args);

        if (ex != null && level == LogVerbosity.VERBOSE) {
            log(LogLevel.DEBUG, "Exception details:\n{}", ex);
        }
    }

    public void warn(String message, Object... args) {
        log(LogLevel.WARNING, message, args);
    }

    public void debug(String message, Object... args) {
        log(LogLevel.DEBUG, message, args);
    }

    public void info(String message, Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public void log(LogLevel level, String message, Object... args) {
        switch (this.level) {
            case SILENT:
                return;
            case DEFAULT:
                if (level == LogLevel.DEBUG) {
                    return;
                }
            case VERBOSE:
                // all log levels should be printed
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];

            if (arg instanceof Exception && level == LogLevel.DEBUG) {
                args[i] = NinjaUtils.printStackToString((Exception) arg);
            }
        }

        String formatted = NinjaUtils.printFormatted(message, args);

        stream.println(ConsoleFormat.formatLogMessage(level, formatted));
    }

    /**
     * Prints raw message without any processing, not even new line character at the end.
     *
     * @param message
     */
    public void logRaw(String message) {
        stream.print(message);
    }
}
