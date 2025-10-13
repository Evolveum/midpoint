/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
