/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedConfigurationType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.schema.TaskExecutionMode.TaskPersistenceMode.*;

/**
 * Describes the execution mode this task runs in. For example, if it is a "full execution" or a preview/simulation.
 * Or, if we should work with the production or development configuration.
 *
 * TEMPORARY IMPLEMENTATION
 */
public class TaskExecutionMode implements Serializable {

    public static final TaskExecutionMode PRODUCTION =
            new TaskExecutionMode("PRODUCTION", FULL, true);
    public static final TaskExecutionMode SIMULATED_PRODUCTION =
            new TaskExecutionMode("SIMULATED_PRODUCTION", SHADOWS, true);
    public static final TaskExecutionMode SIMULATED_DEVELOPMENT =
            new TaskExecutionMode("SIMULATED_DEVELOPMENT", SHADOWS, false);
    public static final TaskExecutionMode SIMULATED_SHADOWS_PRODUCTION =
            new TaskExecutionMode("SIMULATED_SHADOWS_PRODUCTION", NONE, true);
    public static final TaskExecutionMode SIMULATED_SHADOWS_DEVELOPMENT =
            new TaskExecutionMode("SIMULATED_SHADOWS_DEVELOPMENT", NONE, false);

    private final String name;

    /** Should the effects of the task be persistent? The value is `false` for preview/simulation. */
    @NotNull private final TaskPersistenceMode persistenceMode;

    /** Should the production configuration be used? */
    private final boolean productionConfiguration;

    private TaskExecutionMode(String name, @NotNull TaskPersistenceMode persistenceMode, boolean productionConfiguration) {
        this.name = name;
        this.persistenceMode = persistenceMode;
        this.productionConfiguration = productionConfiguration;
    }

    /** TODO */
    public boolean isFullyPersistent() {
        return persistenceMode == FULL;
    }

    public boolean isPersistentAtShadowLevelButNotFully() {
        return persistenceMode == SHADOWS;
    }

    public boolean isNothingPersistent() {
        return persistenceMode == NONE;
    }

    public boolean areShadowChangesSimulated() {
        return persistenceMode == NONE;
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
     * If {@link #persistenceMode} is `true` then {@link #productionConfiguration} should be `true` as well.
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
                .predefined(productionConfiguration ?
                        PredefinedConfigurationType.PRODUCTION : PredefinedConfigurationType.DEVELOPMENT);
    }

    enum TaskPersistenceMode {
        /** Every change is persistent. This is the traditional midPoint operation. */
        FULL,

        /**
         * Only changes at the level of shadows are persistent: shadow kind/intent/tag and correlation state.
         * This is OK for regular "model-level" simulations that can be run after shadow classification and correlation
         * configuration is fine-tuned.
         *
         * TODO specify more precisely
         */
        SHADOWS,

        /**
         * Only changes of very basic shadow operational items (like cached attributes) are persistent.
         *
         * TODO specify more precisely
         */
        NONE
    }
}
