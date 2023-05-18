/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class UpgradeMidpointHomeStep implements UpgradeStep<Void> {

    private static final String VAR_DIRECTORY = "var";

    private final UpgradeMidpointHomeStepOptions options;

    public UpgradeMidpointHomeStep(UpgradeMidpointHomeStepOptions options) {
        this.options = options;
    }

    @Override
    public String getIdentifier() {
        return "upgradeMidpointHome";
    }

    public Void execute() throws Exception {
        downloadDistributionFile();

        upgradeMidpointHome();

        return null;
    }

    private void downloadDistributionFile() throws IOException {
        DistributionManager manager = new DistributionManager();
        String version = "";
        ProgressListener listener = null;

        File distributionZip = manager.downloadDistribution(version, listener);
    }

    private void upgradeMidpointHome() throws IOException {
        File midpointHomeDirectory = options.getMidpointHomeDirectory();
        File distributionDirectory = options.getDistributionDirectory();

        File backupDirectory = null;
        if (options.isBackupFiles()) {
            backupDirectory = new File(midpointHomeDirectory, ".backup-" + System.currentTimeMillis());
            backupDirectory.mkdir();
        }

        for (File file : distributionDirectory.listFiles()) {
            String fileName = file.getName();

            if (options.isBackupFiles()) {
                File newFile = new File(midpointHomeDirectory, fileName);

                if (!VAR_DIRECTORY.equals(fileName)) {
                    if (newFile.exists()) {
                        FileUtils.moveToDirectory(newFile, backupDirectory, false);
                    }
                } else {
                    // don't back up var directory, upgrade shouldn't touch it, back up only content if needed
                    FileUtils.forceMkdir(new File(backupDirectory, fileName));
                }
            }

            if (VAR_DIRECTORY.equals(fileName)) {
                copyFiles(file, new File(midpointHomeDirectory, fileName), new File(backupDirectory, fileName), options.isBackupFiles());
            } else {
                FileUtils.moveToDirectory(file, midpointHomeDirectory, false);
            }
        }
    }

    private void copyFiles(File srcDir, File dstDir, File backupDir, boolean backup) throws IOException {
        File[] files = srcDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();

            if (backup) {
                File newFile = new File(dstDir, fileName);
                if (newFile.exists()) {
                    FileUtils.moveToDirectory(newFile, backupDir, false);
                }
            }

            FileUtils.moveToDirectory(file, dstDir, false);
        }
    }
}
