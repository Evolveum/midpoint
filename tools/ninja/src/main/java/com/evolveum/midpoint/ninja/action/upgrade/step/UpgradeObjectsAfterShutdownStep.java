package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

public class UpgradeObjectsAfterShutdownStep extends UpgradeObjectsStep {

    @Override
    public String getIdentifier() {
        return "upgradeObjectsAfterShutdown";
    }

    @Override
    public StepResult execute() throws Exception {
        return null;
    }
}
