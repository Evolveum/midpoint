package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeDistribution")
public class UpgradeDistributionOptions {

    public static final String P_TEMP_DIR_LONG = "--temp-directory";

    public static final String P_DISTRIBUTION_ARCHIVE = "--distribution-archive";

    public static final String P_BACKUP_MIDPOINT_DIRECTORY = "--backup-midpoint-directory";

    public static final String P_INSTALLATION_DIRECTORY = "--installation-directory";

    @Parameter(names = { P_TEMP_DIR_LONG }, descriptionKey = "upgradeDistribution.tempDir")
    private File tempDirectory;

    @Parameter(names = { P_DISTRIBUTION_ARCHIVE }, descriptionKey = "upgradeDistribution.distributionArchive")
    private File distributionArchive;

    @Parameter(names = { P_BACKUP_MIDPOINT_DIRECTORY }, descriptionKey = "upgradeDistribution.backupMidpointDirectory")
    private boolean backupMidpointDirectory;

    @Parameter(names = { P_INSTALLATION_DIRECTORY }, descriptionKey = "upgradeDistribution.installationDirectory")
    private File installationDirectory;

    public File getTempDirectory() {
        return tempDirectory;
    }

    public File getDistributionArchive() {
        return distributionArchive;
    }

    public boolean isBackupMidpointDirectory() {
        return backupMidpointDirectory;
    }

    public File getInstallationDirectory() {
        return installationDirectory;
    }

}
