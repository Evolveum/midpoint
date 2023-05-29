package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

public class UpgradeObjectsBeforeShutdownStep extends UpgradeObjectsStep {

    @Override
    public String getIdentifier() {
        return "upgradeObjectsBeforeShutdown";
    }

    @Override
    public String getPresentableName() {
        return "upgrade objects before shutdown";
    }

    @Override
    public StepResult execute() throws Exception {
        return new StepResult() {
        };
    }
}
