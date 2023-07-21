package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "messages", commandDescriptionKey = "upgradeDistribution")
public class UpgradeDistributionOptions {

    public static final String P_TEMP_DIR_LONG = "--temp-directory";

    public static final String P_DISTRIBUTION_ARCHIVE = "--distribution-archive";

    public static final String P_BACKUP_MIDPOINT_DIRECTORY = "--backup-midpoint-directory";

    public static final String P_INSTALLATION_DIRECTORY = "--installation-directory";

    public static final String P_SKIP_VERIFICATION = "--skip-verification";
    public static final String P_VERIFICATION_THREADS = "--verification-threads";
    public static final String P_SKIP_PRE_CHECK = "--skip-pre-check";
    public static final String P_STOP_ON_CRITICAL_ERROR = "--stop-on-critical-error";

    @Parameter(names = { P_TEMP_DIR_LONG }, descriptionKey = "upgradeDistribution.tempDir")
    private File tempDirectory;

    @Parameter(names = { P_DISTRIBUTION_ARCHIVE }, descriptionKey = "upgradeDistribution.distributionArchive")
    private File distributionArchive;

    @Parameter(names = { P_BACKUP_MIDPOINT_DIRECTORY }, descriptionKey = "upgradeDistribution.backupMidpointDirectory")
    private boolean backupMidpointDirectory;

    @Parameter(names = { P_INSTALLATION_DIRECTORY }, descriptionKey = "upgradeDistribution.installationDirectory")
    private File installationDirectory;

    @Parameter(names = { P_SKIP_VERIFICATION }, descriptionKey = "upgradeDistribution.skipVerification")
    private boolean skipVerification;

    @Parameter(names = { P_VERIFICATION_THREADS }, descriptionKey = "upgradeDistribution.verificationThreads")
    private int verificationThreads = 1;

    @Parameter(names = { P_STOP_ON_CRITICAL_ERROR }, descriptionKey = "upgradeDistribution.stopOnCriticalError")
    private boolean stopOnCriticalError = true;

    @Parameter(names = { P_SKIP_PRE_CHECK }, descriptionKey = "upgradeDistribution.skipPreCheck")
    private boolean skipPreCheck;

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

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public void setDistributionArchive(File distributionArchive) {
        this.distributionArchive = distributionArchive;
    }

    public void setBackupMidpointDirectory(boolean backupMidpointDirectory) {
        this.backupMidpointDirectory = backupMidpointDirectory;
    }

    public void setInstallationDirectory(File installationDirectory) {
        this.installationDirectory = installationDirectory;
    }

    public boolean isSkipVerification() {
        return skipVerification;
    }

    public void setSkipVerification(boolean skipVerification) {
        this.skipVerification = skipVerification;
    }

    public int getVerificationThreads() {
        return verificationThreads;
    }

    public void setVerificationThreads(int verificationThreads) {
        this.verificationThreads = verificationThreads;
    }

    public boolean isStopOnCriticalError() {
        return stopOnCriticalError;
    }

    public void setStopOnCriticalError(boolean stopOnCriticalError) {
        this.stopOnCriticalError = stopOnCriticalError;
    }

    public boolean isSkipPreCheck() {
        return skipPreCheck;
    }

    public void setSkipPreCheck(boolean skipPreCheck) {
        this.skipPreCheck = skipPreCheck;
    }
}
