/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class WebApplicationConfiguration implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(WebApplicationConfiguration.class);

    public static final String MIDPOINT_HOME = "midpoint.home"; //todo move somewhere

    private String importFolder;
    private String exportFolder;
    private int progressRefreshInterval;            // how often to refresh 'progress table' (in milliseconds; 0 means this feature is disabled)
    private boolean abortEnabled;                   // should the "Abort" for async operations be enabled? (requires progress reporting to be enabled)

    public WebApplicationConfiguration(Configuration config) {
        importFolder = config.getString("importFolder");
        exportFolder = config.getString("exportFolder");
        progressRefreshInterval = config.getInt("progressRefreshInterval", 400);
        abortEnabled = config.getBoolean("abortEnabled", true);

        if (abortEnabled && !isProgressReportingEnabled()) {
            LOGGER.warn("Abort functionality requires progress reporting to be enabled - set progressRefreshInterval in '"+MidPointApplication.WEB_APP_CONFIGURATION+"' section to a non-zero value");
            abortEnabled = false;
        }
        String midpointHome = System.getProperty(MIDPOINT_HOME);

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
