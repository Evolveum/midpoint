/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author mederly
 */
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
                s.setInternals(new InternalsConfigurationType());           // hopefully prismContext etc is correctly set
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

    // TODO check the method name
    public static String getPublicHttpUrlPattern(SystemConfigurationType sysconfig) {
        if (sysconfig == null) {
            return null;
        } else if (sysconfig.getInfrastructure() != null && sysconfig.getInfrastructure().getPublicHttpUrlPattern() != null) {
            String publicHttpUrlPattern = sysconfig.getInfrastructure().getPublicHttpUrlPattern();
            String defaultHostname = getDefaultHostname(sysconfig);
            if (defaultHostname != null) {
                return publicHttpUrlPattern.replace("$host", defaultHostname);
            } else {
                // TODO What if $host is specified but default hostname is not? Then we should use the current hostname.
                return publicHttpUrlPattern;
            }
        } else {
            return null;
        }
    }
}
