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

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.File;

public class ApplicationHomeSetup {

    private static final transient Trace LOGGER = TraceManager.getTrace(ApplicationHomeSetup.class);
    private String MIDPOINT_HOME;

    public void init(String midpointHome) {

        MIDPOINT_HOME = midpointHome;

        LOGGER.info(MIDPOINT_HOME + " = " + System.getProperty(MIDPOINT_HOME));
        System.out.println(MIDPOINT_HOME + " = " + System.getProperty(MIDPOINT_HOME));

        String mh = System.getProperty(MIDPOINT_HOME);

        if (!checkDirectoryExistence(mh)) {
            createDir(mh);
        }

        directorySetup(mh);

    }

    /**
     * Creates directory structure under root
     * <p/>
     * Directory information based on: http://wiki.evolveum.com/display/midPoint/midpoint.home+-+directory+structure
     *
     * @param midpointHomeDir
     */

    protected void directorySetup(String midpointHomeDir) {
        String[] directories = {
                midpointHomeDir + "/icf-connectors",
                midpointHomeDir + "/idm-legacy",
                midpointHomeDir + "/log",
                midpointHomeDir + "/schema",
                midpointHomeDir + "/import",
                midpointHomeDir + "/export"
        };

        for (String directory : directories) {
            if (checkDirectoryExistence(directory)) {
                continue;
            }
            LOGGER.warn("Missing directory '{}'. Regeneration in progress...", new Object[]{directory});
            createDir(directory);
        }
    }

    /**
     * Checking directory existence
     *
     * @param dir
     * @return
     */
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

    /**
     * Creates directory
     *
     * @param dir
     */
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
