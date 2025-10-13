/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.simulation;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

public interface ExecutionModeProvider {

    /** Returns the execution mode of this task. */
    @NotNull TaskExecutionMode getExecutionMode();

    default boolean isExecutionFullyPersistent() {
        return getExecutionMode().isFullyPersistent();
    }

    default boolean areShadowChangesSimulated() {
        return getExecutionMode().areShadowChangesSimulated();
    }

    default boolean isPersistentAtShadowLevelButNotFully() {
        return getExecutionMode().isPersistentAtShadowLevelButNotFully();
    }

    default boolean isProductionConfiguration() {
        return getExecutionMode().isProductionConfiguration();
    }

    /** Just a convenience method. */
    default boolean canSee(AbstractMappingType mapping) {
        return SimulationUtil.isVisible(mapping, getExecutionMode());
    }

    /** Just a convenience method. */
    default boolean canSee(ObjectType object) {
        return SimulationUtil.isVisible(object, getExecutionMode());
    }

    /** Just a convenience method. */
    default boolean canSee(String lifecycleState) {
        return SimulationUtil.isVisible(lifecycleState, getExecutionMode());
    }

}
