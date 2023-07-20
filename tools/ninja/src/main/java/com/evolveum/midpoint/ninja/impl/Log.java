/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.impl;

import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Log {

    private static final Pattern PATTERN = Pattern.compile("\\{}");

    private enum Level {
        ERROR, INFO, DEBUG;

        public ConsoleFormat.Level toConsoleFormatLevel() {
            switch (this) {
                case ERROR:
                    return ConsoleFormat.Level.ERROR;
                case INFO:
                    return ConsoleFormat.Level.INFO;
                case DEBUG:
                    return ConsoleFormat.Level.DEFAULT;
                default:
                    throw new IllegalStateException("Unknown log level: " + this);
            }
        }
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
        if (ex != null) {
            message = message + ". Reason: " + ex.getMessage();
        }

        log(Level.ERROR, message, args);

        if (ex != null && level == LogVerbosity.VERBOSE) {
            log(Level.DEBUG, "Exception details:\n{}", ex);
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

        Matcher matcher = PATTERN.matcher(message);

        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (matcher.find()) {
            Object arg = args[i++];
            if (arg instanceof Exception && level == Level.DEBUG) {
                arg = NinjaUtils.printStackToString((Exception) arg);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(arg.toString()));
        }
        matcher.appendTail(sb);

        ConsoleFormat.formatMessageWithParameter(level + ": ", sb.toString(), level.toConsoleFormatLevel());

        stream.println(level + ": " + sb);
    }
}
