package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.schema.internals.InternalInspector;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions, Void> {

    @Override
    public Void execute() throws Exception {
        DownloadDistributionOptions downloadOptions = new DownloadDistributionOptions();
        // todo options

        DownloadDistributionAction downloadAction = new DownloadDistributionAction();
        downloadAction.init(context, downloadOptions);
        DownloadDistributionResult downloadResult = downloadAction.execute();

        UpgradeInstallationOptions installationOptions = new UpgradeInstallationOptions();
        installationOptions.setDistributionDirectory(downloadResult.getDistributionDirectory());
//        installationOptions.setBackup();
//        installationOptions.setInstallationDirectory();
        // todo options

        UpgradeInstallationAction installationAction = new UpgradeInstallationAction();
        installationAction.init(context, installationOptions);
        installationAction.execute();

        UpgradeDatabaseOptions databaseOptions = new UpgradeDatabaseOptions();
//        databaseOptions.setAuditOnly();
//        databaseOptions.setNoAudit();
//        databaseOptions.setScripts();
//        databaseOptions.setScriptsDirectory();
//        databaseOptions.setAuditScripts();
        // todo options

        UpgradeDatabaseAction databaseAction = new UpgradeDatabaseAction();
        databaseAction.init(context, databaseOptions);
        databaseAction.execute();

        return null;
    }
}
