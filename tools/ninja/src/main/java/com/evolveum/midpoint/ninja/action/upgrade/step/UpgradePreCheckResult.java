/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;

public class UpgradePreCheckResult implements StepResult {

    private String currentVersion;

    public String currentVersion() {
        return currentVersion;
    }

    public UpgradePreCheckResult currentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
        return this;
    }
}
