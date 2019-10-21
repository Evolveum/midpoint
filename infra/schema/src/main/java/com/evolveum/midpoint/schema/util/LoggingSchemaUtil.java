/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import ch.qos.logback.classic.Level;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class LoggingSchemaUtil {

    public static Level toLevel(@NotNull LoggingLevelType level) {
        switch (level) {
            case ALL: return Level.ALL;
            case TRACE: return Level.TRACE;
            case DEBUG: return Level.DEBUG;
            case INFO: return Level.INFO;
            case WARN: return Level.WARN;
            case ERROR: return Level.ERROR;
            case OFF: return Level.OFF;
            default: throw new IllegalArgumentException("level: " + level);
        }
    }
}
