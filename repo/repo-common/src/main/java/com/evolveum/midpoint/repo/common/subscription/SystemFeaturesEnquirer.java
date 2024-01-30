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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
                enquiry.isPublicHttpsUrlPatternDefined(),
                enquiry.isRemoteHostAddressHeaderDefined(),
                enquiry.isCustomLoggingDefined(),
                enquiry.areRealNotificationsEnabled(),
                enquiry.isClusteringEnabled(),
                enquiry.isGenericNonH2DatabaseUsed());
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

        boolean isPublicHttpsUrlPatternDefined() {
            var pattern = SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfiguration);
            return pattern != null && pattern.toLowerCase().startsWith("https:");
        }

        boolean isRemoteHostAddressHeaderDefined() {
            return !SystemConfigurationTypeUtil.getRemoteHostAddressHeader(systemConfiguration).isEmpty();
        }

        boolean isCustomLoggingDefined() {
            return LoggingConfigurationManager.isExtraAppenderPresent(
                    SystemConfigurationTypeUtil.getLogging(systemConfiguration));
        }

        boolean areRealNotificationsEnabled() {
            if (systemConfiguration == null) {
                return false;
            }

            // 4.5+ style ("new")
            MessageTransportConfigurationType transportConfiguration = systemConfiguration.getMessageTransportConfiguration();
            if (transportConfiguration != null) {
                if (isMailOrSmsTransportEnabled(transportConfiguration.getMail())) {
                    return true;
                }
                if (isMailOrSmsTransportEnabled(transportConfiguration.getSms())) {
                    return true;
                }
                if (isMailOrSmsTransportEnabled(transportConfiguration.getCustomTransport())) {
                    return true;
                }
            }

            // legacy style
            return areLegacyMailNotificationsEnabled(SystemConfigurationTypeUtil.getLegacyMailTransportConfiguration(systemConfiguration))
                    || areLegacySmsNotificationsEnabled(SystemConfigurationTypeUtil.getLegacySmsTransportConfigurations(systemConfiguration));
        }

        private boolean isMailOrSmsTransportEnabled(@NotNull List<? extends GeneralTransportConfigurationType> transports) {
            return transports.stream()
                    .anyMatch(t ->
                            t instanceof MailTransportConfigurationType mail && isMailTransportEnabled(mail)
                                    || t instanceof SmsTransportConfigurationType sms && isSmsTransportEnabled(sms));
        }

        private boolean isMailTransportEnabled(MailTransportConfigurationType config) {
            return !config.getServer().isEmpty()
                    && config.getRedirectToFile() == null;
        }

        private boolean isSmsTransportEnabled(SmsTransportConfigurationType config) {
            return !config.getGateway().isEmpty()
                    && config.getRedirectToFile() == null;
        }

        private boolean areLegacyMailNotificationsEnabled(@Nullable MailConfigurationType config) {
            return config != null && !config.getServer().isEmpty() && config.getRedirectToFile() == null;
        }

        private boolean areLegacySmsNotificationsEnabled(Collection<SmsConfigurationType> configs) {
            return configs.stream()
                    .anyMatch(c -> !c.getGateway().isEmpty() && c.getRedirectToFile() == null);
        }

        boolean isClusteringEnabled() {
            return taskManager.isClustered();
        }

        boolean isGenericNonH2DatabaseUsed() {
            return repositoryService.isGenericNonH2();
        }
    }
}
