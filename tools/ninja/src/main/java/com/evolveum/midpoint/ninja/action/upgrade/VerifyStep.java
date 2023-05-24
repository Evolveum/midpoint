/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

public class VerifyStep implements UpgradeStep<StepResult> {

    @Override
    public String getIdentifier() {
        return "verify";
    }

    @Override
    public StepResult execute() throws Exception {
        // todo implement

        return new StepResult() {};
    }
}
