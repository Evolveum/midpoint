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
        DownloadDistributionResult downloadResult = downloadAction.execute();

        // todo next actions should be executed from downloaded ninja (as not to replace ninja.jar that's currently running), or maybe not?
//        log.info("Starting ninja");
//        new ProcessBuilder(
//                "../../_mess/mid8842/.upgrade-process/1685390031006-midpoint-latest-dist/bin/ninja.sh -v --offline -h".split(" ")
//        ).inheritIO().start();
//        context.out.println("Finished main");

        // upgrade installation
        UpgradeInstallationOptions installationOpts = new UpgradeInstallationOptions();
        installationOpts.setDistributionDirectory(downloadResult.getDistributionDirectory());
        installationOpts.setBackup(options.isBackupMidpointDirectory());
        installationOpts.setInstallationDirectory(options.getInstallationDirectory());

        UpgradeInstallationAction installationAction = new UpgradeInstallationAction();
        installationAction.init(context, installationOpts);
        installationAction.execute();

        File installationDirectory = NinjaUtils.computeInstallationDirectory(options.getInstallationDirectory(), context);

        // upgrade repository
        RunSqlOptions upgradeRepositoryOpts = new RunSqlOptions();
        upgradeRepositoryOpts.setUpgrade(true);
        upgradeRepositoryOpts.setMode(RunSqlOptions.Mode.REPOSITORY);
        upgradeRepositoryOpts.setScripts(RunSqlOptions.Mode.REPOSITORY.updateScripts.stream()
                .map(f -> new File(installationDirectory, f.getPath()))
                .collect(Collectors.toList()));

        RunSqlAction upgradeRepositoryAction = new RunSqlAction();
        upgradeRepositoryAction.init(context, upgradeRepositoryOpts);
        upgradeRepositoryAction.execute();

        RunSqlOptions upgradeAuditOpts = new RunSqlOptions();
        upgradeAuditOpts.setUpgrade(true);
        upgradeAuditOpts.setMode(RunSqlOptions.Mode.AUDIT);
        upgradeRepositoryOpts.setScripts(RunSqlOptions.Mode.AUDIT.updateScripts.stream()
                .map(f -> new File(installationDirectory, f.getPath()))
                .collect(Collectors.toList()));

        RunSqlAction upgradeAuditAction = new RunSqlAction();
        upgradeAuditAction.init(context, upgradeAuditOpts);
        upgradeAuditAction.execute();

        return null;
    }
}
