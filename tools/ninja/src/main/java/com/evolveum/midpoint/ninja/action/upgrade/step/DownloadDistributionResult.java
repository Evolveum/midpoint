/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

import java.io.File;

public class DownloadDistributionResult implements StepResult {

    private File distributionZipFile;

    private File distributionDirectory;

    public DownloadDistributionResult(File distributionZipFile, File distributionDirectory) {
        this.distributionZipFile = distributionZipFile;
        this.distributionDirectory = distributionDirectory;
    }

    public File getDistributionZipFile() {
        return distributionZipFile;
    }

    public File getDistributionDirectory() {
        return distributionDirectory;
    }
}
