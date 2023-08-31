/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public abstract class UpgradeBaseAction<O, R> extends Action<O, R> {

    protected File createTmpDirectory(File optsTempDirectory) throws IOException {
        File tempDirectory = optsTempDirectory != null ?
                optsTempDirectory : new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);

        FileUtils.forceMkdir(tempDirectory);

        return tempDirectory;
    }

    protected <O, T> T executeAction(Action<O, T> action, O options) throws Exception {
        action.init(context, options);

        log.info("");
        log.info(ConsoleFormat.formatActionStartMessage(action));
        log.info("");

        return action.execute();
    }
}
