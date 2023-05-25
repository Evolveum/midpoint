/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.File;

public class UpgradeMidpointHomeOptions {

    private File distributionFile;

    private File midpointHomeDirectory;

    private File distributionDirectory;

    private boolean backupFiles;

    public File getMidpointHomeDirectory() {
        return midpointHomeDirectory;
    }

    public void setMidpointHomeDirectory(File midpointHomeDirectory) {
        this.midpointHomeDirectory = midpointHomeDirectory;
    }

    public File getDistributionDirectory() {
        return distributionDirectory;
    }

    public void setDistributionDirectory(File distributionDirectory) {
        this.distributionDirectory = distributionDirectory;
    }

    public boolean isBackupFiles() {
        return backupFiles;
    }

    public void setBackupFiles(boolean backupFiles) {
        this.backupFiles = backupFiles;
    }

    public File getDistributionFile() {
        return distributionFile;
    }

    public void setDistributionFile(File distributionFile) {
        this.distributionFile = distributionFile;
    }
}
