/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.security;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class WebApplicationConfiguration implements Serializable {

    private static final String MIDPOINT_HOME = "midpoint.home"; //todo move somewhere
    private String importFolder;
    private String exportFolder;

    public WebApplicationConfiguration(Configuration config) {
        importFolder = config.getString("importFolder");
        exportFolder = config.getString("exportFolder");
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
}
