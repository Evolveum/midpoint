/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import org.jspecify.annotations.NullMarked;

/**
 * Provides typed access to the "connectors" section of the {@code config.xml} file.
 */
@NullMarked
public interface ConnectorsConfigurationSection {

    /** @see MidpointConfiguration#isConnectorDevelopmentToolsEnabled() */
    boolean developmentToolsEnabled();
}
