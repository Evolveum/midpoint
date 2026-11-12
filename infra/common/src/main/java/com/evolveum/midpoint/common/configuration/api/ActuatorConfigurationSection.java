/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

/**
 * Provides typed access to the "actuator" section of the {@code config.xml} file.
 *
 * Each actuator endpoint has its own startup option {@code midpoint.actuator.<endpoint>.enabled}
 * (e.g. {@code midpoint.actuator.metrics.enabled}).
 * The {@code health} endpoint is enabled by default, all other endpoints are disabled by default.
 */
@NullMarked
public interface ActuatorConfigurationSection {

    /** Identifiers of the enabled actuator endpoints. */
    Set<String> enabledEndpoints();

    /** Is the endpoint enabled when its startup option is not set? Only {@code health} is. */
    static boolean isEnabledByDefault(String endpoint) {
        return "health".equals(endpoint);
    }
}
