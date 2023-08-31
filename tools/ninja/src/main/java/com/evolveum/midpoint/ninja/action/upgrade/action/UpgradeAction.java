/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.action.*;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.ThrowableSupplier;

public class UpgradeAction extends UpgradeBaseAction<UpgradeOptions, ActionResult<Void>> {

    private static final String VERIFY_OUTPUT_FILE = "verify-output.csv";

    @Override
    public String getOperationName() {
        return "upgrade";
    }

    /**
     * Asks for input from user if batch mode is not enabled.
     *
     * @return If batch mode is not enabled, user input is returned. Otherwise, default value is returned.
     */
    private String checkInputOrDefault(ThrowableSupplier<String> inputSupplier, String defaultValue) throws Exception {
        BaseOptions baseOptions = context.getOptions(BaseOptions.class);
        if (baseOptions.isBatchMode()) {
            return defaultValue;
        }

        return inputSupplier.get();
    }

    private ActionResult<Void> createCanceledResult() {
        return new ActionResult<>(null, 0, "Process cancelled by user");
    }

    @Override
    public ActionResult<Void> execute() throws Exception {
        File tempDirectory = createTmpDirectory(null);

        // objects verification
        log.info("Do you want to run objects verification before upgrade? yes/skip/cancel (y/s/C) [y] ");
        String verifyResp = checkInputOrDefault(
                () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[ysC]")),
                "y"
        );

        if ("C".equals(verifyResp)) {
            return createCanceledResult();
        }

        File verificationFile = null;

        if (StringUtils.isEmpty(verifyResp) || "y".equalsIgnoreCase(verifyResp)) {
            verificationFile = new File(tempDirectory, VERIFY_OUTPUT_FILE);

            VerifyOptions verifyOptions = new VerifyOptions();
            verifyOptions.setOutput(verificationFile);
            verifyOptions.setOverwrite(true);
            verifyOptions.setReportStyle(VerifyOptions.ReportStyle.CSV);

            executeAction(new VerifyAction(), verifyOptions);
        }

        // objects upgrade
        if ("s".equalsIgnoreCase(verifyResp)) {
            log.warn("Skipping objects verification");
        }

        log.info("");
        log.info("Do you want to update objects before upgrade? yes/skip/cancel (y/s/C) [y] ");
        String upgradeObjectsResp = checkInputOrDefault(
                () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[ysC]")),
                "y"
        );

        if ("C".equals(upgradeObjectsResp)) {
            return createCanceledResult();
        }

        if ("s".equals(upgradeObjectsResp)) {
            log.warn("Skipping objects upgrade");
        }

        if (StringUtils.isEmpty(upgradeObjectsResp) || "y".equals(upgradeObjectsResp)) {
            if (verificationFile == null) {
                log.info("Do you want to provide a file with verification issues? yes/no (y/n) [n] ");
                String verifyFileResp = checkInputOrDefault(
                        () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[yn]")),
                        "n"
                );

                if ("y".equals(verifyFileResp)) {
                    // todo maybe more interactive validation? check file existence etc...
                    String filePath = NinjaUtils.readInput(log, input -> StringUtils.isNotEmpty(input));
                    verificationFile = new File(filePath);
                }
            } else {
                checkVerificationReviewed();
            }

            if (verificationFile == null) {
                // todo how to report them?
                // * we should report separately if there are CRITICAL issues with phase=AFTER - cause they would be updated
                // after installation is upgraded before midpoint is started (via ninja)
                // * we should report separately if there are CRITICAL issues with type=MANUAL - cause they would not be updated by ninja
                // * we should report separately if there are CRITICAL issues with type=PREVIEW - cause they probably should not be
                log.info(
                        "Verification file was not created, nor provided. "
                                + "Ninja will try to update all objects that contain verification issues."
                                + "Verification issues that were skipped will be reported");
            }

            UpgradeObjectsOptions upgradeObjectsOptions = new UpgradeObjectsOptions();
            upgradeObjectsOptions.setVerification(verificationFile);

//            executeAction(new UpgradeObjectsAction(), upgradeObjectsOptions);
        }

//        PreUpgradeCheckOptions preUpgradeCheckOptions = new PreUpgradeCheckOptions();
//        // todo options
//        executeAction(new PreUpgradeCheckAction(), preUpgradeCheckOptions);
//
//        DownloadDistributionOptions downloadDistributionOptions = new DownloadDistributionOptions();
//        // todo options
//        executeAction(new DownloadDistributionAction(), downloadDistributionOptions);
//
//        // todo run sql 2x
//
//        UpgradeInstallationOptions upgradeInstallationOptions = new UpgradeInstallationOptions();
//        executeAction(new UpgradeInstallationAction(), upgradeInstallationOptions);

        return null;
    }

    private boolean checkVerificationReviewed() throws Exception {
        log.info("Have you reviewed verification issues? yes/no (y/n) [y] ");
        String reviewedVerificationResp = checkInputOrDefault(
                () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[yn]")),
                "y"
        );

        if ("n".equals(reviewedVerificationResp)) {
            return checkVerificationReviewed();
        }

        return true;
    }
}
