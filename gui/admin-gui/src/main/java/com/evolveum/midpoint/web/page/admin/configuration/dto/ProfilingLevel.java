/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_2.LoggingLevelType;

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
