package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.*;

public class UpgradeDistributionAction extends UpgradeBaseAction<UpgradeDistributionOptions, ActionResult<Void>> {

    public static final File SCRIPT_48_AND_LATER = new File("./doc/config/sql/native");

    public enum Scripts48AndLater {

        /**
         * This will create raw datasource from JDBC url/username/password. Midpoint home doesn't have to be defined.
         */
        RAW(RunSqlOptions.Mode.RAW, Collections.emptyList(), Collections.emptyList()),

        /**
         * This mode will set up datasource based on midpoint home config.xml pointing to midpoint repository.
         */
        REPOSITORY(
                RunSqlOptions.Mode.REPOSITORY,
                List.of(new File(SCRIPT_48_AND_LATER, "postgres.sql"),
                        new File(SCRIPT_48_AND_LATER, "postgres-quartz.sql")),
                List.of(new File(SCRIPT_48_AND_LATER, "postgres-upgrade.sql"))
        ),

        /**
         * This mode will set up datasource based on midpoint home config.xml pointing to midpoint audit database.
         */
        AUDIT(
                RunSqlOptions.Mode.AUDIT,
                List.of(new File(SCRIPT_48_AND_LATER, "postgres-audit.sql")),
                List.of(new File(SCRIPT_48_AND_LATER, "postgres-audit-upgrade.sql"))
        );

        public final RunSqlOptions.Mode mode;

        public final List<File> createScripts;

        public final List<File> updateScripts;

        Scripts48AndLater(RunSqlOptions.Mode mode, List<File> createScripts, List<File> updateScripts) {
            this.mode = mode;
            this.createScripts = createScripts;
            this.updateScripts = updateScripts;
        }

        public static Scripts48AndLater getScriptByMode(@NotNull RunSqlOptions.Mode mode) {
            for (Scripts48AndLater script : Scripts48AndLater.values()) {
                if (script.mode == mode) {
                    return script;
                }
            }

            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    @Override
    public String getOperationName() {
        return "upgrade distribution";
    }

    @Override
    public ActionResult<Void> execute() throws Exception {
        File tempDirectory = createTmpDirectory(options.getTempDirectory());

        // pre-upgrade checks
        if (!options.isSkipPreCheck()) {
            PreUpgradeCheckOptions preUpgradeCheckOptions = new PreUpgradeCheckOptions();

            ActionResult<Boolean> shouldContinue = executeAction(new PreUpgradeCheckAction(), preUpgradeCheckOptions);
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

            VerifyResult verifyResult = executeAction(new VerifyAction(true), verifyOptions);
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

        DownloadDistributionResult downloadResult = executeAction(new DownloadDistributionAction(), downloadOpts);

        File distributionDirectory = downloadResult.getDistributionDirectory();

        // upgrade repository
        log.info("Starting repository database structure upgrade");
        runUpgradeSql(RunSqlOptions.Mode.REPOSITORY, distributionDirectory);

        // upgrade audit
        log.info("Starting audit database structure upgrade");
        runUpgradeSql(RunSqlOptions.Mode.AUDIT, distributionDirectory);

        // upgrade installation
        UpgradeInstallationOptions installationOpts = new UpgradeInstallationOptions();
        installationOpts.setDistributionDirectory(downloadResult.getDistributionDirectory());
        installationOpts.setBackup(options.isBackupMidpointDirectory());
        installationOpts.setInstallationDirectory(options.getInstallationDirectory());

        executeAction(new UpgradeInstallationAction(), installationOpts);

        return null;
    }

    private void runUpgradeSql(RunSqlOptions.Mode mode, File distributionDirectory) throws Exception {
        RunSqlOptions runSqlOptions = new RunSqlOptions();
        runSqlOptions.setUpgrade(true);
        runSqlOptions.setMode(mode);
        runSqlOptions.setScripts(
                Scripts48AndLater.getScriptByMode(mode).updateScripts.stream()
                        .map(f -> new File(distributionDirectory, f.getPath()))
                        .collect(Collectors.toList()));

        executeAction(new RunSqlAction(), runSqlOptions);
    }
}
