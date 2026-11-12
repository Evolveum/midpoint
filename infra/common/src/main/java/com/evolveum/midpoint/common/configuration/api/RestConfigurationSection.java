/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Provides typed access to the "rest" section of the {@code config.xml} file.
 */
@NullMarked
public interface RestConfigurationSection {

    /** Value of {@value MidpointConfiguration#REST_API_ENABLED_PROPERTY}; {@code false} if not set. */
    boolean apiEnabled();

    /**
     * Value of {@value MidpointConfiguration#REST_CLUSTER_ENABLED_PROPERTY}; {@code null} if not set
     * (the default then depends on whether the node is clustered).
     */
    @Nullable Boolean clusterEnabled();
}
