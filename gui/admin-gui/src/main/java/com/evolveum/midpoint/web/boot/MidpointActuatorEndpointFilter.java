/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.boot;

import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Exposes only the actuator endpoints enabled by the midPoint startup options {@code midpoint.actuator.<endpoint>.enabled}
 * and {@code midpoint.actuator.all.enabled} (bug:12960), see {@link ActuatorConfigurationSection}.
 *
 * An endpoint is exposed only if it is included in {@code application.yml} and enabled by its midPoint option.
 * A disabled endpoint is not mapped at all and it is not listed on the discovery page ({@code /actuator}).
 */
public class MidpointActuatorEndpointFilter implements EndpointFilter<ExposableWebEndpoint> {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointActuatorEndpointFilter.class);

    @NotNull private final ActuatorConfigurationSection actuatorConfigurationSection;

    public MidpointActuatorEndpointFilter(@NotNull ActuatorConfigurationSection actuatorConfigurationSection) {
        this.actuatorConfigurationSection = actuatorConfigurationSection;
    }

    @Override
    public boolean match(ExposableWebEndpoint endpoint) {
        String endpointId = endpoint.getEndpointId().toLowerCaseString();
        boolean enabled = actuatorConfigurationSection.isEndpointEnabled(endpointId);
        LOGGER.debug("Actuator endpoint '{}' is {} by midPoint startup options", endpointId, enabled ? "enabled" : "disabled");
        return enabled;
    }

    /** Logs the actuator endpoints that are really exposed. */
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        event.getApplicationContext().getBeanProvider(WebEndpointsSupplier.class).ifAvailable(supplier -> {
            Set<String> exposedEndpoints = new TreeSet<>();
            for (ExposableWebEndpoint endpoint : supplier.getEndpoints()) {
                exposedEndpoints.add(endpoint.getEndpointId().toLowerCaseString());
            }
            LOGGER.info("Exposed actuator endpoints: {} .", exposedEndpoints);
        });
    }
}
