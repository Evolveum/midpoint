package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.ninja.action.Action;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions> {

    @Override
    public void execute() throws Exception {
        DownloadDistributionOptions downloadOptions = new DownloadDistributionOptions();
        // todo options

        DownloadDistributionAction downloadAction = new DownloadDistributionAction();
        downloadAction.init(context, downloadOptions);
        downloadAction.execute();

        UpgradeInstallationOptions installationOptions = new UpgradeInstallationOptions();
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
    }
}
