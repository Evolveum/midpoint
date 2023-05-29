/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.VerifyRepositoryAction;
import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import com.evolveum.midpoint.ninja.opts.VerifyOptions;

public class VerifyStep implements UpgradeStep<StepResult> {

    private UpgradeStepsContext context;

    public VerifyStep(UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "verify";
    }

    @Override
    public StepResult execute() throws Exception {
        // todo implement


        VerifyOptions options = new VerifyOptions();

        VerifyRepositoryAction action = new VerifyRepositoryAction();
        action.init(context.getContext(), options);
        action.execute();

        return new StepResult() {};
    }
}
