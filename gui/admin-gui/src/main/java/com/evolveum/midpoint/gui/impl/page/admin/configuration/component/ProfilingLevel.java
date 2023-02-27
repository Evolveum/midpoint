/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;

/**
 * @author lazyman
 */
public enum ProfilingLevel {

    OFF("pageLogging.subsystem.level.off"),
    ENTRY_EXIT("pageLogging.subsystem.level.entryExit"),
    ARGUMENTS("pageLogging.subsystem.level.arguments");

    private String localizationKey;

    private ProfilingLevel(String localizationKey) {
        this.localizationKey = localizationKey;
    }

    public String getLocalizationKey() {
        return localizationKey;
    }

    public static LoggingLevelType toLoggerLevelType(ProfilingLevel level) {
        if (level == null) {
            return null;
        }

        switch (level) {
            case ENTRY_EXIT:
                return LoggingLevelType.DEBUG;
            case ARGUMENTS:
                return LoggingLevelType.TRACE;
            case OFF:
                return LoggingLevelType.OFF;
            default:
                return null;
        }
    }

    public static ProfilingLevel fromLoggerLevelType(LoggingLevelType level) {
        if (level == null) {
            return null;
        }

        switch (level) {
            case OFF:
                return ProfilingLevel.OFF;
            case DEBUG:
                return ProfilingLevel.ENTRY_EXIT;
            case TRACE:
            case ALL:
                return ProfilingLevel.ARGUMENTS;
            default:
                return ProfilingLevel.OFF;
        }
    }
}
