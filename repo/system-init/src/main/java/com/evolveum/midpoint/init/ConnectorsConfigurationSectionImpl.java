/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import org.apache.commons.configuration2.Configuration;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.common.configuration.api.ConnectorsConfigurationSection;

/**
 * Provides typed access to the "connectors" section of the config.xml file.
 */
@NullMarked
record ConnectorsConfigurationSectionImpl(boolean developmentToolsEnabled) implements ConnectorsConfigurationSection {

    private static final String DEVELOPMENT_TOOLS_ENABLED = "developmentToolsEnabled";

    static ConnectorsConfigurationSectionImpl create(Configuration configuration) {
        return new ConnectorsConfigurationSectionImpl(
                configuration.getBoolean(DEVELOPMENT_TOOLS_ENABLED, true));
    }
}
