/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.action.ComplexAction;
import com.evolveum.midpoint.ninja.action.VerifyAction;
import com.evolveum.midpoint.ninja.action.VerifyOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class UpgradeAction extends ComplexAction<UpgradeOptions, ActionResult<Void>> {

    @Override
    public String getOperationName() {
        return "upgrade";
    }

    @Override
    public ActionResult<Void> execute() throws Exception {
        log.info("Do you want to run objects verification before upgrade? yes/skip/cancel (y/s/C) [y] ");
        String response = NinjaUtils.readInput(log, input -> StringUtils.isEmpty(input) || input.matches("[ysC]"));

        if (StringUtils.isEmpty(response) || "y".equalsIgnoreCase(response)) {
            VerifyOptions verifyOptions = new VerifyOptions();
            // todo options
            executeAction(new VerifyAction(), verifyOptions);
        }

        if ("s".equalsIgnoreCase(response)) {
            log.warn("Skipping objects verification");
        }

        if ("C".equals(response)) {
            return new ActionResult<>(null, 0, "Upgrade cancelled by user");
        }

//        UpgradeObjectsOptions upgradeObjectsOptions = new UpgradeObjectsOptions();
//        // todo options
//        executeAction(new UpgradeObjectsAction(), upgradeObjectsOptions);
//
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
}
