/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

/**
 * Describes the execution mode this task runs in. For example, if it is a "full execution" or a preview/simulation.
 * Or, if we should work with the production or development configuration.
 *
 * TEMPORARY IMPLEMENTATION
 */
public class TaskExecutionMode {

    public static final TaskExecutionMode PRODUCTION =
            new TaskExecutionMode("PRODUCTION", true, true);
    public static final TaskExecutionMode SIMULATED_PRODUCTION =
            new TaskExecutionMode("SIMULATED_PRODUCTION", false, true);
    public static final TaskExecutionMode SIMULATED_DEVELOPMENT =
            new TaskExecutionMode("SIMULATED_DEVELOPMENT", false, false);

    private final String name;

    /** Should the effects of the task be persistent? The value is `false` for preview/simulation. */
    private final boolean persistent;

    /** Should the production configuration be used? */
    private final boolean productionConfiguration;

    private TaskExecutionMode(String name, boolean persistent, boolean productionConfiguration) {
        this.name = name;
        this.persistent = persistent;
        this.productionConfiguration = productionConfiguration;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isProductionConfiguration() {
        return productionConfiguration;
    }

    @Override
    public String toString() {
        return name; // temporary
    }
}
