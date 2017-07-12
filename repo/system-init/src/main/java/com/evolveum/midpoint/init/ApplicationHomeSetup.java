/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class ApplicationHomeSetup {

    private static final transient Trace LOGGER = TraceManager.getTrace(ApplicationHomeSetup.class);
    private String midpointHomeSystemPropertyName;

    public void init(String midpointHomeSystemPropertyName) {

        this.midpointHomeSystemPropertyName = midpointHomeSystemPropertyName;

        LOGGER.info(midpointHomeSystemPropertyName + " = " + System.getProperty(midpointHomeSystemPropertyName));
        System.out.println(midpointHomeSystemPropertyName + " = " + System.getProperty(midpointHomeSystemPropertyName));

        String midpointHomePath = System.getProperty(midpointHomeSystemPropertyName);

        createMidpointHomeDirectories(midpointHomePath);
        setupMidpointHomeDirectory(midpointHomePath);

    }
    
    /**
     * Creates directory structure under root
     * <p/>
     * Directory information based on: http://wiki.evolveum.com/display/midPoint/midpoint.home+-+directory+structure
     */
    private void createMidpointHomeDirectories(String midpointHomePath) {
    	
    	if (!checkDirectoryExistence(midpointHomePath)) {
            createDir(midpointHomePath);
        }

        if (!midpointHomePath.endsWith("/")){
            midpointHomePath = midpointHomePath + "/";
        }
        String[] directories = {
                midpointHomePath + "icf-connectors",
                midpointHomePath + "idm-legacy",
                midpointHomePath + "log",
                midpointHomePath + "schema",
                midpointHomePath + "import",
                midpointHomePath + "export",
                midpointHomePath + "tmp"
        };

        for (String directory : directories) {
            if (checkDirectoryExistence(directory)) {
                continue;
            }
            LOGGER.warn("Missing midPoint home directory '{}'. Creating.", new Object[]{directory});
            createDir(directory);
        }
    }
    
    private void setupMidpointHomeDirectory(String midpointHomePath) {
    	try {
			ClassPathUtil.extractFilesFromClassPath("initial-midpoint-home", midpointHomePath, false);
		} catch (URISyntaxException | IOException e) {
			LOGGER.error("Error copying the content of initial-midpoint-home to {}: {}", midpointHomePath, e.getMessage(), e);
		}
    	
    }


    private boolean checkDirectoryExistence(String dir) {
        File d = new File(dir);
        if (d.isFile()) {
            LOGGER.error(dir + " is file and NOT a directory.");
            throw new SystemException(dir + " is file and NOT a directory !!!");
        }

        if (d.isDirectory()) {
            LOGGER.info("Directory " + dir + " already exists. Reusing it.");
            return true;
        } else {
            return false;
        }

    }

    private void createDir(String dir) {
        File d = new File(dir);
        if (d.exists() && d.isDirectory()) {
            return;
        }
        Boolean st = d.mkdirs();
        if (!st) {
            LOGGER.error("Unable to create directory " + dir + " as user " + System.getProperty("user.name"));
            //throw new SystemException("Unable to create directory " + dir + " as user "
            //		+ System.getProperty("user.name"));
        }
    }
}
