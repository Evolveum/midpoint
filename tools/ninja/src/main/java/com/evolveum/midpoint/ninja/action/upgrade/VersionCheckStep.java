/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

public class VersionCheckStep implements UpgradeStep<VersionCheckResult> {

    @Override
    public String getIdentifier() {
        return "versionCheck";
    }

    @Override
    public VersionCheckResult execute() throws Exception {
        String version = "";
        // todo implement midPoint version check
        return new VersionCheckResult()
                .currentVersion(version);
    }
}
