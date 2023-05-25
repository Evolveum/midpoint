/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;

public class VersionCheckStep implements UpgradeStep<VersionCheckResult> {

    public static final String SUPPORTED_VERSION_LTS = "4.4.4";

    public static final String SUPPORTED_VERSION_FEATURE = "4.7.1";

    public static final String[] SUPPORTED_VERSIONS = { SUPPORTED_VERSION_LTS, SUPPORTED_VERSION_FEATURE };

    @Override
    public String getIdentifier() {
        return "versionCheck";
    }

    @Override
    public VersionCheckResult execute() throws Exception {
        String version = "";
        // todo implement midPoint version check, also implement midpoint version support in midpoint sqale db
        return new VersionCheckResult()
                .currentVersion(version);
    }
}
