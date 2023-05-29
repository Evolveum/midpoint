package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

public class UpgradeObjectsAfterShutdownStep extends UpgradeObjectsStep {

    @Override
    public String getIdentifier() {
        return "upgradeObjectsAfterShutdown";
    }

    @Override
    public String getPresentableName() {
        return "upgrade objects after shutdown";
    }

    @Override
    public StepResult execute() throws Exception {
        return new StepResult() {
        };
    }
}
