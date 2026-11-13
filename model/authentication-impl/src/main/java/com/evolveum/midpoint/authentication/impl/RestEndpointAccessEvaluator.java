/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.impl.util.RestEndpointPath;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RestConfigurationSection;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Decides whether the REST endpoints are enabled (bug:12960).
 *
 * <ul>
 * <li><b>REST API</b> ({@code /ws}, {@code /rest}, {@code /api}): the startup option
 * {@value MidpointConfiguration#REST_API_ENABLED_PROPERTY}, disabled if not set.</li>
 * <li><b>Intra-cluster endpoints</b> ({@code .../cluster}): the startup option
 * {@value MidpointConfiguration#REST_CLUSTER_ENABLED_PROPERTY} if set, otherwise enabled only on a clustered node.</li>
 * </ul>
 *
 * The startup options are read once, at startup.
 *
 * TODO Review the default for the intra-cluster endpoints (it is not confirmed yet).
 */
@Component
public class RestEndpointAccessEvaluator implements ApplicationListener<ContextRefreshedEvent> {

    private static final Trace LOGGER = TraceManager.getTrace(RestEndpointAccessEvaluator.class);

    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private TaskManager taskManager;
    @Autowired private ApplicationContext applicationContext;

    /** Value of {@link MidpointConfiguration#REST_API_ENABLED_PROPERTY}. */
    private boolean restApiEnabledProperty;

    /** Value of {@link MidpointConfiguration#REST_CLUSTER_ENABLED_PROPERTY}; {@code null} if not set. */
    private Boolean restClusterEnabledProperty;

    /**
     * Effective state of the intra-cluster endpoints. Determined lazily, because the "clustered" flag of the task manager
     * is not guaranteed to be known when this bean is initialized.
     */
    private volatile Boolean restClusterEnabled;

    @PostConstruct
    public void init() {
        RestConfigurationSection restConfigurationSection = midpointConfiguration.getRestConfigurationSection();
        restApiEnabledProperty = restConfigurationSection.apiEnabled();
        restClusterEnabledProperty = restConfigurationSection.clusterEnabled();
    }

    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
        if (event.getApplicationContext() != applicationContext) {
            return;
        }
        LOGGER.info("REST API is {} (startup option {}).",
                enabledDisabled(restApiEnabledProperty), MidpointConfiguration.REST_API_ENABLED_PROPERTY);
        boolean clusterEnabled = isRestClusterEnabled();
        if (restClusterEnabledProperty != null) {
            LOGGER.info("Intra-cluster REST endpoints are {} by the startup option {}.",
                    enabledDisabled(clusterEnabled), MidpointConfiguration.REST_CLUSTER_ENABLED_PROPERTY);
        } else {
            LOGGER.info("Intra-cluster REST endpoints are {}, because this node is {}clustered.",
                    enabledDisabled(clusterEnabled), clusterEnabled ? "" : "not ");
        }
    }

    /**
     * Returns true if the endpoint of given kind is enabled.
     * Endpoints that are not REST ones are not affected by these startup options.
     */
    public boolean isEnabled(@NotNull RestEndpointPath.Kind kind) {
        return switch (kind) {
            case NOT_REST -> true;
            case API -> restApiEnabledProperty;
            case CLUSTER -> isRestClusterEnabled();
        };
    }

    private boolean isRestClusterEnabled() {
        Boolean value = restClusterEnabled;
        if (value == null) {
            value = resolveClusterEndpointsState(restClusterEnabledProperty, taskManager.isClustered());
            restClusterEnabled = value;
        }
        return value;
    }

    /** Startup option (if set) wins; otherwise the intra-cluster endpoints are enabled only on a clustered node. */
    static boolean resolveClusterEndpointsState(@Nullable Boolean startupOption, boolean clustered) {
        return startupOption != null ? startupOption : clustered;
    }

    private String enabledDisabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
