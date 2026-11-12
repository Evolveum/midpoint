/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;

/**
 * Provides typed access to the "actuator" section of the config.xml file.
 *
 * The endpoints are the ones exposed in {@code application.yml} ({@code management.endpoints.web.exposure.include}).
 * If an endpoint is exposed there, it must be listed here as well, otherwise it stays disabled.
 */
@NullMarked
record ActuatorConfigurationSectionImpl(Set<String> enabledEndpoints) implements ActuatorConfigurationSection {

    /** Known actuator endpoints. */
    private static final Set<String> ENDPOINTS = Set.of("health", "info", "metrics", "env", "threaddump", "heapdump");

    /** Suffix of the per-endpoint keys within the section, e.g. {@code metrics.enabled}. */
    private static final String ENABLED_SUFFIX = ".enabled";

    static ActuatorConfigurationSectionImpl create(Configuration configuration) {
        Set<String> enabled = new HashSet<>();
        for (String endpoint : ENDPOINTS) {
            boolean enabledByDefault = ActuatorConfigurationSection.isEnabledByDefault(endpoint);
            if (configuration.getBoolean(endpoint + ENABLED_SUFFIX, enabledByDefault)) {
                enabled.add(endpoint);
            }
        }
        return new ActuatorConfigurationSectionImpl(Set.copyOf(enabled));
    }
}
