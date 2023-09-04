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
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeObjectsItemsSummary;
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

        // verify
        final PartialActionResult<VerifyResult> partialVerifyResult = startAction(
                "Do you want to run verification before upgrade?",
                "Skipping objects verification",
                () -> {
                    File verificationFile = new File(tempDirectory, VERIFY_OUTPUT_FILE);

                    VerifyOptions verifyOptions = new VerifyOptions();
                    verifyOptions.setOutput(verificationFile);
                    verifyOptions.setOverwrite(true);
                    verifyOptions.setReportStyle(VerifyOptions.ReportStyle.CSV);

                    return executeAction(new VerifyAction(true), verifyOptions);
                });

        if (partialVerifyResult.canceled) {
            return createCanceledResult();
        }

        final File verificationFile = partialVerifyResult.result != null ? partialVerifyResult.result.getVerificationFile() : null;

        // objects upgrade
        final PartialActionResult<ActionResult<UpgradeObjectsItemsSummary>> partialUpgradeObjectsResult = startAction(
                "Do you want to update objects before upgrade?",
                "Skipping objects upgrade",
                () -> {
                    File finalVerifiactionFile = verificationFile;

                    if (finalVerifiactionFile == null) {
                        log.info("Do you want to provide a file with verification issues? yes/no (y/n) [n] ");
                        String verifyFileResp = checkInputOrDefault(
                                () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[yn]")),
                                "n"
                        );

                        if ("y".equals(verifyFileResp)) {
                            // todo maybe more interactive validation? check file existence etc...
                            String filePath = NinjaUtils.readInput(log, input -> StringUtils.isNotEmpty(input));
                            finalVerifiactionFile = new File(filePath);
                        }
                    } else {
                        checkVerificationReviewed();
                    }

                    if (finalVerifiactionFile == null) {
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
                    upgradeObjectsOptions.setVerification(finalVerifiactionFile);

                    return executeAction(new UpgradeObjectsAction(true), upgradeObjectsOptions);
                });

        if (partialUpgradeObjectsResult.canceled) {
            return createCanceledResult();
        }

        // pre-upgrade checks
        PartialActionResult<ActionResult<Boolean>> preUpgradeResult = startAction(
                "Do you want to run pre-upgrade checks?",
                "Skipping pre-upgrade checks",
                () -> {

                    PreUpgradeCheckOptions preUpgradeCheckOptions = new PreUpgradeCheckOptions();
                    // todo options
                    return executeAction(new PreUpgradeCheckAction(true), preUpgradeCheckOptions);
                });

        if (preUpgradeResult.canceled) {
            return createCanceledResult();
        }

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

    private <T> PartialActionResult<T> startAction(String confirmMessage, String skipMessage, ThrowableSupplier<T> supplier) throws Exception {
        log.info("");
        log.info(confirmMessage + " yes/skip/cancel (y/s/C) [y] ");
        String response = checkInputOrDefault(
                () -> NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[ysC]")),
                "y"
        );

        if ("C".equals(response)) {
            return new PartialActionResult(true, response);
        }

        if ("s".equals(response)) {
            log.warn(skipMessage);
            return new PartialActionResult<>(false, response);
        }

        return new PartialActionResult<>(false, response, supplier.get());
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

    private static final class PartialActionResult<T> {

        final boolean canceled;

        final String confirmationChoice;

        final T result;

        PartialActionResult(boolean canceled, String confirmationChoice) {
            this(canceled, confirmationChoice, null);
        }

        PartialActionResult(boolean canceled, String confirmationChoice, T result) {
            this.canceled = canceled;
            this.confirmationChoice = confirmationChoice;
            this.result = result;
        }
    }
}
