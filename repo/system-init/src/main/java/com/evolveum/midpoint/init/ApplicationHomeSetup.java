/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.MIDPOINT_HOME_PROPERTY;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

class ApplicationHomeSetup {

    private static final Trace LOGGER = TraceManager.getTrace(ApplicationHomeSetup.class);

    private final boolean silent;
    private final Path midPointHomePath;

    ApplicationHomeSetup(boolean silent, String midPointHomePath) {
        this.silent = silent;
        this.midPointHomePath = Paths.get(midPointHomePath);
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
     * Creates directory structure under root.
     *
     * Directory information based on: https://docs.evolveum.com/midpoint/reference/deployment/midpoint-home-directory/
     */
    private void createMidpointHomeDirectories() {
        if (!checkDirectoryExistence(midPointHomePath)) {
            createDir(midPointHomePath);
        }

        Path[] directories = {
                midPointHomePath.resolve("icf-connectors"),
                midPointHomePath.resolve("connid-connectors"),
                midPointHomePath.resolve("idm-legacy"),
                midPointHomePath.resolve("log"),
                midPointHomePath.resolve("schema"),
                midPointHomePath.resolve("import"),
                midPointHomePath.resolve("export"),
                midPointHomePath.resolve("tmp"),
                midPointHomePath.resolve("lib"),
                midPointHomePath.resolve("trace")
        };

        for (Path directory : directories) {
            if (checkDirectoryExistence(directory)) {
                continue;
            }
            LOGGER.info("Missing midPoint home directory '{}'. Creating.", directory);
            createDir(directory);
        }
    }

    private void setupMidpointHomeDirectory() {
        try {
            // TODO: only usage is here, why is it in ClassPathUtils? Can we change 2nd arg to Path?
            ClassPathUtil.extractFilesFromClassPath("initial-midpoint-home", midPointHomePath.toString(), false);
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Error copying the content of initial-midpoint-home to {}: {}", midPointHomePath, e.getMessage(), e);
        }
    }

    private boolean checkDirectoryExistence(Path dir) {
        File d = dir.toFile();
        if (d.isFile()) {
            LOGGER.error(dir + " is file and NOT a directory.");
            throw new SystemException(dir + " is file and NOT a directory !!!");
        }

        if (d.isDirectory()) {
            LOGGER.debug("Directory " + dir + " already exists. Reusing it.");
            return true;
        } else {
            return false;
        }
    }

    private void createDir(Path dir) {
        File d = dir.toFile();
        if (!d.exists() || !d.isDirectory()) {
            boolean created = d.mkdirs();
            if (!created) {
                LOGGER.error("Unable to create directory " + dir + " as user " + System.getProperty("user.name"));
            }
        }
    }
}
