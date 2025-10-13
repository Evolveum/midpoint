/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

@FunctionalInterface
public interface CanRunSupplier {

    /**
     * Returns true if the task or any similar process can run, i.e. was not interrupted.
     *
     * Will return false e.g. if shutdown was signaled.
     */
    boolean canRun();
}
