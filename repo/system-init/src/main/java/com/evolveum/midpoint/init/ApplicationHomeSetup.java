/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
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
