/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import org.jspecify.annotations.NullMarked;

/**
 * Provides typed access to the "actuator" section of the {@code config.xml} file.
 *
 * Each actuator endpoint has its own startup option {@code midpoint.actuator.<endpoint>.enabled}
 * (e.g. {@code midpoint.actuator.metrics.enabled}).
 * The common actuator endpoints enabling/disabling is possible with the value {@value #ALL}
 * ({@code midpoint.actuator.all.enabled}).
 * If neither is set, only the {@code health} endpoint is enabled.
 */
@NullMarked
public interface ActuatorConfigurationSection {

    /** Name of the option that stands for all endpoints. */
    String ALL = "all";

    /**
     * Returns true if the actuator endpoint is enabled.
     * Its own option wins, then the {@value #ALL} option, then the default (see {@link #isEnabledByDefault(String)}).
     */
    boolean isEndpointEnabled(String endpointId);

    /** Returns true if at least one actuator endpoint is enabled. Used for the discovery page ({@code /actuator}). */
    boolean isAnyEndpointEnabled();

    /**
     * Returns true if the endpoint is enabled when neither its own option nor the {@value #ALL} option is set.
     * This holds only for {@code health}.
     */
    static boolean isEnabledByDefault(String endpointId) {
        return "health".equals(endpointId);
    }
}
