package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public abstract class UpgradeObjectsStep implements UpgradeStep<StepResult> {

    private final UpgradeStepsContext context;

    public UpgradeObjectsStep(@NotNull UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public StepResult execute() throws Exception {
        final VerifyResult verifyResult = context.getResult(VerifyResult.class);

        final File output = verifyResult.getOutput();

        // todo load CSV, only OIDs + state (whether to update)
        // go through all oids that need to be updated
        // if csv not available go through all

        return null;
    }
}
