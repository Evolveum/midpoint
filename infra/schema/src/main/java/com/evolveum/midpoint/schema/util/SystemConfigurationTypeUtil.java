/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SystemConfigurationTypeUtil {

    public static boolean isExperimentalCodeEnabled(SystemConfigurationType config) {
        if (config == null || config.getInternals() == null || config.getInternals().isEnableExperimentalCode() == null) {
            return false;
        }
        return config.getInternals().isEnableExperimentalCode();
    }

    public static void setEnableExperimentalCode(SystemConfigurationType s, Boolean enableExperimentalCode) {
        if (enableExperimentalCode == null) {
            if (s.getInternals() != null) {
                s.getInternals().setEnableExperimentalCode(null);
                s.asPrismContainerValue().findContainer(SystemConfigurationType.F_INTERNALS).normalize();
            }
        } else {
            if (s.getInternals() == null) {
                s.setInternals(new InternalsConfigurationType()); // hopefully prismContext etc is correctly set
            }
            s.getInternals().setEnableExperimentalCode(enableExperimentalCode);
        }
    }

    public static Integer getMaxModelClicks(PrismObject<SystemConfigurationType> sysconfigObject) {
        if (sysconfigObject == null || sysconfigObject.asObjectable().getInternals() == null) {
            return null;
        }
        return sysconfigObject.asObjectable().getInternals().getMaxModelClicks();
    }

    private static String getDefaultHostname(SystemConfigurationType sysconfig) {
        if (sysconfig != null && sysconfig.getInfrastructure() != null) {
            return sysconfig.getInfrastructure().getDefaultHostname();
        } else {
            return null;
        }
    }

    public static MailConfigurationType getLegacyMailTransportConfiguration(SystemConfigurationType systemConfiguration) {
        if (systemConfiguration == null) {
            return null;
        }
        NotificationConfigurationType notificationConfiguration = systemConfiguration.getNotificationConfiguration();
        if (notificationConfiguration == null) {
            return null;
        }
        return notificationConfiguration.getMail();
    }

    public static @NotNull List<SmsConfigurationType> getLegacySmsTransportConfigurations(SystemConfigurationType systemConfiguration) {
        if (systemConfiguration == null) {
            return List.of();
        }
        NotificationConfigurationType notificationConfiguration = systemConfiguration.getNotificationConfiguration();
        if (notificationConfiguration == null) {
            return List.of();
        }
        return notificationConfiguration.getSms();
    }

    public static LoggingConfigurationType getLogging(SystemConfigurationType systemConfiguration) {
        return systemConfiguration != null ? systemConfiguration.getLogging() : null;
    }

    public static String getPublicHttpUrlPattern(SystemConfigurationType sysconfig) {
        if (sysconfig == null) {
            return null;
        }
        InfrastructureConfigurationType infrastructure = sysconfig.getInfrastructure();
        if (infrastructure == null) {
            return null;
        }
        return infrastructure.getPublicHttpUrlPattern();
    }

    public static @NotNull List<String> getRemoteHostAddressHeader(SystemConfigurationType sysconfig) {
        if (sysconfig == null) {
            return List.of();
        }
        InfrastructureConfigurationType infrastructure = sysconfig.getInfrastructure();
        if (infrastructure == null) {
            return List.of();
        }
        return infrastructure.getRemoteHostAddressHeader();
    }

    // TODO check the method name
    public static String getPublicHttpUrlPattern(SystemConfigurationType sysconfig, String host) {
        var pattern = getPublicHttpUrlPattern(sysconfig);
        final String hostMacro = "$host";

        if (pattern == null || !pattern.contains(hostMacro)) {
            return pattern;
        }
        String defaultHostname = getDefaultHostname(sysconfig);
        if (defaultHostname != null) {
            return pattern.replace(hostMacro, defaultHostname);
        } else if (StringUtils.isNotBlank(host)) {
            return pattern.replace(hostMacro, host);
        } else {
            return pattern;
        }
    }

    public static boolean isAccessesMetadataEnabled(SystemConfigurationType sysconfig) {
        boolean defaultValue = true;
        if (sysconfig == null) {
            return defaultValue;
        }
        RoleManagementConfigurationType roleManagement = sysconfig.getRoleManagement();
        if (roleManagement == null) {
            return defaultValue;
        }
        Boolean accessesMetadataEnabled = roleManagement.isAccessesMetadataEnabled();
        if (accessesMetadataEnabled == null) {
            return defaultValue;
        }
        return Boolean.TRUE.equals(accessesMetadataEnabled);
    }

    public static String getSubscriptionId(SystemConfigurationType systemConfiguration) {
        if (systemConfiguration == null) {
            return null;
        }

        DeploymentInformationType deploymentInformation = systemConfiguration.getDeploymentInformation();
        if (deploymentInformation == null) {
            return null;
        }
        return deploymentInformation.getSubscriptionIdentifier();
    }

    public static @Nullable ShadowCachingPolicyType getShadowCachingDefaultPolicy(
            @Nullable SystemConfigurationType configuration) {
        var internalsConfig = configuration != null ? configuration.getInternals() : null;
        var shadowCaching = internalsConfig != null ? internalsConfig.getShadowCaching() : null;
        return shadowCaching != null ? shadowCaching.getDefaultPolicy() : null;
    }

    public static @Nullable SmartIntegrationConfigurationType getSmartIntegrationConfiguration(
            @Nullable SystemConfigurationType systemConfiguration) {
        return systemConfiguration != null ? systemConfiguration.getSmartIntegration() : null;
    }
}
