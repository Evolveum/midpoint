/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.common.configuration.api.SystemConfigurationSection;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import org.apache.commons.configuration.Configuration;

/**
 *
 */
public class SystemConfigurationSectionImpl implements SystemConfigurationSection {

    private static final String LOG_FILE_CONFIG_KEY = "logFile";
    private static final String JMAP_CONFIG_KEY = "jmap";

    private final Configuration configuration;

    SystemConfigurationSectionImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getJmap() {
        String configured = getStringKey(JMAP_CONFIG_KEY);
        if (configured != null) {
            return configured;
        }
        String javaHome = System.getenv(MidPointConstants.JAVA_HOME_ENVIRONMENT_VARIABLE);
        if (javaHome != null) {
            return javaHome + "/bin/jmap";
        }
        return "jmap";          // Let's give it a chance. Maybe it's on the path.
    }

    @Override
    public String getLogFile() {
        return getStringKey(LOG_FILE_CONFIG_KEY);
    }

    private String getStringKey(String key) {
        if (configuration != null && configuration.containsKey(key)) {
            return (configuration.getString(key));
        } else {
            return null;
        }
    }
}
