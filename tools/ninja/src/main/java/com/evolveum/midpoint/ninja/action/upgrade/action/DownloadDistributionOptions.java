package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;

import com.beust.jcommander.Parameter;

import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeCommonOptions;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "downloadDistribution")
public class DownloadDistributionOptions extends UpgradeCommonOptions {

    public static final String P_DISTRIBUTION_VERSION = "--distribution-version";
    public static final String P_DISTRIBUTION_ARCHIVE = "--distribution-archive";
    public static final String P_DISTRIBUTION_DIRECTORY = "--distribution-directory";

    public static final String P_IGNORE_SSL_ERRORS = "--ignore-ssl-errors";

    @Parameter(names = { P_DISTRIBUTION_ARCHIVE }, descriptionKey = "upgradeDistribution.distributionArchive")
    private File distributionArchive;

    @Parameter(names = { P_DISTRIBUTION_DIRECTORY }, descriptionKey = "upgradeDistribution.distributionDirectory")
    private File distributionDirectory;

    @Parameter(names = { P_DISTRIBUTION_VERSION }, descriptionKey = "upgradeDistribution.distributionVersion")
    private String distributionVersion = UpgradeConstants.SUPPORTED_VERSION_TARGET;

    @Parameter(names = { P_IGNORE_SSL_ERRORS }, descriptionKey = "upgradeDistribution.ignoreSslErrors")
    private boolean ignoreSslErrors;

    public File getDistributionArchive() {
        return distributionArchive;
    }

    public void setDistributionArchive(File distributionArchive) {
        this.distributionArchive = distributionArchive;
    }

    public String getDistributionVersion() {
        return distributionVersion;
    }

    public void setDistributionVersion(String distributionVersion) {
        this.distributionVersion = distributionVersion;
    }

    public File getDistributionDirectory() {
        return distributionDirectory;
    }

    public void setDistributionDirectory(File distributionDirectory) {
        this.distributionDirectory = distributionDirectory;
    }

    public boolean isIgnoreSslErrors() {
        return ignoreSslErrors;
    }

    public void setIgnoreSslErrors(boolean ignoreSslErrors) {
        this.ignoreSslErrors = ignoreSslErrors;
    }
}
