package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;

public class DownloadDistributionResult {

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
