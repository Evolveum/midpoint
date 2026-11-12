/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.RestConfigurationSection;

/**
 * Provides typed access to the "rest" section of the config.xml file.
 */
@NullMarked
record RestConfigurationSectionImpl(
        boolean apiEnabled,
        @Nullable Boolean clusterEnabled) implements RestConfigurationSection {

    /** Key of {@link MidpointConfiguration#REST_API_ENABLED_PROPERTY} within the section. */
    private static final String API_ENABLED = "api.enabled";

    /** Key of {@link MidpointConfiguration#REST_CLUSTER_ENABLED_PROPERTY} within the section. */
    private static final String CLUSTER_ENABLED = "cluster.enabled";

    static RestConfigurationSectionImpl create(Configuration configuration) {
        return new RestConfigurationSectionImpl(
                configuration.getBoolean(API_ENABLED, false),
                configuration.getBoolean(CLUSTER_ENABLED, null));
    }
}
