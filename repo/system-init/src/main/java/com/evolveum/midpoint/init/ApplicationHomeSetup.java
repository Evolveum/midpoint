/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.MIDPOINT_HOME_PROPERTY;

class ApplicationHomeSetup {

    private static final Trace LOGGER = TraceManager.getTrace(ApplicationHomeSetup.class);

    private final boolean silent;
    private final String midPointHomePath;

    ApplicationHomeSetup(boolean silent, String midPointHomePath) {
        this.silent = silent;
        this.midPointHomePath = midPointHomePath;
    }

    void init() {
        String homeMessage = MIDPOINT_HOME_PROPERTY + " = " + midPointHomePath;
        LOGGER.info("{}", homeMessage);
        printToSysout(homeMessage);

        createMidpointHomeDirectories();
        setupMidpointHomeDirectory();
    }

    private void printToSysout(String message) {
        if (!silent) {
            System.out.println(message);
        }
    }

    /**
     * Creates directory structure under root
     * <p/>
     * Directory information based on: http://wiki.evolveum.com/display/midPoint/midpoint.home+-+directory+structure
     */
    private void createMidpointHomeDirectories() {
        if (!checkDirectoryExistence(midPointHomePath)) {
            createDir(midPointHomePath);
        }

        String[] directories = {
                midPointHomePath + "icf-connectors",
                midPointHomePath + "idm-legacy",
                midPointHomePath + "log",
                midPointHomePath + "schema",
                midPointHomePath + "import",
                midPointHomePath + "export",
                midPointHomePath + "tmp",
                midPointHomePath + "lib",
                midPointHomePath + "trace"
        };

        for (String directory : directories) {
            if (checkDirectoryExistence(directory)) {
                continue;
            }
            LOGGER.warn("Missing midPoint home directory '{}'. Creating.", directory);
            createDir(directory);
        }
    }

    private void setupMidpointHomeDirectory() {
        try {
            ClassPathUtil.extractFilesFromClassPath("initial-midpoint-home", midPointHomePath, false);
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Error copying the content of initial-midpoint-home to {}: {}", midPointHomePath, e.getMessage(), e);
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
        if (!d.exists() || !d.isDirectory()) {
            boolean created = d.mkdirs();
            if (!created) {
                LOGGER.error("Unable to create directory " + dir + " as user " + System.getProperty("user.name"));
            }
        }
    }
}
