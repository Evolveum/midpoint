package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

import java.io.File;

public class VerifyResult implements StepResult {

    private File output;

    public File getOutput() {
        return output;
    }
}
