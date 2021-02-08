/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * TODO
 *
 * TODO include SynchronizationOperationResult?
 *
 */
@Experimental
public class SynchronizationResult {

    private final int changesProcessed;

    public SynchronizationResult() {
        this(0);
    }

    public SynchronizationResult(int changesProcessed) {
        this.changesProcessed = changesProcessed;
    }
}
