package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;

public class UpgradeInstallationOptions {

    private File distributionDirectory;

    private boolean backup;

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
