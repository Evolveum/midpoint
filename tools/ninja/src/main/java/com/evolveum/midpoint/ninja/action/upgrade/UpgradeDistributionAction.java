package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;

import org.apache.commons.io.FileUtils;

import java.io.File;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions, Void> {

    @Override
    public Void execute() throws Exception {
        File tempDirectory = options.getTempDirectory() != null ?
                options.getTempDirectory() : new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);

        FileUtils.forceMkdir(tempDirectory);

        // download distribution
        DownloadDistributionOptions downloadOptions = new DownloadDistributionOptions();
        downloadOptions.setTempDirectory(tempDirectory);
        downloadOptions.setDistributionArchive(options.getDistributionArchive());

        DownloadDistributionAction downloadAction = new DownloadDistributionAction();
        downloadAction.init(context, downloadOptions);
        DownloadDistributionResult downloadResult = downloadAction.execute();

        // upgrade installation
        UpgradeInstallationOptions installationOptions = new UpgradeInstallationOptions();
        installationOptions.setDistributionDirectory(downloadResult.getDistributionDirectory());
        installationOptions.setBackup(options.isBackupMidpointDirectory());
        installationOptions.setInstallationDirectory(options.getInstallationDirectory());

        UpgradeInstallationAction installationAction = new UpgradeInstallationAction();
        installationAction.init(context, installationOptions);
        installationAction.execute();

        // upgrade database
        UpgradeDatabaseOptions databaseOptions = new UpgradeDatabaseOptions();

        UpgradeDatabaseAction databaseAction = new UpgradeDatabaseAction();
        databaseAction.init(context, databaseOptions);
        databaseAction.execute();

        return null;
    }
}
