/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.impl;

import java.io.PrintStream;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Log {

    private enum Level {
        ERROR, INFO, DEBUG
    }

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
        log(Level.ERROR, message, args);

        if (ex != null && level == LogVerbosity.VERBOSE) {
            log(Level.DEBUG, "Exception details", ex);
        }
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public void log(Level level, String message, Object... args) {
        switch (this.level) {
            case SILENT:
                return;
            case DEFAULT:
                if (level == Level.DEBUG) {
                    return;
                }
            case VERBOSE:
                // all log levels should be printed
        }

        // todo simple replacement, should be improved later probably
        String msg = message;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i] != null ? args[i].toString() : "null";
            msg = msg.replaceFirst("\\{\\}", arg);
        }

        stream.println(level + ": " + msg);
    }
}
