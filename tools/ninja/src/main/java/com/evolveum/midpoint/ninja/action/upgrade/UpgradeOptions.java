/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.validators.PositiveInteger;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgrade")
public class UpgradeOptions {

    public static final String P_TEMP_DIR_LONG = "--temp-dir";

    public static final String P_ABORT_LONG = "--abort";

    public static final String P_CONFIRM_STEPS = "--confirm-steps";

    public static final String P_DISTRIBUTION_ARCHIVE = "--distribution-archive";

    public static final String P_BACKUP_MIDPOINT_DIRECTORY = "--backup-midpoint-directory";

    public static final String P_INSTALLATION_DIRECTORY = "--installation-directory";

    public static final String P_VERIFY_THREADS = "--verify-threads";

    public static final String P_UPGRADE_THREADS = "--upgrade-threads";

    @Parameter(names = { P_ABORT_LONG }, descriptionKey = "upgrade.abort")
    private Boolean abort;

    @Parameter(names = { P_TEMP_DIR_LONG }, descriptionKey = "upgrade.tempDir")
    private File tempDirectory;

    @Parameter(names = { P_CONFIRM_STEPS }, descriptionKey = "upgrade.confirmSteps")
    private Boolean confirmSteps;

    @Parameter(names = { P_DISTRIBUTION_ARCHIVE }, descriptionKey = "upgrade.distributionArchive")
    private File distributionArchive;

    @Parameter(names = { P_BACKUP_MIDPOINT_DIRECTORY }, descriptionKey = "upgrade.backupMidpointDirectory")
    private Boolean backupMidpointDirectory;

    @Parameter(names = { P_INSTALLATION_DIRECTORY }, descriptionKey = "upgrade.installationDirectory")
    private File installationDirectory;

    @Parameter(names = { P_VERIFY_THREADS }, descriptionKey = "upgrade.verifyThreads", validateWith = PositiveInteger.class)
    private int verifyThreads = 1;

    @Parameter(names = { P_UPGRADE_THREADS }, descriptionKey = "upgrade.upgradeThreads", validateWith = PositiveInteger.class)
    private int upgradeThreads = 1;

    public Boolean isAbort() {
        return abort;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public Boolean isConfirmSteps() {
        return confirmSteps;
    }

    public File getDistributionArchive() {
        return distributionArchive;
    }

    public Boolean isBackupMidpointDirectory() {
        return backupMidpointDirectory;
    }

    public File getInstallationDirectory() {
        return installationDirectory;
    }

    public int getVerifyThreads() {
        return verifyThreads;
    }

    public int getUpgradeThreads() {
        return upgradeThreads;
    }
}
