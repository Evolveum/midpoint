/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Describes the execution mode this task runs in. For example, if it is a "full execution" or a preview/simulation.
 * Or, if we should work with the production or development configuration.
 *
 * TEMPORARY IMPLEMENTATION
 */
public class TaskExecutionMode implements Serializable {

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

    /** Should the effects of this task be persistent or not? The latter means "simulation", "preview", etc. */
    public boolean isPersistent() {
        return persistent;
    }

    public boolean isSimulation() {
        return !persistent;
    }

    /**
     * What configuration should the actions take into account? Production or "development" one?
     *
     * - Production usually means `active` and `deprecated` lifecycle states.
     * - Development usually means `active` and `proposed` states.
     *
     * However, in the future we may provide more customization options here (e.g. explicit enumeration of lifecycle states
     * to use, or even a set of specific deltas to apply).
     *
     * If {@link #persistent} is `true` then {@link #productionConfiguration} should be `true` as well.
     *
     * See https://docs.evolveum.com/midpoint/devel/design/simulations/ for more information.
     */
    public boolean isProductionConfiguration() {
        return productionConfiguration;
    }

    @Override
    public String toString() {
        return name; // temporary
    }

    public @NotNull ConfigurationSpecificationType toConfigurationSpecification() {
        return new ConfigurationSpecificationType()
                .productionConfiguration(productionConfiguration);
    }
}
