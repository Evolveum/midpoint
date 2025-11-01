/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.state;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

public class Initializable {

    private boolean initialized;

    /**
     * Initializes the object.
     *
     * @param reset true if we expect the object is already initialized and we want to reset its state
     * @param initializer code that executes the actual initialization
     */
    protected void doInitialize(boolean reset, Runnable initializer) {
        if (reset) {
            stateCheck(initialized, "Not initialized yet");
        } else {
            stateCheck(!initialized, "Already initialized");
        }
        initializer.run();
        initialized = true;
    }

    protected void assertInitialized() {
        stateCheck(initialized, "Not initialized");
    }

    protected void clear(@NotNull Containerable c) {
        c.asPrismContainerValue().clear();
    }
}
