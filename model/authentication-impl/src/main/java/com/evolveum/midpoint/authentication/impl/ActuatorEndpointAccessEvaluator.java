/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import java.util.Set;
import java.util.TreeSet;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.impl.util.ActuatorEndpointPath;
import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Decides whether the actuator endpoints are enabled (bug:12960).
 *
 * Each endpoint has its own startup option {@code midpoint.actuator.<endpoint>.enabled}, see
 * {@link ActuatorConfigurationSection}: {@code health} is enabled by default, the other endpoints are disabled.
 * The discovery page ({@code /actuator}) is enabled if at least one endpoint that is not enabled by default
 * (i.e. other than {@code health}) is enabled. todo confirm this
 *
 * The startup options are read once, at startup.
 */
@Component
public class ActuatorEndpointAccessEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ActuatorEndpointAccessEvaluator.class);

    @Autowired private MidpointConfiguration midpointConfiguration;

    private ActuatorConfigurationSection actuatorConfigurationSection;

    @PostConstruct
    public void init() {
        actuatorConfigurationSection = midpointConfiguration.getActuatorConfigurationSection();
        LOGGER.info("Enabled actuator endpoints: {} (startup options midpoint.actuator.<endpoint>.enabled).",
                new TreeSet<>(actuatorConfigurationSection.enabledEndpoints()));
    }

    /**
     * Returns true if the actuator endpoint is enabled.
     */
    public boolean isEnabled(@NotNull String endpoint) {
        return isEnabled(actuatorConfigurationSection, endpoint);
    }

    static boolean isEnabled(@NotNull ActuatorConfigurationSection section, @NotNull String endpoint) {
        Set<String> enabledEndpoints = section.enabledEndpoints();
        if (ActuatorEndpointPath.DISCOVERY.equals(endpoint)) {
            //check if discovery endpoint should be enabled at all (only in case if any of the non-default-enabled endpoints
            // is enabled)
            //todo confirm this
            return enabledEndpoints.stream().anyMatch(e -> !ActuatorConfigurationSection.isEnabledByDefault(e));
        }
        return enabledEndpoints.contains(endpoint);
    }
}
