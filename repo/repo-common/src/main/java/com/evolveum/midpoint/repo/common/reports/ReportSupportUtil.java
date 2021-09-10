/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.reports;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * General utilities to support report creation.
 *
 * TODO consider a better place for this class
 */
public class ReportSupportUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ReportSupportUtil.class);

    private static final String EXPORT_DIR_NAME = "export";

    public static File getExportDir() {
        return new File(getMidPointHomeDirName(), EXPORT_DIR_NAME);
    }

    public static String getMidPointHomeDirName() {
        return System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }

    public static @NotNull File getOrCreateExportDir() {
        File exportDir = getExportDir();
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            if (!exportDir.mkdir()) {
                LOGGER.error("Couldn't create export dir {}", exportDir);
            }
        }
        return exportDir;
    }
}
