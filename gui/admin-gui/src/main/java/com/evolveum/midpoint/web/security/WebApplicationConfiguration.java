/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import java.io.Serializable;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class WebApplicationConfiguration implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(WebApplicationConfiguration.class);

    /**
     * How often to refresh 'progress table' (in milliseconds; 0 means this feature is disabled).
     */
    private final int progressRefreshInterval;

    private String importFolder;
    private String exportFolder;
    private boolean abortEnabled; // should the "Abort" for async operations be enabled? (requires progress reporting to be enabled)

    public WebApplicationConfiguration(Configuration config) {
        importFolder = config.getString("importFolder");
        exportFolder = config.getString("exportFolder");
        progressRefreshInterval = config.getInt("progressRefreshInterval", 400);
        abortEnabled = config.getBoolean("abortEnabled", true);

        if (abortEnabled && !isProgressReportingEnabled()) {
            LOGGER.warn("Abort functionality requires progress reporting to be enabled - set progressRefreshInterval in '" + MidpointConfiguration.WEB_APP_CONFIGURATION + "' section to a non-zero value");
            abortEnabled = false;
        }
        String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);

        if (importFolder == null) {
            if (StringUtils.isNotEmpty(midpointHome)) {
                importFolder = midpointHome + "/tmp";
            } else {
                importFolder = ".";
            }
        }

        if (exportFolder == null) {
            if (StringUtils.isNotEmpty(midpointHome)) {
                exportFolder = midpointHome + "/tmp";
            } else {
                exportFolder = ".";
            }
        }
    }

    public String getImportFolder() {
        return importFolder;
    }

    public String getExportFolder() {
        return exportFolder;
    }

    public int getProgressRefreshInterval() {
        return progressRefreshInterval;
    }

    public boolean isProgressReportingEnabled() {
        return progressRefreshInterval > 0;
    }

    public boolean isAbortEnabled() {
        return abortEnabled;
    }
}
