/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.File;

import com.evolveum.midpoint.ninja.action.VerifyRepositoryAction;
import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeConstants;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import com.evolveum.midpoint.ninja.opts.VerifyOptions;

public class VerifyStep implements UpgradeStep<StepResult> {

    private final UpgradeStepsContext context;

    public VerifyStep(UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "verify";
    }

    @Override
    public StepResult execute() throws Exception {
        final VerifyOptions options = new VerifyOptions();
        options.setCreateReport(true);
        options.setOverwrite(true);

        int threads = context.getOptions().getVerifyThreads();
        options.setMultiThread(threads);

        final File tempDirectory = context.getTempDirectory();
        File output = new File(tempDirectory, UpgradeConstants.VERIFY_OUTPUT_FILE);
        options.setOutput(output);

        VerifyRepositoryAction action = new VerifyRepositoryAction();
        action.init(context.getContext(), options);
        action.execute();

        // todo progress reporting
        // todo ask after this finishes, whether to exit or continue to next step

        return new StepResult() {
        };
    }
}
