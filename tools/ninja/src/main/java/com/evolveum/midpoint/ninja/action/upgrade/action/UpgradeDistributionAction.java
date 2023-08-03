package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.fusesource.jansi.Ansi;

import com.evolveum.midpoint.ninja.action.*;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;

public class UpgradeDistributionAction extends Action<UpgradeDistributionOptions, ActionResult<Void>> {

    @Override
    public String getOperationName() {
        return "upgrade distribution";
    }

    @Override
    public ActionResult<Void> execute() throws Exception {
        File tempDirectory = options.getTempDirectory() != null ?
                options.getTempDirectory() : new File(FileUtils.getTempDirectory(), UpgradeConstants.UPGRADE_TEMP_DIRECTORY);

        FileUtils.forceMkdir(tempDirectory);

        // pre-upgrade checks
        if (!options.isSkipPreCheck()) {
            PreUpgradeCheckOptions preUpgradeCheckOptions = new PreUpgradeCheckOptions();

            PreUpgradeCheckAction preUpgradeCheckAction = new PreUpgradeCheckAction();
            preUpgradeCheckAction.init(context, preUpgradeCheckOptions);
            ActionResult<Boolean> shouldContinue = executeAction(preUpgradeCheckAction);
            if (shouldContinue.result()) {
                log.info(Ansi.ansi().fgGreen().a("Pre-upgrade check succeeded.").reset().toString());
            } else {
                log.error(Ansi.ansi().fgRed().a("Pre-upgrade check failed.").reset().toString());
                return new ActionResult<>(null, shouldContinue.exitCode());
            }
        } else {
            log.warn("Pre-upgrade checks skipped.");
        }

        // verification
        if (!options.isSkipVerification()) {
            VerifyOptions verifyOptions = new VerifyOptions();
            verifyOptions.setMultiThread(options.getVerificationThreads());
            verifyOptions.setStopOnCriticalError(options.isStopOnCriticalError());

            VerifyAction verifyAction = new VerifyAction();
            verifyAction.init(context, verifyOptions);
            VerifyResult verifyResult = executeAction(verifyAction);
            if (!verifyResult.hasCriticalItems()) {
                log.info(Ansi.ansi().fgGreen().a("Pre-upgrade verification succeeded.").reset().toString());
            } else {
                log.error(Ansi.ansi().fgRed().a("Pre-upgrade verification failed with {} critical items.").reset().toString(), verifyResult.getCriticalCount());
                log.error("To get rid of critical items, please run 'verify' command, review the results and then run 'upgrade-objects' before upgrading the distribution.\n");
                log.error("Example commands:");
                log.error("To verify all objects and save report to CSV file:");
                log.error("ninja.sh verify --report-style csv --output verify-output.csv");
                log.error("To update all objects");
                log.error("ninja.sh upgrade-objects --verification-file verify-output.csv");

                return null;
            }
        } else {
            log.warn("Verification skipped.");
        }

        // download distribution
        DownloadDistributionOptions downloadOpts = new DownloadDistributionOptions();
        downloadOpts.setTempDirectory(tempDirectory);
        downloadOpts.setDistributionArchive(options.getDistributionArchive());
        downloadOpts.setDistributionVersion(options.getDistributionVersion());

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
        log.info(ConsoleFormat.formatActionStartMessage(action));

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
