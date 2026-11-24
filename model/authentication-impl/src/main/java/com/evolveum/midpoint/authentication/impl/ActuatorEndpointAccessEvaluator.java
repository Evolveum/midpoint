/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.impl.util.ActuatorEndpointPath;
import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

/**
 * Decides whether a request to an actuator endpoint may pass (bug:12960).
 *
 * Each endpoint has its own startup option {@code midpoint.actuator.<endpoint>.enabled}, and there is the common option
 * {@code midpoint.actuator.all.enabled}, see {@link ActuatorConfigurationSection}.
 * The discovery page ({@code /actuator}) is enabled if at least one endpoint is enabled.
 *
 * This evaluator is used to reject requests to the disabled endpoints
 * already at the start of the security filter chain, i.e. without any authentication attempt.
 *
 * The startup options are read once, at startup.
 */
@Component
public class ActuatorEndpointAccessEvaluator {

    @Autowired private MidpointConfiguration midpointConfiguration;

    private ActuatorConfigurationSection actuatorConfigurationSection;

    @PostConstruct
    public void init() {
        actuatorConfigurationSection = midpointConfiguration.getActuatorConfigurationSection();
    }

    /**
     * Returns true if the actuator endpoint (or the discovery page, see {@link ActuatorEndpointPath#DISCOVERY}) is enabled.
     */
    public boolean isEnabled(@NotNull String endpoint) {
        return isEnabled(actuatorConfigurationSection, endpoint);
    }

    static boolean isEnabled(@NotNull ActuatorConfigurationSection section, @NotNull String endpoint) {
        if (ActuatorEndpointPath.DISCOVERY.equals(endpoint)) {
            return section.isAnyEndpointEnabled();
        }
        return section.isEndpointEnabled(endpoint);
    }
}
