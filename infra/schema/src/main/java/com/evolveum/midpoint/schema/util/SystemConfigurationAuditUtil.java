/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Utility methods for audit-related system configuration options.
 */
public class SystemConfigurationAuditUtil {

    public static boolean isEscapingInvalidCharacters(SystemConfigurationAuditType configuration) {
        final boolean defaultValue = false;
        if (configuration == null || configuration.getEventRecording() == null) {
            return defaultValue;
        } else {
            return ObjectUtils.defaultIfNull(configuration.getEventRecording().isEscapeIllegalCharacters(), defaultValue);
        }
    }
}
