/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.init;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;

/**
 * Provides typed access to the "actuator" section of the config.xml file.
 *
 * @param endpointOptions Values of the per-endpoint options ({@code <endpoint>.enabled}) that are set,
 * without the {@value ActuatorConfigurationSection#ALL} option.
 * @param allOption Value of the {@code all.enabled} option; {@code null} if not set.
 */
@NullMarked
record ActuatorConfigurationSectionImpl(Map<String, Boolean> endpointOptions, @Nullable Boolean allOption)
        implements ActuatorConfigurationSection {

    private static final String ENABLED_SUFFIX = ".enabled";

    static ActuatorConfigurationSectionImpl create(Configuration configuration) {
        Map<String, Boolean> endpointOptions = new HashMap<>();
        for (Iterator<String> keys = configuration.getKeys(); keys.hasNext(); ) {
            String key = keys.next();
            String endpointId = StringUtils.removeEnd(key, ENABLED_SUFFIX);
            if (!endpointId.equals(key) && !endpointId.isEmpty() && !endpointId.contains(".")
                    && !ALL.equals(endpointId)) {
                endpointOptions.put(endpointId, configuration.getBoolean(key));
            }
        }
        return new ActuatorConfigurationSectionImpl(
                Map.copyOf(endpointOptions),
                configuration.getBoolean(ALL + ENABLED_SUFFIX, null));
    }

    @Override
    public boolean isEndpointEnabled(String endpointId) {
        Boolean option = endpointOptions.get(endpointId);
        if (option != null) {
            return option;
        }
        if (allOption != null) {
            return allOption;
        }
        return ActuatorConfigurationSection.isEnabledByDefault(endpointId);
    }

    @Override
    public boolean isAnyEndpointEnabled() {
        return Boolean.TRUE.equals(allOption)
                || endpointOptions.containsValue(true)
                // health is enabled unless switched off by its own option or by "all"
                || isEndpointEnabled("health");
    }
}
