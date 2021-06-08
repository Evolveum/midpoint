/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
