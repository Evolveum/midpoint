package com.evolveum.midpoint.ninja.action.upgrade;

import java.io.File;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.RunSqlAction;
import com.evolveum.midpoint.ninja.action.RunSqlOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions, Void> {

    @Override
    public String getOperationName() {
        return "upgrade distribution";
    }

    @Override
    public Void execute() throws Exception {
        File tempDirectory = options.getTempDirectory() != null ?
                options.getTempDirectory() : new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);

        FileUtils.forceMkdir(tempDirectory);

        // download distribution
        DownloadDistributionOptions downloadOpts = new DownloadDistributionOptions();
        downloadOpts.setTempDirectory(tempDirectory);
        downloadOpts.setDistributionArchive(options.getDistributionArchive());

        DownloadDistributionAction downloadAction = new DownloadDistributionAction();
        downloadAction.init(context, downloadOpts);

        DownloadDistributionResult downloadResult = executeAction(downloadAction);

        File distributionDirectory = downloadResult.getDistributionDirectory();

        // upgrade repository
        runUpgradeSql(RunSqlOptions.Mode.REPOSITORY, distributionDirectory);

        // upgrade audit
        runUpgradeSql(RunSqlOptions.Mode.AUDIT, distributionDirectory);

        // upgrade installation
        UpgradeInstallationOptions installationOpts = new UpgradeInstallationOptions();
        installationOpts.setDistributionDirectory(downloadResult.getDistributionDirectory());
        installationOpts.setBackup(options.isBackupMidpointDirectory());
        installationOpts.setInstallationDirectory(options.getInstallationDirectory());

        UpgradeInstallationAction installationAction = new UpgradeInstallationAction();
        installationAction.init(context, installationOpts);
        executeAction(installationAction);

        return null;
    }

    private <O, T> T executeAction(Action<O, T> action) throws Exception {
        log.info(NinjaUtils.formatActionStartMessage(action));

        return action.execute();
    }

    private void runUpgradeSql(RunSqlOptions.Mode mode, File distributionDirectory) throws Exception {
        RunSqlOptions runSqlOptions = new RunSqlOptions();
        runSqlOptions.setUpgrade(true);
        runSqlOptions.setMode(mode);
        runSqlOptions.setScripts(mode.updateScripts.stream()
                .map(f -> new File(distributionDirectory, f.getPath()))
                .collect(Collectors.toList()));

        RunSqlAction action = new RunSqlAction();
        action.init(context, runSqlOptions);

        executeAction(action);
    }
}
