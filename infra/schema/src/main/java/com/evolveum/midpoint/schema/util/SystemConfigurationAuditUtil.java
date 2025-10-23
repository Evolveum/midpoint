/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IndexAdditionalItemPathType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultDetailLevel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationAuditChangedItemPathsType;
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

    public static boolean isIndexingAddDeltaOperation(SystemConfigurationAuditType configuration) {
        final boolean defaultValue = false;
        if (configuration == null || configuration.getChangedItemPaths() == null) {
            return defaultValue;
        }

        SystemConfigurationAuditChangedItemPathsType pathsConfiguration = configuration.getChangedItemPaths();

        return ObjectUtils.defaultIfNull(pathsConfiguration.isIndexAddObjectDeltaOperation(), defaultValue);
    }

    public static Set<ChangedItemPath> getIndexingAdditionalItemPaths(SystemConfigurationAuditType configuration) {
        if (configuration == null || configuration.getChangedItemPaths() == null) {
            return Set.of();
        }

        SystemConfigurationAuditChangedItemPathsType pathsConfiguration = configuration.getChangedItemPaths();

        List<IndexAdditionalItemPathType> paths = pathsConfiguration.getIndexAdditionalItemPath();
        return paths.stream()
                .filter(p -> p.getPath() != null)
                .map(p -> new ChangedItemPath(p.getPath().getItemPath().namedSegmentsOnly(), p.getAll()))
                .collect(Collectors.toSet());
    }
}
