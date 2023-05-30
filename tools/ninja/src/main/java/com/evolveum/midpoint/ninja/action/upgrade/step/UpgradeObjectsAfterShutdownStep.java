package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;

import org.jetbrains.annotations.NotNull;

public class UpgradeObjectsAfterShutdownStep extends UpgradeObjectsStep {

    public UpgradeObjectsAfterShutdownStep(@NotNull UpgradeStepsContext context) {
        super(context);
    }

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
