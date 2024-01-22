/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/** Collects the information to fill the {@link SystemFeatures} instance. */
@Component
public class SystemFeaturesEnquirer {

    @Autowired private TaskManager taskManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private SystemObjectCache systemObjectCache;

    public @NotNull SystemFeatures getSystemFeatures(@NotNull OperationResult result) {
        var enquiry = new Enquiry(getSystemConfiguration(result));
        return new SystemFeatures(
                enquiry.isPublicHttpUrlPatternPresent(),
                enquiry.isCustomLoggingPresent(),
                enquiry.areMailNotificationsPresent(),
                enquiry.isClusterPresent(),
                enquiry.isGenericProductionDatabasePresent());
    }

    private @Nullable SystemConfigurationType getSystemConfiguration(OperationResult result) {
        try {
            return systemObjectCache.getSystemConfigurationBean(result);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when getting system configuration");
        }
    }

    /** Created to avoid the necessity of gathering the system configuration repeatedly. */
    class Enquiry {

        @Nullable private final SystemConfigurationType systemConfiguration;

        public Enquiry(@Nullable SystemConfigurationType systemConfiguration) {
            this.systemConfiguration = systemConfiguration;
        }

        boolean isPublicHttpUrlPatternPresent() {
            return StringUtils.isNotBlank(
                    SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfiguration));
        }

        boolean isCustomLoggingPresent() {
            return LoggingConfigurationManager.isExtraAppenderPresent(
                    SystemConfigurationTypeUtil.getLogging(systemConfiguration));
        }

        boolean areMailNotificationsPresent() {
            if (systemConfiguration == null) {
                return false;
            }

            // 4.5+ style ("new")
            MessageTransportConfigurationType transportConfiguration = systemConfiguration.getMessageTransportConfiguration();
            if (transportConfiguration != null) {
                if (areMailNotificationsPresent(transportConfiguration.getMail())) {
                    return true;
                }
                if (areMailNotificationsPresent(transportConfiguration.getCustomTransport())) {
                    return true;
                }
            }

            // legacy style
            return areMailNotificationsPresent(
                    SystemConfigurationTypeUtil.getLegacyMailTransportConfiguration(systemConfiguration));
        }

        private boolean areMailNotificationsPresent(@NotNull List<? extends GeneralTransportConfigurationType> transports) {
            return transports.stream()
                    .anyMatch(t -> t instanceof MailTransportConfigurationType mail && areMailNotificationsPresent(mail));
        }

        private boolean areMailNotificationsPresent(MailTransportConfigurationType config) {
            return !config.getServer().isEmpty();
        }

        private boolean areMailNotificationsPresent(@Nullable MailConfigurationType config) {
            return config != null && !config.getServer().isEmpty();
        }

        boolean isClusterPresent() {
            return taskManager.isClustered();
        }

        boolean isGenericProductionDatabasePresent() {
            return !repositoryService.isNative()
                    && !repositoryService.getRepositoryDiag().isH2();
        }
    }
}
