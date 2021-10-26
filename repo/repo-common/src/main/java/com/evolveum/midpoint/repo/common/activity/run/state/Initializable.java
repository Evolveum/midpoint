/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class Initializable {

    private boolean initialized;

    protected void doInitialize(Runnable initializer) {
        stateCheck(!initialized, "Already initialized");
        initializer.run();
        initialized = true;
    }

    protected void assertInitialized() {
        stateCheck(initialized, "Not initialized");
    }
}
