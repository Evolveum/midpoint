/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

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
            return ProfilingLevel.OFF;
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
