/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.schema.util.SimulationUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceItemDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/** Definition of the shadow attribute or association. */
public interface ShadowItemDefinition extends ShadowItemLayeredDefinition, Serializable {

    default boolean isVisible(@NotNull ExecutionModeProvider executionModeProvider) {
        return SimulationUtil.isVisible(getLifecycleState(), executionModeProvider);
    }

    /** @see ResourceItemDefinitionType#getLifecycleState() */
    @Nullable String getLifecycleState();
}
