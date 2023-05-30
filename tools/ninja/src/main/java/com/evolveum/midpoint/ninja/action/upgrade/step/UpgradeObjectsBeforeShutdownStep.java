package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;

import org.jetbrains.annotations.NotNull;

public class UpgradeObjectsBeforeShutdownStep extends UpgradeObjectsStep {

    public UpgradeObjectsBeforeShutdownStep(@NotNull UpgradeStepsContext context) {
        super(context);
    }

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
