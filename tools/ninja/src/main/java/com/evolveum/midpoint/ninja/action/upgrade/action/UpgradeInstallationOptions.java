package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeInstallation")
public class UpgradeInstallationOptions {

    public static final String P_DISTRIBUTION_DIRECTORY = "--distribution-directory";

    public static final String P_BACKUP_INSTALLATION_DIRECTORY = "--backup-installation-directory";

    public static final String P_INSTALLATION_DIRECTORY = "--installation-directory";

    @Parameter(names = { P_DISTRIBUTION_DIRECTORY }, descriptionKey = "upgradeInstallation.distributionDirectory")
    private File distributionDirectory;

    @Parameter(names = { P_BACKUP_INSTALLATION_DIRECTORY }, descriptionKey = "upgradeInstallation.backupInstallationDirectory")
    private boolean backup;

    @Parameter(names = { P_INSTALLATION_DIRECTORY }, descriptionKey = "upgradeInstallation.installationDirectory")
    private File installationDirectory;

    public File getDistributionDirectory() {
        return distributionDirectory;
    }

    public void setDistributionDirectory(File distributionDirectory) {
        this.distributionDirectory = distributionDirectory;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }

    public File getInstallationDirectory() {
        return installationDirectory;
    }

    public void setInstallationDirectory(File installationDirectory) {
        this.installationDirectory = installationDirectory;
    }
}
