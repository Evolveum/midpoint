/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import org.apache.commons.lang3.ObjectUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultDetailLevel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditType;

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

    public static OperationResultDetailLevel getDeltaSuccessExecutionResult(
            SystemConfigurationAuditType configuration) {
        OperationResultDetailLevel defaultValue = OperationResultDetailLevel.CLEANED_UP;
        if (configuration == null || configuration.getEventRecording() == null) {
            return defaultValue;
        } else {
            return ObjectUtils.defaultIfNull(configuration.getEventRecording().getDeltaSuccessExecutionResult(), defaultValue);
        }
    }
}
